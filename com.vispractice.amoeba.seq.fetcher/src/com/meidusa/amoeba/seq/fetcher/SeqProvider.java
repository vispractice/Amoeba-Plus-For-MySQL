package com.meidusa.amoeba.seq.fetcher;

public abstract class SeqProvider {
  public abstract SeqOperationResult createSeq(String schema, String seqName, long start, long offset);
  public abstract long getSeqCurrVal(String schema, String seqName);
  public abstract long getSeqNextVal(String schema, String seqName);
  public abstract long batchGetSeqVal(String schema, String seqName, long count);
  public abstract SeqOperationResult deleteSeq(String schema, String seqName);
  
  public void init() throws Exception {
    
  }
  
  public void stop() throws Exception {
    
  }
}
