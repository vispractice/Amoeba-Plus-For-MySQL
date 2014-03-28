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
import com.meidusa.amoeba.mysql.net.CommandInfo;
import com.meidusa.amoeba.mysql.net.MysqlClientConnection;
import com.meidusa.amoeba.mysql.net.MysqlServerConnection;
import com.meidusa.amoeba.mysql.net.packet.CommandPacket;
import com.meidusa.amoeba.mysql.net.packet.ExecutePacket;
import com.meidusa.amoeba.mysql.net.packet.OKforPreparedStatementPacket;
import com.meidusa.amoeba.mysql.net.packet.PreparedStatmentClosePacket;
import com.meidusa.amoeba.mysql.net.packet.QueryCommandPacket;
import com.meidusa.amoeba.net.Connection;
import com.meidusa.amoeba.net.poolable.ObjectPool;
import com.meidusa.amoeba.parser.statement.Statement;
import com.meidusa.amoeba.route.Request;

/**
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public class PreparedStatmentExecuteMessageHandler extends QueryCommandMessageHandler{
	
    protected PreparedStatmentInfo preparedStatmentInfo = null;
    /** 当前的请求数据包 */
    protected Map<Connection, Long>  statmentIdMap        = Collections.synchronizedMap(new HashMap<Connection, Long>());
    
	public PreparedStatmentExecuteMessageHandler(MysqlClientConnection source,PreparedStatmentInfo preparedStatmentInfo,Statement statment,byte[] query,ObjectPool[] pools, Request request, long timeout){
		super(source, query,statment, pools, request, timeout);
		this.preparedStatmentInfo = preparedStatmentInfo;
		preparedStatmentInfo.clearBuffer();
	}

	private ExecutePacket executePacket;
	
	public ExecutePacket getExecutePacket() {
		return executePacket;
	}
	public void setExecutePacket(ExecutePacket executePacket) {
		this.executePacket = executePacket;
	}
	protected void appendPreMainCommand(){
		super.appendPreMainCommand();
		
		QueryCommandPacket preparedCommandPacket = new QueryCommandPacket();
		preparedCommandPacket.command = CommandPacket.COM_STMT_PREPARE;
		preparedCommandPacket.query = preparedStatmentInfo.getSql();
		byte[] buffer = preparedCommandPacket.toByteBuffer(source).array();
		
		CommandInfo info = new CommandInfo();
		info.setBuffer(buffer);
		info.setMain(false);
		commandQueue.appendCommand(info,true);
		for(byte[] longData:this.source.getLongDataList()){
			CommandInfo longDataCommand = new CommandInfo();
			longDataCommand.setBuffer(longData);
			longDataCommand.setMain(false);
			longDataCommand.getCompletedCount().set(this.commandQueue.connStatusMap.size());
			commandQueue.appendCommand(longDataCommand,true);
		}
		source.clearLongData();
	}
	protected  void afterCommand(MysqlServerConnection conn,CommandStatus commStatus){
		super.afterCommand(conn,commStatus);
		
		if (commandType == QueryCommandPacket.COM_STMT_PREPARE) {
        	ConnectionStatuts status = this.commandQueue.connStatusMap.get(conn);
            byte[] buffer = status.buffers.get(0);
            OKforPreparedStatementPacket ok = new OKforPreparedStatementPacket();
            ok.init(buffer, source);
            statmentIdMap.put(conn, ok.statementId);
            
            if(commStatus == CommandStatus.AllCompleted){
	            for(byte[] message :status.buffers){
	            	preparedStatmentInfo.addPacket(message);
	            }
            }
        }
		
		//send close packet to mysql
		if (commandType == QueryCommandPacket.COM_STMT_EXECUTE) {
			PreparedStatmentClosePacket preparedCloseCommandPacket = new PreparedStatmentClosePacket();
	        preparedCloseCommandPacket.command = CommandPacket.COM_STMT_CLOSE;
	        preparedCloseCommandPacket.statementId = statmentIdMap.get(conn);
	        conn.postMessage(preparedCloseCommandPacket.toByteBuffer(conn));
	        if(logger.isDebugEnabled()){
	        	logger.debug("conn="+conn.getSocketId()+", close server statement id="+preparedCloseCommandPacket.statementId);
	        }
		}
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
    		if (commandType == QueryCommandPacket.COM_STMT_PREPARE) {
    			return;
    		}
    	}
    	super.dispatchMessageFrom(fromConn, message);
    }
    /**
     * 替换相应的 prepared Statment id，保存相应的数据包,并且填充 preparedStatmentInfo 的一些信息
     * 如果当前是执行并且当前阶段是prepared，则将不发送到给客户端
     */
    protected void dispatchMessageTo(Connection toConn, byte[] message) {
    	if(message != null){
	        if (toConn == source) {
	            if (commandType == QueryCommandPacket.COM_STMT_PREPARE) {
            		/*if(MysqlPacketBuffer.isOkPacket(message)){
            			OKforPreparedStatementPacket ok = new OKforPreparedStatementPacket(); 
            			ok.init(message,toConn);
            			ok.statementId = preparedStatmentInfo.getStatmentId();
            			message = ok.toByteBuffer(toConn).array();
            		}*/
	            	return;
	            }
	        } else {
	            if (commandType == CommandPacket.COM_STMT_EXECUTE || commandType == CommandPacket.COM_STMT_SEND_LONG_DATA || commandType == CommandPacket.COM_STMT_CLOSE) {
	                Long id = statmentIdMap.get(toConn);
	                message[5] = (byte) (id & 0xff);
	                message[6] = (byte) (id >>> 8);
	                message[7] = (byte) (id >>> 16);
	                message[8] = (byte) (id >>> 24);
	            }
	        }
    	}
        super.dispatchMessageTo(toConn, message);
    }

	@Override
	protected ConnectionStatuts newConnectionStatuts(Connection conn) {
		return new PreparedStatmentExecuteConnectionStatuts(conn,this.preparedStatmentInfo);
	}

	public String toString(){
		String parameter = "";
		if(executePacket.getParameters() != null){
			StringBuffer buffer = new StringBuffer();
			for(Object object :executePacket.getParameters()){
				buffer.append(object).append(",");
			}
			if(buffer.length() > 0){
				parameter = buffer.substring(0, buffer.length()-1);
			}
		}
		return super.toString() +" ,parameter=["+parameter+"]";
	}
}
