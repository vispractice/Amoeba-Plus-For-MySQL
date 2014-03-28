package com.meidusa.amoeba.parser.statement.ddl;

import com.meidusa.amoeba.parser.expression.Expression;
import com.meidusa.amoeba.parser.statement.AbstractStatement;

public abstract class DDLStatement extends AbstractStatement {

  private String targetName;
  private String targetValue;
  private String operate;

  @Override
  public Expression getExpression() {
    // TODO Auto-generated method stub
    return null;
  }

  public void setTargetName(String targetName) {
    this.targetName = targetName;
  }

  public void setTargetValue(String targetValue) {
    this.targetValue = targetValue;
  }

  public String getTargetName() {
    return targetName;
  }

  public String getTargetValue() {
    return targetValue;
  }

  public String getOperate() {
    return operate;
  }

  public void setOperate(String operate) {
    this.operate = operate;
  }

}
