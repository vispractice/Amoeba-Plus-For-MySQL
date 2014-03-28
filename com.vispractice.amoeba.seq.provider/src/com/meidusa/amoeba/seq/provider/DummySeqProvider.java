package com.meidusa.amoeba.seq.provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.meidusa.amoeba.exception.AmoebaRuntimeException;
import com.meidusa.amoeba.parser.dbobject.Table;
import com.meidusa.amoeba.seq.fetcher.SeqProvider;
import com.meidusa.amoeba.seq.fetcher.SeqOperationResult;
import com.meidusa.amoeba.util.StringUtil;

// 一个简单的全局序列发生器

public class DummySeqProvider extends SeqProvider{
  
  private final static Map<DummySeqProvider.Sequence, AtomicLong> seqs = new ConcurrentHashMap<DummySeqProvider.Sequence, AtomicLong>();
  
  @Override
  public SeqOperationResult createSeq(String schema, String seqName, long start, long offset) {
    Sequence seqKey = new Sequence(schema, seqName);
    AtomicLong seqVal = seqs.get(seqKey);
    
    if (seqVal == null) {
      seqVal = new AtomicLong(start);
      seqs.put(seqKey, seqVal);
      return new SeqOperationResult(true, "");
    } else {
      return new SeqOperationResult(false, seqKey +" has existed");
    }
  }

  @Override
  public long getSeqCurrVal(String schema, String seqName) {
    Sequence seqKey = new Sequence(schema, seqName);
    AtomicLong seqVal = seqs.get(seqKey);
    
    if (seqVal == null) {
      throw new AmoebaRuntimeException(seqKey + " not found");
    }
    
    return seqVal.get();
  }

  @Override
  public long getSeqNextVal(String schema, String seqName) {
    Sequence seqKey = new Sequence(schema, seqName);
    AtomicLong seqVal = seqs.get(seqKey);
    
    if (seqVal == null) {
      throw new AmoebaRuntimeException(seqKey + " not found");
    }
    
    return seqVal.incrementAndGet();
  }

  @Override
  public long batchGetSeqVal(String schema, String seqName, long count) {
    Sequence seqKey = new Sequence(schema, seqName);
    AtomicLong seqVal = seqs.get(seqKey);
    
    if (seqVal == null) {
      throw new AmoebaRuntimeException(seqKey + " not found");
    }
    
    return seqVal.addAndGet(count);
  }

  @Override
  public SeqOperationResult deleteSeq(String schema, String seqName) {
    Sequence seqKey = new Sequence(schema, seqName);
    AtomicLong seqVal = seqs.get(seqKey);
    
    if (seqVal == null) {
      return new SeqOperationResult(false, seqKey + " not found");
    }
    else {
      seqs.remove(seqKey);
      return new SeqOperationResult(true, "");
    }
  }
  
  class Sequence{
    public String schema;
    public String seqName;
    
    
    public Sequence(String schema, String seqName) {
      super();
      this.schema = schema;
      this.seqName = seqName;
    }


    @Override
    public String toString() {
      return schema + "." + seqName;
    }


    @Override
    public int hashCode() {
      return 211 + (schema == null ? 0 : schema.toLowerCase().hashCode()) + (seqName == null ? 0 : seqName.toLowerCase().hashCode());
    }


    @Override
    public boolean equals(Object object) {
      boolean isMatched = true;
      if (object instanceof Sequence) {
        Sequence other = (Sequence) object;

        isMatched = isMatched && StringUtil.equalsIgnoreCase(other.schema, schema);
        isMatched = isMatched && StringUtil.equalsIgnoreCase(other.seqName, seqName);

        return isMatched;
      } else {
        return false;
      }
    }
  }
}
