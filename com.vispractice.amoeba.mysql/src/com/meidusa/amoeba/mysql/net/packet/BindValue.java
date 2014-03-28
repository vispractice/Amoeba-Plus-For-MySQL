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
package com.meidusa.amoeba.mysql.net.packet;

import java.io.Serializable;

import com.meidusa.amoeba.mysql.jdbc.MysqlDefs;

/**
 * copy from mysql-connector-j
 *
 */
public class BindValue implements Serializable{
	private static final long serialVersionUID = 1L;

		public long boundBeforeExecutionNum = 0;
		
		public long bindLength; /* Default length of data */

		public int bufferType; /* buffer type */

		public byte byteBinding;

		public double doubleBinding;

		public float floatBinding;

		public int intBinding;

		public boolean isLongData; /* long data indicator */

		public boolean isNull; /* NULL indicator */

		public boolean isSet = false; /* has this parameter been set? */

		public long longBinding;

		public short shortBinding;
		
		public byte scale;
		
		public Object value; /* The value to store */
		
		public String charset;
		
		public BindValue() {
		}

		public BindValue(BindValue copyMe) {
			this.value = copyMe.value;
			this.isSet = copyMe.isSet;
			this.isLongData = copyMe.isLongData;
			this.isNull = copyMe.isNull;
			this.bufferType = copyMe.bufferType;
			this.bindLength = copyMe.bindLength;
			this.byteBinding = copyMe.byteBinding;
			this.shortBinding = copyMe.shortBinding;
			this.intBinding = copyMe.intBinding;
			this.longBinding = copyMe.longBinding;
			this.floatBinding = copyMe.floatBinding;
			this.doubleBinding = copyMe.doubleBinding;
		}

		void reset() {
			this.isSet = false;
			this.value = null;
			this.isLongData = false;

			this.byteBinding = 0;
			this.shortBinding = 0;
			this.intBinding = 0;
			this.longBinding = 0L;
			this.floatBinding = 0;
			this.doubleBinding = 0D;
		}

		public String toString() {
			return toString(false);
		}

		public String toString(boolean quoteIfNeeded) {
			if (this.isLongData) {
				return "' STREAM DATA '";
			}

			switch (this.bufferType) {
			case MysqlDefs.FIELD_TYPE_TINY:
				return String.valueOf(byteBinding);
			case MysqlDefs.FIELD_TYPE_SHORT:
				return String.valueOf(shortBinding);
			case MysqlDefs.FIELD_TYPE_INT24:
				return String.valueOf(intBinding);
			case MysqlDefs.FIELD_TYPE_LONG:
				return String.valueOf(longBinding);
			case MysqlDefs.FIELD_TYPE_LONGLONG:
				return String.valueOf(longBinding);
			case MysqlDefs.FIELD_TYPE_FLOAT:
				return String.valueOf(floatBinding);
			case MysqlDefs.FIELD_TYPE_DOUBLE:
				return String.valueOf(doubleBinding);
			case MysqlDefs.FIELD_TYPE_TIME:
			case MysqlDefs.FIELD_TYPE_DATE:
			case MysqlDefs.FIELD_TYPE_DATETIME:
			case MysqlDefs.FIELD_TYPE_TIMESTAMP:
			case MysqlDefs.FIELD_TYPE_VAR_STRING:
			case MysqlDefs.FIELD_TYPE_STRING:
			case MysqlDefs.FIELD_TYPE_VARCHAR:
				if (quoteIfNeeded) {
					return "'" + String.valueOf(value) + "'";
				} else {
					return String.valueOf(value);
				}
			default:
				if (value instanceof byte[]) {
					return "byte data";

				} else {
					if (quoteIfNeeded) {
						return "'" + String.valueOf(value) + "'";
					} else {
						return String.valueOf(value);
					}
				}
			}
		}
	}