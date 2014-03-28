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
package com.meidusa.amoeba.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

import com.meidusa.amoeba.bean.PureJavaReflectionProvider;
import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.exception.ConfigurationException;
import com.meidusa.amoeba.exception.InitialisationException;
import com.meidusa.amoeba.util.Initialisable;
import com.meidusa.amoeba.util.ObjectUtil;
import com.meidusa.amoeba.util.StringUtil;

/**
 * Bean 的基本配置信息
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 * @version $Id: BeanObjectEntityConfig.java 3594 2006-11-23 07:39:25Z struct $
 */
public class BeanObjectEntityConfig extends ConfigEntity implements Cloneable {

    private static Logger                     logger             = Logger.getLogger(BeanObjectEntityConfig.class);
    private static final long                 serialVersionUID   = 1L;
    private static PureJavaReflectionProvider reflectionProvider = new PureJavaReflectionProvider();

    private String                            name;
    private String                            className;
    private HashMap<String, Object>               params             = new HashMap<String, Object>();

    public String getClassName() {
        return className;
    }

    public void setClassName(String beanObject) {
        this.className = beanObject;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HashMap<String, Object> getParams() {
        return params;
    }

    public void setParams(HashMap<String, Object> params) {
        this.params = params;
    }

    /**
     * 用于clone，子类可覆盖此方法
     */
    protected BeanObjectEntityConfig newObject() {
        Object object = null;
        try {
            object = this.getClass().newInstance();
        } catch (InstantiationException e) {
            logger.error("Instantiation class :" + this.getClass() + " Exception", e);
        } catch (IllegalAccessException e) {
            logger.error("IllegalAccess class :" + this.getClass() + " Exception", e);
        }
        return (BeanObjectEntityConfig) object;
    }

    public Object createBeanObject(boolean initEarly) throws ConfigurationException {
    	return createBeanObject(initEarly,null);
    }
    
    public Object createBeanObject(boolean initEarly,Map context) throws ConfigurationException {
        try {
        	// 用bundle机制加载类
        	Bundle bundle = ProxyRuntimeContext.getInstance().getBackendBundle();
        	Class<?> clazz = bundle.loadClass(className);
        	
            Object object = null;
            
            ServiceReference ref = bundle.getBundleContext().getServiceReference(clazz);
            
            if (ref != null) {
              object = bundle.getBundleContext().getService(ref);
            }
            else {
              object = reflectionProvider.newInstance(clazz);
            }
        	
            ParameterMapping.mappingObject(object, getParams(),context);
            if (initEarly) {
                if (object instanceof Initialisable) {
                    ((Initialisable) object).init();
                }
            }
            return object;
        } catch (ClassNotFoundException e) {
            logger.error("instanceo object error:", e);
            throw new ConfigurationException(e);
        } catch (InitialisationException e) {
            logger.error("instanceo object error:", e);
            throw new ConfigurationException(e);
        }
    }

    public Object clone() {
        BeanObjectEntityConfig entityConfig = newObject();

        if (entityConfig == null) {
            return null;
        }

        entityConfig.className = this.className;
        entityConfig.name = this.name;
        if(params != null){
        	entityConfig.params = (HashMap)params.clone();
        }
        return entityConfig;
    }

    public int hashCode() {
        int hashcode = 37;
        hashcode += (this.name == null ? 0 : name.hashCode());
        hashcode += (this.className == null ? 0 : className.hashCode());
        hashcode += (this.params == null ? 0 : params.hashCode());
        return hashcode;
    }

    public boolean equals(Object object) {
        if (object instanceof BeanObjectEntityConfig) {
            BeanObjectEntityConfig entity = (BeanObjectEntityConfig) object;
            boolean isEquals = StringUtil.equals(this.name, entity.name);
            isEquals = isEquals && StringUtil.equals(this.className, entity.getClassName());
            isEquals = isEquals && (ObjectUtil.equals(this.params, entity.params));

            return isEquals;
        }
        return false;
    }

}
