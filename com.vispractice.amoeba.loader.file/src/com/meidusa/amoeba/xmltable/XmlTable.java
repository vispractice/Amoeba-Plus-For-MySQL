package com.meidusa.amoeba.xmltable;

import java.util.ArrayList;
import java.util.List;

public class XmlTable {
	private String name;
	private String schema;
	private List<String> columns = new ArrayList<String>();
	private List<XmlRow> rows = new ArrayList<XmlRow>();

	public List<String> getColumns() {
		return columns;
	}
	public void setColumns(List<String> columns) {
		this.columns = columns;
	}
	public List<XmlRow> getRows() {
		return rows;
	}
	public void setRows(List<XmlRow> rows) {
		this.rows = rows;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getSchema() {
		return schema;
	}
	
	public void setSchema(String schema) {
		this.schema = schema;
	}
	
	public XmlTable query(Condition condition){
		XmlTable table = new XmlTable();
		for(String column:columns){
			table.columns.add(column);
		}
		for(XmlRow row : rows){
			if(row.isMatch(condition)){
				table.rows.add(row);
			}
		}
		return table;
	}
}
