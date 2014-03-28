package com.meidusa.amoeba.mysql.context;

import java.io.StringReader;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.meidusa.amoeba.context.RuntimeContext;
import com.meidusa.amoeba.exception.InitialisationException;
import com.meidusa.amoeba.mysql.parser.sql.MysqlParser;
import com.meidusa.amoeba.mysql.util.CharsetMapping;
import com.meidusa.amoeba.parser.ParseException;
import com.meidusa.amoeba.parser.Parser;

public class MysqlRuntimeContext extends RuntimeContext {
	public final static String SERVER_VERSION = "amoeba-plus-for-mysql-1.0-RC1";
	private static Logger logger = Logger.getLogger(MysqlRuntimeContext.class);
	private byte               serverCharsetIndex;
	private int statementCacheSize = 500;
	private long statementExpiredTime = 5;
	
    public void setServerCharsetIndex(byte serverCharsetIndex) {
        this.serverCharsetIndex = serverCharsetIndex;
        this.setServerCharset(CharsetMapping.INDEX_TO_CHARSET[serverCharsetIndex & 0xff]);
    }

    public int getServerCharsetIndex() {
        if (serverCharsetIndex > 0) return serverCharsetIndex;
        return CharsetMapping.getCharsetIndex(this.getServerCharset());
    }
    
    public int getStatementCacheSize() {
		return statementCacheSize;
	}

	public void setStatementCacheSize(int statementCacheSize) {
		if(statementCacheSize <0){
			statementCacheSize = 50;
		}
		this.statementCacheSize = statementCacheSize;
		
	}

	public long getStatementExpiredTime() {
		return statementExpiredTime;
	}

	public void setStatementExpiredTime(long statementExpiredTime) {
		this.statementExpiredTime = statementExpiredTime;
	}
	
	public void init() throws InitialisationException{
		super.init();
		
		// 这里是为了给javacc预热
        Parser warmingUP = new MysqlParser(new StringReader(new String("Select 1")));
        try {
          warmingUP.doParse();
        } catch (ParseException e) {
          e.printStackTrace();
        }
        
        Level level = logger.getLevel();
        logger.setLevel(Level.INFO);
        if (logger.isInfoEnabled()) {
          logger.info("Amoeba for Mysql current versoin="+SERVER_VERSION);
        }
		logger.setLevel(level);
	}
}
