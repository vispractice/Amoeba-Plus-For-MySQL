package com.meidusa.amoeba.sqljep.function;

import com.meidusa.amoeba.sqljep.ASTFunNode;
import com.meidusa.amoeba.sqljep.JepRuntime;
import com.meidusa.amoeba.sqljep.ParseException;

public class IfElse extends PostfixCommand {

	public Comparable<?>[] evaluate(ASTFunNode node, JepRuntime runtime)
			throws ParseException {
		node.childrenAccept(runtime.ev, null);
		Comparable<?> limit2 = runtime.stack.pop();
		Comparable<?> limit1 = runtime.stack.pop();
		Comparable<?> source = runtime.stack.pop();
		return new Comparable<?>[] { source, limit1, limit2 };
	}

	public int getNumberOfParameters() {
		return 3;
	}

	public boolean isAutoBox() {
		return false;
	}
	
	public Comparable<?> getResult(Comparable<?>... comparables)
			throws ParseException {
		Comparable<?> limit2 = comparables[2];
		Comparable<?> limit1 = comparables[1];
		Comparable<?> source = comparables[0];
		if (source == null) {
			return (Boolean.FALSE);
		}else{
			Comparable<?> value = source;
			if(source instanceof Comparative){
				value = ((Comparative) source).getValue();
			}
			if(Boolean.valueOf(value.toString())){
				return limit1;
			}else{
				return limit2;
			}
		}
		
		
	}

}
