package com.meidusa.amoeba.mysql.handler;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.mysql.net.MysqlClientConnection;
import com.meidusa.amoeba.mysql.net.packet.QueryCommandPacket;
import com.meidusa.amoeba.net.Sessionable;
import com.meidusa.amoeba.net.poolable.ObjectPool;
import com.meidusa.amoeba.parser.statement.Statement;
import com.meidusa.amoeba.route.SqlQueryObject;

public class PrepareQueryCommand {
  
  private final MysqlClientConnection conn;
  private final long timeout;
  
  public static Logger logger = Logger.getLogger(PrepareQueryCommand.class);

  public PrepareQueryCommand(long timeout, MysqlClientConnection conn) {
    this.conn = conn;
    this.timeout = timeout;
  }
  
  public void execute(ObjectPool[] pools, byte[] message, Statement statement, SqlQueryObject queryObject, QueryCommandPacket command) throws Exception {
    /**
     * 获取之前prepared过的数据，直接返回给客户端，如果没有则需要往后端mysql发起请求，
     * 然后数据以后填充PreparedStatmentInfo，并且给客户端
     */
    PreparedStatmentInfo preparedInf = conn.getPreparedStatmentInfo(command.query);
    if(preparedInf.getByteBuffer() != null && preparedInf.getByteBuffer().length >0){
        conn.postMessage(preparedInf.getByteBuffer());
        return;
    }
    
    PreparedStatmentMessageHandler handler = new PreparedStatmentMessageHandler(conn,preparedInf,statement, message , new ObjectPool[]{pools[0]}, queryObject, timeout);
    if (handler instanceof Sessionable) {
        Sessionable session = (Sessionable) handler;
        try {
            session.startSession();
        } catch (Exception e) {
            logger.error("start Session error:", e);
            session.endSession(false, e);
            throw e;
        }
    }
    return;
  }
  
}
