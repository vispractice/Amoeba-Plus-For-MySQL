package com.meidusa.amoeba.mysql.net.packet.result;

import java.util.ArrayList;
import java.util.List;

import com.meidusa.amoeba.mysql.net.packet.EOFPacket;
import com.meidusa.amoeba.mysql.net.packet.FieldPacket;
import com.meidusa.amoeba.mysql.net.packet.ResultSetHeaderPacket;
import com.meidusa.amoeba.mysql.net.packet.RowDataPacket;
import com.meidusa.amoeba.net.Connection;
import com.meidusa.amoeba.net.packet.AbstractPacketBuffer;
import com.meidusa.amoeba.net.packet.PacketBuffer;

/**
 * 
 * @author struct
 *
 */
public class MysqlResultSetPacket extends ErrorResultPacket {
	
	public ResultSetHeaderPacket resulthead;
	public FieldPacket[] fieldPackets;
	private List<RowDataPacket> rowList;
	private boolean isPrepared;
	
	public boolean isPrepared() {
		return isPrepared;
	}

	public void setPrepared(boolean isPrepared) {
		this.isPrepared = isPrepared;
	}

	private byte[] content;
	
	public void setContent(byte[] content) {
		this.content = content;
	}

	public MysqlResultSetPacket(String query){
		
	}
	
	public void setRowList(List<RowDataPacket> rows){
		this.rowList = rows;
	}
	
	public synchronized void addRowDataPacket(RowDataPacket row){
		if(rowList == null){
			rowList = new ArrayList<RowDataPacket>();
		}
		rowList.add(row);
	}
	
	/* (non-Javadoc)
	 * @see com.meidusa.amoeba.aladdin.io.ResultPacket#wirteToConnection(com.meidusa.amoeba.net.Connection)
	 */
	public void wirteToConnection(Connection conn){
		if(isError()){
			super.wirteToConnection(conn);
			return;
		}
		if(content != null){
			conn.postMessage(content);
			return;
		}
		
		PacketBuffer buffer = new AbstractPacketBuffer(2048);
		byte paketId = 1;
		resulthead.packetId = paketId++;
		
		//write header bytes
		AbstractPacketBuffer.appendBufferToWrite(resulthead.toByteBuffer(conn).array(),buffer,conn,false);
		
		//write fields bytes
		if(fieldPackets != null){
			for(int i=0;i<fieldPackets.length;i++){
				fieldPackets[i].packetId = paketId++;
				AbstractPacketBuffer.appendBufferToWrite(fieldPackets[i].toByteBuffer(conn).array(),buffer,conn,false);
			}
		}
		
		//write eof bytes
		EOFPacket eof = new EOFPacket();
		eof.serverStatus = 2;
		eof.warningCount = 0;
		eof.packetId = paketId++;
		AbstractPacketBuffer.appendBufferToWrite(eof.toByteBuffer(conn).array(),buffer,conn,false);
		
		if(rowList != null && rowList.size()>0){
			//write rows bytes
			for(RowDataPacket row : rowList){
				row.packetId = paketId++;
				AbstractPacketBuffer.appendBufferToWrite(row.toByteBuffer(conn).array(),buffer,conn,false);
			}
			
		}
		
		//write eof bytes
		eof.packetId = paketId++;
		AbstractPacketBuffer.appendBufferToWrite(eof.toByteBuffer(conn).array(),buffer,conn,true);
	}
	
}
