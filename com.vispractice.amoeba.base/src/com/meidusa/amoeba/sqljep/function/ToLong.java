package com.meidusa.amoeba.sqljep.function;

import com.meidusa.amoeba.sqljep.ParseException;

public class ToLong extends ToNumber {

	public Comparable<?> getResult(Comparable<?>... comparables)
			throws ParseException {
		if (comparables.length == 1) {
			return ((Number)to_number(comparables[0])).longValue();
		} else {
			return ((Number)to_number(comparables[0], comparables[1])).longValue();
		}
	}
}
