package com.meidusa.amoeba.parser.function;

import java.util.List;

import com.meidusa.amoeba.parser.expression.Expression;
import com.meidusa.amoeba.sqljep.ParseException;
import com.meidusa.amoeba.util.StringUtil;

public class Ascii extends AbstractFunction {

	@SuppressWarnings("unchecked")
    public Comparable evaluate(List<Expression> list, Object[] parameters)
			throws ParseException {
		if(list.size()==0){
			return null;
		}
			
		Comparable param = list.get(0).evaluate(parameters);
		if(param == null){
			return null;
		}
		String str = String.valueOf(param);
		if(StringUtil.isEmpty(str)){
			return 0;
		}else{
		}
		return (int)str.charAt(0);
	}
}
