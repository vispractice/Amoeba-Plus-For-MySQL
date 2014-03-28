package com.meidusa.amoeba.config.loader;

public interface ConfigModifiedAwareLoader {
  public void startObserve();
  public void setConfigModifiedEventHandler(ConfigModifiedEventHandler handler);
}
