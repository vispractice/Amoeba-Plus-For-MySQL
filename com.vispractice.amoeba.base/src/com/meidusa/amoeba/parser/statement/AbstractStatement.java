package com.meidusa.amoeba.parser.statement;

import java.util.Map;

import com.meidusa.amoeba.parser.dbobject.Table;


public abstract class AbstractStatement implements Statement {
  protected Table[] tables;
  private int parameterCount;

  private boolean isPrepared;
  
  private boolean isExplain;
  
  private Map hintParamsMap;


  public int getParameterCount() {
    return parameterCount;
  }

  public String getSql() {
    return null;
  }

  public void setParameterCount(int count) {
    this.parameterCount = count;
  }

  public Table[] getTables() {
    return tables;
  }

  public void setTables(Table[] tables) {
    this.tables = tables;
  }

  public boolean isPrepared() {
    return isPrepared;
  }

  public void setPrepared(boolean isPrepared) {
    this.isPrepared = isPrepared;
  }

  public String getType() {
    return this.getClass().getSimpleName();
  }

  public boolean isRead() {
    return false;
  }

  @Override
  public boolean isExplain() {
    return isExplain;
  }

  @Override
  public void setExplain(boolean isExplain) {
    this.isExplain = isExplain;
  }

  @Override
  public void setHintParams(Map hintParamsMap) {
    this.hintParamsMap = hintParamsMap;
  }

  @Override
  public Map getHintParams() {
    return hintParamsMap;
  }
  
}
