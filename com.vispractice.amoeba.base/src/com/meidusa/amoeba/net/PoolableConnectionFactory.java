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

import org.apache.commons.pool.PoolableObjectFactory;


/**
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public abstract class PoolableConnectionFactory extends BackendConnectionFactory implements PoolableObjectFactory{

	public void activateObject(Object arg0) throws Exception {

	}

	public void destroyObject(Object arg0) throws Exception {
		if(arg0 instanceof Connection){
			Connection connection = (Connection)arg0;
			connection.postClose(null);
		}
	}
	
	/**
	 * 在非阻塞方式中,如果需要验证连接的身份,则必须采用阻塞的方式来等待验证完成.如果在指定的时间内无法完成连接验证,则新创建的连接将被销毁.
	 */
	public Object makeObject() throws Exception {
		Connection connection = (Connection) createConnection(socketChannelFactory.createSokectChannel(),System.currentTimeMillis());
		return connection;
	}
	
	public void passivateObject(Object arg0) throws Exception {

	}

	public boolean validateObject(Object arg0) {
		boolean validated = true;
		if(arg0 instanceof Connection){
			Connection connection = (Connection)arg0;
			if(connection instanceof AuthingableConnection){
				AuthingableConnection authConn = (AuthingableConnection)connection;
				validated = validated && authConn.isAuthenticated();
			}
			validated = validated && !connection.isClosed();
		}else{
			validated = false;
		}
		return validated;
	}

}
