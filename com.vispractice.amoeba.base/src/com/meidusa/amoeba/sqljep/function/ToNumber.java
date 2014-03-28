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

import com.meidusa.amoeba.sqljep.ASTFunNode;
import com.meidusa.amoeba.sqljep.BaseJEP;
import com.meidusa.amoeba.sqljep.JepRuntime;
import com.meidusa.amoeba.sqljep.ParseException;

public class ToNumber extends PostfixCommand {
	private static final String FORMAT_EXCEPTION = "Wrong number";
	
	@SuppressWarnings("unused")
    private static final String TYPE_EXCEPTION = "Wrong type";
	
	final public int getNumberOfParameters() {
		return -1;
	}
	
	public Comparable<?>[] evaluate(ASTFunNode node, JepRuntime runtime) throws ParseException {
		node.childrenAccept(runtime.ev, null);
		int num = node.jjtGetNumChildren();
		if (num == 1) {
			Comparable<?>  param1 = runtime.stack.pop();
			return new Comparable<?>[]{param1};
		}
		else if (num == 2) {
			Comparable<?>  param2 = runtime.stack.pop();
			Comparable<?>  param1 = runtime.stack.pop();
			return new Comparable<?>[]{param1,param2};
		} else {
			// remove all parameters from stack and push null
			removeParams(runtime.stack, num);
			throw new ParseException(PARAMS_NUMBER+" for instr");
		}
	}

	public static Comparable<?>  to_number(Comparable<?>  param1) throws ParseException {
		if (param1 == null) {
			return null;
		}
		else if (param1 instanceof String) {
			return parse((String)param1);
		}
		else if (param1 instanceof Number) {
			return param1;
		}
		else if (param1 instanceof Boolean) {
			return ((Boolean)param1).booleanValue() ? 1 : 0;
		} else {
			throw new ParseException(FORMAT_EXCEPTION);
		}
	}
	
	@SuppressWarnings("unchecked")
    public static Comparable<?>  to_number(Comparable<?>  param1, Comparable<?>  param2) throws ParseException {
		if (param1 == null || param2 == null) {
			return null;
		}
		if (param1 instanceof String && ((String)param1).length() == 0) {
			return null;
		}
		if (param1 instanceof Number) {
			return param1;
		}
		if (!(param1 instanceof String) || !(param2 instanceof String)) {
			throw new ParseException(WRONG_TYPE+"  to_number("+param1.getClass()+"+"+param2.getClass()+")");
		}
		//StringBuilder d = new StringBuilder((String)param1);
		try {
			OracleNumberFormat format = new OracleNumberFormat((String)param2);
			return (Comparable)format.parseObject((String)param1);
		} catch (java.text.ParseException e) {
			if (BaseJEP.debug) {
				e.printStackTrace();
			}
			throw new ParseException(e.getMessage());
		}
	}

	public Comparable<?> getResult(Comparable<?>... comparables)
			throws ParseException {
		if(comparables.length == 1){
			return to_number(comparables[0]);
		}else{
			return to_number(comparables[0],comparables[1]);
		}
	}
}

