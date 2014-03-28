package com.meidusa.amoeba.seq.provider;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.zookeeper.data.Stat;

import com.meidusa.amoeba.exception.AmoebaRuntimeException;
import com.meidusa.amoeba.seq.fetcher.SeqOperationResult;
import com.meidusa.amoeba.seq.fetcher.SeqProvider;
import com.meidusa.amoeba.seq.provider.utils.Utils;
import com.netflix.curator.RetryPolicy;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.framework.recipes.atomic.AtomicValue;
import com.netflix.curator.framework.recipes.atomic.DistributedAtomicLong;
import com.netflix.curator.framework.recipes.atomic.PromotedToLock;
import com.netflix.curator.retry.ExponentialBackoffRetry;

/**
 * zookeeper 不是一个高并发系统，所以把所有办法弄成同步的以提高响应时间
 * 但是降低吞吐量
 * 
 * 这种生成方式暂不实现offset，如需支持，可以增加一个offset节点来实现
 * 
 * @author WangFei
 *
 */
public class SeqProviderBasedOnZK extends SeqProvider{
  private static final String ROOT_PATH = "/seq";
  private static enum FETCH_OPERATION { CURR, NEXT, BATCH};
  private static final int RETRY_TIMES = Integer.MAX_VALUE;
  private static final int BASE_SLEEP_TIME = 1000;
  
  private CuratorFramework client;
  
  private static final Logger log = Logger.getLogger(SeqProviderBasedOnZK.class);
  
  /**
   * 创建某个序列
   */
  @Override
  public synchronized SeqOperationResult createSeq(String schema, String seqName, long start, long offset) {
    SeqOperationResult result = null;
    
    // 节点路径
    String idPath = String.format("%s/%s/%s", ROOT_PATH, schema, seqName);
    
    try {
      if (checkNodeExisted(idPath, client)) {
        result = new SeqOperationResult(false, String.format("sequence %s is existed", seqName));
      }
      else {
        DistributedAtomicLong dal = buildDAL(idPath, false);
        dal.forceSet(start);
        result = new SeqOperationResult(true, "");
      }
    } catch (Exception e) {
      throw new AmoebaRuntimeException(e.getMessage());
    }
    
    return result;
  }
  
  // 当前序列值
  @Override
  public synchronized long getSeqCurrVal(String schema, String seqName) {
    return commonGetSeqVal(schema, seqName, null, FETCH_OPERATION.CURR);
  }
  
  // 下一个序列值
  @Override
  public synchronized long getSeqNextVal(String schema, String seqName) {
    return commonGetSeqVal(schema, seqName, null, FETCH_OPERATION.NEXT);
  }

  // 批量获取序列值
  @Override
  public synchronized long batchGetSeqVal(String schema, String seqName, long count) {
    return commonGetSeqVal(schema, seqName, count, FETCH_OPERATION.BATCH);
  }
  
  /**
   * 删除某个全局序列
   */
  @Override
  public synchronized SeqOperationResult deleteSeq(String schema, String seqName) {
    SeqOperationResult result = null;
    // 节点路径
    String idPath = String.format("%s/%s/%s", ROOT_PATH, schema, seqName);
    
    try {
      if (checkNodeExisted(idPath, client)) {
        client.delete().forPath(idPath);
        result = new SeqOperationResult(true, "");
      }
      else {
        result = new SeqOperationResult(false, String.format("sequence %s is not existed", seqName));
      }
    } catch (Exception e) {
      throw new AmoebaRuntimeException(e.getMessage());
    }
    
    return result;
  }
  
 //检查节点是否已经存在
 private synchronized boolean checkNodeExisted(String path, CuratorFramework client) throws Exception {
   
   Stat stat = client.checkExists().forPath(path);
   
   if (stat != null) {
     return true;
   }
   
   return false;
 }
 
 /**
  * 提取获取全局序列操作通用流程
  * 
  * @param schema
  * @param seqName
  * @param count
  * @param operation
  * @return
  */
 private long commonGetSeqVal(String schema, String seqName, Long count, FETCH_OPERATION operation) { 
   long id = -1;
   
   // 节点路径
   String idPath = String.format("%s/%s/%s", ROOT_PATH, schema, seqName);
   
   try {
     if (checkNodeExisted(idPath, client)) {
       client.sync(idPath, null);   // 获取前，先从leader那里同步 !! 异步操作，但是有序 !!
       
       DistributedAtomicLong dal = buildDAL(idPath, true);
       
       switch (operation) {
         
         // 获取当前全局序列值
         case CURR:
           AtomicValue<Long> currValue = dal.get();
           if(currValue.succeeded()) {
             id = currValue.postValue();
           }
           else {
            throw new AmoebaRuntimeException("fetch from id server error");
          }
           
           break;
         
         // 获取下一个全局序列
         case NEXT:
           AtomicValue<Long> nextValue = dal.increment();
           if (nextValue.succeeded()) {
            id = nextValue.postValue();
           }
           else {
             throw new AmoebaRuntimeException("fetch from id server error");
           }
           break;
         
         // 批量获取全局序列  
         case BATCH:
           AtomicValue<Long> startValue = dal.get();
           id = startValue.postValue()+count;
           if (startValue.succeeded()) {
             dal.forceSet(id);
           }
           break;
           
         default:
           throw new AmoebaRuntimeException("not support this fetch method");
       }
     }
     else {
       throw new AmoebaRuntimeException(String.format("sequence %s is not existed", seqName));
     }
   } catch (Exception e) {
     throw new AmoebaRuntimeException(e.getMessage());
   }
   
   return id;
 }
 
 /**
  * 全局序列的 创建/获取/删除都通过DistributedAtomicLong接口去操作
  * 而且操作前，使用互斥的方式访问某个节点，避免并发的时候，节点并发读写出错
  * 
  * @param path
  * @param needLock
  * @return
  */
 private DistributedAtomicLong buildDAL(String path, boolean needLock) {
   RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, RETRY_TIMES);
   PromotedToLock lock = PromotedToLock.builder().lockPath(path).build();
   DistributedAtomicLong dal = new DistributedAtomicLong(client, path, retryPolicy);
   
   if (needLock) {
    dal = new DistributedAtomicLong(client, path, retryPolicy, lock);
  }
   
   return dal;
 }

 /**
  * Amoeba启动的时候，初始化zookeeper客户端，另外，给zookeeper预热
  */
 public void init() throws Exception {
   
    Properties props = Utils.readGlobalSeqConfigProps();
    String zkHosts = props.getProperty("zkHosts", "127.0.0.1:2181");
    Integer connTimeOut = Integer.valueOf(props.getProperty("zkConnTimeOut", "60"));
    
    log.info(String.format("zookeeper config: connect string= %s", zkHosts));
    log.info(String.format("zookeeper config: connect timeout= %d seconds", connTimeOut));
    
    client = CuratorFrameworkFactory.builder()
        .connectString(zkHosts).retryPolicy(new ExponentialBackoffRetry(BASE_SLEEP_TIME, RETRY_TIMES))
        .connectionTimeoutMs(connTimeOut*1000).build();  // 时间要由秒转成毫秒
    
    client.start();
    
    // 只是为client热身而调用
    client.checkExists().forPath(ROOT_PATH);
    
  }
  
 /**
  * Amoeba所在jvm停止时，也要把zookeeper客户端停掉
  */
  public void stop() throws Exception {
    if (client != null) {
      client.close();
    }
  }
}
