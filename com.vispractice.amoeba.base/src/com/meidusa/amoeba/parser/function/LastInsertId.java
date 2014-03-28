package com.meidusa.amoeba.parser.function;

import java.util.List;

import com.meidusa.amoeba.parser.expression.Expression;
import com.meidusa.amoeba.sqljep.ParseException;
import com.meidusa.amoeba.util.ThreadLocalMap;

public class LastInsertId extends AbstractFunction implements ThreadLocalSettingFunction {

	public Comparable evaluate(List<Expression> list, Object[] parameters)
			throws ParseException {
		return null;
	}

	public void invoke() {
		ThreadLocalMap.put(LastInsertId.class.getName(), Boolean.TRUE);		
	}

}
