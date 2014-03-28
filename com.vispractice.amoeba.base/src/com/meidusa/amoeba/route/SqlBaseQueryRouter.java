package com.meidusa.amoeba.route;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections.map.LRUMap;
import org.apache.log4j.Logger;

import com.meidusa.amoeba.net.DatabaseConnection;
import com.meidusa.amoeba.parser.Parser;
import com.meidusa.amoeba.parser.dbobject.Column;
import com.meidusa.amoeba.parser.dbobject.Schema;
import com.meidusa.amoeba.parser.dbobject.Table;
import com.meidusa.amoeba.parser.function.LastInsertId;
import com.meidusa.amoeba.parser.statement.AbstractStatement;
import com.meidusa.amoeba.parser.statement.DMLStatement;
import com.meidusa.amoeba.parser.statement.SelectStatement;
import com.meidusa.amoeba.parser.statement.ShowStatement;
import com.meidusa.amoeba.parser.statement.Statement;
import com.meidusa.amoeba.sqljep.function.Comparative;
import com.meidusa.amoeba.util.StringUtil;
import com.meidusa.amoeba.util.ThreadLocalMap;

public abstract class SqlBaseQueryRouter extends AbstractQueryRouter<DatabaseConnection,SqlQueryObject> {

    private static String DIAGONAL = new String(new char[] {(char) 0x5c, (char) 0x5c});
    private static String DOT = new String(new char[] {(char) 0x5c, (char) 0x27});

    private boolean replaceEscapeSymbol = true;
    private Lock mapLock = new ReentrantLock(false);
    
    private static Logger logger = Logger.getLogger(SqlBaseQueryRouter.class);
    private final static Set<String> SQL_READ_STATEMENT_HEAD = new HashSet<String>();

    static {
      SQL_READ_STATEMENT_HEAD.add("SHOW");
      SQL_READ_STATEMENT_HEAD.add("EXPLAIN");
      SQL_READ_STATEMENT_HEAD.add("DESCRIBE");
      SQL_READ_STATEMENT_HEAD.add("HELP");
      SQL_READ_STATEMENT_HEAD.add("SELECT");
    }
    
    public boolean isReplaceEscapeSymbol() {
		return replaceEscapeSymbol;
	}

	public void setReplaceEscapeSymbol(boolean replaceEscapeSymbol) {
		this.replaceEscapeSymbol = replaceEscapeSymbol;
	}

	protected void beforeSelectPool(DatabaseConnection connection, SqlQueryObject queryObject, Statement statement){
		if (statement != null) {
	      queryObject.isRead = statement.isRead();
        }
		// 解析失败后，仍可以通过首个单词判断是读语句还是写语句
		else {
		   String sql = queryObject.sql;
		   if (!StringUtil.isEmpty(sql)) {
            String head = StringUtil.head(sql);
            if (!StringUtil.isEmpty(head)) {
              head = head.toUpperCase();
              if (SQL_READ_STATEMENT_HEAD.contains(head)) {
                queryObject.isRead = true;
              }
            }
          }
		}
    	 
		ThreadLocalMap.put(_CURRENT_QUERY_OBJECT_, queryObject);
    }

	@Override
	protected Map<Table, Map<Column, Comparative>> evaluateTable(DatabaseConnection connection,SqlQueryObject queryObject, Statement statement) {
		Map<Table, Map<Column, Comparative>> tables = null;
		
		/**
		 * 先尝试从statement中取
		 */
		if(statement instanceof DMLStatement){
			tables = ((DMLStatement)statement).evaluate(queryObject.parameters);
		}
		
		else if (statement instanceof ShowStatement) {
        	tables = new HashMap<Table, Map<Column, Comparative>>();
        	
            if (logger.isDebugEnabled()) {
                logger.debug("ShowStatment:[" + queryObject.sql + "]");
            }
            AbstractStatement ast = (ShowStatement)statement;
        	
            if(ast.getTables() != null){
	            for(Table table:ast.getTables()){
	            	tables.put(table, null);
	            }
            }else{
            	tables.put(null, null);
            }
        }
		
		/**
		 *  考虑语句解析失败或语句不带任何schema或table信息时，为了还能正确路由，
		 *  默认从connection中获取schema信息，走 table=* 规则
		 *  如果语句正常解析，实在无法从语句提前schema时，默认也是从connection中获取schema信息
		 */
   	 	if ( tables == null || tables.size() == 0 || tables.get(null) != null ) {
   	 	  if ( !StringUtil.isEmpty(connection.getSchema()) ) {
     	 	Map<Column, Comparative> condition = null;  
            if (tables != null && tables.size() > 0 ) {
              condition = tables.get(null);
            }
            tables = new HashMap<Table, Map<Column,Comparative>>();
            
            Table table = new Table();
            table.setName("*");
            Schema schema = new Schema();
            schema.setName(connection.getSchema());
            table.setSchema(schema);
            table.setUserName(connection.getUser());
            tables.put(table, condition);
          }
		}
   	 	
		return tables;
	}
	
	
	
