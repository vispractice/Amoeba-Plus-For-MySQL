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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.meidusa.amoeba.mysql.handler.session.CommandStatus;
import com.meidusa.amoeba.mysql.handler.session.ConnectionStatuts;
import com.meidusa.amoeba.mysql.net.MysqlClientConnection;
import com.meidusa.amoeba.mysql.net.MysqlServerConnection;
import com.meidusa.amoeba.mysql.net.packet.CommandPacket;
import com.meidusa.amoeba.mysql.net.packet.MysqlPacketBuffer;
import com.meidusa.amoeba.mysql.net.packet.OKforPreparedStatementPacket;
import com.meidusa.amoeba.mysql.net.packet.PreparedStatmentClosePacket;
import com.meidusa.amoeba.mysql.net.packet.QueryCommandPacket;
import com.meidusa.amoeba.net.Connection;
import com.meidusa.amoeba.net.poolable.ObjectPool;
import com.meidusa.amoeba.parser.statement.Statement;
import com.meidusa.amoeba.route.Request;

/**
 * 
 * only for prepared
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public class PreparedStatmentMessageHandler extends QueryCommandMessageHandler {
	
    protected PreparedStatmentInfo preparedStatmentInfo = null;
    /** 当前的请求数据包 */
    protected Map<Connection, Long>  statmentIdMap        = Collections.synchronizedMap(new HashMap<Connection, Long>());

    public PreparedStatmentMessageHandler(MysqlClientConnection source, PreparedStatmentInfo preparedStatmentInfo,Statement statment,
                                          byte[] query, ObjectPool[] pools, Request request, long timeout){
        super(source, query,statment, pools, request, timeout);
        this.preparedStatmentInfo = preparedStatmentInfo;
        this.preparedStatmentInfo.clearBuffer();
    }


    @Override
    protected List<byte[]> mergeResults() {
        if (commandType == QueryCommandPacket.COM_STMT_PREPARE) {
            List<byte[]> list = new ArrayList<byte[]>(16);
            Collection<ConnectionStatuts> statusList = this.commandQueue.connStatusMap.values();
            ConnectionStatuts status = statusList.iterator().next();
            list.addAll(status.buffers);
            return list;
        } else {
            return super.mergeResults();
        }
    }

    /**
     * {@inheritDoc}
     * . 替换从服务器端返回的StatementID，再发送到客户端
     */
    protected void dispatchMessageFrom(Connection fromConn,byte[] message){
    	if(fromConn != source){
    		//from mysql server
    		//cache and buffered prepared messages , send merged messages
    		if (commandType == QueryCommandPacket.COM_STMT_PREPARE) {
				preparedStatmentInfo.addPacket(message);
				if(MysqlPacketBuffer.isEofPacket(message) && commandQueue.connStatusMap.get(fromConn).isCompleted()){
					 this.source.postMessage(preparedStatmentInfo.getByteBuffer());
				}
    		}
    	}else{
    		//from client
    		super.dispatchMessageFrom(fromConn, message);
    	}
    }

    protected void afterCommand(MysqlServerConnection conn,CommandStatus commStatus) {
        super.afterCommand(conn,commStatus);
        if (commandType == QueryCommandPacket.COM_STMT_PREPARE) {
        	ConnectionStatuts status = this.commandQueue.connStatusMap.get(conn);
            byte[] buffer = status.buffers.get(0);
            OKforPreparedStatementPacket ok = new OKforPreparedStatementPacket();
            ok.init(buffer, source);
            
            //send close statement packet to mysql
            PreparedStatmentClosePacket preparedCloseCommandPacket = new PreparedStatmentClosePacket();
            preparedCloseCommandPacket.command = CommandPacket.COM_STMT_CLOSE;
            preparedCloseCommandPacket.statementId = ok.statementId;
            conn.postMessage(preparedCloseCommandPacket.toByteBuffer(conn));
            if(logger.isDebugEnabled()){
            	logger.debug("conn="+conn.getSocketId()+", close server statement id="+preparedCloseCommandPacket.statementId);
            }
        }
    }

    @Override
    protected ConnectionStatuts newConnectionStatuts(Connection conn) {
        return new PreparedStatmentConnectionStatuts(conn, this.preparedStatmentInfo);
    }

}
