package com.meidusa.amoeba.config.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import com.meidusa.amoeba.config.loader.util.ConfigLoaderUtil;
import com.meidusa.amoeba.exception.ConfigurationException;
import com.meidusa.amoeba.exception.InitialisationException;
import com.meidusa.amoeba.route.AbstractQueryRouter;

/**
 * 
 * @author struct
 * 
 * @param <K>
 * @param <V>
 */
public abstract class FunctionFileLoader<K, V> {
  private static Logger logger = Logger.getLogger(FunctionFileLoader.class);
  
  private String dtdPath = "/com/meidusa/amoeba/xml/function.dtd";
  private String dtdSystemID = "function.dtd";

  private String funcFile;
  private long lastFuncFileModified;

  public void setFuncFile(String funcFile) {
    try {
      this.funcFile = new File(funcFile).getCanonicalPath();
    } catch (IOException e) {
      throw new ConfigurationException(e);
    }
  }

  public void loadFunctionMap(Map funMap) {

    DocumentBuilder db;

    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setValidating(true);
      dbf.setNamespaceAware(false);

      db = dbf.newDocumentBuilder();
      db.setEntityResolver(new EntityResolver() {
        public InputSource resolveEntity(String publicId, String systemId) {
          if (systemId.endsWith(dtdSystemID)) {
            InputStream in = this.getClass().getResourceAsStream(dtdPath);
            if (in == null) {
              LogLog.error("Could not find [" + dtdSystemID + "]. Used ["
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

      loadFunctionFile(funcFile, db, funMap);
      
    } catch (Exception e) {
      logger.fatal("Could not load configuration file, failing", e);
      throw new ConfigurationException("Error loading configuration file " + funcFile, e);
    } finally {
      lastFuncFileModified = new File(funcFile).lastModified();
    }
  }

  private void loadFunctionFile(String fileName, DocumentBuilder db, Map<K, V> funMap)
      throws InitialisationException {
    Document doc = null;
    InputStream is = null;
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
        if (nodeName.equals("function")) {
          V function = loadFunction(child);
          putToMap(funMap, function);
        }
      }
    }

    if (logger.isInfoEnabled()) {
      logger.info("Loaded function configuration from: " + fileName);
    }
  }

  public abstract void putToMap(Map<K, V> map, V value);

  public abstract void initBeanObject(BeanObjectEntityConfig config, V bean);

  @SuppressWarnings("unchecked")
  protected V loadFunction(Element current) {
    BeanObjectEntityConfig config = ConfigLoaderUtil.loadBeanConfig(current);
    V function = (V) config.createBeanObject(true);
    initBeanObject(config, function);
    return function;
  }

  public boolean needLoad() {
    if (new File(funcFile).lastModified() != lastFuncFileModified) {
      return true;
    }
    return false;
  }

}
