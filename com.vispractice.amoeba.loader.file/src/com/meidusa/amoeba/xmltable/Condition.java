package com.meidusa.amoeba.xmltable;

public class Condition {
	public static enum TYPE {exist,match,nameMatch}; 
	public String name;
	public String value;
	public TYPE type; 
	
}
