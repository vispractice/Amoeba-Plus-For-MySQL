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

/**
 * 一个可表示是否通过验证的Connection
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public abstract class AuthingableConnection extends Connection implements MessageHandler{
	private static Logger logger = Logger.getLogger(AuthingableConnection.class);
	protected boolean authenticated;//是否验证通过
	private boolean authenticatedSeted = false;
	private String user;
	private String password;
	private Object authenticatLock = new Object();
	private Authenticator authenticator;
	
	public AuthingableConnection(SocketChannel channel, long createStamp){
		super(channel, createStamp);
		setMessageHandler(this);
	}

	public Authenticator getAuthenticator() {
		return authenticator;
	}

	public void setAuthenticator(Authenticator authenticator) {
		this.authenticator = authenticator;
	}

	public boolean isAuthenticated(){
		return authenticated;
	}
	
	public boolean isAuthenticatedSeted() {
		return authenticatedSeted;
	}
	
	public void setAuthenticated(boolean authenticated){
		synchronized (authenticatLock) {
			authenticatedSeted = true;
			this.authenticated = authenticated;
			authenticatLock.notifyAll();
            try {
              if (logger.isDebugEnabled()) {
                logger.debug(this.toString() + " , authenticated: " + authenticated + " (" + this.toString() + ")");
              }
            } catch (Exception e) {
              // TODO handle exception
              logger.warn(e);
            };
		}
	}
	
	protected void beforeAuthing() {
    }
	
    protected void afterAuthing(AuthResponseData data) {
        if (AuthResponseData.SUCCESS.equalsIgnoreCase(data.code)) {
            setAuthenticated(true);
            // and let our observers know about our new connection
            this._cmgr.notifyObservers(ConnectionManager.CONNECTION_ESTABLISHED, this, null);
            connectionAuthenticateSuccess(data);
        } else {
            setAuthenticated(false);
            connectionAuthenticateFaild(data);
        }
    }
	
    protected void connectionAuthenticateSuccess(AuthResponseData data) {
        if (logger.isInfoEnabled()) {
            logger.info("Connection Authenticate success [ conn=" + this + "].");
        }
    }

    protected void connectionAuthenticateFaild(AuthResponseData data) {
        if (logger.isInfoEnabled()) {
            logger.info("Connection Authenticate faild [ conn=" + this + "].");
        }
    }

    public boolean isAuthenticatedWithBlocked(long timeout) {
		synchronized (authenticatLock) {
			if (authenticatedSeted){
				return authenticated;
			}
			try {
				authenticatLock.wait(timeout);
			} catch (InterruptedException e) {
			  logger.error("authenticated waiting time out", e);
			}
	
			if (!authenticatedSeted) {
				logger.warn("authenticate to server:" + toString() + " wait for " + timeout + "s time out");
				
				// 验证等待超时的，需要统计
	            ProxyRuntimeContext.getInstance().getConnTimeOutCount().incrementAndGet();
			}
			return authenticated;
		}
	}
	
	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
