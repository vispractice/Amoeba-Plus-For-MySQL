package com.meidusa.amoeba.parser.statement;

import com.meidusa.amoeba.parser.expression.Expression;

public class HelpStatement extends AbstractStatement{

  @Override
  public Expression getExpression() {
    return null;
  }

  public boolean isRead() {
    return true;
  }
}
