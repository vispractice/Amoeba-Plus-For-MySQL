package com.meidusa.amoeba.config.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
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

import com.meidusa.amoeba.config.BeanObjectEntityConfig;
import com.meidusa.amoeba.config.ConfigUtil;
import com.meidusa.amoeba.config.ParameterMapping;
import com.meidusa.amoeba.config.ProxyServerConfig;
import com.meidusa.amoeba.config.loader.AmoebaContextLoader;
import com.meidusa.amoeba.config.loader.DBServerConfigLoader;
import com.meidusa.amoeba.config.loader.util.ConfigLoaderUtil;
import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.exception.ConfigurationException;
import com.meidusa.amoeba.net.ConnectionManager;
import com.meidusa.amoeba.util.StringUtil;

public class AmoebaContextFileLoader implements AmoebaContextLoader{
  
  protected static Logger logger = Logger.getLogger(AmoebaContextFileLoader.class);
  private ProxyRuntimeContext amoebaContext;

  public void setAmoebaContext(ProxyRuntimeContext amoebaContext) {
    this.amoebaContext = amoebaContext;
  }
  
  @Override
  public ProxyServerConfig loadConfig() {
    String PATH_SEPARATOR = StringUtil.PATH_SEPARATOR;

    String config = amoebaContext.getAmoebaHomePath() + PATH_SEPARATOR + "conf" + PATH_SEPARATOR + "amoeba.xml";
    
    config = ConfigUtil.filter(config);
    File configFile = new File(config);
    
    if(config == null || !configFile.exists()){
        logger.error("could not find config file:"+configFile.getAbsolutePath());
        System.exit(-1);
    }
    return loadConfig(configFile.getAbsolutePath());
  }
  
  private ProxyServerConfig loadConfig(String configFileName) {
    DocumentBuilder db;

    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setValidating(true);
      dbf.setNamespaceAware(false);

      db = dbf.newDocumentBuilder();
      db.setEntityResolver(new EntityResolver() {

        public InputSource resolveEntity(String publicId, String systemId) {
          if (systemId.endsWith("amoeba.dtd")) {
            InputStream in =
                this.getClass().getResourceAsStream("/com/meidusa/amoeba/xml/amoeba.dtd");
            if (in == null) {
              LogLog.error("Could not find [amoeba.dtd]. Used ["
                  + ProxyRuntimeContext.class.getClassLoader() + "] class loader in the search.");
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
      return loadConfigurationFile(configFileName, db);
    } catch (Exception e) {
      logger.fatal("Could not load configuration file, failing", e);
      throw new ConfigurationException("Error loading configuration file " + configFileName, e);
    }
  }
  
  private ProxyServerConfig loadConfigurationFile(String fileName, DocumentBuilder db) {
    Document doc = null;
    InputStream is = null;
    ProxyServerConfig config = new ProxyServerConfig();
    try {
      is = new FileInputStream(new File(fileName));

      if (is == null) {
        throw new Exception("Could not open file " + fileName);
      }

      doc = db.parse(is);
    } catch (Exception e) {
      final String s = "Caught exception while loading file " + fileName;
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
        if (nodeName.equals("proxy")) {
          loadProxyConfig(child, config);
        } else if (nodeName.equals("connectionManagerList")) {
          loadConnectionManagers(child, config);
        } else if (nodeName.equals("dbServerLoader")) {
          loadDbServerLoader(rootElement, config);
        } else if (nodeName.equals("queryRouter")) {
          loadQueryRouter(rootElement, config);
        } else if (nodeName.equals("userLoader")){
          loadUserLoader(rootElement, config);
        }
      }
    }

    if (logger.isInfoEnabled()) {
      logger.info("Loaded Amoeba Proxy configuration from: " + fileName);
    }
    return config;
  }

  private void loadQueryRouter(Element current, ProxyServerConfig config) {
    BeanObjectEntityConfig queryRouter =
        ConfigLoaderUtil.loadBeanConfig(ConfigLoaderUtil.getTheOnlyElement(current, "queryRouter"));
    config.setQueryRouterConfig(queryRouter);
  }

  private void loadDbServerLoader(Element current, ProxyServerConfig config) {
    BeanObjectEntityConfig dbserverLoader =
        ConfigLoaderUtil.loadBeanConfig(ConfigLoaderUtil.getTheOnlyElement(current,
            "dbServerLoader"));
    DBServerConfigLoader loader = (DBServerConfigLoader) dbserverLoader.createBeanObject(true, amoebaContext.getConnectionManagerList());
    config.putAllServers(loader.loadConfig());
  }
  
  private void loadUserLoader(Element current, ProxyServerConfig config) {
    BeanObjectEntityConfig userLoaderConfig = ConfigLoaderUtil.loadBeanConfig(ConfigLoaderUtil.getTheOnlyElement(current, "userLoader"));
    
    userLoaderConfig.createBeanObject(true, null);
  }

  private void loadConnectionManagers(Element current, ProxyServerConfig config) {
    NodeList children = current.getChildNodes();
    int childSize = children.getLength();
    for (int i = 0; i < childSize; i++) {
      Node childNode = children.item(i);
      if (childNode instanceof Element) {
        Element child = (Element) childNode;
        BeanObjectEntityConfig managerConfig = ConfigLoaderUtil.loadBeanConfig(child);
        if (StringUtil.isEmpty(managerConfig.getClassName())) {
          managerConfig.setClassName(amoebaContext.getDefaultServerConnectionManagerClassName());
        }
        config.addManager(managerConfig.getName(), managerConfig);
      }
    }

    // create bean and init
    for (Map.Entry<String, BeanObjectEntityConfig> entry : config.getManagers().entrySet()) {
      BeanObjectEntityConfig beanObjectEntityConfig = entry.getValue();
      try {
        ConnectionManager manager =
            (ConnectionManager) amoebaContext.createBeanObjectEntity(beanObjectEntityConfig, true);
        manager.setName(entry.getKey());
        amoebaContext.getConnectionManagerList().put(manager.getName(), manager);
      } catch (Exception e) {
        throw new ConfigurationException("manager instance error", e);
      }
    }
  }

  private void loadProxyConfig(Element current, ProxyServerConfig config) {
    NodeList children = current.getChildNodes();
    int childSize = children.getLength();
    Map<String, Object> map = new HashMap<String, Object>();
    for (int i = 0; i < childSize; i++) {
      Node childNode = children.item(i);
      if (childNode instanceof Element) {
        Element child = (Element) childNode;
        final String nodeName = child.getNodeName();
        if (nodeName.equals("property")) {
          String key = child.getAttribute("name");
          String value = child.getTextContent();
          map.put(key, value);
        } 
        else if (nodeName.equals("service")) {
          BeanObjectEntityConfig server = ConfigLoaderUtil.loadBeanConfig(child);
          config.addServerConfig(server);
        } 
        else if (nodeName.equals("runtime")) {
          BeanObjectEntityConfig runtime = ConfigLoaderUtil.loadBeanConfig(child);
          config.setRuntimeConfig(runtime);
        }
      }
    }
    ParameterMapping.mappingObject(config, map, null);
  }
}
