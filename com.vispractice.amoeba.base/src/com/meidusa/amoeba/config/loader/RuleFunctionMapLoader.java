package com.meidusa.amoeba.config.loader;

import java.util.Map;

import com.meidusa.amoeba.sqljep.function.PostfixCommand;

public interface RuleFunctionMapLoader {
  public void loadFunctionMap(Map<String, PostfixCommand> funMap);
  
  public boolean needLoad();

}
