/**
 * <pre>
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU AFFERO GENERAL PUBLIC LICENSE for more details. 
 * 	You should have received a copy of the GNU AFFERO GENERAL PUBLIC LICENSE along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * </pre>
 */
package com.meidusa.amoeba.parser.statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.parser.dbobject.Column;
import com.meidusa.amoeba.parser.dbobject.GlobalSeqColumn;
import com.meidusa.amoeba.parser.dbobject.Table;
import com.meidusa.amoeba.parser.expression.AndExpression;
import com.meidusa.amoeba.parser.expression.BaseExpressionList;
import com.meidusa.amoeba.parser.expression.ColumnExpression;
import com.meidusa.amoeba.parser.expression.Expression;
import com.meidusa.amoeba.parser.expression.FunctionExpression;
import com.meidusa.amoeba.sqljep.function.Comparative;
import com.meidusa.amoeba.sqljep.function.ComparativeAND;
import com.meidusa.amoeba.sqljep.function.ComparativeBaseList;
import com.meidusa.amoeba.sqljep.function.ComparativeOR;
import com.meidusa.amoeba.util.Tuple;

/**
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public abstract class DMLStatement extends AbstractStatement {

    private static Logger                        logger = Logger.getLogger(DMLStatement.class);
    protected boolean                            preparedStatment;
    
    protected Expression                         expression;
    private Map<Table, Map<Column, Comparative>> evaluatedTableMap;
    private Map<String,Column> selectColumnMap = new HashMap<String,Column>();
    
    private final List<GlobalSeqColumn<Column>> seqColumns = new ArrayList<GlobalSeqColumn<Column>>();
    private final List<GlobalSeqColumn<FunctionExpression>> batchFetchFuncCalls = new ArrayList<GlobalSeqColumn<FunctionExpression>>();
    
    protected String sql;
    
    private boolean isRead;
    private boolean isProcedure = false;
    
    
	@Override
    public boolean isRead() {
	  return isRead;
    }

  public void setRead(boolean isRead) {
		this.isRead = isRead;
	}

	public boolean isProcedure() {
		return isProcedure;
	}

	public void setProcedure(boolean isProcedure) {
		this.isProcedure = isProcedure;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}
	public String getSql(){
		return sql;
	}
	
	public void addSelectColumn(String key,Column value){
		selectColumnMap.put(key, value);
	}
	
    public Map<String, Column> getSelectColumnMap() {
		return selectColumnMap;
	}
    
    public void addSeqColumn(GlobalSeqColumn<Column> seqColumn) {
      seqColumns.add(seqColumn);
    }
    
    public void clearSeqColumn() {
      seqColumns.clear();
    }
    
    public List<GlobalSeqColumn<Column>> getSeqColumns() {
      return seqColumns;
    }
    
    public void addBatchSeqFetchCall(GlobalSeqColumn<FunctionExpression>  bulkFunExp) {
      batchFetchFuncCalls.add(bulkFunExp);
    }
    
    public void clearBatchSeqFetchCall() {
      batchFetchFuncCalls.clear();
    }

    public List<GlobalSeqColumn<FunctionExpression>> getBatchFetchFuncCalls() {
      return batchFetchFuncCalls;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    public boolean isPreparedStatment() {
        return preparedStatment;
    }

    public void setPreparedStatment(boolean preparedStatment) {
        this.preparedStatment = preparedStatment;
    }

    public Map<Table, Map<Column, Comparative>> evaluate(Object[] parameters) {
        Map<Table, Map<Column, Comparative>> currentEvaluatedTableMap = null;
        if (this.evaluatedTableMap == null) {
            currentEvaluatedTableMap = new HashMap<Table, Map<Column, Comparative>>();
            if (expression != null) {
                evaluateExpression(currentEvaluatedTableMap, parameters);
                if (logger.isDebugEnabled()) {
                    logger.debug("expression:[" + expression + "] evaluated");
                }
            } 
            
            if (getTables() != null) {
              for (Table table : getTables()) {
                if(currentEvaluatedTableMap.get(table) == null){
                    currentEvaluatedTableMap.put(table, null);
                }
              }
            }

            if (expression == null || !expression.isRealtime()) {
                this.evaluatedTableMap = currentEvaluatedTableMap;
            }
        } else {
            currentEvaluatedTableMap = this.evaluatedTableMap;
        }
        return currentEvaluatedTableMap;
    }

    protected void evaluateExpression(Map<Table, Map<Column, Comparative>> tablesMap,
                                             Object[] parameters) {
        if (expression instanceof BaseExpressionList) {
            evaluateExpression((BaseExpressionList) expression, tablesMap, parameters);
        } else if (expression instanceof ColumnExpression) {
            ColumnExpression colExpression = (ColumnExpression) expression;
            Table table = colExpression.getColumn().getTable();
            Map<Column, Comparative> columnMap = tablesMap.get(table);
            if (columnMap == null) {
                columnMap = new HashMap<Column, Comparative>();
                tablesMap.put(table, columnMap);
            }
            columnMap.put(colExpression.getColumn(), (Comparative) colExpression.evaluate(parameters));
        }
        
        
    }

    protected static void evaluateExpression(BaseExpressionList elist, Map<Table, Map<Column, Comparative>> tablesMap,
                                             Object[] parameters) {
        boolean and = false;
        if (elist instanceof AndExpression) {
            and = true;
        }

        List<Expression> blist = elist.getAllExpression();
        for (Expression e : blist) {
            if (e instanceof BaseExpressionList) {
                evaluateExpression((BaseExpressionList) e, tablesMap, parameters);
            } else if (e instanceof ColumnExpression) {
                ColumnExpression colExpression = (ColumnExpression) e;
                Table table = colExpression.getColumn().getTable();
                Map<Column, Comparative> columnMap = tablesMap.get(table);
                if (columnMap == null) {
                    columnMap = new HashMap<Column, Comparative>();
                    tablesMap.put(table, columnMap);
                }
                Comparative col = columnMap.get(colExpression.getColumn());
                Comparative newComparative = (Comparative) colExpression.evaluate(parameters);
                if (col != null) {
                	if(col instanceof ComparativeBaseList){
                		ComparativeBaseList source = (ComparativeBaseList)col;
                		if((source instanceof ComparativeAND && and) || (source instanceof ComparativeOR && !and)){
                			source.addComparative(newComparative);
                		}else{
                			ComparativeBaseList comparativeBaseList = null;
    	                    if (and) {
    	                        comparativeBaseList = new ComparativeAND(col);
    	                    } else {
    	                        comparativeBaseList = new ComparativeOR(col);
    	                    }
    	                    comparativeBaseList.addComparative(newComparative);
    	                    columnMap.put(colExpression.getColumn(), comparativeBaseList);
                		}
                	}else{
	                    ComparativeBaseList comparativeBaseList = null;
	                    if (and) {
	                        comparativeBaseList = new ComparativeAND(col);
	                    } else {
	                        comparativeBaseList = new ComparativeOR(col);
	                    }
	                    comparativeBaseList.addComparative(newComparative);
	                    columnMap.put(colExpression.getColumn(), comparativeBaseList);
                	}
                } else {
                    columnMap.put(colExpression.getColumn(), newComparative);
                }
            }
        }
    }
}
