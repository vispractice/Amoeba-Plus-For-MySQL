package com.meidusa.amoeba.parser.statement.ddl;

import com.meidusa.amoeba.parser.dbobject.Schema;

public class DDLDropSequenceStatement extends DDLStatement{
  private Schema schema;
  private String seqName;
  
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
