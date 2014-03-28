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
package com.meidusa.amoeba.parser.expression;

/**
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public class ParameterExpression extends Expression {
	
	private int index;
	public ParameterExpression(int index){
		this.index = index;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Comparable evaluate(Object[] parameters) {
		if(parameters == null || parameters.length ==0){
			return null;
		}
		if(parameters[index] instanceof Comparable){
			return (Comparable)parameters[index];
		}else if(parameters[index] instanceof byte[]){
			return new String((byte[])parameters[index]);
		}else{
			return parameters[index]==null?null:parameters[index].toString();
		}
	}

	public boolean isRealtime(){
		return true;
	}
	
	@Override
	public Expression reverse() {
		return this;
	}

	@Override
	protected void toString(StringBuilder builder) {
		builder.append("?");
	}

}
