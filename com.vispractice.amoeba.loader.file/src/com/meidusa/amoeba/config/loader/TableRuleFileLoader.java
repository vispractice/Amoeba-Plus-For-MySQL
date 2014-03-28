package com.meidusa.amoeba.config.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.apache.log4j.helpers.LogLog;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.meidusa.amoeba.config.loader.util.ConfigLoaderUtil;
import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.exception.ConfigurationException;
import com.meidusa.amoeba.exception.InitialisationException;
import com.meidusa.amoeba.parser.dbobject.Column;
import com.meidusa.amoeba.parser.dbobject.Schema;
import com.meidusa.amoeba.parser.dbobject.Table;
import com.meidusa.amoeba.route.AbstractQueryRouter;
import com.meidusa.amoeba.route.Request;
import com.meidusa.amoeba.route.Rule;
import com.meidusa.amoeba.route.RuleResult;
import com.meidusa.amoeba.route.TableRule;
import com.meidusa.amoeba.sqljep.RowJEP;
import com.meidusa.amoeba.sqljep.function.Abs;
import com.meidusa.amoeba.sqljep.function.AddDate;
import com.meidusa.amoeba.sqljep.function.AddMonths;
import com.meidusa.amoeba.sqljep.function.AddTime;
import com.meidusa.amoeba.sqljep.function.Ceil;
import com.meidusa.amoeba.sqljep.function.Concat;
import com.meidusa.amoeba.sqljep.function.Datediff;
import com.meidusa.amoeba.sqljep.function.Day;
import com.meidusa.amoeba.sqljep.function.DayName;
import com.meidusa.amoeba.sqljep.function.DayOfWeek;
import com.meidusa.amoeba.sqljep.function.DayOfYear;
import com.meidusa.amoeba.sqljep.function.Decode;
import com.meidusa.amoeba.sqljep.function.Floor;
import com.meidusa.amoeba.sqljep.function.Hash;
import com.meidusa.amoeba.sqljep.function.Hour;
import com.meidusa.amoeba.sqljep.function.IndistinctMatching;
import com.meidusa.amoeba.sqljep.function.Initcap;
import com.meidusa.amoeba.sqljep.function.Instr;
import com.meidusa.amoeba.sqljep.function.LastDay;
import com.meidusa.amoeba.sqljep.function.Length;
import com.meidusa.amoeba.sqljep.function.Lower;
import com.meidusa.amoeba.sqljep.function.Lpad;
import com.meidusa.amoeba.sqljep.function.Ltrim;
import com.meidusa.amoeba.sqljep.function.MakeDate;
import com.meidusa.amoeba.sqljep.function.MakeTime;
import com.meidusa.amoeba.sqljep.function.Microsecond;
import com.meidusa.amoeba.sqljep.function.Minute;
import com.meidusa.amoeba.sqljep.function.Modulus;
import com.meidusa.amoeba.sqljep.function.Month;
import com.meidusa.amoeba.sqljep.function.MonthName;
import com.meidusa.amoeba.sqljep.function.MonthsBetween;
import com.meidusa.amoeba.sqljep.function.NextDay;
import com.meidusa.amoeba.sqljep.function.Nvl;
import com.meidusa.amoeba.sqljep.function.PostfixCommand;
import com.meidusa.amoeba.sqljep.function.Power;
import com.meidusa.amoeba.sqljep.function.Range;
import com.meidusa.amoeba.sqljep.function.Replace;
import com.meidusa.amoeba.sqljep.function.Round;
import com.meidusa.amoeba.sqljep.function.Rpad;
import com.meidusa.amoeba.sqljep.function.Rtrim;
import com.meidusa.amoeba.sqljep.function.Second;
import com.meidusa.amoeba.sqljep.function.Sign;
import com.meidusa.amoeba.sqljep.function.SubDate;
import com.meidusa.amoeba.sqljep.function.SubTime;
import com.meidusa.amoeba.sqljep.function.Substring;
import com.meidusa.amoeba.sqljep.function.ToChar;
import com.meidusa.amoeba.sqljep.function.ToDate;
import com.meidusa.amoeba.sqljep.function.ToLong;
import com.meidusa.amoeba.sqljep.function.ToNumber;
import com.meidusa.amoeba.sqljep.function.Translate;
import com.meidusa.amoeba.sqljep.function.Trim;
import com.meidusa.amoeba.sqljep.function.Trunc;
import com.meidusa.amoeba.sqljep.function.Upper;
import com.meidusa.amoeba.sqljep.function.WeekOfYear;
import com.meidusa.amoeba.sqljep.function.Year;
import com.meidusa.amoeba.sqljep.variable.Variable;
import com.meidusa.amoeba.util.Initialisable;
import com.meidusa.amoeba.util.StringUtil;
import com.meidusa.amoeba.util.ThreadLocalMap;

