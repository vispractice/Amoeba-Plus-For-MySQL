package com.meidusa.amoeba.parser.dbobject;

public class GlobalSeqColumn<T> {
  private T seqObject;
  private String seqName;
  private Long seqValue;
  private int seqTokenEndColumn;
  

  public GlobalSeqColumn(T seqObject, String seqName, Long seqValue, int seqTokenEndColumn) {
    super();
    this.seqObject = seqObject;
    this.seqName = seqName;
    this.seqValue = seqValue;
    this.seqTokenEndColumn = seqTokenEndColumn;
  }

  public T getSeqObject() {
    return seqObject;
  }

  public void setSeqObject(T seqObject) {
    this.seqObject = seqObject;
  }

  public String getSeqName() {
    return seqName;
  }

  public void setSeqName(String seqName) {
    this.seqName = seqName;
  }

  public Long getSeqValue() {
    return seqValue;
  }

  public void setSeqValue(Long seqValue) {
    this.seqValue = seqValue;
  }

  public int getSeqTokenEndColumn() {
    return seqTokenEndColumn;
  }

  public void setSeqTokenEndColumn(int seqTokenEndColumn) {
    this.seqTokenEndColumn = seqTokenEndColumn;
  }

  @Override
  public String toString() {
    return seqObject.toString();
  }
}
