package com.meidusa.amoeba.mysql.net.packet.result;

import com.meidusa.amoeba.mysql.jdbc.MysqlDefs;
import com.meidusa.amoeba.mysql.net.packet.EOFPacket;
import com.meidusa.amoeba.mysql.net.packet.FieldPacket;
import com.meidusa.amoeba.mysql.net.packet.OKforPreparedStatementPacket;
import com.meidusa.amoeba.net.Connection;

public class PreparedResultPacket extends ErrorResultPacket{
	private int parameterCount;
	private long statementId;
	
	public long getStatementId() {
		return statementId;
	}

	public void setStatementId(long statementId) {
		this.statementId = statementId;
	}

	public void setParameterCount(int count){
		this.parameterCount = count;
	}

	public int getParameterCount() {
		return parameterCount;
	}
	
	public void wirteToConnection(Connection conn) {
		if(this.isError()){
			super.wirteToConnection(conn);
		}else{
			OKforPreparedStatementPacket okPaket = new OKforPreparedStatementPacket();
			okPaket.columns = 1;
			okPaket.packetId = 1;
			byte packetId = 1;
			okPaket.parameters = parameterCount;
			okPaket.statementId = statementId;
			conn.postMessage(okPaket.toByteBuffer(conn));
			if(parameterCount>0){
				for(int i=0;i<parameterCount;i++){
					FieldPacket field = new  FieldPacket();
					field.packetId = (byte)(++packetId);
					conn.postMessage(field.toByteBuffer(conn));
				}
				EOFPacket eof = new EOFPacket();
				eof.packetId = ++packetId;
				eof.serverStatus = 2;
				conn.postMessage(eof.toByteBuffer(conn));
			}
			
			if(okPaket.columns>0){
				for(int i=0;i<okPaket.columns;i++){
					FieldPacket field = new  FieldPacket();
					field.name = "test";
					field.length = 8;
					field.type = (byte)MysqlDefs.FIELD_TYPE_VAR_STRING;
					field.packetId = (byte)(++packetId);
					conn.postMessage(field.toByteBuffer(conn));
				}
				EOFPacket eof = new EOFPacket();
				eof.packetId = ++packetId;
				eof.serverStatus = 2;
				conn.postMessage(eof.toByteBuffer(conn));
			}
		}
	}
}
