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
package com.meidusa.amoeba.mysql.net;

import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.exception.InitialisationException;
import com.meidusa.amoeba.mysql.net.packet.MysqlPingPacket;
import com.meidusa.amoeba.net.Connection;
import com.meidusa.amoeba.net.MessageHandler;
import com.meidusa.amoeba.net.PoolableConnectionFactory;
import com.meidusa.amoeba.util.Initialisable;

/**
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public class MysqlServerConnectionFactory extends PoolableConnectionFactory implements Initialisable{
    static Logger logger = Logger.getLogger(MysqlServerConnectionFactory.class);
    private boolean connPing = false;
	
	/**
	 * query timeout (TimeUnit:second)
	 */
	private long queryTimeout;
	
	public boolean isConnPing() {
		return connPing;
	}

	public void setConnPing(boolean connPing) {
		this.connPing = connPing;
	}

	public long getQueryTimeout() {
		return queryTimeout;
	}

	public void setQueryTimeout(long queryTimeout) {
		this.queryTimeout = queryTimeout;
	}

	@Override
	protected Connection newConnectionInstance(SocketChannel channel,
			long createStamp) {
		MysqlServerConnection conn = new MysqlServerConnection(channel,createStamp);
		conn.setQueryTimeout(this.getQueryTimeout());
		conn.setCloseable(conn.isAutoCommit());
		return conn;
	}
	
	public boolean validateObject(Object arg0) {
		boolean isValid = super.validateObject(arg0);
		if(isValid){
			if(connPing){
				MysqlServerConnection conn = (MysqlServerConnection)arg0;
				
				MessageHandler handler = conn.getMessageHandler();
				try{
					synchronized (handler) {
						PingPacketHandler pingHandler = new PingPacketHandler(handler);
						conn.setMessageHandler(pingHandler);
						conn.postMessage(new MysqlPingPacket().toByteBuffer(conn));
						try {
							handler.wait(2*1000);
						} catch (InterruptedException e) {
						  logger.error(e);
						}
						if(pingHandler.msgReturn){
							return true;
						}else{
							return false;
						}
					}
				}finally{
					conn.setMessageHandler(handler);
				}
			}else{
				return true;
			}
		}else{
			return false;
		}
	}
	
	public void init() throws InitialisationException {
		super.init();
		
		if(queryTimeout <=0){
			if(ProxyRuntimeContext.getInstance() != null && ProxyRuntimeContext.getInstance().getRuntimeContext()!= null){
				queryTimeout = ProxyRuntimeContext.getInstance().getRuntimeContext().getQueryTimeout();
			}
		}
	}
	class PingPacketHandler implements MessageHandler{
		private MessageHandler handler;
		private boolean msgReturn = false;
		PingPacketHandler(MessageHandler handler){
			this.handler = handler;
		}
		@Override
		public void handleMessage(Connection conn) {
			byte[] msg = conn.getInQueue().get();
			if(msg != null){
				msgReturn = true;
			}
			synchronized (handler) {
				handler.notifyAll();
			}
		}
		
	}
	
	
	
}
