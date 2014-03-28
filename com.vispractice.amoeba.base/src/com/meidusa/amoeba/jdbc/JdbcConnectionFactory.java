package com.meidusa.amoeba.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.pool.PoolableObjectFactory;

import com.meidusa.amoeba.exception.InitialisationException;
import com.meidusa.amoeba.util.Initialisable;

/**
 * jdbc driver connection factory
 * @author struct
 *
 */
public class JdbcConnectionFactory implements PoolableObjectFactory,Initialisable {
	private String url;
	private Driver driver;
	private Properties properties;
	private ResultSetHandler resultSetHandler;
	
	public void setResultSetHandler(ResultSetHandler ioHandler) {
		this.resultSetHandler = ioHandler;
	}

	public void setDriver(Driver driver) {
		this.driver = driver;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void activateObject(Object obj) throws Exception {
	}

	public void destroyObject(Object obj) throws Exception {
		if(obj instanceof Connection){
			Connection conn = (Connection)obj;
			try {
				if(!conn.isClosed()){
					conn.close();
				}
			} catch (SQLException e) {
			}
		}
	}

	public Object makeObject() throws Exception {
		PoolableJdbcConnection conn = new PoolableJdbcConnection(driver.connect(url, properties));
		conn.setResultSetHandler(resultSetHandler);
		return conn;
	}

	public void passivateObject(Object obj) throws Exception {

	}

	public boolean validateObject(Object obj) {
		if(obj instanceof Connection){
			Connection conn = (Connection)obj;
			try {
				return !conn.isClosed();
			} catch (SQLException e) {
				return false;
			}
		}
		return false;
	}

	public void init() throws InitialisationException {

	}

}
