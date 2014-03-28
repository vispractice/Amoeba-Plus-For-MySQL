package com.meidusa.amoeba.config.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.apache.log4j.helpers.FileWatchdog;
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

import com.meidusa.amoeba.config.UserConfig;
import com.meidusa.amoeba.config.loader.UserLoader;
import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.exception.ConfigurationException;
import com.meidusa.amoeba.exception.InitialisationException;
import com.meidusa.amoeba.util.Initialisable;

public class UserFileLoader implements UserLoader, Initialisable{
  protected static Logger     logger        = Logger.getLogger(UserFileLoader.class);

  private String userFile;
  
  @Override
  public void loadUser() {
    Map<String, UserConfig> users = ProxyRuntimeContext.getInstance().getUserMap();
    
    DocumentBuilder db;

    try {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(true);
        dbf.setNamespaceAware(false);

        db = dbf.newDocumentBuilder();
        db.setEntityResolver(new EntityResolver() {

            public InputSource resolveEntity(String publicId, String systemId) {
                if (systemId.endsWith("user.dtd")) {
                    InputStream in = this.getClass().getResourceAsStream("/com/meidusa/amoeba/xml/user.dtd");
                    if (in == null) {
                        LogLog.error("Could not find [user.dtd]. Used [" + ProxyRuntimeContext.class.getClassLoader() + "] class loader in the search.");
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
        
        Document doc = null;
        InputStream is = null;
        
        try {
          is = new FileInputStream(new File(userFile));

          doc = db.parse(is);
      } catch (Exception e) {
          final String s = "Caught exception while loading file " + userFile;
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

      for(int i=0; i<childSize; i++) {
        Node childNode = children.item(i);
        if (childNode instanceof Element) {
          Element current = (Element) childNode;

          final String nodeName = current.getNodeName();
          if (nodeName.equals("user")) {
            String name = current.getAttribute("name");
            String passwd = current.getAttribute("passwd");
            String isAdmin =  current.getAttribute("isAdmin");
            
            UserConfig user = new UserConfig(name, passwd);
            if ("true".equals(isAdmin)) {
              user.setAdmin(true);
            }
            
            users.put(name, user);
          }
        }
      }
      

    } catch (Exception e) {
        logger.fatal("Could not load configuration file, failing", e);
        throw new ConfigurationException("Error loading configuration file " + userFile, e);
    }
  }

  public String getUserFile() {
    return userFile;
  }

  public void setUserFile(String userFile) {
    try {
      this.userFile = (new File(userFile)).getCanonicalPath();
    } catch (IOException e) {
      throw new ConfigurationException(e);
    }
  }

  @Override
  public void init() throws InitialisationException {
    UserFileWatchdog dog = new UserFileWatchdog(userFile);
    dog.setDaemon(true);
    dog.setDelay(FileWatchdog.DEFAULT_DELAY);
    dog.start();
  }
  
  private class UserFileWatchdog extends FileWatchdog {

    public UserFileWatchdog(String filename) {
      super(filename);
    }
    
    public void doOnChange() {
      UserFileLoader.this.userFile = this.filename;
      UserFileLoader.this.loadUser();
      
      if (logger.isInfoEnabled()) {
        logger.info(String.format("loaded userMap from: %s", userFile));
      }
    }
  }

}
