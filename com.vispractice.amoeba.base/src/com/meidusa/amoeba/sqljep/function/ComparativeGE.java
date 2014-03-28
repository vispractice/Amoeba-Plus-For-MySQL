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

import com.meidusa.amoeba.sqljep.function.ComparativeComparator;
import com.meidusa.amoeba.sqljep.function.PostfixCommand;
import com.meidusa.amoeba.sqljep.ASTFunNode;
import com.meidusa.amoeba.sqljep.JepRuntime;
import com.meidusa.amoeba.sqljep.ParseException;
import com.meidusa.amoeba.util.ThreadLocalMap;

public final class ComparativeGE extends PostfixCommand {
	
	final public int getNumberOfParameters() {
		return 2;
	}

	public boolean isAutoBox() {
		return false;
	}

	public Comparable<?>[] evaluate(ASTFunNode node, JepRuntime runtime)
			throws ParseException {
		node.childrenAccept(runtime.ev, null);
		Comparable<?> param2 = runtime.stack.pop();
		Comparable<?> param1 = runtime.stack.pop();
		return new Comparable<?>[] { param1, param2 };
	}

	public Comparable<?> getResult(Comparable<?>... comparables)
			throws ParseException {
		Comparable<?> param2 = comparables[1];
		Comparable<?> param1 = comparables[0];
		if (param1 == null || param2 == null) {
			return (Boolean.FALSE);
		} else {
			/*boolean intersect = ComparativeComparator.intersect(param1, param2);
			if(intersect){
				return Boolean.TRUE;
			}else{
				return (ComparativeComparator.compareTo(param1, param2) >= 0);
			}*/
			
			if((param1 instanceof Comparative)){
				return ComparativeComparator.intersect((Comparative)param1, Comparative.GreaterThanOrEqual,param2);
			}else{
				return ComparativeComparator.intersect(Comparative.Equivalent,param1, Comparative.GreaterThanOrEqual,param2);
			}
			/*boolean result = param1.intersect(
						Comparative.GreaterThanOrEqual, param2,
						ComparativeComparator.comparator);
				return (result);
			} else {
				return (ComparativeComparator.compareTo(param1, param2) >= 0);
			}
			
			return Intersector.intersect(function,other,this.comparison,this.getValue(),comparator);
*/		}
	}
}
