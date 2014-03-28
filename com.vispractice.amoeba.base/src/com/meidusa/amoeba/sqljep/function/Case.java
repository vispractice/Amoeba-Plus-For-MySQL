/*****************************************************************************
      SQLJEP - Java SQL Expression Parser 0.2
      November 1 2006
         (c) Copyright 2006, Alexey Gaidukov
      SQLJEP Author: Alexey Gaidukov

      SQLJEP is based on JEP 2.24 (http://www.singularsys.com/jep/)
           (c) Copyright 2002, Nathan Funk
 
      See LICENSE.txt for license information.
*****************************************************************************/

package com.meidusa.amoeba.sqljep.function;

import java.util.ArrayList;
import java.util.List;

import com.meidusa.amoeba.sqljep.function.PostfixCommand;
import com.meidusa.amoeba.sqljep.ASTFunNode;
import com.meidusa.amoeba.sqljep.JepRuntime;
import com.meidusa.amoeba.sqljep.ParseException;

/**
 * 
 * @author struct
 *
 */
public final class Case extends PostfixCommand {
	private boolean caseHead = false;
	public Case(boolean caseHead){
		this.caseHead = caseHead; 
	}
	final public int getNumberOfParameters() {
		return -1;
	}
	
	public Comparable<?>[] evaluate(ASTFunNode node, JepRuntime runtime) throws ParseException {
		int num = node.jjtGetNumChildren();
		int count = 0;
		List<Integer> result = new ArrayList<Integer>();
		int startCondition = 0;
		Comparable<?>  headValue = null;
		if(caseHead){
			startCondition = 1;
			node.jjtGetChild(0).jjtAccept(runtime.ev, null);
			headValue = runtime.stack.pop();
		}
		if (num > (caseHead ? 2 :1)) {
			boolean elseCase;
			
			if (num % 2 != 0) {
				elseCase = caseHead?false:true;
				num--;
			} else {
				elseCase = caseHead?true:false;
			}
			
			for (int i = startCondition; i < (elseCase?num-1:num); i += 2) {
				node.jjtGetChild(i).jjtAccept(runtime.ev, null);
				Comparable<?>  cond = runtime.stack.pop();
				if(caseHead){
					if(cond instanceof ComparativeBaseList){
						ComparativeBaseList cpl = (ComparativeBaseList)cond;
						if(headValue instanceof Comparative){
							cond = cpl.intersect((Comparative)headValue, ComparativeComparator.comparator);
						}else{
							cond = cpl.intersect(Comparative.Equivalent,headValue, ComparativeComparator.comparator);
						}
					}else{
						cond = ComparativeEQ.compareTo(headValue, cond);
					}
				}
				if (cond instanceof Boolean) {
					if (((Boolean)cond).booleanValue()) {
						result.add(i+1);
						count ++;
						if(!runtime.isMultValue){
							break;
						}
					}
				} else {
					throw new ParseException("In case only boolean is possible as condition. Found: "+(cond != null ? cond.getClass() : "NULL"));
				}
			}
			if (count <= 0 && elseCase) {
				result.add(num-1);
			}
			if (result.size() > 0) {
				Comparable<?>[] comparables = new Comparable[result.size()];
				int j=0;
				for(int i:result){
					node.jjtGetChild(i).jjtAccept(runtime.ev, null);
					Comparable<?>  variant = runtime.stack.pop();
					comparables[j] = variant;
					j++;
				}
				//runtime.stack.push(variant);
				return comparables;
			} else {
				//runtime.stack.push(null);
				return new Comparable<?>[]{""};
			}
		} else {
			throw new ParseException("Few arguments for case");
		}
	}

	public Comparable<?> getResult(Comparable<?>... comparables)
			throws ParseException {
		StringBuffer buffer = new StringBuffer();
		for(Comparable<?> comp : comparables){
			buffer.append(comp).append(";");
		}
		return buffer.toString();
	}
}
