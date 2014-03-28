package com.meidusa.amoeba.config.loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.helpers.FileWatchdog;

import com.meidusa.amoeba.exception.ConfigurationException;
import com.meidusa.amoeba.util.IPRule;
import com.meidusa.amoeba.util.StringUtil;

public class IpAccessListFileLoader implements IpAccessListLoader, ConfigModifiedAwareLoader{
  protected static Logger logger = Logger.getLogger(IpAccessListFileLoader.class);

  private String ipFile;
  private ConfigModifiedEventHandler handler;

  public void setIpFile(String ipFile) {
    try {
      this.ipFile = new File(ipFile).getCanonicalPath();
    } catch (IOException e) {
      throw new ConfigurationException(e);
    }
  }
  
  @Override
  public List<String> loadIPRule() {
    return null;
  }
  
  @Override
  public List<String> reLoadIPRule() {
    return loadIPRuleFromFile();
  }
  
  public List<String> loadIPRuleFromFile() {
    List<String> list = null;

    BufferedReader reader = null;

    try {
      reader = new BufferedReader(new FileReader(ipFile));
      String ipRuleLine = null;
      list = new ArrayList<String>();

      while ((ipRuleLine = reader.readLine()) != null) {
        ipRuleLine = ipRuleLine.trim();
        if (!StringUtil.isEmpty(ipRuleLine) && !ipRuleLine.startsWith("#")) {
          try {
            IPRule.isAllowIP(new String[] {ipRuleLine}, "127.0.0.1");
            list.add(ipRuleLine);
          } catch (Exception e) {
            logger.warn("'" + ipRuleLine + "' error:" + e.getMessage() + "  ,this rule disabled");
          }
        }
      }
      if (logger.isInfoEnabled()) {
        logger.info("ip access control loaded from: " + ipFile);
      }

    } catch (FileNotFoundException e) {
      logger.warn(" file:" + ipFile + " not found ,ip access control disabled.");
    } catch (IOException e) {
      logger.warn(" reading file:" + ipFile + " error ,ip access control disabled.");
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {}
      }
    }
    return list;
  }


  @Override
  public void startObserve() {
    IPAccessFileWatchdog dog = new IPAccessFileWatchdog(ipFile);
    dog.setDaemon(true);
    dog.setDelay(5000L);
    dog.start();
  }

  private class IPAccessFileWatchdog extends FileWatchdog {

    public IPAccessFileWatchdog(String filename) {
      super(filename);
    }

    public void doOnChange() {
      handler.doOnConfigModified();
    }
  }

  @Override
  public void setConfigModifiedEventHandler(ConfigModifiedEventHandler handler) {
   this.handler = handler; 
  }

}
