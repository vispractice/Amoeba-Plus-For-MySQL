package com.meidusa.amoeba.context;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.meidusa.amoeba.exception.InitialisationException;
import com.meidusa.amoeba.util.Initialisable;

public class RuntimeContext implements Initialisable {
  private String serverCharset;
  // 没有实际用到
  private Executor readExecutor;
  
  // 应用端和amoeba端处理线程池
  private ThreadPoolExecutor clientSideExecutor;
  // amoeba和数据库实例处理线程池
  private Executor serverSideExecutor;

  // 用于定时更新ip访问规则的executor
  private ScheduledExecutorService ipSyncExecutor;
  
  private int readThreadPoolSize = 16;
  private int clientSideThreadPoolSize = 16;
  private int serverSideThreadPoolSize = 16;
  
  // 最多允许多少个请求等待，否则告警
  private int maxQueueSize = 100;
  // 最多允许多少个超时请求
  private int maxConnTimeOutCount = 100;
  
  // 处理超时时间，单位为秒
  private int queryTimeout = 60;
  private boolean useMultipleThread = true;
  // 后端验证超时时间，单位为秒
  private long authTimeOut = 15;
  
  private boolean autocommit = true;

  private int isolationLevel = 8;
  
  /**
   * max result --Overload protection , query session was killed , results returned size exceed
   */
  private int maxResult = -1;

  public int getQueryTimeout() {
    return queryTimeout;
  }

  public void setQueryTimeout(int queryTimeout) {
    this.queryTimeout = queryTimeout;
  }
  
  public long getAuthTimeOut() {
    return authTimeOut;
  }

  public void setAuthTimeOut(long authTimeOut) {
    this.authTimeOut = authTimeOut;
  }

  public boolean isUseMultipleThread() {
    return useMultipleThread;
  }

  public void setUseMultipleThread(boolean useMultipleThread) {
    this.useMultipleThread = useMultipleThread;
  }

  public String getServerCharset() {
    return serverCharset;
  }

  public Executor getReadExecutor() {
    return readExecutor;
  }

  public void setReadExecutor(Executor readExecutor) {
    this.readExecutor = readExecutor;
  }

  public ThreadPoolExecutor getClientSideExecutor() {
    return clientSideExecutor;
  }

  public void setClientSideExecutor(ThreadPoolExecutor clientSideExecutor) {
    this.clientSideExecutor = clientSideExecutor;
  }

  public Executor getServerSideExecutor() {
    return serverSideExecutor;
  }

  public void setServerSideExecutor(Executor serverSideExecutor) {
    this.serverSideExecutor = serverSideExecutor;
  }
  
  public ScheduledExecutorService getIpSyncExecutor() {
    return ipSyncExecutor;
  }

  public void setIpSyncExecutor(ScheduledExecutorService ipSyncExecutor) {
    this.ipSyncExecutor = ipSyncExecutor;
  }

  public int getReadThreadPoolSize() {
    return readThreadPoolSize;
  }

  public void setReadThreadPoolSize(int readThreadPoolSize) {
    this.readThreadPoolSize = readThreadPoolSize;
  }

  public int getClientSideThreadPoolSize() {
    return clientSideThreadPoolSize;
  }

  public void setClientSideThreadPoolSize(int clientSideThreadPoolSize) {
    this.clientSideThreadPoolSize = clientSideThreadPoolSize;
  }

  public int getServerSideThreadPoolSize() {
    return serverSideThreadPoolSize;
  }

  public void setServerSideThreadPoolSize(int serverSideThreadPoolSize) {
    this.serverSideThreadPoolSize = serverSideThreadPoolSize;
  }

  public int getMaxQueueSize() {
    return maxQueueSize;
  }

  public void setMaxQueueSize(int maxQueueSize) {
    this.maxQueueSize = maxQueueSize;
  }

  public int getMaxConnTimeOutCount() {
    return maxConnTimeOutCount;
  }

  public void setMaxConnTimeOutCount(int maxConnTimeOutCount) {
    this.maxConnTimeOutCount = maxConnTimeOutCount;
  }

  public void setServerCharset(String serverCharset) {
    this.serverCharset = serverCharset;
  }

  public int getMaxResult() {
    return maxResult;
  }

  public void setMaxResult(int maxResult) {
    this.maxResult = maxResult;
  }

  public boolean isAutocommit() {
    return autocommit;
  }

  public void setAutocommit(boolean autocommit) {
    this.autocommit = autocommit;
  }

  static class ReNameableThreadExecutor extends ThreadPoolExecutor {

    public ReNameableThreadExecutor(int poolSize) {
      super(poolSize, poolSize, Long.MAX_VALUE, TimeUnit.NANOSECONDS,
          new LinkedBlockingQueue<Runnable>());
    }
  }

  public int getIsolationLevel() {
    return isolationLevel;
  }

  public void setIsolationLevel(int isolationLevel) {
    this.isolationLevel = isolationLevel;
  }
  
  @Override
  public void init() throws InitialisationException {
    readExecutor = new ReNameableThreadExecutor(getReadThreadPoolSize());
    serverSideExecutor = new ReNameableThreadExecutor(getServerSideThreadPoolSize());
    clientSideExecutor = new ReNameableThreadExecutor(getClientSideThreadPoolSize());
    ipSyncExecutor = Executors.newSingleThreadScheduledExecutor();
    
  }
}
