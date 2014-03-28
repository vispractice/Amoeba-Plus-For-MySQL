package com.meidusa.amoeba.sqljep.function;

import java.util.Comparator;

public class ComparativeOR extends ComparativeBaseList{
	
	public ComparativeOR(int function, Comparable<?> value) {
		super(function, value);
	}
	
	public ComparativeOR(){};
	
	public ComparativeOR(Comparative item){
		super(item);
	}
	
	@SuppressWarnings("unchecked")
	public boolean intersect(int function,Comparable other,Comparator comparator){
		Comparative param2 = new Comparative(function,other);
		for(Comparative source :list){
			
			/*if(ComparativeComparator.intersect(source, param2)){
				return true;
			}*/
			if(source.intersect(function, other, comparator)){
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public boolean intersect(Comparative other, Comparator comparator) {
		for(Comparative source :list){
			if(source.intersect(other, comparator)){
				return true;
			}
		}
		return false;
	}

}
