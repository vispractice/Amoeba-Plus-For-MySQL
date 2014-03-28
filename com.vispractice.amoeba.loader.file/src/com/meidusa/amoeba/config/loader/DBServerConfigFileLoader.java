package com.meidusa.amoeba.config.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.apache.log4j.helpers.LogLog;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.meidusa.amoeba.config.BeanObjectEntityConfig;
import com.meidusa.amoeba.config.DBServerConfig;
import com.meidusa.amoeba.config.ParameterMapping;
import com.meidusa.amoeba.config.loader.DBServerConfigLoader;
import com.meidusa.amoeba.config.loader.util.ConfigLoaderUtil;
import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.exception.ConfigurationException;
import com.meidusa.amoeba.util.StringUtil;

public class DBServerConfigFileLoader implements DBServerConfigLoader {
	protected static Logger logger = Logger.getLogger(DBServerConfigFileLoader.class);
	private String configFile;
	
	public String getConfigFile() {
		return configFile;
	}

    public void setConfigFile(String configFile) {
      try {
        this.configFile = new File(configFile).getCanonicalPath();
      } catch (IOException e) {
        throw new ConfigurationException(e);
      }
    }
	
	@Override
	public Map<String, DBServerConfig> loadConfig(List<Long> ids) {
		throw new UnsupportedOperationException("Load DB Server Config by ids is not supported in class " + this.getClass().getName());
	}
	
	@Override
	public Map<String, DBServerConfig> loadConfig() {
	        DocumentBuilder db;

	        try {
	            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	            dbf.setValidating(true);
	            dbf.setNamespaceAware(false);

	            db = dbf.newDocumentBuilder();
	            db.setEntityResolver(new EntityResolver() {

	                public InputSource resolveEntity(String publicId, String systemId) {
	                    if (systemId.endsWith("dbserver.dtd")) {
	                        InputStream in = this.getClass().getResourceAsStream("/com/meidusa/amoeba/xml/dbserver.dtd");
	                        if (in == null) {
	                            LogLog.error("Could not find [dbserver.dtd]. Used [" + ProxyRuntimeContext.class.getClassLoader() + "] class loader in the search.");
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

	                public void warning(SAXParseException exception) {
	                }

	                public void error(SAXParseException exception) throws SAXException {
	                    logger.error(exception.getMessage() + " at (" + exception.getLineNumber() + ":" + exception.getColumnNumber() + ")");
	                    throw exception;
	                }

	                public void fatalError(SAXParseException exception) throws SAXException {
	                    logger.fatal(exception.getMessage() + " at (" + exception.getLineNumber() + ":" + exception.getColumnNumber() + ")");
	                    throw exception;
	                }
	            });
	            return loadConfigurationFile(db);
	        } catch (Exception e) {
	            logger.fatal("Could not load configuration file, failing", e);
	            throw new ConfigurationException("Error loading configuration file " + configFile, e);
	        }
	}
	private Map<String, DBServerConfig> loadConfigurationFile( DocumentBuilder db) {
		Document doc = null;
        InputStream is = null;
        Map<String, DBServerConfig> configMap = new HashMap<String, DBServerConfig>();
		try {
            is = new FileInputStream(new File(configFile));

            if (is == null) {
                throw new Exception("Could not open file " + configFile);
            }

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
        final String nodeName = rootElement.getNodeName();
        if (nodeName.equals("amoeba:dbServers")) {
        	loadServers(rootElement,configMap);
        }
        if (logger.isInfoEnabled()) {
            logger.info("Loaded Dbserver configuration from: " + configFile);
        }
		return configMap;
	}

	private void loadServers(Element current,Map<String, DBServerConfig> configMap){
		
		NodeList children = current.getChildNodes();
	    int childSize = children.getLength();
	    for (int i = 0; i < childSize; i++) {
	        Node childNode = children.item(i);
	        if (childNode instanceof Element) {
	            Element child = (Element) childNode;
	            DBServerConfig serverConfig = loadServer(child);
	            if (serverConfig.getVirtual() != null && serverConfig.getVirtual().booleanValue()) {
	                if (serverConfig.getPoolConfig() != null) {
	                    if (StringUtil.isEmpty(serverConfig.getPoolConfig().getClassName())) {
	                        serverConfig.getPoolConfig().setClassName(getDefaultVirtualPoolClassName());
	                    }
	                }
	            } else {
	                if (serverConfig.getPoolConfig() != null) {
	                    if (StringUtil.isEmpty(serverConfig.getPoolConfig().getClassName())) {
	                        serverConfig.getPoolConfig().setClassName(getDefaultRealPoolClassName());
	                    }
	                }
	            }
	
	            /*if (serverConfig.getFactoryConfig() != null) {
	                if (StringUtil.isEmpty(serverConfig.getFactoryConfig().getClassName())) {
	                	throw new ConfigurationException("DBServer Config name=" + serverConfig.getName()+" factory class must not be null!");
	                }
	            }*/
	            configMap.put(serverConfig.getName(), serverConfig);
	        }
	    }
	}
	
    private DBServerConfig loadServer(Element current) {
        DBServerConfig serverConfig = new DBServerConfig();
        NamedNodeMap nodeMap = current.getAttributes();
        Map<String, Object> map = new HashMap<String, Object>();
        for (int i = 0; i < nodeMap.getLength(); i++) {
            Node node = nodeMap.item(i);
            if (node instanceof org.w3c.dom.Attr) {
                Attr attr = (Attr) node;
                map.put(attr.getName(), attr.getNodeValue());
            }
        }

        ParameterMapping.mappingObject(serverConfig, map,null);

        BeanObjectEntityConfig factory = ConfigLoaderUtil.loadBeanConfig(ConfigLoaderUtil.getTheOnlyElement(current, "factoryConfig"));
        BeanObjectEntityConfig pool = ConfigLoaderUtil.loadBeanConfig(ConfigLoaderUtil.getTheOnlyElement(current, "poolConfig"));
        if (pool != null) {
            serverConfig.setPoolConfig(pool);
        }

        if (factory != null) {
            serverConfig.setFactoryConfig(factory);
        }

        return serverConfig;
    }
    
    protected String getDefaultRealPoolClassName() {
        return DEFAULT_REAL_POOL_CLASS;
    }

    protected String getDefaultVirtualPoolClassName() {
        return DEFAULT_VIRTUAL_POOL_CLASS;
    }
    
}
