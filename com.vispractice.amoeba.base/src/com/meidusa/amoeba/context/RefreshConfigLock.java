package com.meidusa.amoeba.context;

public class RefreshConfigLock {
  public final static Object lock = new Object();
  
  private RefreshConfigLock() {
    
  }
}
