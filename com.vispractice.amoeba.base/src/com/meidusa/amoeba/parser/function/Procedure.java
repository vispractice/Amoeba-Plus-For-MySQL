package com.meidusa.amoeba.parser.function;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.meidusa.amoeba.exception.InitialisationException;
import com.meidusa.amoeba.parser.dbobject.Column;
import com.meidusa.amoeba.parser.dbobject.Table;
import com.meidusa.amoeba.parser.expression.Expression;
import com.meidusa.amoeba.sqljep.ParseException;
import com.meidusa.amoeba.util.Initialisable;

/**
 * 
 * @author Struct
 *
 */
public class Procedure extends AbstractFunction implements Initialisable{
	private Table table;
	private Map<Integer,Column> columnMap = new HashMap<Integer,Column>();
	private String tableName;
	private String params;
	
	@Override
	public Comparable evaluate(List<Expression> list, Object[] parameters)
			throws ParseException {
		
		return null;
	}
	
	@Override
	public void init() throws InitialisationException {
		
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
	public String getParams() {
		return params;
	}
	
	public void setParams(String params) {
		this.params = params;
	}

}
