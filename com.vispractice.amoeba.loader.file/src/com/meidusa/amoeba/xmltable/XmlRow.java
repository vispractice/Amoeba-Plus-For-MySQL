package com.meidusa.amoeba.xmltable;

import java.util.HashMap;
import java.util.Map;

import com.meidusa.amoeba.util.StringUtil;

public class XmlRow {
	private Map<String,XmlColumn> columMap = new HashMap<String,XmlColumn>();
	
	public Map<String, XmlColumn> getColumMap() {
		return columMap;
	}

	public void setColumMap(Map<String, XmlColumn> columMap) {
		this.columMap = columMap;
	}

	public void addColumn(String name,XmlColumn column){
		columMap.put(name, column);
	}
	
	public boolean isMatch(Condition condition){
		if(condition == null) return true;
		if(condition.type == Condition.TYPE.exist){
			return columMap.get(condition.name) != null;
		}else if(condition.type == Condition.TYPE.match){
			XmlColumn column = columMap.get(condition.name); 
			if(column == null) return false;
			return StringUtil.equalsIgnoreCase(column.getValue(), condition.value);
		}else{
			return false;
		}
	}
}
