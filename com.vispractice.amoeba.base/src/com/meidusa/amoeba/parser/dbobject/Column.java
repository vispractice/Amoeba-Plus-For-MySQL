/*
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * AFFERO GENERAL PUBLIC LICENSE for more details. You should have received a copy of the GNU AFFERO
 * GENERAL PUBLIC LICENSE along with this program; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package com.meidusa.amoeba.parser.dbobject;

import com.meidusa.amoeba.util.ObjectUtil;

/**
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 * 
 */
public class Column implements DBObjectBase {

  private Table table;
  private String name;
  private String alias;
  private String seqName;

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public Table getTable() {
    return table;
  }

  public void setTable(Table table) {
    this.table = table;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSeqName() {
    return seqName;
  }

  public void setSeqName(String seqName) {
    this.seqName = seqName;
  }

  public String getSql() {
    return (table == null ? name : table.getSql() + "." + name);
  }

  public boolean equals(Object o){
    if(o instanceof Column){
        Column other = (Column)o;
        if(ObjectUtil.equals(table, other.getTable()) && name.equalsIgnoreCase(other.getName())){
            return true;
        }
    }
    return false;
}

  public int hashCode(){
    return 311+ (name==null?0:name.hashCode())+(table == null?0:table.hashCode());
  }

  public String toString() {
    return getSql();
  }
}
