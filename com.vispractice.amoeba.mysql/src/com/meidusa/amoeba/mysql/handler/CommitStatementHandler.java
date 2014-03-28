package com.meidusa.amoeba.mysql.handler;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.mysql.net.MysqlClientConnection;
import com.meidusa.amoeba.net.poolable.ObjectPool;
import com.meidusa.amoeba.parser.statement.Statement;
import com.meidusa.amoeba.route.Request;

public class CommitStatementHandler extends StickyForTransactionHandler {
  public static Logger logger = Logger.getLogger(CommitStatementHandler.class);

  public CommitStatementHandler(MysqlClientConnection source, byte[] query, Statement statment,
      ObjectPool[] pools, Request request, long timeout) {
    super(source, query, statment, pools, request, timeout);
  }
  
}
