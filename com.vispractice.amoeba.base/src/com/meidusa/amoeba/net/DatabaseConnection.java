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
package com.meidusa.amoeba.net;

import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.context.ProxyRuntimeContext;

public abstract class DatabaseConnection extends AuthingableConnection{
	private static Logger logger = Logger.getLogger(DatabaseConnection.class);
	
	protected volatile String schema;
	
	/**
	 * 是否是自动提交类型
	 */
	private volatile boolean autoCommit;
	
	/**
	 * transaction isolation level
	 */
	private volatile int isolationLevel;
	
	/**
	 * 当前连接相关的终端系统(client/dbserver)所采取的字符编码
	 */
	protected String charset;
	
	
	
	public String getCharset() {
		return charset;
	}

	public boolean setCharset(String charset) {
		this.charset = charset;
		if(logger.isDebugEnabled()){
			logger.debug("set client charset="+charset);
		}
		
		return true;
	}

	
	public DatabaseConnection(SocketChannel channel, long createStamp) {
		super(channel, createStamp);
		this.autoCommit = ProxyRuntimeContext.getInstance().getRuntimeContext().isAutocommit();
		this.isolationLevel = ProxyRuntimeContext.getInstance().getRuntimeContext().getIsolationLevel();
	}



	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	/**
	 * @see {@link java.sql.Connection#getAutoCommit}
	 */
	public boolean isAutoCommit() {
		return autoCommit;
	}

	/**
	 * @see {@link java.sql.Connection#setAutoCommit}
	 */
	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit = autoCommit;
	}

	/**
	 * @see {@link java.sql.Connection#getTransactionIsolation}
	 */
	public int getIsolationLevel() {
		return isolationLevel;
	}

	/**
	 * @see {@link java.sql.Connection#setTransactionIsolation}
	 */
	public void setIsolationLevel(int isolationLevel) {
		this.isolationLevel = isolationLevel;
	}

}
