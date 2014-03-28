package com.meidusa.amoeba.parser.statement;

import java.util.Map;

import com.meidusa.amoeba.parser.dbobject.Column;
import com.meidusa.amoeba.parser.dbobject.Table;
import com.meidusa.amoeba.sqljep.function.Comparative;

public class CallStatement extends DMLStatement {
	public CallStatement(){
		this.setProcedure(true);
	}

  @Override
  public Map<Table, Map<Column, Comparative>> evaluate(Object[] parameters) {
    return null;
  }
}
