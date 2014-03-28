package com.meidusa.amoeba.seq.fetcher;

import org.apache.log4j.Logger;


public class SeqFetchService {
  
  private static SeqProvider provider;
  protected static Logger logger = Logger.getLogger(SeqFetchService.class);

  
  public static SeqOperationResult createSeq(String schema, String seqName, long start, long offset) {
    checkProviderExisted();
    return provider.createSeq(schema, seqName, start, offset);
  }
  
  public static long getSeqCurrVal(String schema, String seqName) {
    checkProviderExisted();
    return provider.getSeqCurrVal(schema, seqName);
  }
  
  public static long getSeqNextVal(String schema, String seqName) {
    checkProviderExisted();
    return provider.getSeqNextVal(schema, seqName);
  }
  
  public static long batchGetSeqVal(String schema, String seqName, long count) {
    checkProviderExisted();
    return provider.batchGetSeqVal(schema, seqName, count);
  }
  
  public static SeqOperationResult deleteSeq(String schema, String seqName) {
    checkProviderExisted();
    return provider.deleteSeq(schema, seqName);
  }
  
  public void setProvider(SeqProvider provider) {
    SeqFetchService.provider = provider;
  }
  
  public void removeProvider() {
    if (provider != null) {
      try {
        provider.stop();
      } catch (Exception e) {
        logger.error(String.format("stop sequence number provide service error, since %s", e.getMessage()));
      }
    }
    provider = null;
  }
  
  public static void init() throws Exception {
    if (provider != null ) {
      provider.init();
      if (logger.isInfoEnabled()) {
        logger.info("sequence service is ready");
      }
    }
  }
  
  public static void stop() throws Exception {
    if (provider != null) {
      provider.stop();
    }
  }
  
  public static void checkProviderExisted() {
    if (provider == null) {
      throw new RuntimeException("no sequence number service provider found");
    }
  }
 
}
