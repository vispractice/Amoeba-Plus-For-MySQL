package com.meidusa.amoeba.mysql.handler;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.exception.AmoebaRuntimeException;
import com.meidusa.amoeba.mysql.net.MysqlClientConnection;
import com.meidusa.amoeba.mysql.net.MysqlConnection;
import com.meidusa.amoeba.mysql.net.packet.ConstantPacketBuffer;
import com.meidusa.amoeba.mysql.net.packet.ErrorPacket;
import com.meidusa.amoeba.net.Sessionable;
import com.meidusa.amoeba.parser.expression.Expression;
import com.meidusa.amoeba.parser.statement.PropertyStatement;
import com.meidusa.amoeba.parser.statement.Statement;
import com.meidusa.amoeba.route.SqlBaseQueryRouter;
import com.meidusa.amoeba.route.SqlQueryObject;

public class PropertyCommand {
  private long timeout;
  private MysqlClientConnection conn;
  
  public PropertyCommand(long timeout, MysqlClientConnection conn) {
    this.timeout = timeout;
    this.conn = conn;
  }
  
  public static Logger logger = Logger.getLogger(PropertyCommand.class);

  public void execute(Statement statement, SqlQueryObject queryObject) throws Exception {
    PropertyStatement st = (PropertyStatement) statement;
    Expression value = null;
    // set autocommit 语句
    if ((value = st.getValue("autocommit")) != null) {
      setAutocommit(value, queryObject);
      logger.debug("set autocommit=" + conn.isAutoCommit());
    }
    // set names 语句
    else if ((value = st.getValue("names")) != null) {
      setNames(value, queryObject);
      logger.debug("set names=" + conn.getCharset());
    }
    // Set 字符集 语句
    else if ((value = st.getValue("charset")) != null ||
        (value = st.getValue("character_set_results")) != null) {
      setCharset(value, queryObject);
      logger.debug("set charset=" + conn.getCharset());
    }
    // 事务隔离级别语句
    else if ((value = st.getValue("transactionisolation")) != null) {
      conn.setIsolationLevel(((Integer) value.evaluate(null)).intValue());
      conn.postMessage(ConstantPacketBuffer.STATIC_OK_BUFFER);
      logger.debug("set isolationLevel= " + conn.getIsolationLevel());
    } 
    // use schema语句
    else if ((value = st.getValue("schema")) != null) {
      setSchema(value, queryObject);
      logger.debug("use " + conn.getSchema());
    }
    // 标记是否xa
    else if ((value = st.getValue("isXA")) != null) {
      if (((Long) value.evaluate(null)).longValue() == 1) {
        conn.setIsXaActive(true);
      }
      else {
        conn.setIsXaActive(false);
      }
      logger.debug("set isXA=" + conn.isXaActive());
      conn.postMessage(ConstantPacketBuffer.STATIC_OK_BUFFER);
    }
    // 直接忽略不支持的语句
    else {
      logger.warn(queryObject.sql + " is not executed");
      conn.postMessage(ConstantPacketBuffer.STATIC_OK_BUFFER);
    }
  }
  
  private void setAutocommit(Expression value, SqlQueryObject queryObject) throws Exception {
    if (((Long) value.evaluate(null)).longValue() == 1) {
      if (conn.getStickyConnMap().size() > 0) {
        
        // 构造完提交命令，开始提交
        CommitCommand commitCMD = new CommitCommand(timeout, conn);
        commitCMD.execute();
        
        // 因为session是读写是两个线程，为了让两个线程同步，这里需要阻塞到session结束
        Sessionable commitSession = commitCMD.getSession();
        if (commitSession == null) {
          throw new AmoebaRuntimeException("session start error when implict commit execute");
        }
        else {
          while(!commitSession.isEnded()) {
            if (commitSession.isEnded()) {
              break;
            }
          }
        }
        
        // 检测会话是否成功执行
        if (commitSession.isSuccess()) {
          conn.setAutoCommit(true);
        }
        else {
          throw new AmoebaRuntimeException("command execute error, pls retry again");
        }
        
        
      } else {
        conn.setAutoCommit(true);
        conn.postMessage(ConstantPacketBuffer.STATIC_OK_BUFFER);
      }
    } else {
      conn.setAutoCommit(false);
      conn.postMessage(ConstantPacketBuffer.STATIC_OK_BUFFER);
    }
  }
  
  private void setSchema(Expression value, SqlQueryObject queryObject) {
    String schemaName = (String) value.evaluate(queryObject.parameters);
    String userName = conn.getUser();
    SqlBaseQueryRouter router = (SqlBaseQueryRouter) ProxyRuntimeContext.getInstance().getQueryRouter();
    
    if (conn.isAdmin() || router.isSchemaExistedForUser(userName, schemaName)) {
      conn.setSchema(schemaName);
      conn.postMessage(ConstantPacketBuffer.STATIC_OK_BUFFER);
    } 
    else {
      ErrorPacket error = new ErrorPacket();
      error.errno = 1049;
      error.packetId = 1;
      error.sqlstate = "42000";
      error.serverErrorMessage = "Unknown database=" + schemaName;
      conn.postMessage(error.toByteBuffer(conn).array());
    }
    
  }
  
  private void setNames(Expression value, SqlQueryObject queryObject) {
    String names = (String) value.evaluate(queryObject.parameters);
    if (((MysqlConnection) conn).setCharset(names)) {
      conn.postMessage(ConstantPacketBuffer.STATIC_OK_BUFFER);
    } 
    else {
      ErrorPacket error = new ErrorPacket();
      error.errno = 1115;
      error.packetId = 1;
      error.sqlstate = "42000";
      error.serverErrorMessage = "Unknown charset '" + names + "'";
      
      conn.postMessage(error.toByteBuffer(null).array());
    }
  }
  
  private void setCharset(Expression value, SqlQueryObject queryObject) {
    String charset = (String) value.evaluate(queryObject.parameters);
    if (charset.equals("null")) {
      conn.postMessage(ConstantPacketBuffer.STATIC_OK_BUFFER);
    } else if ( ((MysqlConnection) conn).setCharset(charset)) {
      conn.postMessage(ConstantPacketBuffer.STATIC_OK_BUFFER);
    } else {
      ErrorPacket error = new ErrorPacket();
      error.errno = 1115;
      error.packetId = 1;
      error.sqlstate = "42000";
      error.serverErrorMessage = "Unknown charset '" + charset + "'";
      
      conn.postMessage(error.toByteBuffer(null).array());
    }
  }
}
