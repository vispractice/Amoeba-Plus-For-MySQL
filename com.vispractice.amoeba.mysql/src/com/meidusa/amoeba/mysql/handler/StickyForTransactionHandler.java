package com.meidusa.amoeba.mysql.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.mysql.net.MysqlClientConnection;
import com.meidusa.amoeba.mysql.net.MysqlConnection;
import com.meidusa.amoeba.mysql.net.MysqlServerConnection;
import com.meidusa.amoeba.mysql.net.packet.ConstantPacketBuffer;
import com.meidusa.amoeba.net.Connection;
import com.meidusa.amoeba.net.poolable.ObjectPool;
import com.meidusa.amoeba.parser.statement.Statement;
import com.meidusa.amoeba.route.Request;
import com.meidusa.amoeba.util.StringUtil;

/**
 * 对于事务模式的commit和rollback语句，直接发往set autocommit = 0语句后，所有路由过的连接
 * 对于前端来说，像是和后端server connection建立了某种粘性
 * 
 * 这个handler主要是处理commit、rollback、xa end、xa prepare、xa commite这些事务结束性语句
 * 这些语句不需要再次路由，直接从连接缓存map中取
 * 
 * @author WangFei
 *
 */
public class StickyForTransactionHandler extends QueryCommandMessageHandler {

  public static Logger logger = Logger.getLogger(StickyForTransactionHandler.class);


  @Override
  public synchronized void startSession() throws Exception {
    
    Collection<MysqlConnection> resources = source.getStickyConnMap().values();
    
    if(logger.isInfoEnabled()){
      logger.info("sticky session start[type="+this.command.command+"]:ip="+this.source.getSocketId()+",handlerId="+this.hashCode()
              +",time="+(System.currentTimeMillis()-createTime)
              +",request="+(getRequest() ==null ? null: getRequest().toString()));
    }
    
    // 如果管理的资源列表为空，那么直接结束掉session
    if (resources != null && resources.size() > 0) {
      StringBuilder logAllResource = new StringBuilder();
      ArrayList<String> conns = new ArrayList<String>();
      logAllResource.append((getRequest() ==null ? null: getRequest().toString()) + " to " + resources.size() + " conns ");

      for(Connection  conn : resources) {
        if (conn instanceof MysqlServerConnection) {
          
          handlerMap.put(conn, conn.getMessageHandler());
          
          if(conn.getMessageHandler() instanceof CommandMessageHandler){
              logger.error("current handler="+conn.getMessageHandler().toString()+",");
          }
          
          conn.setMessageHandler(this);
          commandQueue.connStatusMap.put((MysqlServerConnection) conn, newConnectionStatuts(conn));
          conns.add(conn.hashCode()+"");
        }
      }
      
      logAllResource.append(StringUtil.join(Arrays.asList(conns), ","));
      if (logger.isInfoEnabled()) {
        logger.info(logAllResource.toString());
      }

      this.started = true;
      appendPreMainCommand();
      this.commandQueue.appendCommand(info, true);
      startNextCommand();
    }
    else {
      source.postMessage(ConstantPacketBuffer.STATIC_OK_BUFFER);
      
      if(!isEnded()){
        synchronized (this) {
            if(!ended){
                ended = true;
            }else{
                return;
            }
        }
      }
      
      if(logger.isInfoEnabled()){
        logger.info("sticky session end[type="+this.command.command+"],but it is not executed:ip="+this.source.getSocketId()
                +",handlerId="+this.hashCode()
                +",request="+(getRequest() ==null ? null: getRequest().toString()));
      }
    }
  }
  
  public StickyForTransactionHandler(MysqlClientConnection source, byte[] query,
      Statement statment, ObjectPool[] pools, Request request, long timeout) {
    super(source, query, statment, pools, request, timeout);
  }

}
