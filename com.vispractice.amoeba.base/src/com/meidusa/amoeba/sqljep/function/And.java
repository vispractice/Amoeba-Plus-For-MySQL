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

import java.math.BigDecimal;

import com.meidusa.amoeba.sqljep.function.PostfixCommand;
import com.meidusa.amoeba.sqljep.ASTFunNode;
import com.meidusa.amoeba.sqljep.JepRuntime;
import com.meidusa.amoeba.sqljep.ParseException;

public final class And extends PostfixCommand {
	final static String DATE_ADDITION = "Wrong operation";
	final static BigDecimal DAY_MILIS = new BigDecimal(86400000);
	
	final public int getNumberOfParameters() {
		return 2;
	}
	
	/**
	 * Calculates the result of applying the "+" operator to the arguments from
	 * the stack and pushes it back on the stack.
	 */
	public Comparable<?>[] evaluate(ASTFunNode node, JepRuntime runtime) throws ParseException {
		node.childrenAccept(runtime.ev, null);
		Comparable<?>  param2 = runtime.stack.pop();
		Comparable<?>  param1 = runtime.stack.pop();
		return new Comparable<?>[]{param1,param2};
	}

	public static Comparable<?>  and(Comparable<?>  param1, Comparable<?>  param2) throws ParseException {
		if (param1 == null || param2 == null) {
			return null;
		}
		if (param1 instanceof String) {
			param1 = parse((String)param1);
		}
		if (param2 instanceof String) {
			param2 = parse((String)param2);
		}
		if (param1 instanceof Number && param2 instanceof Number) {
			// BigInteger type is not supported
			Number n1 = (Number)param1;
			Number n2 = (Number)param2;
			if (n1 instanceof BigDecimal || n2 instanceof BigDecimal) {
				BigDecimal b1 = getBigDecimal(n1);
				BigDecimal b2 = getBigDecimal(n2);
				return b1.toBigInteger().and(b2.toBigInteger());
			}
			if (n1 instanceof Double || n2 instanceof Double || n1 instanceof Float || n2 instanceof Float) {
				return n1.longValue() & n2.longValue();
			} else {	// Long, Integer, Short, Byte 
				long l1 = n1.longValue();
				long l2 = n2.longValue();
				return l1 & l2;
				
			}
		}
		 else {
			throw new ParseException(WRONG_TYPE+"  ("+param1.getClass()+"+"+param2.getClass()+")");
		}
	}
	
	public Comparable<?> getResult(Comparable<?>... comparables)
			throws ParseException {
		return and(comparables[0], comparables[1]);
	}
}

