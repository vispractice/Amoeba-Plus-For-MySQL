package com.meidusa.amoeba.util;


public class SqlUtil {

	public static boolean isUseStatement(String sql){
		
		if (!StringUtil.isEmpty(sql)) {
			
			sql = sql.trim();
			String[] words = sql.split("\\s+");
			
			if (!StringUtil.isEmpty(words[0]) && words[0].toLowerCase().equals("use")) {
				return true;
			}
		}
		
		return false;
	}
	
	
	public static String getUseSchema(String sql){
		
		String schema = null;
		
		if (isUseStatement(sql)) {
			if (!StringUtil.isEmpty(sql)) {
				sql = sql.trim();
				String[] words = sql.split("\\s+");
				
				if (words != null && words.length >= 2) {
					if (!StringUtil.isEmpty(words[1])) {
						schema = words[1].trim();
					}
				}
			}
		}
		
		return schema;
	}
	
}
