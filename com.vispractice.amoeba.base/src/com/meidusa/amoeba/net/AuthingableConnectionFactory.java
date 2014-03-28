package com.meidusa.amoeba.net;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.context.ProxyRuntimeContext;

/**
 * 需要等待验证得连接工厂
 * @author struct
 *
 */
public abstract class AuthingableConnectionFactory extends AbstractConnectionFactory {
  
  private static Logger logger = Logger.getLogger(AuthingableConnectionFactory.class);
    
	public Connection createConnection(SocketChannel channel, long createStamp) throws IOException {
	    Connection connection = (Connection) super.createConnection(channel, createStamp);
		waitforAuthenticate(connection);
		
		if (logger.isDebugEnabled()) {
	      logger.debug("make backend connection success, connection=" + connection );
        }
		
		return connection;
	}

	
	
	protected void waitforAuthenticate(Connection connection){
		if(connection instanceof AuthingableConnection){ 
			AuthingableConnection authconn = (AuthingableConnection)connection;
			
			// 验证超时时间
			long authTimeOut = ProxyRuntimeContext.getInstance().getRuntimeContext().getAuthTimeOut() * 1000; 
			
			if( authTimeOut >0 ){
				authconn.isAuthenticatedWithBlocked(authTimeOut);
			}
		}else{
			connection.getConnectionManager().notifyObservers(ConnectionManager.CONNECTION_ESTABLISHED, connection, null);
		}
	}
}
