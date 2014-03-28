package com.meidusa.amoeba.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHash<T> {
	public static class Entry<T>{
		T object;
		int replicas;
		Entry(){}
		Entry(T obj,int replicas){
			this.object = obj;
			this.replicas = replicas;
		}
		
		Entry(T obj){
			this.object = obj;
			this.replicas = 1;
		}
	}
	private final HashFunction hashFunction;
	private final SortedMap<Long, T> circle = new TreeMap<Long, T>();
	private final Map<T,Integer> replicas = new HashMap<T,Integer>();

	public ConsistentHash(HashFunction hashFunction) {
		this.hashFunction = hashFunction;
		/*for (T node : nodes) {
			add(node);
		}*/
	}

	public void addAll(Collection<Entry<T>> collection){
		for(Entry<T> entry : collection){
			add(entry.object,entry.replicas);
		}
	}
	
	public void add(T node,int numberOfReplicas) {
		for (int i = 0; i < numberOfReplicas; i++) {
			circle.put((long)hashFunction.hash(node.toString()+i), node);
		}
		replicas.put(node, numberOfReplicas);
	}

	public void remove(T node) {
		Integer numberOfReplicas = replicas.get(node);
		if(numberOfReplicas == null){
			return;
		}
		
		for (int i = 0; i < numberOfReplicas; i++) {
			circle.remove(hashFunction.hash(node.toString() + i));
		}
	}

	public T get(Object key) {
		if (circle.isEmpty()) {
			return null;
		}
		long hash = hashFunction.hash(key);
		if (!circle.containsKey(hash)) {
			SortedMap<Long, T> tailMap = circle.tailMap(hash);
			hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
		}
		return circle.get(hash);
	}

	public static void main(String[] args){
		Collection<Entry<Object>> coll = new ArrayList<Entry<Object>>();
		coll.add(new Entry("asdfqwer"));
		coll.add(new Entry("b"));
		coll.add(new Entry("c"));
		coll.add(new Entry("d"));
		coll.add(new Entry("e"));
		coll.add(new Entry("f"));
		coll.add(new Entry("g"));
		coll.add(new Entry("h"));
		coll.add(new Entry("i"));
		coll.add(new Entry("j"));
		coll.add(new Entry("k"));
		coll.add(new Entry("l"));
		
		ConsistentHash<Object> hash = new ConsistentHash<Object>(new HashFunction(){

			public long hash(Object string) {
				return string.hashCode() % 1024;
			}
			
		});
		
		hash.addAll(coll);
		long start = System.currentTimeMillis();
		for(int i=0;i<1000000;i++){
			//System.out.println(hash.get(i));
			hash.get(i);
		}
		System.out.println(System.currentTimeMillis() - start);
	}
}
