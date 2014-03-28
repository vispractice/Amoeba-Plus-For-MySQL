package com.meidusa.amoeba.config;

public class UserConfig {
  private String username;
  private String password;
  private boolean isAdmin = false;
  
  
  public UserConfig(String username, String password) {
    super();
    this.username = username;
    this.password = password;
  }
  public String getUsername() {
    return username;
  }
  public void setUsername(String username) {
    this.username = username;
  }
  public String getPassword() {
    return password;
  }
  public void setPassword(String password) {
    this.password = password;
  }
  public boolean isAdmin() {
    return isAdmin;
  }
  public void setAdmin(boolean isAdmin) {
    this.isAdmin = isAdmin;
  }
  
  
}
