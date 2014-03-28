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
package com.meidusa.amoeba.parser.statement;

import java.util.HashMap;
import java.util.Map;

import com.meidusa.amoeba.parser.expression.Expression;

/**
 * 用于设置连接属性的query语句（比如：mysql的“set names utf8”之类的语句）
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public class PropertyStatement extends AbstractStatement  {
	private Map<String,Expression> properties = new HashMap<String,Expression>();
	
	public void addProperty(String key,Expression value){
		
		this.properties.put(key==null?null:key.toLowerCase(), value);
	}
	
	public Expression getExpression() {
		return null;
	}
	
	public Expression getValue(String key){
		return this.properties.get(key==null?null:key.toLowerCase());
	}

	public Map<String, Expression> getProperties() {
		return properties;
	}

}
