package com.meidusa.amoeba.util;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentHashSet<E> extends MapBackedSet<E> {

  private static final long serialVersionUID = -4662964657125844327L;

  public ConcurrentHashSet() {
    super(new ConcurrentHashMap<E, Boolean>());
  }

  public ConcurrentHashSet(Collection<E> c) {
    super(new ConcurrentHashMap<E, Boolean>(), c);
  }

  @Override
  public boolean add(E o) {
    Boolean answer = ((ConcurrentMap<E, Boolean>) map).putIfAbsent(o,
        Boolean.TRUE);
    return answer == null;
  }
}

class MapBackedSet<E> extends AbstractSet<E> implements Serializable {

  private static final long serialVersionUID = 4563664838935004295L;
  protected final Map<E, Boolean> map;

  public MapBackedSet(Map<E, Boolean> map) {
    this.map = map;
  }

  public MapBackedSet(Map<E, Boolean> map, Collection<E> c) {
    this.map = map;
    addAll(c);
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean contains(Object o) {
    return map.containsKey(o);
  }

  @Override
  public Iterator<E> iterator() {
    return map.keySet().iterator();
  }

  @Override
  public boolean add(E o) {
    return map.put(o, Boolean.TRUE) == null;
  }

  @Override
  public boolean remove(Object o) {
    return map.remove(o) != null;
  }

  @Override
  public void clear() {
    map.clear();
  }
}
