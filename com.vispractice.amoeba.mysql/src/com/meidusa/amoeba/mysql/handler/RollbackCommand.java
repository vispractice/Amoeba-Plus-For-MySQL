package com.meidusa.amoeba.mysql.handler;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.mysql.net.MysqlClientConnection;
import com.meidusa.amoeba.mysql.net.packet.QueryCommandPacket;
import com.meidusa.amoeba.net.Sessionable;
import com.meidusa.amoeba.parser.statement.Statement;
import com.meidusa.amoeba.parser.statement.XARollBackStatement;
import com.meidusa.amoeba.route.SqlQueryObject;

public class RollbackCommand {
  private MysqlClientConnection source;
  private long timeout;
  
  public static Logger logger = Logger.getLogger(RollbackCommand.class);

  
  public RollbackCommand( long timeout, MysqlClientConnection conn) {
    this.source = conn;
    this.timeout = timeout;
  }


  public void execute(byte[] message, Statement statement, SqlQueryObject queryObject) throws Exception {
    Sessionable session = null;
    
    // xa 模式的提交
    if (source.isXaActive()) {
      // 构造xa rollback命令
      Statement xaRollbackStatements = new XARollBackStatement();
      QueryCommandPacket xaRollbackCommand = new QueryCommandPacket();
      xaRollbackCommand.query = "xa rollback '" + source.cid() + "'";
      xaRollbackCommand.command = QueryCommandPacket.COM_QUERY;
      byte[] xaRollbackbuffer = xaRollbackCommand.toByteBuffer(source).array();
      
      queryObject.sql = xaRollbackCommand.query;
      
      session =  new CommitStatementHandler(source, xaRollbackbuffer, xaRollbackStatements, null, queryObject, timeout);
    }
    // 非xa模式提交
    else {
      session = new CommitStatementHandler(source, message, statement, null, queryObject, timeout);
    }
    
    try {
      session.startSession();
    } catch (Exception e) {
      logger.error("start Session error:", e);
      session.endSession(false, e);
      throw e;
    }
  }
}
