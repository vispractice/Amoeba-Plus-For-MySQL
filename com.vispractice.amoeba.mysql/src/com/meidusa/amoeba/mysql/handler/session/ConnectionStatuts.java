package com.meidusa.amoeba.mysql.handler.session;

import java.util.ArrayList;
import java.util.List;

import com.meidusa.amoeba.mysql.net.packet.ErrorPacket;
import com.meidusa.amoeba.mysql.net.packet.MysqlPacketBuffer;
import com.meidusa.amoeba.mysql.net.packet.QueryCommandPacket;
import com.meidusa.amoeba.net.Connection;

/**
 * 描述服务端连接的状态。包括当前命令的状态,当前连接的数据包
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public abstract class ConnectionStatuts{
	public Connection conn;
	public ErrorPacket errorPacket = null;
	public ConnectionStatuts(Connection conn){
		this.conn = conn;
	}
	public int statusCode;
	public int packetIndex;
	public List<byte[]> buffers = new ArrayList<byte[]>();
	protected  byte commandType;
	public int lastStatusCode;
	public boolean isMerged;
	public boolean isCall = false;
	public void clearBuffer(){
		if(buffers != null){
			buffers.clear();
		}
	}
	
	public void setCommandType(byte commandType,boolean isCall){
		this.commandType = commandType;
		statusCode = 0;
		packetIndex = 0; 
		isMerged = false;
		this.isCall = isCall;
	}
	
	public boolean isCompleted(){
		return (statusCode & SessionStatus.COMPLETED) == SessionStatus.COMPLETED;
	}
	/**
	 * 判断从服务器端返回得数据包是否表示当前请求的结束。
	 * @param buffer
	 * @return
	 */
	public boolean isCompleted(byte[] buffer) {
		if(this.commandType == QueryCommandPacket.COM_INIT_DB){
			boolean isCompleted = false; 
			if(packetIndex == 0 && MysqlPacketBuffer.isErrorPacket(buffer)){
				statusCode |= SessionStatus.ERROR;
				statusCode |= SessionStatus.COMPLETED;
				setErrorPacket(buffer);
				isCompleted = true;
			}else if(packetIndex == 0 && MysqlPacketBuffer.isOkPacket(buffer)){
				statusCode |= SessionStatus.OK;
				statusCode |= SessionStatus.COMPLETED;
				isCompleted = true;
			}
			return isCompleted;
		}else{
			return false;
		}
	}
	
	public void setErrorPacket(byte[] buffer){
		errorPacket = new ErrorPacket();
		errorPacket.init(buffer, conn);
		errorPacket.serverErrorMessage = errorPacket.serverErrorMessage +" from mysqlServer:"+conn.getSocketId();
	}
}