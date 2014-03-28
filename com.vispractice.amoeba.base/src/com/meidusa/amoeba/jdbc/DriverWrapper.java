package com.meidusa.amoeba.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;


import com.alibaba.china.jdbc.common.CharsetParameter;
import com.alibaba.china.jdbc.factory.ProxyFactory;
import com.meidusa.amoeba.util.JVM;

/**
 * <pre>
 * 针对Oracle JDBC Driver的wrapper，主要解决编码转换等问题
 * 载入驱动类:Class.forName(&quot;com.alibaba.china.jdbc.SimpleDriver&quot;) 
 * URL连接格式:jdbc:oracle:thin:@10.0.65.55:1521:ocndb
 * </pre>
 * 
 * @author hexianmao 2007 四月 6 11:38:25
 */
public class DriverWrapper implements Driver {

    private ProxyFactory factory = new com.alibaba.china.jdbc.factory.SimpleProxyFactory();

    private Driver driver;

    private String clientEncoding;

	private String serverEncoding;
	
    public String getClientEncoding() {
		return clientEncoding;
	}


	public void setClientEncoding(String clientEncoding) {
		this.clientEncoding = clientEncoding;
	}


	public String getServerEncoding() {
		return serverEncoding;
	}


	public void setServerEncoding(String serverEncoding) {
		this.serverEncoding = serverEncoding;
	}
	
    public ProxyFactory getFactory() {
		return factory;
	}


	public Driver getDriver() {
		return driver;
	}


	public void setFactory(ProxyFactory factory) {
		this.factory = factory;
	}


	public void setDriver(Driver driver) {
		this.driver = driver;
	}


	public DriverWrapper(){
    }


    /**
     * <pre>
     * 检索驱动程序是否认为它可以打开到给定 URL 的连接。
     * 注意该方法在#DriverManager.getDriver(String)中被调用，用来取得相应的driver。
     * </pre>
     */
    public boolean acceptsURL(String url) throws SQLException {
        return driver.acceptsURL(url);
    }

    /**
     * 试图创建一个到给定 URL 的数据库连接。
     */
    public Connection connect(String url, Properties info) throws SQLException {
        Properties p = new Properties();
        p.putAll(info);
        Connection conn = driver.connect(url, p);

        CharsetParameter param = new CharsetParameter();
        param.setClientEncoding(this.getClientEncoding());
        param.setServerEncoding(this.getServerEncoding());
        return factory.getConnection(param, conn);
    }

    /**
     * 检索此驱动程序的主版本号。
     */
    public int getMajorVersion() {
        return driver.getMajorVersion();
    }

    /**
     * 获得此驱动程序的次版本号。
     */
    public int getMinorVersion() {
        return driver.getMajorVersion();
    }

    /**
     * 获得此驱动程序的可能属性信息。
     */
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return driver.getPropertyInfo(url, info);
    }

    /**
     * 报告此驱动程序是否是一个真正的 JDBC CompliantTM 驱动程序。
     */
    public boolean jdbcCompliant() {
        return driver.jdbcCompliant();
    }
    
    //following codes for jdk 1.7
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
	  if (JVM.is17()) {
        return ClassWrapperUtil.invoke(driver, Logger.class, "getParentLogger", (Object[])null, (Class[])null);
      }
	  else {
        throw new UnsupportedOperationException();
      }
	}

}
