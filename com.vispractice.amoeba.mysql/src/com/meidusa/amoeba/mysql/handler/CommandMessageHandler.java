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
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.exception.AmoebaRuntimeException;
import com.meidusa.amoeba.mysql.handler.session.CommandStatus;
import com.meidusa.amoeba.mysql.handler.session.ConnectionStatuts;
import com.meidusa.amoeba.mysql.handler.session.SessionStatus;
import com.meidusa.amoeba.mysql.jdbc.IsolationLevels;
import com.meidusa.amoeba.mysql.net.CommandInfo;
import com.meidusa.amoeba.mysql.net.MysqlClientConnection;
import com.meidusa.amoeba.mysql.net.MysqlConnection;
import com.meidusa.amoeba.mysql.net.MysqlServerConnection;
import com.meidusa.amoeba.mysql.net.packet.ConstantPacketBuffer;
import com.meidusa.amoeba.mysql.net.packet.ErrorPacket;
import com.meidusa.amoeba.mysql.net.packet.MysqlPacketBuffer;
import com.meidusa.amoeba.mysql.net.packet.OkPacket;
import com.meidusa.amoeba.mysql.net.packet.QueryCommandPacket;
import com.meidusa.amoeba.net.Connection;
import com.meidusa.amoeba.net.MessageHandler;
import com.meidusa.amoeba.net.Sessionable;
import com.meidusa.amoeba.net.packet.AbstractPacketBuffer;
import com.meidusa.amoeba.net.packet.Packet;
import com.meidusa.amoeba.net.packet.PacketBuffer;
import com.meidusa.amoeba.net.poolable.ObjectPool;
import com.meidusa.amoeba.net.poolable.PoolableObject;
import com.meidusa.amoeba.parser.statement.CommitCMD;
import com.meidusa.amoeba.parser.statement.DMLStatement;
import com.meidusa.amoeba.parser.statement.RollbackCMD;
import com.meidusa.amoeba.parser.statement.Statement;
import com.meidusa.amoeba.route.Request;
import com.meidusa.amoeba.util.Reporter;
import com.meidusa.amoeba.util.StringUtil;

