package com.meidusa.amoeba.parser.statement.ddl;

import com.meidusa.amoeba.parser.dbobject.Schema;

public class DDLCreateSequenceStatenment extends DDLStatement{
  private long startWith = 1;
  private long offset = 1;
  
  private Schema schema;
  private String seqName;
  
  public long getStartWith() {
    return startWith;
  }
  public void setStartWith(long startWith) {
    this.startWith = startWith;
  }
  public long getOffset() {
    return offset;
  }
  public void setOffset(long offset) {
    this.offset = offset;
  }

  public Schema getSchema() {
    return schema;
  }
  public void setSchema(Schema schema) {
    this.schema = schema;
  }
  
  public String getSeqName() {
    return seqName;
  }
  public void setSeqName(String seqName) {
    this.seqName = seqName;
  }
  
  
}
