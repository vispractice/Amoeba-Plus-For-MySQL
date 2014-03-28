package com.meidusa.amoeba.data;

import java.util.HashMap;
import java.util.Map;

public final class Constants {
	
	public final static String NORMAL = "X";
	
	public final static Map<String, Integer> sqlReadKeyWord = new HashMap<String, Integer>();
	static {
		sqlReadKeyWord.put("show", null);
		sqlReadKeyWord.put("print", null);
		sqlReadKeyWord.put("help", null);
		sqlReadKeyWord.put("explain", null);
		sqlReadKeyWord.put("describe", null);
		sqlReadKeyWord.put("select", null);
	}
	
	
	private Constants(){
		
	}
}