/**
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public abstract class CommandMessageHandler implements MessageHandler,Sessionable,Reporter.SubReporter {
	static Logger logger = Logger.getLogger(CommandMessageHandler.class); 
	
	protected MysqlClientConnection source;
	private boolean completed;
	protected long createTime;
	protected long timeout;
	private Request request;
	private long endTime;
	protected boolean ended = false;
	protected CommandQueue commandQueue;
	private boolean forceEnded =  false; 
	private ObjectPool[] pools;
	protected CommandInfo info = new CommandInfo();
	protected byte commandType;
	protected Map<Connection,MessageHandler> handlerMap = Collections.synchronizedMap(new HashMap<Connection,MessageHandler>());
	private PacketBuffer buffer = new AbstractPacketBuffer(10240);
	protected boolean started;
	protected long lastTimeMillis = System.currentTimeMillis();
	private ErrorPacket errorPacket;
	protected Statement statment;
	protected QueryCommandPacket command = new QueryCommandPacket();
	private volatile boolean isAllConnSuccess;
	private volatile boolean isTimeout;
	
	/*
	 * 后端连接的回调，即是mysql返回的时候，处理响应的地方
	 */
	public CommandMessageHandler(final MysqlClientConnection source,byte[] query,Statement statment, ObjectPool[] pools,Request request, long timeout){
		commandQueue = new CommandQueue(source,statment);
		command.init(query,source);
		this.pools = pools;
		info.setBuffer(query);
		info.setMain(true);
		this.statment = statment;
		this.source = source;
		this.createTime = System.currentTimeMillis();
		this.timeout = timeout;
		this.request = request;
		this.isAllConnSuccess = false;
	}
	
	public boolean isMultiplayer(){
		return commandQueue.isMultiple();
	}
	/**
	 * 判断被handled的Connection 消息传送是否都完成
	 * @return
	 */
	public boolean isCompleted(){
		return completed;
	}
	
	/**
	 * 主要是为了服务端连接 与 客户端连接的环境一致（比如，当前的schema 、charset等）
	 * 
	 * 在发送主命令之前，预先需要发送一些额外的命令，比如sourceConnection、destConnection 当前的database不一致，需要发送init_db Command
	 * 为了减少复杂度，只要一个Connection需要发送命令，那么所有连接都必须发送一次相同的命令。
	 * 
	 * @param sourceMysql
	 * @param destMysqlConn
	 */
	protected void appendPreMainCommand(){
		Set<MysqlServerConnection> connSet = commandQueue.connStatusMap.keySet();
		final MysqlConnection sourceMysql =(MysqlConnection) source;
		
		for(Connection destConn : connSet){
			final MysqlConnection destMysqlConn = (MysqlConnection)destConn;
			if(!StringUtil.equalsIgnoreCase(sourceMysql.getSchema(), destMysqlConn.getSchema())){
				if(sourceMysql.getSchema() != null){
					QueryCommandPacket selectDBCommand = new QueryCommandPacket();
					selectDBCommand.query = sourceMysql.getSchema();
					selectDBCommand.command = QueryCommandPacket.COM_INIT_DB;
					
					byte[] buffer = selectDBCommand.toByteBuffer(destMysqlConn).array();
					CommandInfo info = new CommandInfo();
					info.setBuffer(buffer);
					info.setMain(false);
					info.setTarget(destMysqlConn);
					
		            // 命令结束后的回调，premain是内部产生的命令，需要自己用回调进行成功返回后的处理
					info.setRunnable(new Runnable(){
						public void run() {
						  destMysqlConn.setSchema(sourceMysql.getSchema());
					        if (logger.isInfoEnabled()) {
                              logger.info("sync schema " + sourceMysql.getSchema() + " between client " + sourceMysql + " and server " + destMysqlConn);
					        }
						}
					});
					commandQueue.appendCommand(info,true);
				}
			}
			
			if(sourceMysql.getCharset()!= null &&
                !StringUtil.equalsIgnoreCase(sourceMysql.getCharset(),destMysqlConn.getCharset())){
              QueryCommandPacket charsetCommand = new QueryCommandPacket();
              charsetCommand.query = "set names " + sourceMysql.getCharset();
              charsetCommand.command = QueryCommandPacket.COM_QUERY;
              
              byte[] buffer = charsetCommand.toByteBuffer(destMysqlConn).array();
              CommandInfo info = new CommandInfo();
              info.setBuffer(buffer);
              info.setMain(false);
              info.setTarget(destMysqlConn);
              
              info.setRunnable(new Runnable(){
                  public void run() {
                      destMysqlConn.setCharset(sourceMysql.getCharset());
                      if (logger.isInfoEnabled()) {
                        logger.info("sync charset " + sourceMysql.getCharset() + " between client " + sourceMysql + " and server " + destMysqlConn);
                      }
                    }
                  });
              commandQueue.appendCommand(info,true);
            }
			
			if(sourceMysql.isAutoCommit() != destMysqlConn.isAutoCommit()){
              QueryCommandPacket autoCommitCommand = new QueryCommandPacket();
              autoCommitCommand.query = "set autocommit = " + (sourceMysql.isAutoCommit()? "1": "0");
              autoCommitCommand.command = QueryCommandPacket.COM_QUERY;
              
              byte[] buffer = autoCommitCommand.toByteBuffer(destMysqlConn).array();
              CommandInfo info = new CommandInfo();
              info.setBuffer(buffer);
              info.setMain(false);
              info.setTarget(destMysqlConn);
              
              info.setRunnable(new Runnable(){
                  public void run() {
                    destMysqlConn.setAutoCommit(sourceMysql.isAutoCommit());
                    destMysqlConn.setCloseable(destMysqlConn.isAutoCommit());
                    if (logger.isInfoEnabled()) {
                      logger.info("sync autocommit [" + sourceMysql.isAutoCommit() + "] " + " between client " + sourceMysql + " and server " + destMysqlConn);
                    }
                  }
              });
              
              commandQueue.appendCommand(info,true);
            }
			
			if (sourceMysql.isXaActive()) {
              if (destMysqlConn.getIsolationLevel() != IsolationLevels.SERIALIZABLE) {
                QueryCommandPacket isolationLvlCommand = new QueryCommandPacket();
                isolationLvlCommand.query = getTxIsolationLevelQuery(IsolationLevels.SERIALIZABLE);
                isolationLvlCommand.command = QueryCommandPacket.COM_QUERY;
                
                byte[] buffer = isolationLvlCommand.toByteBuffer(destMysqlConn).array();
                CommandInfo info = new CommandInfo();
                info.setBuffer(buffer);
                info.setMain(false);
                info.setTarget(destMysqlConn);
                
                info.setRunnable(new Runnable(){
                  public void run() {
                    destMysqlConn.setIsolationLevel(IsolationLevels.SERIALIZABLE);
                    if (logger.isInfoEnabled()) {
                      logger.info("set server's isolation level to SERIALIZABLE");
                    }
                  }
                });
              
                commandQueue.appendCommand(info,true);
              }
            }
			else {
			  if (sourceMysql.getIsolationLevel() != destMysqlConn.getIsolationLevel()) {
	              QueryCommandPacket isolationLvlCommand = new QueryCommandPacket();
	              isolationLvlCommand.query = getTxIsolationLevelQuery(sourceMysql.getIsolationLevel());
	              isolationLvlCommand.command = QueryCommandPacket.COM_QUERY;
	              
	              byte[] buffer = isolationLvlCommand.toByteBuffer(destMysqlConn).array();
	              CommandInfo info = new CommandInfo();
	              info.setBuffer(buffer);
	              info.setMain(false);
	              info.setTarget(destMysqlConn);
	              
	              info.setRunnable(new Runnable(){
	                  public void run() {
	                    destMysqlConn.setIsolationLevel(sourceMysql.getIsolationLevel());
	                    if (logger.isInfoEnabled()) {
	                      logger.info("sync isolation level [" + sourceMysql.getIsolationLevel() + "]" + " between client " + sourceMysql + " and server " + destMysqlConn);
	                    }
	                  }
	              });
	              
	              commandQueue.appendCommand(info,true);
	            }
            }
			
			
			if (sourceMysql.isXaActive() && !destMysqlConn.isXaActive()) {
			  QueryCommandPacket xaSyncCommand = new QueryCommandPacket();
			  xaSyncCommand.query = "xa start '" + sourceMysql.cid() + "'";
			  xaSyncCommand.command = QueryCommandPacket.COM_QUERY;
              
              byte[] buffer = xaSyncCommand.toByteBuffer(destMysqlConn).array();
              CommandInfo info = new CommandInfo();
              info.setBuffer(buffer);
              info.setMain(false);
              info.setTarget(destMysqlConn);
              
              info.setRunnable(new Runnable(){
                  public void run() {
                    destMysqlConn.setIsXaActive(true);
                    if (logger.isInfoEnabled()) {
                      logger.info("active xa [" + destMysqlConn.isXaActive()+ "] " + " for server " + destMysqlConn);
                    }
                  }
              });
              
              commandQueue.appendCommand(info,true);
            }
			
			if (statment instanceof RollbackCMD || statment instanceof CommitCMD) {
              if (sourceMysql.isXaActive() && destMysqlConn.isXaActive()) {
                QueryCommandPacket xaSyncCommand = new QueryCommandPacket();
                xaSyncCommand.query = "xa end '" + sourceMysql.cid() + "'";
                xaSyncCommand.command = QueryCommandPacket.COM_QUERY;
                
                byte[] buffer = xaSyncCommand.toByteBuffer(destMysqlConn).array();
                CommandInfo info = new CommandInfo();
                info.setBuffer(buffer);
                info.setMain(false);
                info.setTarget(destMysqlConn);
                
                info.setRunnable(new Runnable(){
                    public void run() {
                      destMysqlConn.setIsXaActive(false);
                      if (logger.isInfoEnabled()) {
                        logger.info("deactive xa [" + destMysqlConn.isXaActive()+ "] " + " for server " + destMysqlConn);
                      }
                    }
                });
                
                commandQueue.appendCommand(info,true);
              }
            }
			
			if (statment instanceof CommitCMD) {
              if (sourceMysql.isXaActive()) {
                QueryCommandPacket xaSyncCommand = new QueryCommandPacket();
                xaSyncCommand.query = "xa prepare '" + sourceMysql.cid() +"'";
                xaSyncCommand.command = QueryCommandPacket.COM_QUERY;
                
                byte[] buffer = xaSyncCommand.toByteBuffer(destMysqlConn).array();
                CommandInfo info = new CommandInfo();
                info.setBuffer(buffer);
                info.setMain(false);
                info.setTarget(destMysqlConn);
                
                commandQueue.appendCommand(info,true);
              }
            }
		}
	}
	
	/**
	 * this method will be invoked after main command response completed 
	 * @param conn
	 */
	protected void afterCommand(MysqlServerConnection conn,CommandStatus commStatus){
		
	}
	
	/*
	 * 如果是发到多个物理db server上的时候，返回结果要等到所有连接完成所有命令
	 * 所有连接完成命令之后，判断是否需要合并，如果不需要，则返回最后一个完成命令的连接的返回值
	 * 
	 * 简单说，最后一个返回连接负责组装返回值，然后释放掉该连接，如果再无命令，那么调用endSession
	 * 释放所有完成的连接
	 */
	public synchronized void handleMessage(Connection fromConn) {
		byte[] message = null;
		lastTimeMillis = System.currentTimeMillis();
		if(fromConn == source){
			while((message = fromConn.getInQueue().getNonBlocking()) != null){
				CommandInfo info = new CommandInfo();
				info.setBuffer(message);
				info.setMain(true);
				
				if(!commandQueue.appendCommand(info,false)){
					dispatchMessageFrom(source,message);
				}
				logger.error("handle message from client after session started,handler="+this+", packet=\n"+StringUtil.dumpAsHex(message, message.length));
			}
			
		}else{
			while((message = fromConn.getInQueue().getNonBlocking()) != null){
				//判断命令是否完成了
				CommandStatus commStatus = commandQueue.checkResponseCompleted(fromConn, message);
				
				if(CommandStatus.AllCompleted == commStatus || CommandStatus.ConnectionCompleted == commStatus){
					
					//记录 prepared statement ID 或者 close statement
					afterCommand((MysqlServerConnection)fromConn,commStatus);
					
					if(commandQueue.currentCommand.isMain() || this.ended){
		                //mysqlServer connection return to pool
                        releaseConnection(fromConn);
					}
					if(this.ended){
						return;
					}
				}
				
				/*
				 * 所有连接完成后
				 */
				if(CommandStatus.AllCompleted == commStatus){
				    // 检测是否连接是否成功只有在所有都完成时才有意义
                    isAllConnSuccess = isFailedGet();
                    ConnectionStatuts fromConnStatus = commandQueue.connStatusMap.get(fromConn);
					
					try{
						
						/**
						 * 如果是客户端请求的命令则:
						 * 1、请求是多台server的，需要进行合并数据
						 * 2、单台server直接写出到客户端
						 */
						if(commandQueue.currentCommand.isMain()){
						  
							commandQueue.mainCommandExecuted = true;
							// 判断是否需要合并
							if(commandQueue.isMultiple()){
                                // 发送合并后的响应到client
								if(fromConnStatus.isMerged){
								    List<byte[]> list = this.mergeResults();
                                    if(list != null){
                                        for(byte[] buffer : list){
                                            dispatchMessageFrom(fromConn,buffer);
                                        }
                                    }
								}
							}
							// 不需要合并，也即是单个连接
							else{
			                    //发送响应到client
                                dispatchMessageFrom(fromConn,message);
							}
						}
						// 如果是非主命令，做错误检查
						else{
						  // 检测到错误
						  if (!isAllConnSuccess) {
						    this.commandQueue.currentCommand.setStatusCode(SessionStatus.ERROR);
                            if(!commandQueue.mainCommandExecuted){
                                this.endSession(false, null);
                            }else{
                                if(logger.isDebugEnabled()){
                                    byte[] commandBuffer = commandQueue.currentCommand.getBuffer();
                                    StringBuffer buffer = new StringBuffer();
                                    buffer.append("Current Command Execute Error:\n");
                                    buffer.append(StringUtil.dumpAsHex(commandBuffer,commandBuffer.length));
                                    Packet errorPacket = new ErrorPacket();
                                    errorPacket.init(message, fromConn);
                                    buffer.append("\n error Packet:\n");
                                    buffer.append(errorPacket.toString());
                                    logger.debug(buffer.toString());
                                }
                            }
                            // 没有检测到错误直接返回
                            return;
                          }
						}
					}
					// 无论如何，只要所有后端连接都完成后，都要清除该命令的响应buffer
					// 并开始尝试下一条命令，如果没有下一条了，那么结束
					finally{
						if(fromConnStatus.isMerged){
						  afterCommandCompleted(commandQueue.currentCommand);
						}
					}
				}
				// 单条命令完成/单条命令未完成
				else{
					if(commandQueue.currentCommand.isMain()){
						if(!commandQueue.isMultiple()){
                            dispatchMessageFrom(fromConn,message);
						}
					}
				}
			}
		}
	}
	
	/**
	 * 当一个命令结束的时候，清理缓存的数据包。并且尝试发送下一个command
	 * 如果队列中没有命令，则结束当前回话
	 * @param oldCommand 当前的command
	 */
	protected synchronized void afterCommandCompleted(CommandInfo oldCommand){
		if(this.commandQueue.currentCommand != oldCommand){
			return;
		}
		
		// 如果当前命令时执行成功的，那么就可以执行回调
        if(oldCommand.getRunnable()!= null){
          if(isAllConnSuccess) {
            oldCommand.getRunnable().run();
          }
		}
		
		commandQueue.clearAllBuffer();

		//完成命令后,将当前的命令从队列中删除,以便继续下一个任务
		commandQueue.sessionInitQueryQueue.remove(0);
		if(!ended){
		  if (isAllConnSuccess) {
            startNextCommand();
          }
		  else {
            endSession(false, null);
          }
		}
	}
	
	//判断是否需要继续发送下一条客户端命令
	//发送下一条命令
	protected synchronized void startNextCommand(){
    	if(commandQueue.currentCommand != null && (commandQueue.currentCommand.getStatusCode() & SessionStatus.ERROR) >0){
            this.endSession(false, null);
            return;
        }
    	
		// 队列中还有命令
		if(!this.ended && commandQueue.tryNextCommandTuple()){
			commandType = commandQueue.currentCommand.getBuffer()[4];
			Collection<ConnectionStatuts> connSet = commandQueue.connStatusMap.values();
			
			boolean commandCompleted = commandQueue.currentCommand.getCompletedCount().get() == commandQueue.connStatusMap.size();
			
			boolean isProcedure = false;
			if(statment instanceof DMLStatement){
				DMLStatement dmlStatement = (DMLStatement)statment;
				isProcedure = dmlStatement.isProcedure();
			}
			
			for(ConnectionStatuts status : connSet){
				if(commandQueue.currentCommand.isMain() && isProcedure){
					status.setCommandType(commandType,true);
				}else{
					status.setCommandType(commandType,false);
				}
			}
			
			// 发送命令到物理数据库
			dispatchMessageFrom(source,commandQueue.currentCommand.getBuffer());
			
			if(commandCompleted){
				afterCommandCompleted(commandQueue.currentCommand);
			}
		}
		// 队列中已经没有命令了
		else{
			this.endSession(false, null);
		}
	}
	
	/**
	 * <pre>
	 * 任何在handler里面需要发送到目标连接的数据包，都调用该方法发送出去。
	 * 从服务器端发送过来的消息到客户端，或者从客户端发送命令到各个mysql server。
	 * 
	 * 这儿主要发送的消息有2种：
	 * 1、从客户端发送过来的消息
	 * 2、reponse当前的主要命令（是客户端发出来的命令而不是该proxy内部产生的命令）的数据包
	 * 以上2种数据包通过dispatchMessage 方法发送出去的。
	 * 由内部产生的命令数据包可以在 afterCommandCompleted()之后 根据ConnectionStatus.buffers中保存。
	 * commandQueue.clearAllBuffer() 以后buffers 将被清空
	 * </pre>
	 * @param fromServer 是否是从mysql server 端发送过来的
	 * @param message 消息内容
	 */
	protected void dispatchMessageFrom(Connection fromConn,byte[] message){
		if(fromConn != source){
			dispatchMessageTo(source,message);
		}
		else{
		  
		  CommandInfo preMainCommand = commandQueue.currentCommand;
		  Connection preMainCommandSpecifiedDestServer = null;
		  if (preMainCommand != null && !preMainCommand.isMain()) {
            preMainCommandSpecifiedDestServer = preMainCommand.getTarget();
          }
		  
		  // 对于非主命令，只需要按指定的后端地址来转发，而不需要全部转发
		  if (preMainCommandSpecifiedDestServer != null) {
            dispatchMessageTo(preMainCommandSpecifiedDestServer, message);
          }
          else {
            Collection<MysqlServerConnection> connSet =  commandQueue.connStatusMap.keySet();
            for(Connection conn : connSet){
              dispatchMessageTo(conn,message);
            }
          }
		}
	}
	
	/**
	 * 这儿将启动一些缓存机制，避免小数据包频繁调用 系统write, CommandMessageHandler类或者其子类必须通过该方法发送数据包
	 * @param toConn
	 * @param message
	 */
	protected void dispatchMessageTo(Connection toConn,byte[] message){
      
      if(toConn == source){
        if(message != null){
          appendBufferToWrite(message,buffer,toConn,false);
        }else{
            appendBufferToWrite(message,buffer,toConn,true);
        }
      }
      else{
          toConn.postMessage(message);
      }
    }
	
	/**
	 * 缓冲写数据到目的地
	 * @param byts
	 * @param buffer
	 * @param conn
	 * @param writeNow
	 * @return
	 */
	private synchronized boolean appendBufferToWrite(byte[] byts,PacketBuffer buffer,Connection conn,boolean writeNow){
		if(byts == null){
			if(buffer.getPosition()>0){
				conn.postMessage(buffer.toByteBuffer());
				buffer.reset();
			}
			return true;
		}else{
			if(writeNow || buffer.remaining() < byts.length){
				if(buffer.getPosition()>0){
					buffer.writeBytes(byts);
					conn.postMessage(buffer.toByteBuffer());
					buffer.reset();
				}else{
					conn.postMessage(byts);
				}
				return true;
			}else{
				buffer.writeBytes(byts);
				return true;
			}
		}
	}
	
	// 释放单个返回的后端连接
    protected synchronized void releaseConnection(Connection connection) {
      MessageHandler handler = handlerMap.remove(connection);
      if (handler != null) {
        connection.setMessageHandler(handler);
      }
  
      if (source.isAutoCommit()) {
        if (!connection.isClosed()) {
          if (connection instanceof MysqlServerConnection) {
            PoolableObject pooledObject = (PoolableObject) connection;
            if (pooledObject.getObjectPool() != null && pooledObject.isActive()) {
              try {
                pooledObject.getObjectPool().returnObject(connection);
                if (logger.isDebugEnabled()) {
                  logger.debug("connection:" + connection + " return to pool");
                }
              } catch (Exception e) {
                // TODO handle exception
                logger.warn(e);
              }
            }
          }
        }
      }
    }
	
  /**
   * 释放Session相关的连接
   */
  protected void releaseSessionConnection() {
    Set<Map.Entry<Connection, MessageHandler>> handlerSet = handlerMap.entrySet();
    for (Map.Entry<Connection, MessageHandler> entry : handlerSet) {
      MessageHandler handler = entry.getValue();
      Connection connection = entry.getKey();
      ConnectionStatuts status = this.commandQueue.connStatusMap.get(connection);

      // 非事务模式下重置message handler和把完成的连接返回pool，未完成的连接通过超时空闲机制返回pool或关闭
      if (source.isAutoCommit()) {
        if (this.commandQueue.currentCommand == null || !isStarted()
            || (status != null && (status.statusCode & SessionStatus.COMPLETED) > 0)) {
          
          connection.setMessageHandler(handler);

          if (!connection.isClosed()) {
            if (connection instanceof MysqlServerConnection) {
              PoolableObject pooledObject = (PoolableObject) connection;
              if (pooledObject.getObjectPool() != null && pooledObject.isActive()) {
                try {
                  pooledObject.getObjectPool().returnObject(connection);
                  if (logger.isDebugEnabled()) {
                    logger.debug("connection:" + connection + " return to pool");
                  }
                } catch (Exception e) {
                  // TODO handle exception
                  logger.warn(e);
                }
              }
            }
          }
        }
      }
      // 事务模式只要重置message handler，超时空闲机制对事务模式无效
      else {
        connection.setMessageHandler(handler);
      }
    }
  }
	
  // after拦截，做log时，合并多个连接上的错误信息
  protected synchronized String mergeErrorMessage() {
    Collection<ConnectionStatuts> connectionStatutsSet = commandQueue.connStatusMap.values();
    
    StringBuilder errMsg = new StringBuilder();
    String msg = "unKnown";
    int errno = -1;
    
    for(ConnectionStatuts connStatus : connectionStatutsSet){
      if(connStatus.errorPacket != null) {
        errno = connStatus.errorPacket.errno;
        msg = connStatus.errorPacket.serverErrorMessage;
        errMsg.append("[" + connStatus.conn.getInetAddress() + " errno: " + errno + " err msg " + msg + "]");
      }
    }
    
    return errMsg.toString();
  }
  
	/**
	 * 合并多服务端的消息，发送到客户端
	 * 只有在多连接的情况下需要进行数据包聚合，聚合以后逐一将数据包通过 {@link #dispatchMessageFrom(Connection, byte[])}方法发送出去,
	 * 一对一的连接直接通过{@link #dispatchMessageFrom(Connection, byte[])} 方法 直接发送出去,而不需要merge。
	 * @return
	 */
	protected synchronized List<byte[]> mergeResults(){
		if(this.commandQueue.currentCommand.isMerged()){
			return null;
		}
		this.commandQueue.currentCommand.setMerged(true);
		Collection<ConnectionStatuts> connectionStatutsSet = commandQueue.connStatusMap.values();
		
		
		/**
		 * 表示是否返回具有查询结果的请求
		 */
		boolean isSelectQuery = true;
		boolean isCall = false;
		boolean isRead = false;
		
		List<byte[]> buffers = null;
		List<byte[]> returnList = new ArrayList<byte[]>();
		
		for(ConnectionStatuts connStatus : connectionStatutsSet){
          //看是否每个服务器返回的数据包都没有异常信息。
         if(connStatus.buffers.size() ==0){
             for(ConnectionStatuts connStatus1 : connectionStatutsSet){
                 
                 StringBuffer buffer = new StringBuffer();
                 
                 buffer.append("<---connection="+connStatus1.conn.getInetAddress()+"=="+connStatus1.conn.getInetAddress()+"------->\n");
                 for(byte[] buf : connStatus1.buffers){
                     buffer.append(StringUtil.dumpAsHex(buf,buf.length)+"\n");
                     buffer.append("------------\n");
                 }
                 buffer.append("<----error Packet:"+connStatus1.conn.getInetAddress()+"------>\n");
                 logger.error(buffer.toString());
             }
             continue;
         }
         
         buffers = connStatus.buffers;
         
         // 发现错误，则只返回第一个发现的错误buffers，和after拦截需要合并错误信息略有不同
         if((connStatus.statusCode & SessionStatus.ERROR) >0){
             return buffers;
         }
       }
		
		// 只有DML Statement才有可能做合并, Property Statement、DDL、Database Administration Statements 等语句直接返回一个连接的结果就可以了
		if (commandQueue.statment instanceof DMLStatement) {
			DMLStatement dmlStatement = (DMLStatement)commandQueue.statment;
			if(this.commandQueue.currentCommand.isMain()){
				isCall = dmlStatement.isProcedure();
				isRead = dmlStatement.isRead();
			}
			
			//以下是判断是否是更新操作还是查询操作
			if(!isCall){
				isSelectQuery = MysqlPacketBuffer.isEofPacket(buffers.get(buffers.size()-1));
			}else{
				isSelectQuery = !MysqlPacketBuffer.isOkPacket(buffers.get(0));
			}
			
			// 如果是读语句
			if (isRead) {
				// 非存储过程的select语句
				if (!isCall && isSelectQuery) {
					// 当前的packetId
					byte paketId = 0;

					// 发送field信息
					for (byte[] buffer : buffers) {
						if (MysqlPacketBuffer.isEofPacket(buffer)) {
							returnList.add(buffer);
							paketId = buffer[3];
							break;
						} else {
							returnList.add(buffer);
							paketId = buffer[3];
						}
					}
					paketId += 1;

					// 发送rows数据包
					for (ConnectionStatuts connStatus : connectionStatutsSet) {
						boolean rowStart = false;
						boolean isRowEnd = false;
						for (byte[] buffer : connStatus.buffers) {
							if (!rowStart) {
								if (MysqlPacketBuffer.isEofPacket(buffer)) {
									rowStart = true;
								} else {
									continue;
								}
							} else {
								if (!MysqlPacketBuffer.isEofPacket(buffer)) {
									if (!isRowEnd) {
										buffer[3] = paketId;
										paketId += 1;
										returnList.add(buffer);
									}
								} else {
									isRowEnd = true;
								}
							}
						}
					}

					byte[] eofBuffer = buffers.get(buffers.size() - 1);
					eofBuffer[3] = paketId;
					returnList.add(eofBuffer);
					
					return returnList;
				}
			} 
			// 如果是写语句
			else {
				// update/insert等要计算影响了几行
				if (!isCall && !isSelectQuery) {
					OkPacket ok = new OkPacket();
					StringBuffer strbuffer = new StringBuffer();
					for (ConnectionStatuts connStatus : connectionStatutsSet) {
						byte[] buffer = connStatus.buffers
								.get(connStatus.buffers.size() - 1);
						OkPacket connOK = new OkPacket();
						connOK.init(buffer, connStatus.conn);
						ok.affectedRows = connOK.affectedRows;
						ok.insertId = connOK.insertId;
						ok.packetId = 1;
						
						if (connOK.message != null) {
	                      strbuffer.append(connOK.message);
                        }
						ok.warningCount = connOK.warningCount;
					}
					ok.message = strbuffer.toString();
					returnList.add(ok.toByteBuffer(source).array());
					
					return returnList;
				}
			}
		}
		
		return buffers;
	}

	protected abstract ConnectionStatuts newConnectionStatuts(Connection conn);

	public boolean isStarted(){
		return this.started;
	}
	
	public synchronized void startSession() throws Exception {
		if(logger.isInfoEnabled()){
			logger.info("session start[type="+this.command.command+"]:ip="+this.source.getSocketId()+",handlerId="+this.hashCode()
					+",time="+(System.currentTimeMillis()-createTime)
					+",request="+(getRequest() ==null ? null: getRequest().toString()));
		}
		
		Map<String, Boolean> haveSentToThesePools = new HashMap<String, Boolean>();
		
		for(ObjectPool pool:pools){
			
		    MysqlServerConnection conn = null;
            
		    // 读请求需要服用上次使用的pool
            if (request.isRead()) {
                pool = source.getLastReadPool(pool);
            } 
            
            if (pool != null && !haveSentToThesePools.containsKey(pool.getName())) {

              haveSentToThesePools.put(pool.getName(), true);
              
              if (!source.isAutoCommit()) {
                // 优先从client connection中取后端连接
                conn = (MysqlServerConnection) source.getStickyConnMap().get(pool);
              }
              
              // 如果还没有缓存，从连接池中换取连接，然后加入缓存
              // 非事务模式肯定总是从池中拿
              if (conn == null) {
                conn = (MysqlServerConnection) pool.borrowObject();
                
                if (conn == null) {
                  throw new AmoebaRuntimeException("can not establishe db server connection ");
                }
              }
              
              if (!source.isAutoCommit()) {
                source.bindTransaction(pool, conn);
                
                if (logger.isInfoEnabled()) {
                  logger.info("cache dest [" + conn.hashCode() + "], have cached " + source.getStickyConnMap().size() + " conns for client " + source.hashCode());
                }
            }
              
              if (logger.isDebugEnabled()) {
                  logger.debug(this.command.query + " " + pool.getName() + " " + conn.getSocketId());
              }
              
              // 先缓存当前的MessageHandler，以便在结束session的时候恢复这些MessageHandler
              handlerMap.put(conn, conn.getMessageHandler());
              
              if(conn.getMessageHandler() instanceof CommandMessageHandler){
                  logger.error("current handler="+conn.getMessageHandler().toString()+",");
              }
              
              // 设置connection的回调，即处理响应的handler，以便调用handler的handleMessage方法
              conn.setMessageHandler(this);
              
              // 如果需要发往多个后端连接，那么在处理响应的时候，需要检查每个后端连接是否成功
              // 所以这里需要保存所有后端连接的列表
              commandQueue.connStatusMap.put(conn, newConnectionStatuts(conn));
          }
		}
		
	    if (logger.isInfoEnabled()) {
	        Collection<String> pools = haveSentToThesePools.keySet();
	        if (logger.isInfoEnabled()) {
	          logger.info(request.toString() + " dispatch to " + StringUtil.join(pools, ","));
	        }
	    }
		
		this.started = true;
		
		// 在发送主命令前，需要做一些设置同步，比如字符编码，autocommit等，这些都不是主命令
		appendPreMainCommand();
		this.commandQueue.appendCommand(info, true);
		
		// 开始发命令，如果检测到已无命令，那么结束session
		startNextCommand();
	}
	
	public boolean checkIdle(long now) {
		if(timeout >0){
			isTimeout = (now - createTime)>timeout;
		}else{
			if(ended){
				/**
				 * 如果该session已经结束，此时如果serverConnection端还在等待所有数据访问。并且超过15s,则需要当空闲的会话
				 * 避免由于各种原因造成服务器端没有发送数据或者已经结束的会话而ServerConnection无法返回Pool中。
				 */
				isTimeout = (now - endTime)>15000;
			}else{
				isTimeout = (now - lastTimeMillis) > ProxyRuntimeContext.getInstance().getRuntimeContext().getQueryTimeout() * 1000;
			}
		}
		
		if (isTimeout) {
          endSession(false, new AmoebaRuntimeException("Session: " + this + " time out"));
        }
		
		return isTimeout;
	}
	
    //看是否每个服务器返回的数据包都没有异常信息。
	private boolean isFailedGet() {
	    Collection<ConnectionStatuts> connectionStatutsSet = commandQueue.connStatusMap.values();
	    
	    for(ConnectionStatuts connStatus : connectionStatutsSet){
	      if (connStatus.errorPacket != null) {
	        this.errorPacket = connStatus.errorPacket;
	        return false;
          }
	    }
	    
	    return true;
	  }

	public  void endSession(boolean force, Exception trigger) {
	  
	 try {
	   // 避免重复关闭session
       if (isEnded()) {
         return;
       }
     
       if(!isEnded()){
           synchronized (this) {
               if(!ended){
                   forceEnded = force;
                   endTime = System.currentTimeMillis();
                   ended = true;
               }else{
                   return;
               }
           }
       }
       
       this.releaseSessionConnection();
       
       if(!this.commandQueue.mainCommandExecuted){
           StringBuilder exceptionMsg = new StringBuilder("session was killed before {" + this.command.query + "} executed. cause:");
           String cause = "amoeba fatal inner error";
           
           if(this.errorPacket == null){
             errorPacket = new ErrorPacket();
             // 非标准mysql错误码
             errorPacket.errno = 9999;
             errorPacket.packetId = 2;
             errorPacket.sqlstate = "42000";
             if (trigger != null) {
              cause = trigger.getMessage();
             }
           }
           else {
             cause = errorPacket.serverErrorMessage;
           }
           
           errorPacket.serverErrorMessage = exceptionMsg.append(cause).toString();
           this.dispatchMessageTo(source, errorPacket.toByteBuffer(source).array());
           // 简单错误log信息
           logger.error(exceptionMsg.toString());
           
           try {
             // 构造详细的错误log信息
             StringBuffer buffer = new StringBuffer();
             buffer.append("\n");
             buffer.append("<<---\n").append("client connection=").append(source.getSocketId()).append("\n");
             buffer.append("source handler ischanged=").append((source.getMessageHandler()==this)).append("\n");
             buffer.append("session Handler "+this);

             for(Map.Entry<MysqlServerConnection, ConnectionStatuts> entry : commandQueue.connStatusMap.entrySet()){
                 if((entry.getValue().statusCode & SessionStatus.COMPLETED) == 0){
                     buffer.append("<----start-connection="+entry.getKey().getSocketId()
                             +",queueSize="+entry.getKey().getInQueueSize()
                             +",manager="+entry.getKey().getConnectionManager().getName() 
                             +",managerRunning="+entry.getKey().getConnectionManager().isRunning()
                             +",selectorOpened="+entry.getKey().getConnectionManager().getSelector().isOpen()+"-------\n");
                     for(byte[] buf : entry.getValue().buffers){
                         buffer.append(StringUtil.dumpAsHex(buf,buf.length)+"\n");
                         buffer.append("\n");
                     }
                     buffer.append("<----end connection:"+entry.getKey().getSocketId()+", NOT COMPLETE------>\n");
                 }
                 else{
                     if ((entry.getValue().statusCode & SessionStatus.ERROR) > 0) {
                       buffer.append("<----start -- end Packet:"+entry.getKey().getSocketId()+", Failed ------>\n");   
                     }
                     else {
                       buffer.append("<----start -- end Packet:"+entry.getKey().getSocketId()+", Success------>\n");   
                    }
                 }
                 
                 if(force){
                   entry.getKey().postClose(null);
                 }
                 
             }
             
             buffer.append("----->>\n");
             logger.error(buffer.toString());
             
             if (force) {
               source.postClose(null);
             }

          } catch (Exception e) {
            logger.error("make detail log for {" + exceptionMsg + "} failed");
          }
       }
       else{
             if(logger.isInfoEnabled()){
                 logger.info("session end[type="+this.command.command+"]:ip="+this.source.getSocketId()
                         +",handlerId="+this.hashCode()
                         +",request="+(getRequest() ==null ? null: getRequest().toString()));
             }
         }
     } 
	 finally {
	    // 非事务模式下释放资源
        if (source.isAutoCommit()) {
          /**
           *  虽然自动提交的连接不会缓存资源
           *  但是有可能出现先autocommit=0，执行一些语句之后，
           *  然后再autocommit=1，这时候需要清空之前缓存的资源
           *  
           *  因为autocommit=1之后用的连接都是从连接池里重新拿，所以不用担心
           *  缓存的资源和正在使用的连接有冲突
           */
          if (statment instanceof CommitCMD) {
            source.clearTransaction(isAllConnSuccess && !isTimeout);
          }
          else {
            source.clearTransaction(true);
          }
        }
        // 事务模式下释放资源
        else {
          if (statment instanceof CommitCMD || statment instanceof RollbackCMD) {
            // 超时处理
            source.clearTransaction(isAllConnSuccess && !isTimeout);
          }
          // TODO： 其他语句怎么释放资源
        }
        
        this.dispatchMessageTo(source,null);
      }
	}
	

	public synchronized boolean isEnded() {
		return this.ended;
	}
	
	public void appendReport(StringBuilder buffer, long now, long sinceLast,boolean reset,Level level) {
		buffer.append("    -- MessageHandler:").append("multiple Size:").append(commandQueue.connStatusMap.size());
		if(commandQueue.currentCommand != null){
			buffer.append(",currentCommand completedCount:");
			buffer.append(commandQueue.currentCommand.getCompletedCount()).append("\n");
		}else{
			buffer.append("\n");
		}
	}
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append("class=").append(this.getClass().getName()).append("\n");
	    buffer.append("handlerid=").append(this.hashCode()).append("\n");
		buffer.append("createTime=").append(createTime).append("\n");
		buffer.append("endTime=").append(this.endTime).append("\n");
		buffer.append("lastTimeMillis=").append(this.lastTimeMillis).append("\n");
		buffer.append("ended=").append(this.ended ).append("\n");
		buffer.append("forceEnded=").append(this.forceEnded ).append("\n");
		buffer.append("started=").append(this.started ).append("\n");
		buffer.append("ServerConnectionSize=").append(this.handlerMap.size()).append("\n");
	    buffer.append("sql=").append(statment!= null?statment.getSql():"null").append("\n");

		if(commandQueue.currentCommand != null){
			buffer.append("currentCommand[").append("CompletedCount=").append(this.commandQueue.currentCommand != null ?this.commandQueue.currentCommand.getCompletedCount().get():"");
			buffer.append("buffer=\n").append(StringUtil.dumpAsHex(commandQueue.currentCommand.getBuffer(),commandQueue.currentCommand.getBuffer().length));
		}
		return buffer.toString();
	}

    public Request getRequest() {
      return request;
    }
    
    public boolean isSuccess() {
      return isAllConnSuccess && !isTimeout;
    }

    public String getTxIsolationLevelQuery(int isolationLevl) {
      switch (isolationLevl) {
        case IsolationLevels.READ_UNCOMMITTED:
          return ConstantPacketBuffer._READ_UNCOMMITTED;
        case IsolationLevels.READ_COMMITTED:
          return ConstantPacketBuffer._READ_COMMITTED;
        case IsolationLevels.REPEATED_READ:
          return ConstantPacketBuffer._REPEATED_READ;
        case IsolationLevels.SERIALIZABLE:
          return ConstantPacketBuffer._SERIALIZABLE;
        default:
          throw new AmoebaRuntimeException("unknown isolation level");
      }
    }
}
