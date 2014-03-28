/**
 * <pre>
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU AFFERO GENERAL PUBLIC LICENSE for more details. 
 * 	You should have received a copy of the GNU AFFERO GENERAL PUBLIC LICENSE along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * </pre>
 */
package com.meidusa.amoeba.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;

import com.meidusa.amoeba.config.BeanObjectEntityConfig;
import com.meidusa.amoeba.config.DBServerConfig;
import com.meidusa.amoeba.config.ProxyServerConfig;
import com.meidusa.amoeba.config.UserConfig;
import com.meidusa.amoeba.config.loader.AmoebaContextLoader;
import com.meidusa.amoeba.exception.AmoebaRuntimeException;
import com.meidusa.amoeba.exception.ConfigurationException;
import com.meidusa.amoeba.exception.InitialisationException;
import com.meidusa.amoeba.exception.UpdateDBServerRuntimeException;
import com.meidusa.amoeba.heartbeat.HeartbeatDelayed;
import com.meidusa.amoeba.heartbeat.Status;
import com.meidusa.amoeba.net.ConnectionManager;
import com.meidusa.amoeba.net.poolable.MultipleLoadBalanceObjectPool;
import com.meidusa.amoeba.net.poolable.ObjectPool;
import com.meidusa.amoeba.net.poolable.PoolableObject;
import com.meidusa.amoeba.route.QueryRouter;
import com.meidusa.amoeba.seq.fetcher.SeqFetchService;
import com.meidusa.amoeba.util.Initialisable;
import com.meidusa.amoeba.util.Reporter;
import com.meidusa.amoeba.util.StringUtil;

