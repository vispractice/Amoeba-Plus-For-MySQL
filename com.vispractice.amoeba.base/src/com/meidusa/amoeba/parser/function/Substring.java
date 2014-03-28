package com.meidusa.amoeba.parser.function;

import java.util.List;

import com.meidusa.amoeba.parser.expression.Expression;
import com.meidusa.amoeba.sqljep.ParseException;

/**
 * @author struct
 */
public class Substring extends AbstractFunction {

	@SuppressWarnings("unchecked")
    public Comparable evaluate(List<Expression> list, Object[] parameters)
			throws ParseException {
		int size = list.size();
		if(size<2){
			return null;
		}
		String str = (String)list.get(0).evaluate(parameters);
		if(str == null){
			return null;
		}
		int start = ((Long)list.get(1).evaluate(parameters)).intValue();
		
		if(size == 2){
			return substring(str,start,-1);
		}else if(size == 3){
			int length = ((Long)list.get(1).evaluate(parameters)).intValue();
			if(length<=0) return "";
			return substring(str,start,length);
		}else{
			return null;
		}
		
	}

	/**
	 * pos 
	 * @param str
	 * @param pos 第几个位置开始(从1开始)
	 * @param length 子字符串长度
	 * @return
	 */
	public static String substring(String str,int pos,int length){
		
		if(pos==0){
			return "";
		}
		int beginIndex = 0;
		if(pos<0){
			beginIndex = str.length() + pos;
			if(beginIndex <0)return "";
		}else{
			beginIndex = pos -1;
		}
		
		if(length <=-1){
			return str.substring(beginIndex);
		}else{
			int endIndex = beginIndex+length>str.length()-1?str.length():beginIndex+length;
			return str.substring(beginIndex, endIndex);
		}
	}
	
	public static void main(String[] args){
		System.out.println(substring("Sakila",-3,-2));
	}
}
