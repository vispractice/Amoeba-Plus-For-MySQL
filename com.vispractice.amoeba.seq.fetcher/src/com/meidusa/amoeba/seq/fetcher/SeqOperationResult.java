package com.meidusa.amoeba.seq.fetcher;

public class SeqOperationResult {
  private boolean isSuccessed;
  private String errMsg;
  
  public SeqOperationResult(boolean isSuccessed, String errMsg) {
    this.isSuccessed = isSuccessed;
    this.errMsg = errMsg;
  }
  
  public boolean isSuccessed() {
    return isSuccessed;
  }
  public void setSuccessed(boolean isSuccessed) {
    this.isSuccessed = isSuccessed;
  }
  public String getErrMsg() {
    return errMsg;
  }
  public void setErrMsg(String errMsg) {
    this.errMsg = errMsg;
  }
}
