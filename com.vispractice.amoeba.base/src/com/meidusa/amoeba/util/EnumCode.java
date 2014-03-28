package com.meidusa.amoeba.util;

import java.util.EnumMap;

public class EnumCode {
	public enum ErrorCode {
		TABLE_NUM_LIMIT,
		RECORDS_IN_SINGLE_TABLE_LIMIT,
		USER_RULE_LIMIT,
		UNKNOWN_FATAL_ERROR,
		OK
	};
	
	public final static EnumMap<ErrorCode, Integer> errNoMapping = new EnumMap<EnumCode.ErrorCode, Integer>(ErrorCode.class);
	
	static {
		errNoMapping.put(ErrorCode.TABLE_NUM_LIMIT, 11002);
		errNoMapping.put(ErrorCode.RECORDS_IN_SINGLE_TABLE_LIMIT, 11003);
		errNoMapping.put(ErrorCode.USER_RULE_LIMIT, 11004);
		
		errNoMapping.put(ErrorCode.UNKNOWN_FATAL_ERROR, 11001);
		errNoMapping.put(ErrorCode.OK, 11000);
	}
	
	
	public final static EnumMap<ErrorCode, String> errMsgMapping = new EnumMap<EnumCode.ErrorCode, String>(ErrorCode.class);
	
	static {
		errMsgMapping.put(ErrorCode.TABLE_NUM_LIMIT, "Number of created table beyond the SLA limit");
		errMsgMapping.put(ErrorCode.RECORDS_IN_SINGLE_TABLE_LIMIT, "Records beyond the SLA limit in single table");
		errMsgMapping.put(ErrorCode.USER_RULE_LIMIT, "Don't have the authority for the operation");
		errMsgMapping.put(ErrorCode.UNKNOWN_FATAL_ERROR, "Unknown fatal error!");
	}
	
	public static int getErrorNo(ErrorCode code) {
		return errNoMapping.get(code) != null ? errNoMapping.get(code) : 0;
	}
	
	public static String getErrorMsg(ErrorCode code) {
		return errMsgMapping.get(code) != null ? errMsgMapping.get(code) : "";
	}
	
	private EnumCode(){
		
	}
	
}
