package com.meidusa.amoeba.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.net.poolable.ObjectPool;
import com.meidusa.amoeba.net.poolable.PoolableObject;

/**
 * 
 * @author struct
 *
 */
public class PoolableJdbcConnection extends ConnectionWrapper implements PoolableObject {
	private static Logger logger = Logger.getLogger(PoolableJdbcConnection.class);
	private ObjectPool objectPool;
	private boolean active;
	
	private ResultSetHandler resultSetHandler;
	
	public ResultSetHandler getResultSetHandler() {
		return resultSetHandler;
	}

	public void setResultSetHandler(ResultSetHandler ioHandler) {
		this.resultSetHandler = ioHandler;
	}

	public PoolableJdbcConnection(Connection conn){
		super(conn);
	}

	public ObjectPool getObjectPool() {
		return objectPool;
	}

	public synchronized void setObjectPool(ObjectPool pool) {
		this.objectPool = pool;
	}
	
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isRemovedFromPool() {
		return objectPool == null;
	}
	
	public void close(){
		try {
			if(isClosed()) return;
		} catch (SQLException e1) {
			logger.error("when invoke isclosed error",e1);
		}
		
		try {
			super.close();
		} catch (SQLException e) {
			logger.error("when invoke close error",e);
		}
		
		final ObjectPool tmpPool = objectPool;
		objectPool = null;
		try {
			if(tmpPool != null){
				
				/**
				 * 处于active 状态的 poolableObject，可以用ObjectPool.invalidateObject 方式从pool中销毁
				 * 否则只能等待被borrow 或者 idle time out
				 */
				if(isActive()){
					tmpPool.invalidateObject(this);
				}
			}
		} catch (Exception e) {
			// TODO handle exception
			logger.error("close pool error", e);
		}
	}


}
