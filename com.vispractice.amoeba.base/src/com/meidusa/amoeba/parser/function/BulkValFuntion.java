package com.meidusa.amoeba.parser.function;

import java.util.List;

import com.meidusa.amoeba.parser.dbobject.Table;
import com.meidusa.amoeba.parser.expression.Expression;
import com.meidusa.amoeba.sqljep.ParseException;

public class BulkValFuntion extends AbstractFunction{
  
  private Table table;
  
  public Table getTable() {
    return table;
  }

  public void setTable(Table table) {
    this.table = table;
  }
  
  public String getSql() {
    return (table == null ? name : table.getSql() + "." + name);
  }
  
  public void toString(List<Expression> list,StringBuilder builder) {
    
    if(list == null){
        builder.append(getSql());
        builder.append("(");
        builder.append(")");
    }else{
        int current = 0;
        builder.append(getSql());
        builder.append("(");
        for(Expression e:list){
            builder.append(e);
            current ++;
            if(current != list.size()){
                builder.append(",");
            }
        }
        builder.append(")");
    }
  }
  
  @Override
  public Comparable evaluate(List<Expression> list, Object[] parameters) throws ParseException {
    throw new UnsupportedOperationException("evaluate is not supported in class="+ this.getClass().getName());
  }

  public String toString() {
    return getSql();
  }
  
}
