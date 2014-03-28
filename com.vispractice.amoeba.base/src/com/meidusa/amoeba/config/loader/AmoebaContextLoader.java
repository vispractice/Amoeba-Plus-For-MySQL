package com.meidusa.amoeba.config.loader;

import com.meidusa.amoeba.config.ProxyServerConfig;
import com.meidusa.amoeba.context.ProxyRuntimeContext;

public interface AmoebaContextLoader {
  public ProxyServerConfig loadConfig();
  public void setAmoebaContext(ProxyRuntimeContext amoebaContext);
}
