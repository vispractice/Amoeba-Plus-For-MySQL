package com.meidusa.amoeba.net;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * 这儿ConnectoinManager将负责管理从这个工厂创建出来得连接.
 * 
 * @author struct
 *
 */
public abstract class AbstractConnectionFactory implements ConnectionFactory {
	private int sendBufferSize = 64;
	private int receiveBufferSize = 64;
	private boolean tcpNoDelay = true;
	private boolean keepAlive = true;
	/**
	 * 创建一个连接,初始化连接,注册到连接管理器,
	 * 
	 * @return Connection 返回该连接实例
	 */
	public Connection createConnection(SocketChannel channel, long createStamp) throws IOException {
		Connection connection = (Connection) newConnectionInstance(channel,System.currentTimeMillis());
		initConnection(connection);
		return connection;
	}
	
	/**
	 * 创建以后,在这儿将对新创建得连接做一些初始化
	 * @param connection
	 */
	protected void initConnection(Connection connection) throws IOException{
		connection.getChannel().socket().setSendBufferSize(sendBufferSize * 1024);
        connection.getChannel().socket().setReceiveBufferSize(receiveBufferSize * 1024);
        connection.getChannel().socket().setTcpNoDelay(tcpNoDelay);
        connection.getChannel().socket().setKeepAlive(keepAlive);
	}
	
	public int getSendBufferSize() {
		return sendBufferSize;
	}

	public void setSendBufferSize(int sendBufferSize) {
		this.sendBufferSize = sendBufferSize;
	}

	public int getReceiveBufferSize() {
		return receiveBufferSize;
	}

	public void setReceiveBufferSize(int receiveBufferSize) {
		this.receiveBufferSize = receiveBufferSize;
	}

	public boolean isTcpNoDelay() {
		return tcpNoDelay;
	}

	public void setTcpNoDelay(boolean tcpNoDelay) {
		this.tcpNoDelay = tcpNoDelay;
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	/**
	 * 创建连接实例
	 * @param channel
	 * @param createStamp
	 * @return
	 */
	protected abstract Connection newConnectionInstance(SocketChannel channel, long createStamp);

}
