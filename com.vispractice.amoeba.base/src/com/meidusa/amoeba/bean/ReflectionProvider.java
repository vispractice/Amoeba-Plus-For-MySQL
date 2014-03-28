/*
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU AFFERO GENERAL PUBLIC LICENSE for more details. 
 * 	You should have received a copy of the GNU AFFERO GENERAL PUBLIC LICENSE along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.meidusa.amoeba.bean;

import java.lang.reflect.Field;

/**
 * Provides core reflection services.
 */
@SuppressWarnings("unchecked")
public interface ReflectionProvider {

	/**
	 * Creates a new instance of the specified type using the default (null) constructor.
	 * @param type	the type to instantiate
	 * @return	a new instance of this type
	 */
    
	Object newInstance(Class type);

    void visitSerializableFields(Object object, Visitor visitor);

    void writeField(Object object, String fieldName, Object value, Class definedIn);

    public void invokeMethod(Object object, String methodName, Object value, Class definedIn);

    Class getFieldType(Object object, String fieldName, Class definedIn);

    boolean fieldDefinedInClass(String fieldName, Class type);

    /**
     * A visitor interface for serializable fields defined in a class. 
     *
     */
    interface Visitor {
    	
    	/**
    	 * Callback for each visit
    	 * @param name	field name
    	 * @param type	field type
    	 * @param definedIn	where the field was defined
    	 * @param value	field value
    	 */
        void visit(String name, Class type, Class definedIn, Object value);
    }
    
    /**
     * Returns a field defined in some class.
     * @param definedIn	class where the field was defined
     * @param fieldName	field name
     * @return	the field itself
     */
	Field getField(Class definedIn, String fieldName);
	
}