/**
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public class ProxyRuntimeContext implements Reporter {

  public static final String DEFAULT_SERVER_CONNECTION_MANAGER_CLASS =
      "com.meidusa.amoeba.net.AuthingableConnectionManager";
  public static final String DEFAULT_REAL_POOL_CLASS =
      "com.meidusa.amoeba.net.poolable.PoolableObjectPool";
  public static final String DEFAULT_VIRTUAL_POOL_CLASS =
      "com.meidusa.amoeba.server.MultipleServerPool";

  protected static Logger logger = Logger.getLogger(ProxyRuntimeContext.class);

  private static ProxyRuntimeContext context = null;

  private ProxyServerConfig config;

  private Map<String, ConnectionManager> conMgrMap =
      new ConcurrentHashMap<String, ConnectionManager>();

  private Map<String, ObjectPool> poolMap = new ConcurrentHashMap<String, ObjectPool>();
  private Map<String, UserConfig> userMap = new ConcurrentHashMap<String, UserConfig>();

  private List<ContextChangedListener> listeners =
      new CopyOnWriteArrayList<ContextChangedListener>();

  @SuppressWarnings("unchecked")
  private QueryRouter queryRouter;
  private RuntimeContext runtimeContext;

  private Map<String, Object> beanContext = new ConcurrentHashMap<String, Object>();
  
  //可能是mysql, mongodb, aladdin 插件中的一个，为了获得他们的类加载器
  private Bundle backendBundle; 
  
  // 兼容文件加载
  private String amoebaHomePath;
  
  // 后端因超负载，验证超时个数
  private AtomicInteger connTimeOutCount;
  
  private AmoebaContextLoader amoebaLoader;
  
  public void setAmoebaLoader(AmoebaContextLoader amoebaLoader) {
    this.amoebaLoader = amoebaLoader;
  }
  
  public RuntimeContext getRuntimeContext() {
    return runtimeContext;
  }

  public static ProxyRuntimeContext getInstance() {
    return context;
  }
  
  public boolean isUserExisted(String username) {
    if (userMap.containsKey(username)) {
      return true;
    }
    
    return false;
  }
  
  public UserConfig getUserConfigByName(String username) {
    return userMap.get(username);
  }

  public static void setInstance(ProxyRuntimeContext context) {
    ProxyRuntimeContext.context = context;
  }

  public String getDefaultServerConnectionManagerClassName() {
    return DEFAULT_SERVER_CONNECTION_MANAGER_CLASS;
  }

  public String getDefaultRealPoolClassName() {
    return DEFAULT_REAL_POOL_CLASS;
  }

  public String getDefaultVirtualPoolClassName() {
    return DEFAULT_VIRTUAL_POOL_CLASS;
  }

  public ProxyServerConfig getConfig() {
    return config;
  }

  public QueryRouter getQueryRouter() {
    return queryRouter;
  }

  public Bundle getBackendBundle() {
    return backendBundle;
  }

  public void setBackendBundle(Bundle backendBundle) {
    this.backendBundle = backendBundle;
  }

  public Map<String, ConnectionManager> getConnectionManagerList() {
    return conMgrMap;
  }

  public Map<String, ObjectPool> getPoolMap() {
    return poolMap;
  }

  public Map<String, UserConfig> getUserMap() {
    return userMap;
  }

  public String getAmoebaHomePath() {
    return amoebaHomePath;
  }

  public void setAmoebaHomePath(String amoebaHomePath) {
    this.amoebaHomePath = amoebaHomePath;
  }

  public AtomicInteger getConnTimeOutCount() {
    return connTimeOutCount;
  }

  private List<Initialisable> initialisableList = new ArrayList<Initialisable>();

  /**
   * db server间的配置继承
   * 
   * @param parent
   * @param dest
   * @return
   */
  protected void inheritDBServerConfig(DBServerConfig parent, DBServerConfig dest) {
    BeanObjectEntityConfig destBeanConfig = dest.getFactoryConfig();
    BeanObjectEntityConfig parentBeanConfig = parent.getFactoryConfig();

    if (destBeanConfig != null) {
      if (parentBeanConfig != null) {
        inheritBeanObjectEntityConfig(parentBeanConfig, destBeanConfig);
      }
    } else {
      dest.setFactoryConfig(parentBeanConfig);
    }

    destBeanConfig = dest.getPoolConfig();
    parentBeanConfig = parent.getPoolConfig();

    if (destBeanConfig != null) {
      if (parentBeanConfig != null) {
        inheritBeanObjectEntityConfig(parentBeanConfig, destBeanConfig);
      }
    } else {
      dest.setPoolConfig(parentBeanConfig);
    }

    if (dest.getVirtual() == null) {
      dest.setVirtual(parent.getVirtual());
    }

    if (dest.getAbstractive() == null) {
      dest.setAbstractive(parent.getAbstractive());
    }

  }

  public void addContextChangedListener(ContextChangedListener listener) {
    if (!listeners.contains(listener)) {
      this.listeners.add(listener);
    }
  }

  public void removeContextChangedListener(ContextChangedListener listener) {
    this.listeners.remove(listener);
  }

  public void notifyAllContextChangedListener() {
    for (ContextChangedListener listener : listeners) {
      listener.doChange();
    }
  }


  protected void inheritBeanObjectEntityConfig(BeanObjectEntityConfig parent,
      BeanObjectEntityConfig dest) {
    
    BeanObjectEntityConfig parentCloned = (BeanObjectEntityConfig) parent.clone();
    
    if (!StringUtil.isEmpty(dest.getClassName())) {
      parentCloned.setClassName(dest.getClassName());
    }

    /*
     * if(!StringUtil.isEmpty(dest.getName())){ parentCloned.setName(dest.getName()); }
     */

    if (dest.getParams() != null) {
      if (parentCloned.getParams() == null) {
        parentCloned.setParams(dest.getParams());
      } else {
        parentCloned.getParams().putAll(dest.getParams());
      }
    }

    dest.setClassName(parentCloned.getClassName());
    // dest.setName(parentCloned.getName());
    dest.setParams(parentCloned.getParams());
  }


  public Object createBeanObjectEntity(BeanObjectEntityConfig config, boolean initEarly) {
    Object object = config.createBeanObject(initEarly);
    if (!StringUtil.isEmpty(config.getName())) {
      beanContext.put(config.getName(), object);
    }
    return object;
  }

  public void initConfig() {
    config = amoebaLoader.loadConfig();
  }

  public void init() {

    this.runtimeContext = (RuntimeContext) createBeanObjectEntity(config.getRuntimeConfig(), true);
    /*
     * for (Map.Entry<String, BeanObjectEntityConfig> entry : config.getManagers().entrySet()) {
     * BeanObjectEntityConfig beanObjectEntityConfig = entry.getValue(); try { ConnectionManager
     * manager = (ConnectionManager) beanObjectEntityConfig.createBeanObject(false);
     * manager.setName(entry.getKey()); initialisableList.add(manager);
     * conMgrMap.put(manager.getName(), manager); } catch (Exception e) { throw new
     * ConfigurationException("manager instance error", e); } }
     */

    for (Map.Entry<String, DBServerConfig> entry : config.getDbServers().entrySet()) {
      DBServerConfig dbServerConfig = (DBServerConfig) entry.getValue().clone();
      String parent = dbServerConfig.getParent();
      if (!StringUtil.isEmpty(parent)) {
        DBServerConfig parentConfig = config.getDbServers().get(parent);
        if (parentConfig == null || parentConfig.getParent() != null) {
          throw new ConfigurationException(entry.getKey() + " cannot found parent with name="
              + parent + " or parent's parent must be null");
        }
        inheritDBServerConfig(parentConfig, dbServerConfig);
      }

      // ignore if dbServer is abstract
      if (dbServerConfig.getAbstractive() != null && dbServerConfig.getAbstractive().booleanValue()) {
        continue;
      }

      try {
        BeanObjectEntityConfig poolConfig = dbServerConfig.getPoolConfig();
        ObjectPool pool = (ObjectPool) poolConfig.createBeanObject(false, conMgrMap);
        pool.setName(StringUtil.isEmpty(dbServerConfig.getName())
            ? poolConfig.getName()
            : dbServerConfig.getName());

        if (pool instanceof Initialisable) {
          initialisableList.add((Initialisable) pool);
        }
        // 一般来说，virtual的factory config为null，但是为了避免因为错误配置而导致初始化virtual pool，这里需要
        // 排除virtual pool
        if (dbServerConfig.getFactoryConfig() != null && !dbServerConfig.getVirtual()) {
          PoolableObjectFactory factory =
              (PoolableObjectFactory) dbServerConfig.getFactoryConfig().createBeanObject(false,
                  conMgrMap);
          if (factory instanceof Initialisable) {
            initialisableList.add((Initialisable) factory);
          }
          pool.setFactory(factory);
        }
        poolMap.put(entry.getKey(), pool); 
      } catch (Exception e) {
        throw new ConfigurationException("createBean error dbServer=" + dbServerConfig.getName(), e);
      }
    }

    if (config.getQueryRouterConfig() != null) {
      BeanObjectEntityConfig queryRouterConfig = config.getQueryRouterConfig();
      try {
        queryRouter = (QueryRouter) queryRouterConfig.createBeanObject(false, conMgrMap);
        if (queryRouter instanceof Initialisable) {
          initialisableList.add((Initialisable) queryRouter);
        }
        if (queryRouter instanceof ContextChangedListener) {
          this.addContextChangedListener((ContextChangedListener) queryRouter);
        }
      } catch (Exception e) {
        throw new ConfigurationException("queryRouter instance error", e);
      }
    }

    initAllInitialisableBeans();
    initialisableList.clear();
    for (ConnectionManager cm : getConnectionManagerList().values()) {
      cm.start();
    }
    initPools();
    
    
    connTimeOutCount = new AtomicInteger(0);
    
    // 全局序列服务初始化
    try {
      SeqFetchService.init();
    } catch (Exception e) {
      logger.error("init sequence service error!");
      throw new AmoebaRuntimeException(e);
    }
    
  }

  protected void initPools() {
    for (Map.Entry<String, ObjectPool> entry : poolMap.entrySet()) {
      ObjectPool pool = entry.getValue();
      if (pool instanceof MultipleLoadBalanceObjectPool) {
        MultipleLoadBalanceObjectPool multiPool = (MultipleLoadBalanceObjectPool) pool;
        multiPool.initAllPools();
      } else {
        PoolableObject object = null;
        try {
          object = (PoolableObject) pool.borrowObject();
        } catch (Exception e) {
          logger.error("init pool " + pool.getName() + " error!", e);
        } finally {
          if (object != null) {
            try {
              pool.returnObject(object);
            } catch (Exception e) {
              logger.error("return init pools error", e);
            }
          }
        }
      }

      if (pool instanceof ContextChangedListener) {
        this.addContextChangedListener((ContextChangedListener) pool);
      }
    }
  }

  private void initAllInitialisableBeans() {
    for (Initialisable bean : initialisableList) {
      try {
        bean.init();
        if (bean instanceof ContextChangedListener) {
          this.addContextChangedListener((ContextChangedListener) bean);
        }
      } catch (InitialisationException e) {
        throw new ConfigurationException("Initialisation bean=" + bean + " error", e);
      }
    }
  }

  

  public void appendReport(StringBuilder buffer, long now, long sinceLast, boolean reset,
      Level level) {
    for (Map.Entry<String, ObjectPool> entry : getPoolMap().entrySet()) {
      ObjectPool pool = entry.getValue();
      String poolName = entry.getKey();
      buffer.append("* Server pool=").append(poolName == null ? "default pool" : poolName)
          .append("\n").append(" - pool active Size=").append(pool.getNumActive());
      buffer.append(", pool Idle size=").append(pool.getNumIdle()).append(StringUtil.LINE_SEPARATOR);
    }
  }


  static class CloseObjectPoolHeartbeatDelayed extends HeartbeatDelayed {
    private ObjectPool pool;

    public CloseObjectPoolHeartbeatDelayed(long nsTime, TimeUnit timeUnit, ObjectPool pool) {
      super(nsTime, timeUnit);
      this.pool = pool;
    }

    @Override
    public Status doCheck() {
      if (pool.getNumActive() == 0) {
        return Status.VALID;
      }
      return null;
    }

    public boolean isCycle() {
      return false;
    }

    public void cancel() {
      try {
        this.pool.close();
      } catch (Exception e) {
        // TODO handle exception
        logger.warn("close pool error", e);
      }
    }

    public boolean equals(Object obj) {
      if (obj instanceof CloseObjectPoolHeartbeatDelayed) {
        CloseObjectPoolHeartbeatDelayed other = (CloseObjectPoolHeartbeatDelayed) obj;
        return other.pool == this.pool && this.getClass() == obj.getClass();
      } else {
        return false;
      }
    }

    public int hashCode() {
      return pool == null ? this.getClass().hashCode() : this.getClass().hashCode()
          + pool.hashCode();
    }

    @Override
    public String getName() {
      return "closing Pool=" + pool.getName();
    }

  }

  private ObjectPool createObjectPool(DBServerConfig dbServerConfig)
      throws UpdateDBServerRuntimeException {
    ObjectPool pool = null;
    try {
      BeanObjectEntityConfig poolConfig = dbServerConfig.getPoolConfig();
      pool = (ObjectPool) createBeanObjectEntity(poolConfig, true);
      pool.setName(StringUtil.isEmpty(dbServerConfig.getName())
          ? poolConfig.getName()
          : dbServerConfig.getName());

      if (dbServerConfig.getFactoryConfig() != null && !dbServerConfig.getVirtual()) {
        PoolableObjectFactory factory =
            (PoolableObjectFactory) dbServerConfig.getFactoryConfig().createBeanObject(true,
                conMgrMap);
        pool.setFactory(factory);
      }
    } catch (Exception e) {
      String poolName = dbServerConfig.getName();
      String msg = String.format("Create pool %s bean error", poolName);
      logger.error(msg, e);
      throw new UpdateDBServerRuntimeException(poolName, msg, e);
    }

    if (pool instanceof MultipleLoadBalanceObjectPool) {
      MultipleLoadBalanceObjectPool multiPool = (MultipleLoadBalanceObjectPool) pool;
      multiPool.initAllPools();
    } else {
      PoolableObject object = null;
      try {
        object = (PoolableObject) pool.borrowObject();
      } catch (Exception e) {
        String poolName = pool.getName();
        String msg = String.format("init pool %s error", poolName);
        logger.error(msg, e);
        throw new UpdateDBServerRuntimeException(poolName, msg, e);
      } finally {
        if (object != null) {
          try {
            pool.returnObject(object);
          } catch (Exception e) {
            String poolName = pool.getName();
            String msg = String.format("return init pool %s error", poolName);
            logger.error(msg, e);
            throw new UpdateDBServerRuntimeException(poolName, msg, e);
          }
        }
      }
    }

    return pool;
  }

  public void closePool(ObjectPool pool) {
    if (pool != null) {
      try {
        pool.close();
      } catch (Exception e) {
        String poolName = pool.getName();
        String msg = String.format("close pool %s error", poolName);
        throw new UpdateDBServerRuntimeException(poolName, msg, e);
      }
    }
  }
  
  /**
   * 更新单个DB Server
   * 捕获所有异常，然后简单跳过异常，这样可以在批量更新多个实例的时候，出错的实例不影响正常的实例
   * 
   * @param sourceConfig 单个db server 配置
   * @param tryUpdate 是否是真实更新或是只是尝试更新
   * @param dbServers 内存（缓存） 中的db server 配置映射
   * @param poolMap 内存（缓存） 中的pool 配置映射
   * @param ctxChangedListeners 内存（缓存）中的ConetextListner列表
   * @throws ConfigurationException
   */
  public void updateDBServer(DBServerConfig sourceConfig, boolean tryUpdate,
      Map<String, DBServerConfig> dbServers, Map<String, ObjectPool> poolMap,
      List<ContextChangedListener> ctxChangedListeners) {
    
    try {
      if (sourceConfig == null) {
        throw new ConfigurationException("config cannot be null");
      }

      boolean abstractive = sourceConfig.getAbstractive();

      // 尝试更新，并不做实际操作
      if (tryUpdate) {
        // try to create ObjectPool with this sourceConfig
        if (!abstractive) {
          DBServerConfig config = (DBServerConfig) sourceConfig.clone();
          if (sourceConfig.getParent() != null) {
            DBServerConfig parent = dbServers.get(config.getParent());
            if (parent == null || !parent.isEnable()) {
              throw new ConfigurationException("parent config withe name=" + config.getParent()
                  + " not found or disable");
            }

            this.inheritDBServerConfig(parent, config);
          }

          if (sourceConfig.isEnable()) {
            ObjectPool pool = createObjectPool(config);
            closePool(pool);
          } else {
            dbServers.remove(sourceConfig.getName());
          }

        } else {
          if (sourceConfig.isEnable()) {
            for (Map.Entry<String, DBServerConfig> entry : dbServers.entrySet()) {
              if (StringUtil.equals(entry.getValue().getParent(), sourceConfig.getName())) {
                if (!entry.getValue().getAbstractive()) {
                  DBServerConfig child = (DBServerConfig) entry.getValue().clone();
                  this.inheritDBServerConfig(sourceConfig, child);
                  ObjectPool pool = createObjectPool(child);
                  closePool(pool);
                  break;
                }
              }
            }
          }
        }

      }
      // 实际更新，重建连接，刷新缓存map
      else {

        /**
         * close old objectPool if this configuration is abstractive then close all children's
         * objectPools else the old ObjectPool will be closed
         * 
         */
        if (!abstractive) {
          DBServerConfig config = (DBServerConfig) sourceConfig.clone();
          if (sourceConfig.getParent() != null) {
            DBServerConfig parent = dbServers.get(sourceConfig.getParent());
            if (parent == null || !parent.isEnable()) {
              throw new ConfigurationException("parent config withe name=" + sourceConfig.getParent()
                  + " not found or disable");
            }

            this.inheritDBServerConfig(parent, config);
          }

          // close old ObjectPool
          ObjectPool oldObjectPool = poolMap.get(sourceConfig.getName());

          if (sourceConfig.isEnable()) {
            ObjectPool pool = createObjectPool(config);

            if (pool != null) {
              poolMap.put(sourceConfig.getName(), pool);
            }

            // 新增了一个ContextChangedListener
            if (oldObjectPool == null) {
              if (pool instanceof ContextChangedListener) {
                ctxChangedListeners.add((ContextChangedListener) pool);
              }
            }

          } else {
            dbServers.remove(sourceConfig.getName());
            poolMap.remove(sourceConfig.getName());

            // 删除了一个ContextChangedListener
            if (oldObjectPool != null) {
              if (oldObjectPool instanceof ContextChangedListener) {
                ctxChangedListeners.remove((ContextChangedListener) oldObjectPool);
              }
            }
          }

          if (oldObjectPool != null) {
            closePool(oldObjectPool);
          }
        }
        // update abstractive pool
        else {
          for (Map.Entry<String, DBServerConfig> entry : dbServers.entrySet()) {
            if (StringUtil.equals(entry.getValue().getParent(), sourceConfig.getName())) {
              if (!entry.getValue().getAbstractive()) {

                DBServerConfig child = (DBServerConfig) entry.getValue().clone();
                this.inheritDBServerConfig(sourceConfig, child);

                // close all children's ObjectPools
                ObjectPool oldObjectPool = poolMap.get(child.getName());

                if (sourceConfig.isEnable()) {
                  ObjectPool pool = createObjectPool(child);

                  if (pool != null) {
                    poolMap.put(child.getName(), pool);
                  }

                  // 新增了一个ContextChangedListener
                  if (oldObjectPool == null) {
                    if (pool instanceof ContextChangedListener) {
                      ctxChangedListeners.add((ContextChangedListener) pool);
                    }
                  }
                } else {
                  dbServers.remove(child.getName());
                  poolMap.remove(child.getName());

                  // 删除了一个ContextChangedListener
                  if (oldObjectPool != null) {
                    if (oldObjectPool instanceof ContextChangedListener) {
                      ctxChangedListeners.remove((ContextChangedListener) oldObjectPool);
                    }
                  }
                }

                if (oldObjectPool != null) {
                  closePool(oldObjectPool);
                }
              }
            }
          }

          if (!sourceConfig.isEnable()) {
            dbServers.remove(sourceConfig.getName());
            poolMap.remove(sourceConfig.getName());
          }
        }
      }
    
    } catch (Exception e) {
      String poolName = sourceConfig == null ? "unknown" : sourceConfig.getName();
      logger.error(String.format("update db pool name=%s failed since %s", poolName, e.getMessage()));
    }
  }
}
