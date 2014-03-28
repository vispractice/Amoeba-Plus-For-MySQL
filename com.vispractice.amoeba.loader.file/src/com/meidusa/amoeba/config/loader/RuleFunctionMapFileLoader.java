package com.meidusa.amoeba.config.loader;

import java.util.Map;

import com.meidusa.amoeba.config.BeanObjectEntityConfig;
import com.meidusa.amoeba.sqljep.function.PostfixCommand;

public class RuleFunctionMapFileLoader extends FunctionFileLoader<String, PostfixCommand> implements RuleFunctionMapLoader{
  
  @Override
  public void initBeanObject(BeanObjectEntityConfig config, PostfixCommand bean) {
      bean.setName(config.getName());
  }

  @Override
  public void putToMap(Map<String, PostfixCommand> map, PostfixCommand value) {
      map.put(value.getName(), value);
  }


}
