/**
 * <pre>
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU AFFERO GENERAL PUBLIC LICENSE for more details. 
 * 	You should have received a copy of the GNU AFFERO GENERAL PUBLIC LICENSE along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * </pre>
 */
package com.meidusa.amoeba.route;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.LRUMap;
import org.apache.log4j.Logger;

import com.meidusa.amoeba.config.loader.ConfigModifiedAwareLoader;
import com.meidusa.amoeba.config.loader.ConfigModifiedEventHandler;
import com.meidusa.amoeba.config.loader.SqlFunctionMapLoader;
import com.meidusa.amoeba.config.loader.TableRuleLoader;
import com.meidusa.amoeba.context.ContextChangedListener;
import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.exception.ConfigurationException;
import com.meidusa.amoeba.exception.InitialisationException;
import com.meidusa.amoeba.net.Connection;
import com.meidusa.amoeba.net.poolable.ObjectPool;
import com.meidusa.amoeba.parser.AmoebaSqlHintPropNames;
import com.meidusa.amoeba.parser.ParseException;
import com.meidusa.amoeba.parser.dbobject.Column;
import com.meidusa.amoeba.parser.dbobject.Table;
import com.meidusa.amoeba.parser.function.Function;
import com.meidusa.amoeba.parser.statement.Statement;
import com.meidusa.amoeba.sqljep.function.Comparative;
import com.meidusa.amoeba.sqljep.function.ComparativeBaseList;
import com.meidusa.amoeba.util.ConcurrentHashSet;
import com.meidusa.amoeba.util.Initialisable;
import com.meidusa.amoeba.util.StringUtil;
import com.meidusa.amoeba.util.ThreadLocalMap;
import com.meidusa.amoeba.util.Tuple;

/**
 * @author struct
 */
