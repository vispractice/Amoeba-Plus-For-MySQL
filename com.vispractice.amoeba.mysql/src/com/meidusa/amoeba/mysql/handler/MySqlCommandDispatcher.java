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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.exception.AmoebaRuntimeException;
import com.meidusa.amoeba.mysql.context.MysqlRuntimeContext;
import com.meidusa.amoeba.mysql.jdbc.MysqlDefs;
import com.meidusa.amoeba.mysql.net.MysqlClientConnection;
import com.meidusa.amoeba.mysql.net.packet.BindValue;
import com.meidusa.amoeba.mysql.net.packet.ConstantPacketBuffer;
import com.meidusa.amoeba.mysql.net.packet.ErrorPacket;
import com.meidusa.amoeba.mysql.net.packet.FieldPacket;
import com.meidusa.amoeba.mysql.net.packet.MysqlPacketBuffer;
import com.meidusa.amoeba.mysql.net.packet.QueryCommandPacket;
import com.meidusa.amoeba.mysql.net.packet.ResultSetHeaderPacket;
import com.meidusa.amoeba.mysql.net.packet.RowDataPacket;
import com.meidusa.amoeba.mysql.net.packet.result.MysqlResultSetPacket;
import com.meidusa.amoeba.net.Connection;
import com.meidusa.amoeba.net.MessageHandler;
import com.meidusa.amoeba.net.poolable.ObjectPool;
import com.meidusa.amoeba.parser.dbobject.Column;
import com.meidusa.amoeba.parser.dbobject.GlobalSeqColumn;
import com.meidusa.amoeba.parser.dbobject.Schema;
import com.meidusa.amoeba.parser.expression.FunctionExpression;
import com.meidusa.amoeba.parser.statement.BeginStatement;
import com.meidusa.amoeba.parser.statement.CommitCMD;
import com.meidusa.amoeba.parser.statement.DMLStatement;
import com.meidusa.amoeba.parser.statement.HelpStatement;
import com.meidusa.amoeba.parser.statement.PropertyStatement;
import com.meidusa.amoeba.parser.statement.RollbackCMD;
import com.meidusa.amoeba.parser.statement.SelectStatement;
import com.meidusa.amoeba.parser.statement.StartTansactionStatement;
import com.meidusa.amoeba.parser.statement.Statement;
import com.meidusa.amoeba.parser.statement.XAStatement;
import com.meidusa.amoeba.parser.statement.ddl.DDLCreateSequenceStatenment;
import com.meidusa.amoeba.parser.statement.ddl.DDLDropSequenceStatement;
import com.meidusa.amoeba.route.SqlBaseQueryRouter;
import com.meidusa.amoeba.route.SqlQueryObject;
import com.meidusa.amoeba.seq.fetcher.SeqFetchService;
import com.meidusa.amoeba.seq.fetcher.SeqOperationResult;
import com.meidusa.amoeba.util.StringUtil;