	protected String amoebaRouterSql(String sql){
		sql = sql.trim();
		int sIndex = sql.indexOf("@amoeba");
		if(sIndex >0){
			String subSql = sql.substring(sIndex);
			int lIndex = subSql.indexOf("*/");
			if(lIndex>0 ){
				subSql = subSql.substring(0,lIndex);
				int pIndex = subSql.lastIndexOf("]");
				
				lIndex = subSql.lastIndexOf(")");
				if(pIndex < lIndex){
					subSql = subSql.substring(0,lIndex+1);
				}else{
					subSql = subSql.substring(0,pIndex+1) +" " + sql ; 
				}
				sql = subSql;
			}
		}
		
		sql = sql.trim();
			
		
		if(replaceEscapeSymbol){
			sql = StringUtil.replace(sql,DIAGONAL,"");
			sql = StringUtil.replace(sql,DOT,"");
		}
		return sql;
	}
	
	public Statement parseStatement(DatabaseConnection connection, String sql) {
		if(sql == null) return null;
        Statement statment = null;
        String defaultSchema = (connection == null || StringUtil.isEmpty(connection.getSchema())) ? null : connection.getSchema();

        long sqlKey = ((long) sql.length() << 32) | (long) (defaultSchema != null ? (defaultSchema.hashCode() ^ sql.hashCode()) : sql.hashCode());
        mapLock.lock();
        String username = connection.getUser();

        try {
            LRUMap cachedStatementsForUser = map.get(username);
            if (cachedStatementsForUser != null) {
              statment = (Statement)cachedStatementsForUser.get(sql);
            }
        } finally {
            mapLock.unlock();
        }
        if (statment == null) {
            synchronized (sql) {
                statment = (Statement) map.get(sqlKey);
                if (statment != null) {
                    return statment;
                }

                Parser parser = newParser(amoebaRouterSql(sql));
                parser.setFunctionMap(this.sqlFunctionMap);
                if (defaultSchema != null) {
                    Schema schema = new Schema();
                    schema.setName(defaultSchema);
                    parser.setDefaultSchema(schema);
                }
                
                if (connection.getUser() != null) {
                  parser.setUserName(connection.getUser());
                }

                try {
                    statment = parser.doParse();
                    if(statment instanceof SelectStatement){
                    	SelectStatement st = (SelectStatement)statment;
                    	if(st.getTables() == null || st.getTables().length == 0){
                    		Boolean queryInsertId = (Boolean)ThreadLocalMap.get(LastInsertId.class.getName());
                    		if(queryInsertId != null && queryInsertId.booleanValue()){
                    			st.setQueryLastInsertId(true);
                    		}
                    	}
                    }
                    mapLock.lock();
                    boolean isContainGlobalSeq = false;
                    
                    if(statment instanceof DMLStatement){
                      DMLStatement dml = (DMLStatement)statment;
                    	dml.setSql(sql);
                    	if (dml.getSeqColumns().size() > 0 || dml.getBatchFetchFuncCalls().size() > 0) {
                          isContainGlobalSeq = true;
                        }
                    }
                    try {
                        // 含有全局序列的，不能缓存语句，需要在解析时替换语句中的全局序列
                        if (!isContainGlobalSeq) {
                          LRUMap cachedStatementsForUser = map.get(username);
                          if (cachedStatementsForUser == null) {
                            cachedStatementsForUser = new LRUMap();
                          }
                          cachedStatementsForUser.put(sql, statment);
                          map.put(username, cachedStatementsForUser);
                        }
                    } finally {
                        mapLock.unlock();
                    }
                } catch (Error e) {
                    logger.warn("Can not parse the statement " + sql);
                    return null;
                }catch(Exception e){
                	logger.warn("Can not parse the statement " + sql);
                    return null;
                }
               
            }
        }
        return statment;
    }

	public int parseParameterCount(DatabaseConnection connection, String sql) {
		Statement statment = parseStatement(connection, sql);
		if (statment != null) {
			return statment.getParameterCount();
		} else {
			return 0;
		}
	}
	 
	public abstract Parser newParser(String sql);

}
