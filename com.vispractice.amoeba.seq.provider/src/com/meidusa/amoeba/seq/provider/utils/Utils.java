package com.meidusa.amoeba.seq.provider.utils;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.util.StringUtil;

public class Utils {
  
  public final static String SEQ_CONFIG_FILENAME = "seq.properties";
  
  static public Properties readGlobalSeqConfigProps() throws IOException{
    Properties props = new Properties();
    String amoebaHomePath = ProxyRuntimeContext.getInstance().getAmoebaHomePath();
    String PATH_SEPARATOR = StringUtil.PATH_SEPARATOR;
    String seqConfigPath = amoebaHomePath + PATH_SEPARATOR + "conf" + PATH_SEPARATOR + SEQ_CONFIG_FILENAME;
    FileReader reader = new FileReader(seqConfigPath);
    props.load(reader);
    return props;
  }
}
