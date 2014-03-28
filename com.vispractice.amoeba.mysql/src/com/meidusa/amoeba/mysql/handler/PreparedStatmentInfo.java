/*
 * Copyright 2008-2108 amoeba.meidusa.com 
 * 
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
package com.meidusa.amoeba.mysql.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.mysql.net.packet.MysqlPacketBuffer;
import com.meidusa.amoeba.mysql.net.packet.OKforPreparedStatementPacket;
import com.meidusa.amoeba.net.DatabaseConnection;
import com.meidusa.amoeba.net.packet.AbstractPacketBuffer;
import com.meidusa.amoeba.net.packet.PacketBuffer;
import com.meidusa.amoeba.parser.ParseException;
import com.meidusa.amoeba.parser.statement.Statement;
import com.meidusa.amoeba.route.SqlBaseQueryRouter;

/**
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public class PreparedStatmentInfo {

    /**
     * 客户端发送过来的 prepared statment sql语句
     */
    private String sql;
    private Statement statment;

    private int    parameterCount;

    // cache parameterTypes
    private int[]  parameterTypes;

    /**
     * 需要返回给客户端
     */
    private byte[] packetBuffer;

    private long   statmentId;

    private List<byte[]> preparedPackets = new ArrayList<byte[]>();
    private Lock   typesLock = new ReentrantLock(false);

    public PreparedStatmentInfo(DatabaseConnection conn, long id, String preparedSql)throws ParseException{
    	SqlBaseQueryRouter router = (SqlBaseQueryRouter)ProxyRuntimeContext.getInstance().getQueryRouter();
    	statment = router.parseStatement(conn, preparedSql);
        statmentId = id;
        this.sql = preparedSql;
        parameterCount = router.parseParameterCount(conn, preparedSql);
    }
    
    public PreparedStatmentInfo (DatabaseConnection conn,long id, String preparedSql,List<byte[]> messageList) throws ParseException{
    	SqlBaseQueryRouter router = (SqlBaseQueryRouter)ProxyRuntimeContext.getInstance().getQueryRouter();
    	statment = router.parseStatement(conn, preparedSql);
    	PacketBuffer buffer = new AbstractPacketBuffer(2048);
        statmentId = id;
        this.sql = preparedSql;
        OKforPreparedStatementPacket okPaket = new OKforPreparedStatementPacket();
        okPaket.init(messageList.get(0),conn);
        okPaket.statementId = id;
        parameterCount = router.parseParameterCount(conn, preparedSql);
        messageList.remove(0);
        messageList.add(0, okPaket.toByteBuffer(conn).array());
        for(byte[] message : messageList){
        	buffer.writeBytes(message);
        }
        packetBuffer = buffer.toByteBuffer().array();
    }

    public void setParameterTypes(int[] parameterTypes) { typesLock.lock();
        try {
            this.parameterTypes = parameterTypes;
        } finally {
            typesLock.unlock();
        }
    }

    public int[] getParameterTypes() {
        typesLock.lock();
        try {
            return parameterTypes;
        } finally {
            typesLock.unlock();
        }
    }

    public int getParameterCount() {
        return parameterCount;
    }

    public byte[] getByteBuffer() {
    	if(packetBuffer == null){
    		if(preparedPackets.size() >0){
	    		PacketBuffer buffer = new AbstractPacketBuffer(2048);
	    		for(byte[] message : preparedPackets){
	            	buffer.writeBytes(message);
	            }
	            packetBuffer = buffer.toByteBuffer().array();
    		}
    	}
        return packetBuffer;
    }

    public long getStatmentId() {
        return statmentId;
    }

    public void clearBuffer(){
    	packetBuffer = null;
    	preparedPackets.clear();
    }
    public Statement getStatment(){
    	return statment;
    }
    public String getSql() {
        return sql;
    }

    public void addPacket(byte[] message){
    	if(MysqlPacketBuffer.isOkPacket(message)){
			OKforPreparedStatementPacket ok = new OKforPreparedStatementPacket(); 
			ok.init(message,null);
			ok.statementId = this.getStatmentId();
			message = ok.toByteBuffer(null).array();
		}
    	preparedPackets.add(message);
    }
    public void setSql(String preparedStatment) {
        this.sql = preparedStatment;
    }
}
