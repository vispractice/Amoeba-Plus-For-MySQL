package com.meidusa.amoeba.mysql.handler;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.mysql.net.MysqlClientConnection;
import com.meidusa.amoeba.mysql.net.packet.ErrorPacket;
import com.meidusa.amoeba.mysql.net.packet.ExecutePacket;
import com.meidusa.amoeba.mysql.net.packet.LongDataPacket;
import com.meidusa.amoeba.mysql.net.packet.result.MysqlResultSetPacket;
import com.meidusa.amoeba.net.Sessionable;
import com.meidusa.amoeba.net.poolable.ObjectPool;
import com.meidusa.amoeba.parser.statement.SelectStatement;
import com.meidusa.amoeba.parser.statement.Statement;
import com.meidusa.amoeba.route.SqlBaseQueryRouter;
import com.meidusa.amoeba.route.SqlQueryObject;

public class PrepareExecuteCommand {
  private final MysqlClientConnection conn;
  private final long timeout;
  private final SqlBaseQueryRouter router;
  
  public static Logger logger = Logger.getLogger(PrepareExecuteCommand.class);

  public PrepareExecuteCommand(long timeout, MysqlClientConnection conn, SqlBaseQueryRouter router) {
    this.conn = conn;
    this.timeout = timeout;
    this.router = router;
  }
  
  public void execute(byte[] message, Statement statement) throws Exception {
    try{
      long statmentId = ExecutePacket.readStatmentID(message);
      PreparedStatmentInfo preparedInf = conn.getPreparedStatmentInfo(statmentId);
      if (preparedInf == null) {
          ErrorPacket error = new ErrorPacket();
          error.errno = 1044;
          error.packetId = 1;
          error.sqlstate = "42000";
          error.serverErrorMessage = "Unknown prepared statment id=" + statmentId;
          conn.postMessage(error.toByteBuffer(conn).array());
          logger.warn("Unknown prepared statment id:" + statmentId);
      } else {
          Statement statment = preparedInf.getStatment();
          if (statment != null && statment instanceof SelectStatement && ((SelectStatement)statment).isQueryLastInsertId()) {
              MysqlResultSetPacket lastPacketResult = MySqlCommandDispatcher.createLastInsertIdPacket(conn,(SelectStatement)statment,true);
              lastPacketResult.wirteToConnection(conn);
              return;
          }
          
          Map<Integer, Object> longMap = new HashMap<Integer, Object>();
          for (byte[] longdate : conn.getLongDataList()) {
              LongDataPacket packet = new LongDataPacket();
              packet.init(longdate, conn);
              longMap.put(packet.parameterIndex, packet.data);
          }

          ExecutePacket executePacket = new ExecutePacket(preparedInf, longMap);
          executePacket.init(message, conn);

          SqlQueryObject queryObject = new SqlQueryObject();
          queryObject.isPrepared = false;
          queryObject.sql = preparedInf.getSql();
          queryObject.parameters = executePacket.getParameters();
          
          ObjectPool[] pools = router.doRoute(conn, queryObject, statement);

        PreparedStatmentExecuteMessageHandler handler =
            new PreparedStatmentExecuteMessageHandler(conn, preparedInf, statment, message, pools,
                queryObject, timeout);
        
          handler.setExecutePacket(executePacket);
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
      }
    } finally {
      conn.clearLongData();
    }
 }
}
