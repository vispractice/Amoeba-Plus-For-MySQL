package com.meidusa.amoeba.config.loader;

import java.util.Map;

import com.meidusa.amoeba.parser.function.Function;

public interface SqlFunctionMapLoader {
  public void loadFunctionMap(Map<String, Function> funMap);
  
  public boolean needLoad();

}