public class TableRuleFileLoader
    implements
      TableRuleLoader,
      Initialisable,
      ConfigModifiedAwareLoader {
  
  protected static Logger logger = Logger.getLogger(TableRuleFileLoader.class);
  private Map<String, PostfixCommand> ruleFunctionMap = new HashMap<String, PostfixCommand>();
  public final static Map<String, PostfixCommand> ruleFunTab =
      new HashMap<String, PostfixCommand>();

  static {
    ruleFunTab.put("abs", new Abs());
    ruleFunTab.put("power", new Power());
    ruleFunTab.put("mod", new Modulus());
    ruleFunTab.put("substr", new Substring());
    ruleFunTab.put("sign", new Sign());
    ruleFunTab.put("ceil", new Ceil());
    ruleFunTab.put("floor", new Floor());
    ruleFunTab.put("trunc", new Trunc());
    ruleFunTab.put("round", new Round());
    ruleFunTab.put("length", new Length());
    ruleFunTab.put("concat", new Concat());
    ruleFunTab.put("instr", new Instr());
    ruleFunTab.put("trim", new Trim());
    ruleFunTab.put("rtrim", new Rtrim());
    ruleFunTab.put("ltrim", new Ltrim());
    ruleFunTab.put("rpad", new Rpad());
    ruleFunTab.put("lpad", new Lpad());
    ruleFunTab.put("lower", new Lower());
    ruleFunTab.put("upper", new Upper());
    ruleFunTab.put("translate", new Translate());
    ruleFunTab.put("replace", new Replace());
    ruleFunTab.put("initcap", new Initcap());
    ruleFunTab.put("value", new Nvl());
    ruleFunTab.put("decode", new Decode());
    ruleFunTab.put("to_char", new ToChar());
    ruleFunTab.put("to_number", new ToNumber());
    ruleFunTab.put("long", new ToLong());
    ruleFunTab.put("to_long", new ToLong());
    ruleFunTab.put("imatch", new IndistinctMatching()); // replacement for of Oracle's SOUNDEX
    ruleFunTab.put("months_between", new MonthsBetween());
    ruleFunTab.put("add_months", new AddMonths());
    ruleFunTab.put("last_day", new LastDay());
    ruleFunTab.put("next_day", new NextDay());
    ruleFunTab.put("to_date", new ToDate());
    // ruleFunTab.put("case", new Case()); // replacement for CASE WHEN digit = 0 THEN ...;WHEN
    // digit = 1
    // THEN...;ELSE... END CASE
    ruleFunTab.put("index", new Instr()); // maxdb
    ruleFunTab.put("num", new ToNumber()); // maxdb
    ruleFunTab.put("chr", new ToChar()); // maxdb
    ruleFunTab.put("dayname", new DayName()); // maxdb
    ruleFunTab.put("adddate", new AddDate()); // maxdb
    ruleFunTab.put("subdate", new SubDate()); // maxdb
    ruleFunTab.put("addtime", new AddTime()); // maxdb
    ruleFunTab.put("subtime", new SubTime()); // maxdb
    ruleFunTab.put("year", new Year()); // maxdb
    ruleFunTab.put("month", new Month()); // maxdb
    ruleFunTab.put("day", new Day()); // maxdb
    ruleFunTab.put("dayofmonth", new Day()); // maxdb
    ruleFunTab.put("hour", new Hour()); // maxdb
    ruleFunTab.put("minute", new Minute()); // maxdb
    ruleFunTab.put("second", new Second()); // maxdb
    ruleFunTab.put("microsecond", new Microsecond()); // maxdb
    ruleFunTab.put("datediff", new Datediff()); // maxdb
    ruleFunTab.put("dayofweek", new DayOfWeek()); // maxdb
    ruleFunTab.put("weekofyear", new WeekOfYear()); // maxdb
    ruleFunTab.put("dayofyear", new DayOfYear()); // maxdb
    ruleFunTab.put("dayname", new DayName()); // maxdb
    ruleFunTab.put("monthname", new MonthName()); // maxdb
    ruleFunTab.put("makedate", new MakeDate()); // maxdb
    ruleFunTab.put("maketime", new MakeTime()); // maxdb
    ruleFunTab.put("hash", new Hash()); //
    ruleFunTab.put("range", new Range()); //
  }

  Map<String, Variable> variableMap = new HashMap<String, Variable>();
  {
    variableMap.put("isReadStatement", new Variable() {
      @Override
      public Comparable<?> getValue() {
        Object st = (Object) ThreadLocalMap.get(AbstractQueryRouter._CURRENT_QUERY_OBJECT_);
        if (st instanceof Request) {
          return ((Request) st).isRead();
        } else {
          return null;
        }
      }
    });
  }

  private String ruleFile;
  private long lastRuleFileModified;
  private RuleFunctionMapLoader ruleFuncMapLoader;
  private ConfigModifiedEventHandler configModifiedEventHandler;

  public String getRuleFile() {
    return ruleFile;
  }

  public void setRuleFile(String configFile) {
    try {
      this.ruleFile = new File(configFile).getCanonicalPath();
    } catch (IOException e) {
      throw new ConfigurationException(e);
    }
  }

  @Override
  public void init() throws InitialisationException {
    if (StringUtil.isEmpty(ruleFile) || !new File(ruleFile).exists()) {
      throw new InitialisationException("rule File not found with name=" + ruleFile);
    }

  }

  public void setRuleFuncMapLoader(RuleFunctionMapLoader ruleFuncMapLoader) {
    this.ruleFuncMapLoader = ruleFuncMapLoader;
  }

  public TableRuleFileLoader() {
    ruleFunctionMap.putAll(ruleFunTab);
  }

  @Override
  public synchronized Map<Table, TableRule> loadRule() {

    ruleFuncMapLoader.loadFunctionMap(this.ruleFunctionMap);

    DocumentBuilder db;
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setValidating(true);
      dbf.setNamespaceAware(false);
      db = dbf.newDocumentBuilder();
      db.setEntityResolver(new EntityResolver() {

        public InputSource resolveEntity(String publicId, String systemId) {
          if (systemId.endsWith("rule.dtd")) {
            InputStream in =
                this.getClass().getResourceAsStream("/com/meidusa/amoeba/xml/rule.dtd");
            if (in == null) {
              LogLog.error("Could not find [rule.dtd]. Used ["
                  + AbstractQueryRouter.class.getClassLoader() + "] class loader in the search.");
              return null;
            } else {
              return new InputSource(in);
            }
          } else {
            return null;
          }
        }
      });

      db.setErrorHandler(new ErrorHandler() {

        public void warning(SAXParseException exception) {}

        public void error(SAXParseException exception) throws SAXException {
          logger.error(exception.getMessage() + " at (" + exception.getLineNumber() + ":"
              + exception.getColumnNumber() + ")");
          throw exception;
        }

        public void fatalError(SAXParseException exception) throws SAXException {
          logger.fatal(exception.getMessage() + " at (" + exception.getLineNumber() + ":"
              + exception.getColumnNumber() + ")");
          throw exception;
        }
      });
      return loadConfigurationFile(ruleFile, db);
    } catch (Exception e) {
      logger.fatal("Could not load configuration file, failing", e);
      throw new ConfigurationException("Error loading configuration file " + ruleFile, e);
    } finally {
      lastRuleFileModified = new File(ruleFile).lastModified();
    }
  }

  private Map<Table, TableRule> loadConfigurationFile(String configFile, DocumentBuilder db)
      throws InitialisationException {
    Document doc = null;
    InputStream is = null;
    Map<Table, TableRule> tableRuleMap = new HashMap<Table, TableRule>();
    try {
      is = new FileInputStream(new File(configFile));
      doc = db.parse(is);
    } catch (Exception e) {
      final String s = "Caught exception while loading file " + configFile;
      logger.error(s, e);
      throw new ConfigurationException(s, e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          logger.error("Unable to close input stream", e);
        }
      }
    }
    Element rootElement = doc.getDocumentElement();
    NodeList children = rootElement.getChildNodes();
    int childSize = children.getLength();

    for (int i = 0; i < childSize; i++) {
      Node childNode = children.item(i);

      if (childNode instanceof Element) {
        Element child = (Element) childNode;

        final String nodeName = child.getNodeName();
        if (nodeName.equals("tableRule")) {
          List<TableRule> list = loadTableRule(child);
          for (TableRule rule : list) {
            tableRuleMap.put(rule.table.getName() == null ? null : rule.table, rule);
          }
        }
      }
    }

    if (logger.isInfoEnabled()) {
      logger.info("Loaded rule configuration from: " + configFile);
    }
    return tableRuleMap;
  }

  private List<TableRule> loadTableRule(Element current) throws InitialisationException {

    String name = current.getAttribute("name");
    String schemaName = current.getAttribute("schema");
    List<TableRule> list = new ArrayList<TableRule>();

    String[] names = new String[] {name};
    if (name != null) {
      names = name.split(",");
    }

    String[] snames = new String[] {schemaName};
    if (!StringUtil.isEmpty(schemaName)) {
      snames = schemaName.split(",");
    }

    String usersAttr = current.getAttribute("users");
    String[] users = new String[] {usersAttr};
    if (usersAttr != null) {
      users = usersAttr.split(",");
    }


    String defaultPools = current.getAttribute("defaultPools");
    String[] arrayDefaultPools = null;

    if (defaultPools != null) {
      arrayDefaultPools = readTokenizedString(defaultPools, " ,");
    }

    String readPools = current.getAttribute("readPools");
    String[] arrayReadPools = null;
    if (readPools != null) {
      arrayReadPools = readTokenizedString(readPools, " ,");
    }

    String writePools = current.getAttribute("writePools");
    String[] arrayWritePools = null;
    if (writePools != null) {
      arrayWritePools = readTokenizedString(writePools, " ,");
    }

    for (String user : users) {
      if (ProxyRuntimeContext.getInstance().isUserExisted(user)) {
        for (String sname : snames) {
          for (String tableName : names) {
            TableRule tableRule = new TableRule();
            Table table = new Table();

            table.setUserName(user);
            String[] tableSchema = StringUtil.split(tableName, ".");
            if (tableSchema.length >= 2) {
              String tbName = tableName.substring(tableSchema[0].length() + 1);
              if ("*".equals(tbName)) {
                tbName = "^*";
              }
              table.setName(tbName);
              Schema schema = new Schema();
              String sName = tableSchema[0];
              if ("*".equals(sName)) {
                sName = "^*";
              }
              schema.setName(sName);
              table.setSchema(schema);
            } else {
              table.setName(tableName);
              if (!StringUtil.isEmpty(sname)) {
                Schema schema = new Schema();
                schema.setName(sname);
                table.setSchema(schema);
              }
            }
            tableRule.defaultPools = arrayDefaultPools;
            tableRule.readPools = arrayReadPools;
            tableRule.writePools = arrayWritePools;
            tableRule.table = table;

            NodeList children = current.getChildNodes();
            int childSize = children.getLength();

            for (int i = 0; i < childSize; i++) {
              Node childNode = children.item(i);

              if (childNode instanceof Element) {
                Element child = (Element) childNode;

                final String nodeName = child.getNodeName();
                if (nodeName.equals("rule")) {
                  tableRule.ruleList.add(loadRule(child, tableRule.table));
                }
              }
            }

            list.add(tableRule);
          }
        }
      } else {
        throw new ConfigurationException("user " + user + " not existed");
      }
    }


    return list;
  }

  private Rule loadRule(Element current, Table table) throws InitialisationException {
    Rule rule = new Rule();

    // root
    rule.name = current.getAttribute("name");
    String group = current.getAttribute("group");
    rule.group = StringUtil.isEmpty(group) ? null : group;
    String ignoreArray = current.getAttribute("ignoreArray");
    rule.ignoreArray = Boolean.parseBoolean(ignoreArray);
    String isSwitch = current.getAttribute("isSwitch");
    rule.isSwitch = Boolean.parseBoolean(isSwitch);
    String result = current.getAttribute("ruleResult");
    if (!StringUtil.isEmpty(result)) {
      result = result.toUpperCase();
      rule.result = Enum.valueOf(RuleResult.class, result);
    }
    // parameters
    Element parametersNode = ConfigLoaderUtil.getTheOnlyElement(current, "parameters");
    if (parametersNode != null) {
      String[] tokens = readTokenizedString(parametersNode.getTextContent(), " ,");
      int index = 0;
      for (String parameter : tokens) {
        rule.parameterMap.put(parameter, index);
        Column column = new Column();
        column.setName(parameter);
        column.setTable(table);
        rule.cloumnMap.put(column, index);
        index++;
      }

      tokens = readTokenizedString(parametersNode.getAttribute("excludes"), " ,");
      if (tokens != null) {
        for (String parameter : tokens) {
          Column column = new Column();
          column.setName(parameter);
          column.setTable(table);
          rule.excludes.add(column);
        }
      }
    }

    // expression
    Element expression = ConfigLoaderUtil.getTheOnlyElement(current, "expression");
    rule.expression = expression.getTextContent();
    rule.rowJep = new RowJEP(rule.expression);
    try {
      rule.rowJep.parseExpression(rule.parameterMap, variableMap, this.ruleFunctionMap);
    } catch (com.meidusa.amoeba.sqljep.ParseException e) {
      throw new InitialisationException("parser expression:" + rule.expression + " error", e);
    }

    // defaultPools
    Element defaultPoolsNode = ConfigLoaderUtil.getTheOnlyElement(current, "defaultPools");
    if (defaultPoolsNode != null) {
      String defaultPools = defaultPoolsNode.getTextContent();
      rule.defaultPools = readTokenizedString(defaultPools, " ,");
    }

    // readPools
    Element readPoolsNode = ConfigLoaderUtil.getTheOnlyElement(current, "readPools");
    if (readPoolsNode != null) {
      rule.readPools = readTokenizedString(readPoolsNode.getTextContent(), " ,");
    }

    // writePools
    Element writePoolsNode = ConfigLoaderUtil.getTheOnlyElement(current, "writePools");
    if (writePoolsNode != null) {
      rule.writePools = readTokenizedString(writePoolsNode.getTextContent(), " ,");
    }

    return rule;
  }

  public static String[] readTokenizedString(String string, String delim) {
    return StringUtil.split(string, delim);
  }

  @Override
  public boolean needLoad() {
    if (new File(ruleFile).lastModified() != lastRuleFileModified) {
      return true;
    }

    if (ruleFuncMapLoader.needLoad()) {
      return true;
    }
    return false;
  }

  @Override
  public Map<Table, TableRule> loadRule(List<Long> ids) {
    throw new UnsupportedOperationException("Load Rule Config by ids is not supported in class "
        + this.getClass().getName());

  }

  class ConfigCheckTread extends Thread {

    private ConfigCheckTread() {
      this.setDaemon(true);
      this.setName("ruleConfigCheckThread");
    }

    public void run() {
      while (true) {
        try {
          Thread.sleep(5000l);
          if (needLoad()) {
            configModifiedEventHandler.doOnConfigModified();
          }
        } catch (InterruptedException e) {}
      }
    }
  }

  @Override
  public Map<Table, TableRule> reLoadRule() {
    return loadRule();
  }

  @Override
  public void startObserve() {
    new ConfigCheckTread().start();
  }

  @Override
  public void setConfigModifiedEventHandler(ConfigModifiedEventHandler handler) {
    this.configModifiedEventHandler = handler;
  }
}
