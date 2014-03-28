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

import org.apache.log4j.Logger;

import com.meidusa.amoeba.mysql.handler.session.ConnectionStatuts;
import com.meidusa.amoeba.mysql.handler.session.SessionStatus;
import com.meidusa.amoeba.mysql.net.MysqlClientConnection;
import com.meidusa.amoeba.mysql.net.packet.MysqlPacketBuffer;
import com.meidusa.amoeba.mysql.net.packet.QueryCommandPacket;
import com.meidusa.amoeba.net.Connection;
import com.meidusa.amoeba.net.poolable.ObjectPool;
import com.meidusa.amoeba.parser.statement.Statement;
import com.meidusa.amoeba.route.Request;

/**
 * Command Query 多连接消息处理
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public class QueryCommandMessageHandler extends CommandMessageHandler {

    public static Logger logger = Logger.getLogger(QueryCommandMessageHandler.class);

    static class QueryConnectionStatus extends ConnectionStatuts {
        public QueryConnectionStatus(Connection conn){
            super(conn);
        }

        /**
         * <pre>
         * Command Query:几种结束mysql Query的条件
         * 1、select 语句，第二个EofPacket到达
         * 2、当从server端返回的错误包时候
         * 3、当OK包到达
         * </pre>
         */
        @Override
        public boolean isCompleted(byte[] buffer) {
            if (this.commandType == QueryCommandPacket.COM_QUERY) {
                boolean isCompleted = false;
                if (packetIndex == 0){
                	if(MysqlPacketBuffer.isErrorPacket(buffer)){
                		statusCode |= SessionStatus.ERROR;
                        statusCode |= SessionStatus.COMPLETED;
                        lastStatusCode = SessionStatus.ERROR;
                        setErrorPacket(buffer);
                        isCompleted = true;
                	}else if(MysqlPacketBuffer.isOkPacket(buffer)){
                		 statusCode |= SessionStatus.OK;
                         statusCode |= SessionStatus.COMPLETED;
                         lastStatusCode = SessionStatus.OK;
                         isCompleted = true;
                	}else {
                        if (statusCode == SessionStatus.QUERY) {
                            statusCode |= SessionStatus.RESULT_HEAD;
                        }
                    }
                }else{
                	if(lastStatusCode == SessionStatus.EOF_FIELDS && MysqlPacketBuffer.isErrorPacket(buffer)){
                		statusCode |= SessionStatus.ERROR;
                        statusCode |= SessionStatus.COMPLETED;
                        lastStatusCode = SessionStatus.ERROR;
                        setErrorPacket(buffer);
                        isCompleted = true;
                	}else if((isCall && (lastStatusCode == SessionStatus.EOF_ROWS)) && MysqlPacketBuffer.isOkPacket(buffer)){
                		statusCode |= SessionStatus.OK;
                        statusCode |= SessionStatus.COMPLETED;
                        lastStatusCode = SessionStatus.OK;
                        isCompleted = true;
                	}else if (MysqlPacketBuffer.isEofPacket(buffer) ) {
                        if ((statusCode & SessionStatus.EOF_FIELDS) > 0) {
                            statusCode |= SessionStatus.EOF_ROWS;
                            lastStatusCode = SessionStatus.EOF_ROWS;
                            if(!isCall){
                            	statusCode |= SessionStatus.COMPLETED;
                            	isCompleted = true;
                            }
                        } else {
                            statusCode |= SessionStatus.EOF_FIELDS;
                            lastStatusCode = SessionStatus.EOF_FIELDS;
                            isCompleted = false;
                        }
                    } 
                }
                
                
              /*  if ((packetIndex == 0 || lastStatusCode == SessionStatus.EOF_FIELDS) && MysqlPacketBuffer.isErrorPacket(buffer)) {
                    statusCode |= SessionStatus.ERROR;
                    statusCode |= SessionStatus.COMPLETED;
                    lastStatusCode = SessionStatus.ERROR;
                    isCompleted = true;
                } else if ((packetIndex == 0 || (isCall && (lastStatusCode == SessionStatus.EOF_ROWS))) && MysqlPacketBuffer.isOkPacket(buffer)) {
                    statusCode |= SessionStatus.OK;
                    statusCode |= SessionStatus.COMPLETED;
                    lastStatusCode = SessionStatus.OK;
                    isCompleted = true;
                } else if (packetIndex == 0 && MysqlPacketBuffer.isFieldListPacket(buffer)) {
                	isCall = true;
                } else if (MysqlPacketBuffer.isEofPacket(buffer) ) {
                    if ((statusCode & SessionStatus.EOF_FIELDS) > 0) {
                        statusCode |= SessionStatus.EOF_ROWS;
                        lastStatusCode = SessionStatus.EOF_ROWS;
                        if(!isCall){
                        	statusCode |= SessionStatus.COMPLETED;
                        	isCompleted = true;
                        }
                    } else {
                        statusCode |= SessionStatus.EOF_FIELDS;
                        lastStatusCode = SessionStatus.EOF_FIELDS;
                        isCompleted = false;
                    }
                } else {
                    if (statusCode == SessionStatus.QUERY) {
                        statusCode |= SessionStatus.RESULT_HEAD;
                    }
                }*/
                return isCompleted;
            } else {
                return super.isCompleted(buffer);
            }
        }
    }

    public QueryCommandMessageHandler(MysqlClientConnection source, byte[] query,Statement statment, ObjectPool[] pools, Request request, long timeout){
        super(source, query,statment, pools, request, timeout);
    }

    @Override
    protected ConnectionStatuts newConnectionStatuts(Connection conn) {
        return new QueryConnectionStatus(conn);
    }

}