/**
 * handler
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public class MySqlCommandDispatcher implements MessageHandler {

    protected static Logger logger  = Logger.getLogger(MySqlCommandDispatcher.class);
    private long timeout = ProxyRuntimeContext.getInstance().getRuntimeContext().getQueryTimeout() * 1000;
    
    

    /**
     * Ping 、COM_STMT_SEND_LONG_DATA command remove to @MysqlClientConnection #doReceiveMessage()
     */
    public void handleMessage(Connection connection) {
    	
    	byte[] message = null;
		while((message = connection.getInQueue().getNonBlocking()) != null){
	        MysqlClientConnection conn = (MysqlClientConnection) connection;
	
	        QueryCommandPacket command = new QueryCommandPacket();
	        command.init(message, connection);
	        
	        
        	SqlBaseQueryRouter router = (SqlBaseQueryRouter)ProxyRuntimeContext.getInstance().getQueryRouter();
        	Statement statement = router.parseStatement(conn, command.query);
	        
	        if (logger.isDebugEnabled()) {
	            logger.debug(command.query);
	        }
            
	        try {
	            if (MysqlPacketBuffer.isPacketType(message, QueryCommandPacket.COM_QUERY)) {
	            	
	            	if(command.query != null && (command.query.indexOf("'$version'")>0 || command.query.indexOf("@amoebaversion")>0)){
	            		MysqlResultSetPacket lastPacketResult = createAmoebaVersion(conn,(SelectStatement)statement,false);
            			lastPacketResult.wirteToConnection(conn);
            			return;
	            	}
	            	
	                SqlQueryObject queryObject = new SqlQueryObject();
	                queryObject.isPrepared = false;
	                queryObject.sql = command.query;
	                
	                // 属性配置语句
                    if (statement instanceof PropertyStatement) {
                      PropertyStatement st = (PropertyStatement) statement;
                      PropertyCommand propertyCommand = new PropertyCommand(timeout, conn);
                      propertyCommand.execute(st, queryObject);
                    }
                    // xa语句
                    else if (statement instanceof XAStatement && conn.isXaActive()) {
                        ErrorPacket error = new ErrorPacket();
                        error.errno = 1044;
                        error.packetId = 1;
                        error.sqlstate = "42000";
                        error.serverErrorMessage = "can not use xa statement in xa model";
                        conn.postMessage(error.toByteBuffer(connection).array());
                        logger.warn("can not use xa statement in xa model");
                    } 
                    // begin 语句
                    else if (statement instanceof BeginStatement) {
                      conn.setAutoCommit(false);
                      conn.postMessage(ConstantPacketBuffer.STATIC_OK_BUFFER);
                    }
                    // start transaction 语句
                    else if (statement instanceof StartTansactionStatement) {
                      conn.setAutoCommit(false);
                      conn.postMessage(ConstantPacketBuffer.STATIC_OK_BUFFER);
                    }
                    // commit语句
                    else if (statement instanceof CommitCMD) {
                      CommitCommand commitCommand = new CommitCommand(timeout, conn);
                      commitCommand.execute(message, statement, queryObject);
                    }
                    // rollback语句
                    else if(statement instanceof RollbackCMD){
                      RollbackCommand rollbackCommand = new RollbackCommand(timeout, conn);
                      rollbackCommand.execute(message, statement, queryObject);
                    }
                    // 创建sequence语句
                    else if (statement instanceof DDLCreateSequenceStatenment) {
                      CreateSequence(conn, statement);
                    }
                    // 删除sequence语句
                    else if (statement instanceof DDLDropSequenceStatement) {
                      DropSequence(conn, statement);
                    }
                    // 其他查询语句
                    else {
                      ObjectPool[] pools = null;
                      // help 语句只要发一个default pool就好
                      if (statement instanceof HelpStatement) {
                        pools = router.getDefaultObjectPool();
                        if(pools != null && pools.length>1){
                          pools = new ObjectPool[]{pools[0]};
                        }
                      }
                      else {
                        pools = router.doRoute(conn, queryObject, statement);
                      }
                      
                      /*
                       *  替换全局序列
                       *  而且只替换SELECT/INSERT/UPDATE/DELETE且不是Explain的语句
                       */
                      if (statement instanceof DMLStatement) {
                        DMLStatement dmlStmt = (DMLStatement)statement;
                        List<GlobalSeqColumn<Column>> seqColumns = dmlStmt.getSeqColumns();
                        List<GlobalSeqColumn<FunctionExpression>> batchSeqFetchCalls = dmlStmt.getBatchFetchFuncCalls();
                        String targetSQL = queryObject.sql;
                        boolean isNeedReplace = false;
                        
                        // 先替换 seq.nextval 或 seq.currval
                        if (seqColumns.size() > 0) {
                          isNeedReplace = true;
                          targetSQL = replaceSeqValue(conn, seqColumns, targetSQL, statement);
                        }
                        
                        // 再替换批量获取的, seq.bulkval(count)
                        if (batchSeqFetchCalls.size() > 0) {
                          isNeedReplace = true;
                          targetSQL = replaceBatchSeqValue(conn, batchSeqFetchCalls, targetSQL, statement);
                        }
                        
                        // 已替换过的sql，需要重新生成 byte[]
                        if (isNeedReplace) {
                          QueryCommandPacket autoCommitCommand = new QueryCommandPacket();
                          autoCommitCommand.query = targetSQL;
                          autoCommitCommand.command = QueryCommandPacket.COM_QUERY;
                          message = autoCommitCommand.toByteBuffer(conn).array();
                          
                          queryObject.sql = targetSQL;
                        }
                      }
                      
                      QueryCommand queryCommand = new QueryCommand(timeout, conn);
                      queryCommand.execute(pools, message, statement, queryObject);
                    }
	            } 
	            
	            else if (MysqlPacketBuffer.isPacketType(message, QueryCommandPacket.COM_STMT_PREPARE)) {
  	                SqlQueryObject queryObject = new SqlQueryObject();
  	                queryObject.isPrepared = true;
  	                queryObject.sql = command.query;
  	              
  	                ObjectPool[] pools = router.doRoute(conn, queryObject, statement);
  	                PrepareQueryCommand prepareQueryCommand = new PrepareQueryCommand(timeout, conn);
  	                prepareQueryCommand.execute(pools, message, statement, queryObject, command);
	              
	            } 
	            else if (MysqlPacketBuffer.isPacketType(message, QueryCommandPacket.COM_STMT_EXECUTE)) {
	            	PrepareExecuteCommand prepareExecuteCommand = new PrepareExecuteCommand(timeout, conn, router);
	            	prepareExecuteCommand.execute(message, statement);
	            } 
	            else if (MysqlPacketBuffer.isPacketType(message, QueryCommandPacket.COM_INIT_DB)) {
	                conn.setSchema(command.query);
	                conn.postMessage(ConstantPacketBuffer.STATIC_OK_BUFFER);
	            } 
	            else if (MysqlPacketBuffer.isPacketType(message, QueryCommandPacket.COM_CHANGE_USER)){
	            	conn.postMessage(ConstantPacketBuffer.STATIC_OK_BUFFER);
	            }
	            else{
	                ErrorPacket error = new ErrorPacket();
	                error.errno = 1044;
	                error.packetId = 1;
	                error.sqlstate = "42000";
	                error.serverErrorMessage = "can not use this command here!!";
	                conn.postMessage(error.toByteBuffer(connection).array());
	                logger.warn("unsupport packet:" + command);
	            }
	        } catch (Exception e) {
	            ErrorPacket error = new ErrorPacket();
	            error.errno = 1044;
	            error.packetId = 1;
	            error.sqlstate = "42000";
	            error.serverErrorMessage = e.getMessage();
	            conn.postMessage(error.toByteBuffer(connection).array());
	            
	            logger.error("messageDispate error", e);
	        }
		}
    }
    
    private String replaceSeqValue(MysqlClientConnection conn, List<GlobalSeqColumn<Column>> seqColumns, String sourceSql, Statement stmt) {
      StringBuffer replacedSQL = new StringBuffer(sourceSql);
      
      /*
       *  主要为了兼容一些客户端可能会为了获取执行计划，查询函数性能
       *  但是我们的序列是自己生成的，所以只能通过模拟一个执行计划来返回给客户端
       */
      if(stmt.isExplain()) {
        replacedSQL = new StringBuffer("EXPLAIN EXTENDED SELECT 1 FROM DUAL");
      } 
      else {
        for(GlobalSeqColumn<Column> globalSeq : seqColumns) {
          Column column = globalSeq.getSeqObject();
          String seqName = globalSeq.getSeqName();
          Long seqVal = globalSeq.getSeqValue();
          int endIndex = replacedSQL.length() - sourceSql.length() +  globalSeq.getSeqTokenEndColumn();
          int startIndex = endIndex - seqName.length();
          
          if (seqVal < 0) {
             logger.error(String.format("skip to replace global sequence %s since its value is negative number", seqName));
          }
          else {
              StringBuilder replaceColumn = new StringBuilder();
              replaceColumn.append(seqVal+"");
              
              // 通过增加别名来避免别名丢失
              if (StringUtil.isEmpty(column.getAlias()) && (stmt instanceof SelectStatement)) {
                replaceColumn.append(" as " + column.getName());
              }
              
              if (startIndex >= 0 && endIndex >= startIndex) {
                try {
                  replacedSQL.replace(startIndex, endIndex, replaceColumn.toString());
                } catch (Exception e) {
                  logger.error(String.format("error occours when replace global sequence name with value since: %s", e.getMessage()));
                }
              }
            }
          }
      }
      
      return replacedSQL.toString();
    }
    
    private String replaceBatchSeqValue(MysqlClientConnection conn, List<GlobalSeqColumn<FunctionExpression>> batchSeqFetchCalls, String sourceSql, Statement stmt) {
      StringBuffer replacedSQL = new StringBuffer(sourceSql);
      /*
       *  主要为了兼容一些客户端可能会为了获取执行计划，查询函数性能
       *  但是我们的序列是自己生成的，所以只能通过模拟一个执行计划来返回给客户端
       */
      if(stmt.isExplain()) {
        replacedSQL = new StringBuffer("EXPLAIN EXTENDED SELECT 1 FROM DUAL");
      } 
      else {
        
        for(GlobalSeqColumn<FunctionExpression> bulkFunCall: batchSeqFetchCalls) {
          // 函数表达式
          FunctionExpression funExp = bulkFunCall.getSeqObject();
          Long seqVal = bulkFunCall.getSeqValue();
          // 函数名
          String bulkFuncName = bulkFunCall.getSeqName();
          int endIndex = replacedSQL.length() - sourceSql.length() + bulkFunCall.getSeqTokenEndColumn();
          int startIndex = endIndex - funExp.toString().length();

          if (seqVal > 0) {
            StringBuilder replaceColumn = new StringBuilder();
            replaceColumn.append(seqVal+"");
            
            // 通过增加别名来避免别名丢失
            if (stmt instanceof SelectStatement) {
              replaceColumn.append(" as " + bulkFuncName);
            }
            
            if (startIndex >= 0 && endIndex >= startIndex) {
              try {
                replacedSQL = replacedSQL.replace(startIndex, endIndex, replaceColumn.toString());
              } catch (Exception e) {
                logger.error(String.format("error occours when replace bulkval function with value since: %s", e.getMessage()));
              }
            }
          }
        }
      }
      
      return replacedSQL.toString();
    }
    
    private void DropSequence(MysqlClientConnection conn, Statement statement) {
      DDLDropSequenceStatement seqStmt = (DDLDropSequenceStatement)statement;
      Schema schema = seqStmt.getSchema();
      
      if (schema == null || StringUtil.isEmpty(schema.getName())) {
        if (StringUtil.isEmpty(conn.getSchema())) {
          throw new AmoebaRuntimeException("can not delete seq since schema is null");
        }
        else {
          schema = new Schema();
          schema.setName(conn.getSchema());
        }
      }
      
      String schemaName = schema.getName();
      String seqName = seqStmt.getSeqName();
      
      SeqOperationResult result = SeqFetchService.deleteSeq(schemaName, seqName); 
      if (result.isSuccessed()) {
        conn.postMessage(ConstantPacketBuffer.STATIC_OK_BUFFER);
      }
      else {
        sendInternalErrorMsg(conn, result.getErrMsg());
      }
    }
    
    private void CreateSequence(MysqlClientConnection conn, Statement statement) {
      DDLCreateSequenceStatenment seqStmt = (DDLCreateSequenceStatenment)statement;
      Schema schema = seqStmt.getSchema();
      
      if (schema == null || StringUtil.isEmpty(schema.getName())) {
        if (StringUtil.isEmpty(conn.getSchema())) {
          throw new AmoebaRuntimeException("can not create seq since schema is null");
        }
        else {
          schema = new Schema();
          schema.setName(conn.getSchema());
        }
      }
      
      String schemaName = schema.getName();
      String seqName = seqStmt.getSeqName();
      long start = seqStmt.getStartWith();
      long offset = seqStmt.getOffset();
      
      SeqOperationResult result = SeqFetchService.createSeq(schemaName, seqName, start, offset); 
      
      if (result.isSuccessed()) {
        conn.postMessage(ConstantPacketBuffer.STATIC_OK_BUFFER);
      }
      else {
        sendInternalErrorMsg(conn, result.getErrMsg());
      }
    }
    
    private MysqlResultSetPacket createAmoebaVersion(MysqlClientConnection conn,SelectStatement statment,boolean isPrepared){
    	Map<String,Column> selectedMap = ((SelectStatement)statment).getSelectColumnMap();
		MysqlResultSetPacket lastPacketResult = new MysqlResultSetPacket(null);
		lastPacketResult.resulthead = new ResultSetHeaderPacket();
		lastPacketResult.resulthead.columns = (selectedMap.size()==0?1:selectedMap.size());
		if(selectedMap.size() == 0){
			Column column = new Column();
			column.setName("@amoebaversion");
			selectedMap.put("@amoebaversion", column);
		}
		lastPacketResult.resulthead.extra = 1;
		RowDataPacket row = new RowDataPacket(isPrepared);
		row.columns = new ArrayList<Object>();
		int index =0; 
		lastPacketResult.fieldPackets = new FieldPacket[selectedMap.size()];
		for(Map.Entry<String, Column> entry : selectedMap.entrySet()){
			FieldPacket field = new FieldPacket();
			String alias = entry.getValue().getAlias();
			
			
			if("@amoebaversion".equalsIgnoreCase(entry.getValue().getName()) 
				||  "'$version'".equalsIgnoreCase(entry.getValue().getName())){
				BindValue value = new BindValue();
				value.bufferType = MysqlDefs.FIELD_TYPE_VARCHAR;
				value.value = MysqlRuntimeContext.SERVER_VERSION;
				value.scale = 20;
				value.isSet = true;
				row.columns.add(value);
				field.name = (alias == null?entry.getValue().getName()+"()":alias);
			}else{
				BindValue value = new BindValue();
				value.bufferType = MysqlDefs.FIELD_TYPE_VARCHAR;
				value.scale = 20;
				value.isNull = true;
				row.columns.add(value);
				field.name = (alias == null?entry.getValue().getName():alias);
			}
			
			field.type = (byte)MysqlDefs.FIELD_TYPE_VARCHAR;
			field.catalog = "def";
			field.length = 20;
			lastPacketResult.fieldPackets[index] = field; 
			index++;
		}
			
		List<RowDataPacket> list = new ArrayList<RowDataPacket>();
		list.add(row);
		lastPacketResult.setRowList(list);
		return lastPacketResult;
    }
    
    private void sendInternalErrorMsg(MysqlClientConnection conn, String msg) {
      ErrorPacket error = new ErrorPacket();
      error.errno = 1044;
      error.packetId = 1;
      error.sqlstate = "42000";
      error.serverErrorMessage = msg;
      conn.postMessage(error.toByteBuffer(conn).array());
    }
    
    public static MysqlResultSetPacket createLastInsertIdPacket(MysqlClientConnection conn,SelectStatement statment,boolean isPrepared){
    	Map<String,Column> selectedMap = ((SelectStatement)statment).getSelectColumnMap();
		MysqlResultSetPacket lastPacketResult = new MysqlResultSetPacket(null);
		lastPacketResult.resulthead = new ResultSetHeaderPacket();
		lastPacketResult.resulthead.columns = selectedMap.size();
		lastPacketResult.resulthead.extra = 1;
		RowDataPacket row = new RowDataPacket(isPrepared);
		row.columns = new ArrayList<Object>();
		int index =0; 
		lastPacketResult.fieldPackets = new FieldPacket[selectedMap.size()];
		for(Map.Entry<String, Column> entry : selectedMap.entrySet()){
			FieldPacket field = new FieldPacket();
			String alias = entry.getValue().getAlias();
			
			
			if("LAST_INSERT_ID".equalsIgnoreCase(entry.getValue().getName())){
				BindValue value = new BindValue();
				value.bufferType = MysqlDefs.FIELD_TYPE_LONGLONG;
				value.longBinding = conn.getLastInsertId();
				value.scale = 20;
				value.isSet = true;
				row.columns.add(value);
				field.name = (alias == null?entry.getValue().getName()+"()":alias);
				
			}else if("@@IDENTITY".equalsIgnoreCase(entry.getValue().getName())){

				BindValue value = new BindValue();
				value.bufferType = MysqlDefs.FIELD_TYPE_LONGLONG;
				value.longBinding = conn.getLastInsertId();
				value.scale = 20;
				value.isSet = true;
				row.columns.add(value);
				
				row.columns.add(value);
				field.name = (alias == null?entry.getValue().getName():alias);
				
			}else{
				BindValue value = new BindValue();
				value.bufferType = MysqlDefs.FIELD_TYPE_STRING;
				value.scale = 20;
				value.isNull = true;
				row.columns.add(value);
				field.name = (alias == null?entry.getValue().getName():alias);
			}
			
			field.type = MysqlDefs.FIELD_TYPE_LONGLONG;
			field.catalog = "def";
			field.length = 20;
			lastPacketResult.fieldPackets[index] = field; 
			index++;
		}
			
		List<RowDataPacket> list = new ArrayList<RowDataPacket>();
		list.add(row);
		lastPacketResult.setRowList(list);
		return lastPacketResult;
    }
}