public abstract class AbstractQueryRouter<T extends Connection, V extends Request>
    implements
      QueryRouter<T, V>,
      Initialisable,
      ContextChangedListener,
      ConfigModifiedEventHandler {
  
  public static final String _CURRENT_QUERY_OBJECT_ = "_CURRENT_QUERY_OBJECT_";
  protected static Logger logger = Logger.getLogger(AbstractQueryRouter.class);

  private ConcurrentHashMap<String, Pattern> patternMap = new ConcurrentHashMap<String, Pattern>();

  /* 默认1000 */
  private int LRUMapSize = 1000;
  private Long routerID;
  protected Map<String, LRUMap> map;

  private Map<Table, TableRule> tableRuleMap = new ConcurrentHashMap<Table, TableRule>();
  private Map<Table, TableRule> regexTableRuleMap = new ConcurrentHashMap<Table, TableRule>();
  private Map<String, Set<String>> userWithTableRule = new ConcurrentHashMap<String, Set<String>>();
  
  protected Map<String, Function> sqlFunctionMap = new ConcurrentHashMap<String, Function>();

  protected Tuple<Statement, ObjectPool[]> tuple;
  
  private SqlFunctionMapLoader sqlFuncMapLoader;
  
  // 以下为pool的名称字符串
  private String defaultPool;
  private String readPool;
  private String writePool;

  // 以下为真实的ObjectPool
  protected ObjectPool[] defaultPools;
  protected ObjectPool[] readPools;
  protected ObjectPool[] writePools;

  private boolean needParse = true;
  private TableRuleLoader ruleLoader;

  public void setSqlFuncMapLoader(SqlFunctionMapLoader sqlFuncMapLoader) {
    this.sqlFuncMapLoader = sqlFuncMapLoader;
  }

  public ObjectPool[] doRoute(T connection, V queryObject, Statement statement) throws ParseException {
    if (queryObject == null) {
      return defaultPools;
    }
    if (needParse) {
      
      // route from sql hint
      if (isSpecifiedPoolsInSqlHint(statement)) {
        String poolsHint = (String) statement.getHintParams().get(AmoebaSqlHintPropNames.POOLS_HINT);
        String[] poolNames = poolsHint.split(",");
        
        ObjectPool[] pools = new ObjectPool[poolNames.length];
        int i = 0;
        for (String name : poolNames) {
          name =  name.trim();
          ObjectPool pool = ProxyRuntimeContext.getInstance().getPoolMap().get(name);
          if (pool == null) {
            logger.error("cannot found Pool=" + name + ",sqlObject=" + queryObject);
            throw new RuntimeException("cannot found Pool=" + name + ",sqlObject=" + queryObject);
          }
          pools[i++] = pool;
        }
        
        return pools;
      }
      else {
        return selectPool(connection, queryObject, statement);
      }
    } else {
      return defaultPools;
    }
  }
  
  private boolean isSpecifiedPoolsInSqlHint(Statement statement) {
    boolean isSpecifiedPoolsInSqlHint = false;
    if (statement != null && statement.getHintParams() != null ) {
      String pools = (String) statement.getHintParams().get(AmoebaSqlHintPropNames.POOLS_HINT);
      if (!StringUtil.isEmpty(pools)) {
        isSpecifiedPoolsInSqlHint = true;
      }
    }
    
    return isSpecifiedPoolsInSqlHint;
  }

  protected abstract Map<Table, Map<Column, Comparative>> evaluateTable(T connection, V queryObject, Statement statement);

  /**
   * 返回Query 被route到目标地址 ObjectPool集合 如果返回null，则是属于DatabaseConnection 自身属性设置的请求。
   * 
   * @throws ParseException
   */

  protected void beforeSelectPool(T connection, V queryObject, Statement statement) {
    ThreadLocalMap.put(_CURRENT_QUERY_OBJECT_, queryObject);
  }

  protected List<String> evaluate(StringBuffer loggerBuffer, T connection, V queryObject, Statement statement) {
    boolean isRead = true;
    boolean isPrepared = false;

    isRead = queryObject.isRead();
    isPrepared = queryObject.isPrepared();

    List<String> poolNames = new ArrayList<String>();
    Map<Table, Map<Column, Comparative>> tables = evaluateTable(connection, queryObject, statement);

    if (tables != null && tables.size() > 0) {
      Set<Map.Entry<Table, Map<Column, Comparative>>> entrySet = tables.entrySet();
      for (Map.Entry<Table, Map<Column, Comparative>> entry : entrySet) {
        boolean regexMatched = false;
        Map<Column, Comparative> columnMap = entry.getValue();
        
        TableRule tableRule = this.tableRuleMap.get(entry.getKey());
        
        // admin对于单表的规则，是取不到值的，如需支持非*的表规则，这里要重新取值
        if(connection.isAdmin() && tableRule == null) {
          Set<Table> keySet = this.tableRuleMap.keySet();
          Table tableFromClient = entry.getKey();
          for(Table table : keySet) {
            if (table.equalsForAdminUser(tableFromClient)) {
              tableRule = this.tableRuleMap.get(table);
              break;
            }
          }
        }

        // sql语句中包含的表
        Table table = entry.getKey();

        // 强制指定schema
        if (tableRule == null && !StringUtil.isEmpty(table.getName()) && table.getSchema() != null
            && !StringUtil.isEmpty(table.getSchema().getName())) {

          /**
           * foreach regex table rule
           */
          for (Map.Entry<Table, TableRule> ruleEntry : this.regexTableRuleMap.entrySet()) {
            // 表规则中包含的表
            Table ruleTable = ruleEntry.getKey();

            boolean tableMatched = false;
            boolean schemaMatched = false;
            boolean userIdMatched = false;

            /**
             * check table name matched or not.
             */
            Pattern pattern = this.getPattern(ruleTable.getName());
            java.util.regex.Matcher matcher = pattern.matcher(table.getName());
            if (matcher.find()) {
              tableMatched = true;
            }

            /**
             * check table schema matched or not.
             */
            if (table.getSchema().equals(ruleTable.getSchema())) {
              schemaMatched = true;
            }

            // check userid matched or not.
            if (connection.isAdmin()) {
              userIdMatched = true;
            } else {
              if (ruleTable.getUserName().equals(table.getUserName())) {
                userIdMatched = true;
              }
            }

            if (tableMatched && schemaMatched && userIdMatched) {
              tableRule = ruleEntry.getValue();
              regexMatched = true;
              break;
            }
          }
        }

        // 如果存在table Rule 则需要看是否有Rule
        if (tableRule != null) {
          // 没有列的sql语句，使用默认的tableRule
          if (columnMap == null || isPrepared) {
            String[] pools = (isRead ? tableRule.readPools : tableRule.writePools);
            if (pools == null || pools.length == 0) {
              pools = tableRule.defaultPools;
            }

            for (String poolName : pools) {
              if (!poolNames.contains(poolName)) {
                poolNames.add(poolName);
              }
            }

            if (!isPrepared) {
              if (logger.isDebugEnabled()) {
                loggerBuffer.append(", no Column rule, using table:" + tableRule.table
                    + " default rules:" + Arrays.toString(tableRule.defaultPools));
              }
            }
            continue;
          }

          List<String> groupMatched = new ArrayList<String>();
          if (tableRule.ruleList != null && tableRule.ruleList.size() > 0) {
            for (Rule rule : tableRule.ruleList) {
              if (rule.group != null) {
                if (groupMatched.contains(rule.group)) {
                  continue;
                }
              }

              boolean matched = true;
              // 如果查询语句中包含了该规则不需要的参数，则该规则将被忽略
              for (Column exclude : rule.excludes) {
                Comparable<?> condition = columnMap.get(exclude);
                if (condition != null) {
                  matched = false;
                  break;
                }
              }

              // 如果不匹配将继续下一条规则
              if (!matched) {
                continue;
              }

              Comparable<?>[] comparables = new Comparable[rule.parameterMap.size()];
              // 规则中参数如果在dmlstatement中，则为条件赋值，如果不存在，那么赋空
              for (Map.Entry<Column, Integer> parameter : rule.cloumnMap.entrySet()) {
                Comparative condition = null;
                if (regexMatched) {
                  Column column = new Column();
                  column.setName(parameter.getKey().getName());
                  column.setTable(table);
                  condition = columnMap.get(column);
                } else {
                  condition = columnMap.get(parameter.getKey());
                }

                if (condition != null) {
                  // 如果规则忽略 数组的 参数，并且参数有array 参数，则忽略该规则
                  if (rule.ignoreArray && condition instanceof ComparativeBaseList) {
                    matched = false;
                    break;
                  }

                  comparables[parameter.getValue()] = (Comparative) condition.clone();
                }
              }

              // 如果不匹配将继续下一条规则
              if (!matched) {
                continue;
              }

              try {
                Comparable<?> result = rule.rowJep.getValue(comparables);
                Integer i = 0;
                if (result instanceof Comparative) {
                  if (rule.result == RuleResult.INDEX) {
                    i = (Integer) ((Comparative) result).getValue();
                    if (i < 0) {
                      continue;
                    }
                    matched = true;
                  } else if (rule.result == RuleResult.POOLNAME) {
                    String matchedPoolsString = ((Comparative) result).getValue().toString();
                    String[] poolNamesMatched = matchedPoolsString.split(",");

                    if (poolNamesMatched != null && poolNamesMatched.length > 0) {
                      for (String poolName : poolNamesMatched) {
                        if (!poolNames.contains(poolName)) {
                          poolNames.add(poolName);
                        }
                      }

                      if (logger.isDebugEnabled()) {
                        loggerBuffer.append(", matched table:" + tableRule.table + ", rule:"
                            + rule.name);
                      }
                    }
                    continue;
                  } else {
                    matched = (Boolean) ((Comparative) result).getValue();
                  }
                } else {

                  if (rule.result == RuleResult.INDEX) {
                    i = (Integer) Integer.valueOf(result.toString());
                    if (i < 0) {
                      continue;
                    }
                    matched = true;
                  } else if (rule.result == RuleResult.POOLNAME) {
                    String matchedPoolsString = result.toString();
                    String[] poolNamesMatched = StringUtil.split(matchedPoolsString, ";,");
                    if (poolNamesMatched != null && poolNamesMatched.length > 0) {
                      for (String poolName : poolNamesMatched) {
                        if (!poolNames.contains(poolName)) {
                          poolNames.add(poolName);
                        }
                      }

                      if (logger.isDebugEnabled()) {
                        loggerBuffer.append(", matched table:" + tableRule.table + ", rule:"
                            + rule.name);
                      }
                    }
                    continue;
                  } else {
                    matched = (Boolean) result;
                  }
                }

                if (matched) {
                  if (rule.group != null) {
                    groupMatched.add(rule.group);
                  }
                  String[] pools = (isRead ? rule.readPools : rule.writePools);
                  if (pools == null || pools.length == 0) {
                    pools = rule.defaultPools;
                  }
                  if (pools != null && pools.length > 0) {
                    if (rule.isSwitch) {
                      if (!poolNames.contains(pools[i])) {
                        poolNames.add(pools[i]);
                      }
                    } else {
                      for (String poolName : pools) {
                        if (!poolNames.contains(poolName)) {
                          poolNames.add(poolName);
                        }
                      }
                    }
                  } else {
                    logger.error("rule:" + rule.name + " matched, but pools is null");
                  }

                  if (logger.isDebugEnabled()) {
                    loggerBuffer.append(", matched table:" + tableRule.table + ", rule:"
                        + rule.name);
                  }
                }
              } catch (com.meidusa.amoeba.sqljep.ParseException e) {
                logger.error("parse rule error:" + rule.expression);
              }
            
            }
          }

          // 如果所有规则都无法匹配，则默认采用TableRule中的pool设置。
          if (poolNames.size() == 0) {
            String[] pools = (isRead ? tableRule.readPools : tableRule.writePools);
            if (pools == null || pools.length == 0) {
              pools = tableRule.defaultPools;
            }

            if (!isPrepared) {
              if (tableRule.ruleList != null && tableRule.ruleList.size() > 0) {
                if (logger.isDebugEnabled()) {
                  loggerBuffer.append(", no rule matched, using tableRule:[" + tableRule.table
                      + "] defaultPools");
                }
              } else {
                if (logger.isDebugEnabled()) {
                  if (pools != null) {
                    StringBuffer buffer = new StringBuffer();
                    for (String pool : pools) {
                      buffer.append(pool).append(",");
                    }
                    loggerBuffer.append(", using tableRule:[" + tableRule.table + "] defaultPools="
                        + buffer.toString());
                  }
                }
              }
            }
            for (String poolName : pools) {
              if (!poolNames.contains(poolName)) {
                poolNames.add(poolName);
              }
            }
          }
        }
      }
    }
    return poolNames;
  }

  public ObjectPool[] selectPool(T connection, V queryObject, Statement statement) {
    beforeSelectPool(connection, queryObject, statement);

    StringBuffer loggerBuffer = null;

    boolean isRead = true;

    isRead = queryObject.isRead();

    if (logger.isDebugEnabled()) {
      loggerBuffer = new StringBuffer("query=");
      loggerBuffer.append(queryObject);

      loggerBuffer.append(queryObject.isPrepared() ? ",prepared=true" : "");
    }
    List<String> poolNames = new ArrayList<String>();
    poolNames = evaluate(loggerBuffer, connection, queryObject, statement);
    ObjectPool[] pools = new ObjectPool[poolNames.size()];
    int i = 0;
    for (String name : poolNames) {
      ObjectPool pool = ProxyRuntimeContext.getInstance().getPoolMap().get(name);
      if (pool == null) {
        logger.error("cannot found Pool=" + name + ",sqlObject=" + queryObject);
        throw new RuntimeException("cannot found Pool=" + name + ",sqlObject=" + queryObject);
      }
      pools[i++] = pool;
    }

    if (pools == null || pools.length == 0) {
      // 如果不是use语句才有可能被转到default pool，use 语句就不能再有这样的尝试，该是null还是null
      if (!queryObject.isUseStatement()) {
        pools = (isRead ? this.readPools : this.writePools);
        if (logger.isDebugEnabled() && pools != null && pools.length > 0) {
          if (isRead) {
            loggerBuffer.append(",  route to queryRouter readPool:" + readPool + "\r\n");
          } else {
            loggerBuffer.append(",  route to queryRouter writePool:" + writePool + "\r\n");
          }
        }

        if (pools == null || pools.length == 0) {
          pools = this.defaultPools;
          if (logger.isDebugEnabled() && pools != null && pools.length > 0) {
            loggerBuffer.append(",  route to queryRouter defaultPool:" + defaultPool + "\r\n");
          }
        }
      }
    } else {
      if (logger.isDebugEnabled() && pools != null && pools.length > 0) {
        loggerBuffer.append(",  route to pools:" + poolNames + "\r\n");
      }
    }


    if (logger.isDebugEnabled()) {
      if (loggerBuffer != null) {
        logger.debug(loggerBuffer.toString());
      }
    }
    return pools;
  }

  public void doChange() {
    defaultPools =
        new ObjectPool[] {ProxyRuntimeContext.getInstance().getPoolMap().get(defaultPool)};

    if (readPool != null && !StringUtil.isEmpty(readPool)) {
      readPools = new ObjectPool[] {ProxyRuntimeContext.getInstance().getPoolMap().get(readPool)};
    }
    if (writePool != null && !StringUtil.isEmpty(writePool)) {
      writePools = new ObjectPool[] {ProxyRuntimeContext.getInstance().getPoolMap().get(writePool)};
    }
  }

  public void init() throws InitialisationException {
    if (defaultPool == null || defaultPool.isEmpty()) {
      throw new InitialisationException("default pool required!");
    }
    defaultPools =
        new ObjectPool[] {ProxyRuntimeContext.getInstance().getPoolMap().get(defaultPool)};

    if (defaultPools == null || defaultPools[0] == null) {
      throw new InitialisationException("default pool required!,defaultPool=" + defaultPool
          + " invalid");
    }
    if (readPool != null && !StringUtil.isEmpty(readPool)) {
      ObjectPool pool = ProxyRuntimeContext.getInstance().getPoolMap().get(readPool);
      if (pool == null) {
        logger.error("cannot found Pool=" + readPool);
        throw new InitialisationException("cannot found Pool=" + readPool);
      }
      readPools = new ObjectPool[] {pool};
    }
    if (writePool != null && !StringUtil.isEmpty(writePool)) {
      ObjectPool pool = ProxyRuntimeContext.getInstance().getPoolMap().get(writePool);
      if (pool == null) {
        logger.error("cannot found Pool=" + writePool);
        throw new InitialisationException("cannot found Pool=" + writePool);
      }
      writePools = new ObjectPool[] {pool};
    }

    map = new LRUMap(LRUMapSize);
    
    if (needParse) {
      
      sqlFuncMapLoader.loadFunctionMap(this.sqlFunctionMap);

      // 如果配置了ruleLoader
      if (ruleLoader != null) {
        this.tableRuleMap = ruleLoader.loadRule();

        if (tableRuleMap != null) {
          for (Map.Entry<Table, TableRule> ruleEntry : this.tableRuleMap.entrySet()) {
            Table table = ruleEntry.getKey();
            String tableName = table.getName();
            String schemaName = table.getSchema().getName();
            String userName = table.getUserName();
            
            storeUserSchemaMap(userName, schemaName, this.userWithTableRule);

            if (tableName.indexOf("*") >= 0 || (schemaName != null && schemaName.indexOf("*") >= 0)
                || tableName.indexOf("^") >= 0
                || (schemaName != null && schemaName.indexOf("^") >= 0)) {
              this.getPattern(tableName);
              this.regexTableRuleMap.put(table, ruleEntry.getValue());
            }
          }
        }
      }
    }
    
    if (ruleLoader instanceof ConfigModifiedAwareLoader) {
      ((ConfigModifiedAwareLoader) ruleLoader).setConfigModifiedEventHandler(this);
      ((ConfigModifiedAwareLoader) ruleLoader).startObserve();
    }
    
  }
  
  public int getLRUMapSize() {
    return LRUMapSize;
  }

  public String getDefaultPool() {
    return defaultPool;
  }

  public void setDefaultPool(String defaultPoolName) {
    this.defaultPool = defaultPoolName;
  }

  public void setLRUMapSize(int mapSize) {
    LRUMapSize = mapSize;
  }

  public boolean isNeedParse() {
    return needParse;
  }

  public void setNeedParse(boolean needParse) {
    this.needParse = needParse;
  }

  public ObjectPool getObjectPool(Object key) {
    if (key instanceof String) {
      return ProxyRuntimeContext.getInstance().getPoolMap().get(key);
    } else {
      for (ObjectPool pool : ProxyRuntimeContext.getInstance().getPoolMap().values()) {
        if (pool.hashCode() == key.hashCode()) {
          return pool;
        }
      }
    }
    return null;
  }

  public ObjectPool[] getDefaultObjectPool() {
    return this.defaultPools;
  }

  private Pattern getPattern(String source) {

    Pattern pattern = null;

    if (!StringUtil.isEmpty(source)) {
      if (source.indexOf("*") == 0) {
        source = "^" + source;
      }

      pattern = this.patternMap.putIfAbsent(source, Pattern.compile(source));
    }

    return pattern;
  }

  public TableRuleLoader getRuleLoader() {
    return ruleLoader;
  }

  public void setRuleLoader(TableRuleLoader ruleLoader) {
    this.ruleLoader = ruleLoader;
  }

  public AbstractQueryRouter() {}

  public void setReadPool(String readPool) {
    this.readPool = readPool;
  }

  public String getReadPool() {
    return readPool;
  }

  public String getWritePool() {
    return writePool;
  }

  public void setWritePool(String writePool) {
    this.writePool = writePool;
  }

  public Long getRouterID() {
    return routerID;
  }

  public void setRouterID(Long routerID) {
    this.routerID = routerID;
  }
  
  public boolean isExistedTableRuleForUser(String username) {
    if (userWithTableRule.containsKey(username)) {
      Set<String> schemasUserCanAccess = userWithTableRule.get(username);
      if (schemasUserCanAccess.size() > 0) {
        return true;
      }
    }
    return false;
  }
  
  public boolean isSchemaExistedForUser(String userName, String schemaName) {
    if (userWithTableRule.containsKey(userName)) {
      Set<String> schemasUserCanAccess = userWithTableRule.get(userName);
      if (schemasUserCanAccess.contains(schemaName)) {
        return true;
      }
    }
    
    return false;
  }
  
  private void storeUserSchemaMap(String userName, String schemaName, Map<String, Set<String>> userWithTableRule) {
    if (!userWithTableRule.containsKey(userName)) {
      Set<String> schemaNames = new ConcurrentHashSet<String>();
      schemaNames.add(schemaName);
      userWithTableRule.put(userName, schemaNames);
    }
    else {
      Set<String> schemaNames = userWithTableRule.get(userName);
      schemaNames.add(schemaName);
    }
  }

  @Override
  public void doOnConfigModified() {
    try {
      Map<String, Function> funMap = null;
      Map<Table, TableRule> tableRuleMap = null;
      Map<String, Set<String>> userWithTableRule = null;
      Map<Table, TableRule> regexTableRuleMap = null;
      
      if (sqlFuncMapLoader.needLoad()) {
        funMap = new ConcurrentHashMap<String, Function>();
        sqlFuncMapLoader.loadFunctionMap(funMap);
      }
      
      tableRuleMap = ruleLoader.loadRule();

      if (tableRuleMap != null) {
        userWithTableRule = new ConcurrentHashMap<String, Set<String>>();
        regexTableRuleMap = new ConcurrentHashMap<Table, TableRule>();

        for (Map.Entry<Table, TableRule> ruleEntry : tableRuleMap.entrySet()) {
          Table table = ruleEntry.getKey();
          String tableName = table.getName();
          String schemaName = table.getSchema().getName();
          String userName = table.getUserName();

          storeUserSchemaMap(userName, schemaName, userWithTableRule);

          if (tableName.indexOf("*") >= 0 || (schemaName != null && schemaName.indexOf("*") >= 0)
              || tableName.indexOf("^") >= 0
              || (schemaName != null && schemaName.indexOf("^") >= 0)) {
            getPattern(tableName);
            regexTableRuleMap.put(table, ruleEntry.getValue());
          }
        }
      }

      if (funMap != null) {
        this.sqlFunctionMap = funMap;
      }

      if (tableRuleMap != null) {
        this.tableRuleMap = tableRuleMap;

        if (regexTableRuleMap != null) {
          this.regexTableRuleMap = regexTableRuleMap;
        }

        if (userWithTableRule != null) {
          this.userWithTableRule = userWithTableRule;
        }
      }
    } catch (ConfigurationException e) {}
  }

  public static void main(String[] aa) {
    String[] aaa = StringUtil.split("asdfasdf,asdf;aqwer", ";,");
    for (String aaaaa : aaa) {
      System.out.println(aaaaa);
    }
    System.out.println(System.currentTimeMillis());
    String source = "^fileSys_[a-zA-Z0-9_]*";
    Pattern pattern = null;
    if (pattern == null) {
      pattern = Pattern.compile(source);
    }
    java.util.regex.Matcher matcher = pattern.matcher("fileSys_abc12d");
    System.out.println(matcher.matches());
  }
}
