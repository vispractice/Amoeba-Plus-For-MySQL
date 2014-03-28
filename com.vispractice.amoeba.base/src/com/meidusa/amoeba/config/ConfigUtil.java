package com.meidusa.amoeba.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections.map.LRUMap;

import com.meidusa.amoeba.exception.ConfigurationException;

import ognl.Ognl;
import ognl.OgnlException;

/**
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 * @author hexianmao
 */
public class ConfigUtil {

    public static String filter(String text) throws ConfigurationException {
        return filter(text, System.getProperties());
    }

    public static String filter(String text, Properties properties) throws ConfigurationException {
        // String result = "";
        StringBuilder result = new StringBuilder();
        int cur = 0;
        int textLen = text.length();
        int propStart = -1;
        int propStop = -1;
        String propName = null;
        String propValue = null;
        for (; cur < textLen; cur = propStop + 1) {
            propStart = text.indexOf("${", cur);
            if (propStart < 0) {
                break;
            }
            result.append(text.substring(cur, propStart));
            // result = result + text.substring(cur, propStart);
            propStop = text.indexOf("}", propStart);
            if (propStop < 0) {
                throw new ConfigurationException("Unterminated property: " + text.substring(propStart));
            }
            propName = text.substring(propStart + 2, propStop);
            propValue = properties.getProperty(propName);
            if (propValue == null) {
                throw new ConfigurationException("No such property: " + propName);
            }
            result.append(propValue);
            // result = result + propValue;
        }

        // result = result + text.substring(cur);
        return result.append(text.substring(cur)).toString();
    }
    
    public static String filterWtihOGNL(String text, Map<String,Object> context,Object root) throws ConfigurationException {
    	return filterWtihOGNL(text,context,root,System.getProperties());
    }
    
    public static String filterWtihOGNL(String text, Map<String,Object> context) throws ConfigurationException {
    	return filterWtihOGNL(text,context,new Object(),System.getProperties());
    }
    
    public static String filterWtihOGNL(String text, Map<String,Object> context,Object root,Properties properties) throws ConfigurationException {
        // String result = "";
    	if(properties == null){
    		properties = System.getProperties();
    	}
        StringBuilder result = new StringBuilder();
        int cur = 0;
        int textLen = text.length();
        int propStart = -1;
        int propStop = -1;
        String propName = null;
        String propValue = null;
        for (; cur < textLen; cur = propStop + 1) {
            propStart = text.indexOf("${", cur);
            if (propStart < 0) {
                break;
            }
            result.append(text.substring(cur, propStart));
            // result = result + text.substring(cur, propStart);
            propStop = text.indexOf("}", propStart);
            if (propStop < 0) {
                throw new ConfigurationException("Unterminated property: " + text.substring(propStart));
            }
            propName = text.substring(propStart + 2, propStop);
            if(propName.startsWith("#") || propName.startsWith("@")){
				try {
					Object tree = lruMap.get(propName);
					if(tree == null){
						synchronized (lruMap) {
							tree = lruMap.get(propName);
							if(tree == null){
								tree = Ognl.parseExpression(propName);
								lruMap.put(propName,tree);
							}
						}
					}
					propValue = Ognl.getValue(tree, context,root).toString();
				} catch (OgnlException e) {
					throw new ConfigurationException("parseException expression="+propName,e);
				}
			}else{
				propValue = properties.getProperty(propName);
	            if (propValue == null) {
	                throw new ConfigurationException("No such property: " + propName);
	            }
			}
            result.append(propValue);
        }

        return result.append(text.substring(cur)).toString();
    }
    static LRUMap lruMap = new LRUMap(10000);
    public static Map<String,Object> preparedOGNL(String text) throws ConfigurationException {
    	Map<String,Object> ognlExpressionMap = new HashMap<String,Object>();
        // String result = "";
        int cur = 0;
        int textLen = text.length();
        int propStart = -1;
        int propStop = -1;
        String propName = null;
        Object propValue = null;
        for (; cur < textLen; cur = propStop + 1) {
            propStart = text.indexOf("${", cur);
            if (propStart < 0) {
                break;
            }
            // result = result + text.substring(cur, propStart);
            propStop = text.indexOf("}", propStart);
            if (propStop < 0) {
                throw new ConfigurationException("Unterminated property: " + text.substring(propStart));
            }
            propName = text.substring(propStart + 2, propStop);
            
			if(propName.startsWith("#") || propName.startsWith("@")){
				try {
					Object tree = lruMap.get(propName);
					if(tree == null){
						synchronized (lruMap) {
							tree = lruMap.get(propName);
							if(tree == null){
								tree = Ognl.parseExpression(propName);
								lruMap.put(propName,tree);
							}
						}
					}
				} catch (OgnlException e) {
					throw new ConfigurationException("parseException expression="+propName,e);
				}
			}
        }

        // result = result + text.substring(cur);
        return ognlExpressionMap;
    }
}
