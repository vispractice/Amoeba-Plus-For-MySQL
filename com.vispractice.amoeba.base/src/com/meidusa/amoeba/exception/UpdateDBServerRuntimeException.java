package com.meidusa.amoeba.exception;

public class UpdateDBServerRuntimeException extends AmoebaRuntimeException{
  private static final long serialVersionUID = 1L;
  private String poolName;

  public UpdateDBServerRuntimeException(String poolName, String s, Throwable cause) {
    super(s);
    this.throwable = cause;
    this.poolName = poolName;
  }

  public String getPoolName() {
    return poolName;
  }

  public void setPoolName(String poolName) {
    this.poolName = poolName;
  }
  
}
