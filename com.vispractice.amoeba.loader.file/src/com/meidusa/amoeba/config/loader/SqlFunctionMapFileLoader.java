package com.meidusa.amoeba.config.loader;

import java.util.Map;

import com.meidusa.amoeba.config.BeanObjectEntityConfig;
import com.meidusa.amoeba.parser.function.Function;

public class SqlFunctionMapFileLoader extends FunctionFileLoader<String, Function> implements SqlFunctionMapLoader{
  
  @Override
  public void initBeanObject(BeanObjectEntityConfig config, Function bean) {
    bean.setName(config.getName());
  }

  @Override
  public void putToMap(Map<String, Function> map, Function value) {
    map.put(value.getName(), value);
  }


}
