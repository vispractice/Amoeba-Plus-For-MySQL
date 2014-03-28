package com.meidusa.amoeba.parser.function;

import java.util.List;

import com.meidusa.amoeba.parser.expression.Expression;
import com.meidusa.amoeba.sqljep.ParseException;

public class UnixTimestamp extends AbstractFunction {

	public Comparable evaluate(List<Expression> list, Object[] parameters)
			throws ParseException {
		return System.currentTimeMillis() /1000;
	}

}
