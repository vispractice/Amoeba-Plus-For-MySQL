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

import org.apache.log4j.Logger;

import com.meidusa.amoeba.mysql.handler.session.CommandStatus;
import com.meidusa.amoeba.mysql.handler.session.ConnectionStatuts;
import com.meidusa.amoeba.mysql.net.CommandInfo;
import com.meidusa.amoeba.mysql.net.MysqlClientConnection;
import com.meidusa.amoeba.mysql.net.MysqlServerConnection;
import com.meidusa.amoeba.mysql.net.packet.EOFPacket;
import com.meidusa.amoeba.mysql.net.packet.ErrorPacket;
import com.meidusa.amoeba.mysql.net.packet.MysqlPacketBuffer;
import com.meidusa.amoeba.mysql.net.packet.OkPacket;
import com.meidusa.amoeba.mysql.net.packet.QueryCommandPacket;
import com.meidusa.amoeba.net.Connection;
import com.meidusa.amoeba.net.packet.Packet;
import com.meidusa.amoeba.parser.statement.InsertStatement;
import com.meidusa.amoeba.parser.statement.Statement;
import com.meidusa.amoeba.util.StringUtil;

class CommandQueue{
	
	private static Logger logger = Logger.getLogger(CommandQueue.class);
	protected List<CommandInfo> sessionInitQueryQueue; //所有的从客户端发送过来的 command 队列
	protected CommandInfo currentCommand;//当前的query
	protected Map<MysqlServerConnection,ConnectionStatuts> connStatusMap = new HashMap<MysqlServerConnection,ConnectionStatuts>();
	boolean mainCommandExecuted;
	private MysqlClientConnection source;
	protected Statement statment;
	public CommandQueue(MysqlClientConnection source,Statement statment){
		this.source = source;
		this.statment = statment;
	}
	public boolean isMultiple(){
		return connStatusMap.size()>1;
	}
	
	public void clearAllBuffer(){
		Collection<ConnectionStatuts> collection = connStatusMap.values();
		for(ConnectionStatuts status : collection){
			status.clearBuffer();
		}
	}
	
	/**
	 * 尝试下一个命令，如果返回false，表示队列中没有命令了。
	 * 
	 * @return
	 */
	boolean tryNextCommandTuple(){
		if(sessionInitQueryQueue == null){
			return false;
		}else{
			if(sessionInitQueryQueue.size()>0){
				currentCommand = sessionInitQueryQueue.get(0);
				if(logger.isDebugEnabled()){
					QueryCommandPacket command = new QueryCommandPacket();
					command.init(currentCommand.getBuffer(),source);
					logger.debug(command);
				}
				return true;
			}
			return false;
		}
	}
	
	/**
	 * 判断返回的数据是否是当前命令的结束包。
	 * 当前全部连接都全部返回以后则表示当前命令完全结束。
	 * @param conn
	 * @param buffer
	 * @return
	 */
	protected  CommandStatus checkResponseCompleted(Connection conn,byte[] buffer){
		boolean isCompleted = false;
		ConnectionStatuts connStatus = connStatusMap.get(conn);
		if(connStatus == null){
			logger.error("connection Status not Found, byffer="+StringUtil.dumpAsHex(buffer, buffer.length));
		}
		try{
			connStatus.buffers.add(buffer);
			isCompleted = connStatus.isCompleted(buffer);
			/**
			 * 如果是多个连接的，需要将数据缓存起来，等待命令全部完成以后，将数据进行组装，然后发送到客户端
			 * {@link #CommandMessageHandler.mergeMessageToClient}
			 */
			
			if(isCompleted){
				//set last insert id to client connection;
				if(conn != source){
					if(connStatus.packetIndex == 0 && MysqlPacketBuffer.isOkPacket(buffer)){
						if(statment instanceof InsertStatement && currentCommand.isMain()){
							OkPacket packet = new OkPacket();
							packet.init(buffer,conn);
							if(packet.insertId>0){
								source.setLastInsertId(packet.insertId);
							}
						}
					}
				}
				
				boolean isAllCompleted = true;
				if (currentCommand.isMain()) {
				  isAllCompleted = currentCommand.getCompletedCount().incrementAndGet() == connStatusMap.size();
                }
				
				if(isAllCompleted){
					connStatus.isMerged = true;
				}
				
				if(isAllCompleted){
					Packet packet = null;
					if(MysqlPacketBuffer.isErrorPacket(buffer)){
						packet = new ErrorPacket();
					}else if(MysqlPacketBuffer.isEofPacket(buffer)){
						packet = new EOFPacket();
					}else if(MysqlPacketBuffer.isOkPacket(buffer)){
						packet = new OkPacket();
					}
					packet.init(buffer,conn);
					
					if(logger.isDebugEnabled()){
						logger.debug("returned Packet:"+packet);
					}
					return CommandStatus.AllCompleted;
					
				}else{
					return CommandStatus.ConnectionCompleted;
				}
			}else{
				return CommandStatus.ConnectionNotComplete;
			}
		}finally{
			connStatus.packetIndex ++;
		}
	}
	
	/**
	 * 是否append 成功，如果成功则表示以前曾经堆积过，需要队列来保证发送命令的循序。
	 * 如果队列中没有堆积的命令，则返回false.
	 * 否则返回true， 则表示可直接发送命令
	 * @param commandInfo
	 * @param force 强制append 命令，返回为true
	 * @return
	 */
	public synchronized  boolean appendCommand(CommandInfo commandInfo,boolean force){
		if(force){
			if(sessionInitQueryQueue == null){
				sessionInitQueryQueue = Collections.synchronizedList(new ArrayList<CommandInfo>());
			}
			if(!sessionInitQueryQueue.contains(commandInfo)){
				sessionInitQueryQueue.add(commandInfo);
			}
			return true;
		}else{
			if(sessionInitQueryQueue == null){
				return false;
			}else{
				if(sessionInitQueryQueue.size() ==0){
					return false;
				}
				if(!sessionInitQueryQueue.contains(commandInfo)){
					sessionInitQueryQueue.add(commandInfo);
				}
				return true;
			}
		}
	}
}