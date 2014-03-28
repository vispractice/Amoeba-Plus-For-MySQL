package com.meidusa.amoeba.exception;

public class LoadTableRuleRuntimeException extends AmoebaRuntimeException {


  private static final long serialVersionUID = 1L;
  private String tableName;

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public LoadTableRuleRuntimeException(String tableName, String s, Throwable cause) {
    super(s);
    this.throwable = cause;
    this.tableName = tableName;
  }

}
