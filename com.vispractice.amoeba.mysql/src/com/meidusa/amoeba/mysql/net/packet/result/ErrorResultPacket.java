package com.meidusa.amoeba.mysql.net.packet.result;

import com.meidusa.amoeba.mysql.net.packet.ErrorPacket;
import com.meidusa.amoeba.net.Connection;

/**
 * 
 * @author struct
 *
 */
public class ErrorResultPacket implements ResultPacket{
	private boolean isError;
	private String errorMessage;
	public boolean isError() {
		return isError;
	}

	private int errorCode;
	public void setError(int errorCode,String errorMessage) {
		isError = true;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	public void wirteToConnection(Connection conn) {
		ErrorPacket packet = new  ErrorPacket(); 
		packet.packetId = 1;
		packet.errno = errorCode;
		packet.serverErrorMessage = errorMessage;
		conn.postMessage(packet.toByteBuffer(conn));
	}

}
