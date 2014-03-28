package com.meidusa.amoeba.mysql.jdbc;

public class IsolationLevels {
  public static final int READ_UNCOMMITTED = 1;
  public static final int READ_COMMITTED = 2;
  
  public static final int REPEATED_READ = 4;
  
  public static final int SERIALIZABLE = 8;
}
