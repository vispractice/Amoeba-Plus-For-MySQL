package com.meidusa.amoeba.route;

import com.meidusa.amoeba.util.SqlUtil;
import com.meidusa.amoeba.util.StringUtil;

public class SqlQueryObject implements Request{
	public boolean isPrepared;
	public String sql;
	public Object[] parameters;
	public boolean isRead;
	@Override
	public boolean isPrepared() {
		return isPrepared;
	}
	
	@Override
	public boolean isRead() {
		return isRead;
	}
	
	@Override
	public boolean isUseStatement() {
		return SqlUtil.isUseStatement(sql);
	}

	public String toString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append("{sql=[").append(sql).append("]").append(", parameter=");
		buffer.append(StringUtil.toString(parameters)).append("}");
		return buffer.toString();
	}
}
