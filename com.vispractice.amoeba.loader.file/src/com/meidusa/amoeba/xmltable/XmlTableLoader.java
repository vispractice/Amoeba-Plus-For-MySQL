package com.meidusa.amoeba.xmltable;

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

import com.meidusa.amoeba.exception.ConfigurationException;
import com.meidusa.amoeba.exception.InitialisationException;
import com.meidusa.amoeba.route.AbstractQueryRouter;
import com.meidusa.amoeba.util.StringUtil;

/**
 * 
 * @author struct
 *
 * @param <K>
 * @param <V>
 */
public class XmlTableLoader {
	private static Logger logger = Logger.getLogger(XmlTableLoader.class);
	private String dtdPath;
	private String dtdSystemID;
	public void setDTD(String dtdPath){
		this.dtdPath = dtdPath;
	}
	
	public void setDTDSystemID(String dtdSystemID){
		this.dtdSystemID = dtdSystemID;
	}
	
	public Map<String,XmlTable> loadXmlTable(String configFileName){
		DocumentBuilder db;

    try {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(true);
        dbf.setNamespaceAware(false);

        db = dbf.newDocumentBuilder();
        db.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) {
            	if (systemId.endsWith(dtdSystemID)) {
          	      InputStream in = AbstractQueryRouter.class.getResourceAsStream(dtdPath);
          	      if (in == null) {
          		LogLog.error("Could not find ["+dtdSystemID+"]. Used [" + AbstractQueryRouter.class.getClassLoader() 
          			     + "] class loader in the search.");
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
        
        
	       return loadXmlTable(configFileName, db);
	    } catch (Exception e) {
	        logger.fatal("Could not load configuration file, failing", e);
	        throw new ConfigurationException("Error loading configuration file " + configFileName, e);
	    }
	
	}

	private Map<String,XmlTable> loadXmlTable(String fileName, DocumentBuilder db) throws InitialisationException {
	    Document doc = null;
	    InputStream is = null;
	    Map<String,XmlTable> tableMap = new HashMap<String,XmlTable>();
	    
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
	            if (nodeName.equals("table")) {
	            	XmlTable table = loadXmlTable(child);
	            	tableMap.put(table.getName().toLowerCase(), table);
	            }
	        }
	    }
	
	    if (logger.isInfoEnabled()) {
	        logger.info("Loaded xml table from: " + fileName);
	    }
	    return tableMap;
	}

	protected XmlTable loadXmlTable(Element current){
		XmlTable table = new XmlTable();
		String name = current.getAttribute("name");
		table.setName(name);
		NodeList children = current.getChildNodes();
	    int childSize = children.getLength();
		
		for (int i = 0; i < childSize; i++) {
	        Node childNode = children.item(i);
	        if (childNode instanceof Element) {
	            Element child = (Element) childNode;
	
	            final String nodeName = child.getNodeName();
	            if (nodeName.equals("row")) {
	            	XmlRow row = loadRow(table,child);
	            	table.getRows().add(row);
	            }
	        }
		}
		return table;
	}
	
	protected XmlRow loadRow(XmlTable table,Element current){
		XmlRow row = new XmlRow();
		NodeList children = current.getChildNodes();
	    int childSize = children.getLength();
		
		for (int i = 0; i < childSize; i++) {
	        Node childNode = children.item(i);
	
	        if (childNode instanceof Element) {
	            Element child = (Element) childNode;
	
	            final String nodeName = child.getNodeName();
	            if (nodeName.equals("field")) {
	            	String name = child.getAttribute("name").toLowerCase();
	            	if(!table.getColumns().contains(name)){
	            		table.getColumns().add(name);
	            	}
	            	String value = child.getAttribute("value");
	            	if(StringUtil.isEmpty(value)){
	            		value = child.getTextContent();
	            	}
	            	XmlColumn column = new XmlColumn();
	            	column.setName(name);
	            	column.setValue(value);
	            	row.addColumn(name, column);
	            }
	        }
	    }
		return row;
	}
}
