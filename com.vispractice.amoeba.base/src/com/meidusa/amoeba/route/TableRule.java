/**
 * 
 */
package com.meidusa.amoeba.route;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.meidusa.amoeba.parser.dbobject.Table;

public class TableRule implements Serializable {

    private static final long serialVersionUID = 1L;
    public List<Rule>         ruleList         = new ArrayList<Rule>();
    public Table              table;
    public boolean isEnable =  false;
    public String[]           defaultPools;
    public String[]           readPools;
    public String[]           writePools;
}
