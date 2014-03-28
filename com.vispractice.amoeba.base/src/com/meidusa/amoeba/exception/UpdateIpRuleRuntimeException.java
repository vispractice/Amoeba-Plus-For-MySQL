package com.meidusa.amoeba.exception;

public class UpdateIpRuleRuntimeException extends AmoebaRuntimeException{
  private static final long serialVersionUID = 1L;
  private String ipRule;
  
  public UpdateIpRuleRuntimeException(String ipRule, String s, Throwable cause) {
    super(s);
    this.throwable = cause;
    this.ipRule = ipRule;
  }

  public String getIpRule() {
    return ipRule;
  }

  public void setIpRule(String ipRule) {
    this.ipRule = ipRule;
  }
  
  
}
