package com.meidusa.amoeba.sqljep.function;

import java.util.Comparator;

public class ComparativeRange extends ComparativeBaseList {
	
	@Override
	public boolean intersect(int function, Comparable other,
			Comparator comparator) {
		Comparative left = this.list.get(0);
		Comparative right = this.list.get(1);
		return left.intersect(function, other, comparator) 
			&& right.intersect(function, other, comparator);
	}

	@Override
	public boolean intersect(Comparative other, Comparator comparator) {
		
		if(other instanceof ComparativeBaseList){
			ComparativeBaseList target = (ComparativeBaseList)other;
			return target.intersect(this, comparator);
		}else{
			Comparative left = this.list.get(0);
			Comparative right = this.list.get(1);
			return left.intersect(other, comparator) 
			    && right.intersect(other, comparator);
		}
	}

}
