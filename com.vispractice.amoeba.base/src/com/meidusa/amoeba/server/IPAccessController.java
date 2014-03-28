package com.meidusa.amoeba.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.apache.log4j.helpers.FileWatchdog;

import com.meidusa.amoeba.config.ConfigUtil;
import com.meidusa.amoeba.config.loader.ConfigModifiedEventHandler;
import com.meidusa.amoeba.config.loader.ConfigModifiedAwareLoader;
import com.meidusa.amoeba.config.loader.IpAccessListLoader;
import com.meidusa.amoeba.exception.InitialisationException;
import com.meidusa.amoeba.net.AuthResponseData;
import com.meidusa.amoeba.net.AuthingableConnection;
import com.meidusa.amoeba.util.IPRule;
import com.meidusa.amoeba.util.Initialisable;
import com.meidusa.amoeba.util.StringUtil;

/**
 * IP 访问控制过滤 IP v4
 * 
 * @author struct
 * @author hexianmao
 */
public class IPAccessController implements AuthenticateFilter, Initialisable, ConfigModifiedEventHandler {

  protected static Logger logger = Logger.getLogger(IPAccessController.class);
  private static final String DENAY_MESSAGE = "Access denied for ip: '${host}' to amoeba server";
  private boolean isEnabled;
  private String[] ipRule = null;

  private IpAccessListLoader ipAccessListLoader;

  public IPAccessController() {}

  @Override
  public boolean doFilte(AuthingableConnection conn, AuthResponseData rdata) {
    if (isEnabled) {
      if (ipRule != null && ipRule.length > 0) {
        String ip = conn.getInetAddress().getHostAddress();
        try {
          boolean access = IPRule.isAllowIP(ipRule, ip);
          if (!access) {
            Properties properties = new Properties();
            properties.setProperty("host", ip);
            rdata.message = ConfigUtil.filter(DENAY_MESSAGE, properties);
          }
          return access;
        } catch (Exception e) {
          logger.warn(ip + " check access error:", e);
        }
      }
    }

    return true;
  }
  
  public void setIpAccessListLoader(IpAccessListLoader ipAccessListLoader) {
    this.ipAccessListLoader = ipAccessListLoader;
  }

  @Override
  public void doOnConfigModified() {
    List<String> accessList = ipAccessListLoader.reLoadIPRule();
    buildIPAccessRule(accessList);
  }

  @Override
  public void init() throws InitialisationException {
    List<String> accessList = ipAccessListLoader.loadIPRule();
    buildIPAccessRule(accessList);
    
    if (ipAccessListLoader instanceof ConfigModifiedAwareLoader) {
      ((ConfigModifiedAwareLoader) ipAccessListLoader).setConfigModifiedEventHandler(this);
      ((ConfigModifiedAwareLoader)ipAccessListLoader).startObserve();
    }
  }
  
  private void buildIPAccessRule(List<String> list) {
    if (list != null && list.size() > 0) {
      this.ipRule = list.toArray(new String[list.size()]);
      isEnabled = true;
    } else {
      this.ipRule = null;
      isEnabled = false;
    }
  }
}
