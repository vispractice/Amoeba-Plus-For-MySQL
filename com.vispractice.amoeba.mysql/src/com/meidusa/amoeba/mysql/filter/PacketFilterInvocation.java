/*
 * Copyright 2008-2108 amoeba.meidusa.com 
 * 
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
package com.meidusa.amoeba.mysql.filter;

import java.util.Iterator;
import java.util.List;

import com.meidusa.amoeba.mysql.net.MysqlClientConnection;

/**
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public abstract class PacketFilterInvocation implements FilterInvocation {
	private Iterator<IOFilter> filters;
	private byte[] byteBuffer;
	private Result resultCode;
	private boolean filted;
	private MysqlClientConnection connection;
	private byte[] resultBuffer;
	public boolean isFilted() {
		return filted;
	}

	public PacketFilterInvocation(List<IOFilter> filterList,MysqlClientConnection conn,byte[] byteBuffer){
		filters = filterList.iterator();
		this.byteBuffer = byteBuffer;
		this.connection = conn;
	}
	
	public void invoke() {
		
		if(filted) {
		       throw new IllegalStateException("FilterInvocation has already invoked");
		}
		
		if(resultCode == null){
			if(filters != null && filters.hasNext()) {
				IOFilter filter = (IOFilter) filters.next();
				try{
					try{
						filter.startFiltrate();
						resultCode = filter.doFilter(this);
						resultBuffer = filter.getFiltedResult();
					}finally{
						filter.finishFiltrate();
					}
				}finally{
					invoke();
				}
			}else {
			    resultCode = doProcess();
			}
		}else{
			if(resultCode == Result.RETURN){
				if(resultBuffer != null){
					connection.postMessage(resultBuffer);
				}
			}else if(resultCode == FilterInvocation.Result.QUIT){
				connection.postClose(null);
			}
		}
		
		if(!filted){
			filted = true;
		}
	}

	public Result getResultCode() {
		return resultCode;
	}

	public byte[] getByteBuffer() {
		return byteBuffer;
	}
	
	public void setByteBuffer(byte[] byteBuffer) {
		this.byteBuffer = byteBuffer;
	}
	
	protected abstract Result doProcess();

	public MysqlClientConnection getConnection() {
		return connection;
	}
}
