package com.meidusa.amoeba.mysql.handler;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.mysql.net.MysqlClientConnection;
import com.meidusa.amoeba.mysql.net.packet.ConstantPacketBuffer;
import com.meidusa.amoeba.mysql.net.packet.result.MysqlResultSetPacket;
import com.meidusa.amoeba.net.Sessionable;
import com.meidusa.amoeba.net.poolable.ObjectPool;
import com.meidusa.amoeba.parser.statement.SelectStatement;
import com.meidusa.amoeba.parser.statement.ShowStatement;
import com.meidusa.amoeba.parser.statement.Statement;
import com.meidusa.amoeba.route.SqlQueryObject;
import com.meidusa.amoeba.util.StringUtil;

public class QueryCommand {
  private final MysqlClientConnection conn;
  private final long timeout;
  
  public QueryCommand(long timeout, MysqlClientConnection conn) {
    this.conn = conn;
    this.timeout = timeout;
  }


  public static Logger logger = Logger.getLogger(QueryCommand.class);

  
  public void execute(ObjectPool[] pools, byte[] message, Statement statement, SqlQueryObject queryObject) throws Exception {
    
    if (statement != null && statement instanceof SelectStatement && ((SelectStatement)statement).isQueryLastInsertId()) {
        MysqlResultSetPacket lastPacketResult = MySqlCommandDispatcher.createLastInsertIdPacket(conn,(SelectStatement)statement,false);
        lastPacketResult.wirteToConnection(conn);
        return;
    }
    
    if(statement instanceof ShowStatement){
        if(pools != null && pools.length>1){
            pools = new ObjectPool[]{pools[0]};
        }
    }
    
    if(pools == null || pools.length == 0){
        conn.postMessage(ConstantPacketBuffer.STATIC_OK_BUFFER);
        return;
    }
    
    Sessionable session = new QueryCommandMessageHandler(conn, message, statement, pools, queryObject, timeout);
    try {
      session.startSession();
    } catch (Exception e) {
      logger.error("start Session error:", e);
      session.endSession(false, e);
      throw e;
  }
  }
  
}
