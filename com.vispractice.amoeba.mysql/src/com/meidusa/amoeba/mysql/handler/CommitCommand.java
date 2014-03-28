package com.meidusa.amoeba.mysql.handler;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.mysql.net.MysqlClientConnection;
import com.meidusa.amoeba.mysql.net.packet.QueryCommandPacket;
import com.meidusa.amoeba.net.Sessionable;
import com.meidusa.amoeba.parser.statement.CommitStatement;
import com.meidusa.amoeba.parser.statement.Statement;
import com.meidusa.amoeba.parser.statement.XACommitStatement;
import com.meidusa.amoeba.route.SqlQueryObject;

public class CommitCommand {
  private MysqlClientConnection source;
  private long timeout;
  private Sessionable session;
  
  public static Logger logger = Logger.getLogger(CommitCommand.class);

  
  public CommitCommand( long timeout, MysqlClientConnection conn) {
    this.source = conn;
    this.timeout = timeout;
  }


  public void execute(byte[] message, Statement statement, SqlQueryObject queryObject) throws Exception {
    
    // xa 模式的提交
    if (source.isXaActive()) {
      // 构造xa commit命令
      Statement xaCommitStatement = new XACommitStatement();
      QueryCommandPacket xaCommitCommand = new QueryCommandPacket();
      xaCommitCommand.query = "xa commit '" + source.cid() + "'";
      xaCommitCommand.command = QueryCommandPacket.COM_QUERY;
      byte[] xaCommitbuffer = xaCommitCommand.toByteBuffer(source).array();
      queryObject.sql = xaCommitCommand.query;
      
      session =  new CommitStatementHandler(source, xaCommitbuffer, xaCommitStatement, null, queryObject, timeout);
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
  
  public void execute() throws Exception {
    
    // xa 模式的提交
    if (source.isXaActive()) {
      // 构造xa commit命令
      Statement xaCommitStatement = new XACommitStatement();
      QueryCommandPacket xaCommitCommand = new QueryCommandPacket();
      xaCommitCommand.query = "xa commit '" + source.cid() + "'";
      xaCommitCommand.command = QueryCommandPacket.COM_QUERY;
      byte[] xaCommitbuffer = xaCommitCommand.toByteBuffer(source).array();
      
      SqlQueryObject xaCommitQueryObject = new SqlQueryObject();
      xaCommitQueryObject.isPrepared = false;
      xaCommitQueryObject.sql = xaCommitCommand.query;
      
      
      session =  new CommitStatementHandler(source, xaCommitbuffer, xaCommitStatement, null, xaCommitQueryObject, timeout);
    }
    // 非xa模式提交
    else {
      
      Statement statement = new CommitStatement();
      QueryCommandPacket commitCommand = new QueryCommandPacket();
      commitCommand.query = "commit";
      commitCommand.command = QueryCommandPacket.COM_QUERY;
      byte[] buffer = commitCommand.toByteBuffer(source).array();
      
      SqlQueryObject commitQueryObject = new SqlQueryObject();
      commitQueryObject.isPrepared = false;
      commitQueryObject.sql = commitCommand.query;
      
      session = new CommitStatementHandler(source, buffer, statement, null, commitQueryObject, timeout);
    }
    
    try {
      session.startSession();
    } catch (Exception e) {
      logger.error("start Session error:", e);
      session.endSession(false, e);
      throw e;
    }
  }

  public Sessionable getSession() {
    return session;
  }
}
