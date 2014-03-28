package com.meidusa.amoeba.sqljep.function;

import com.meidusa.amoeba.sqljep.ASTFunNode;
import com.meidusa.amoeba.sqljep.JepRuntime;
import com.meidusa.amoeba.sqljep.ParseException;

/**
 * id in range(12,16,0,1) 表示 id>12 and id<=16 
 * id in range(12,16,0,0) 表示 id>12 and id<16 
 * id in range(12,16,1,0) 表示 id>=12 and id<16 
 * id in range(12,16,1,1) 表示 id>=12 and id<=16 
 * @author struct
 *
 */
public class Range extends PostfixCommand{

	public Comparable<?>[] evaluate(ASTFunNode node, JepRuntime runtime)
			throws ParseException {
		node.jjtGetChild(0).jjtAccept(runtime.ev, null);
		Comparable<?> param0 = runtime.stack.pop();
		
		node.jjtGetChild(1).jjtAccept(runtime.ev, null);
		Comparable<?> param1 = runtime.stack.pop();
		
		node.jjtGetChild(2).jjtAccept(runtime.ev, null);
		Comparable<?> param2 = runtime.stack.pop();
		int leftEquals = Integer.valueOf(param2.toString());
		
		node.jjtGetChild(3).jjtAccept(runtime.ev, null);
		Comparable<?> param3 = runtime.stack.pop();
		
		int rightEquals = Integer.valueOf(param3.toString());
		
		ComparativeRange range = new ComparativeRange();
		range.addComparative(new Comparative(leftEquals==0?Comparative.GreaterThan:Comparative.GreaterThanOrEqual,param0));
		range.addComparative(new Comparative(rightEquals ==0?Comparative.LessThan:Comparative.LessThanOrEqual,param1));
		/*ComparativeAND and = new ComparativeAND(leftEquals==0?Comparative.GreaterThan:Comparative.GreaterThanOrEqual,param0);
		and.addComparative(new Comparative(rightEquals ==0?Comparative.LessThan:Comparative.LessThanOrEqual,param1));
*/		return new Comparable[]{range};
	}

	public int getNumberOfParameters() {
		return 4;
	}

	public Comparable<?> getResult(Comparable<?>... comparables)
			throws ParseException {
		return comparables[0];
	}

}
