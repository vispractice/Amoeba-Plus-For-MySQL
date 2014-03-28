package com.meidusa.amoeba.net.packet;

/**
 * 对原有packet的封装
 * @author WangFei
 *
 */
public class Response {
  
  public boolean isSuccess;
  
  public int    errno;

  /**
   * 5个字节
   */
  public String sqlstate;

  /**
   * 错误信息
   */
  public String serverErrorMessage;
  
}
