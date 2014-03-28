package com.meidusa.amoeba.route;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.meidusa.amoeba.parser.dbobject.Column;
import com.meidusa.amoeba.sqljep.RowJEP;

public class Rule implements Serializable {

    private static final long   serialVersionUID = 1L;
    
    public String               name;
    public String               group;
    public boolean              ignoreArray;
    public boolean              isSwitch;
    public RuleResult result = RuleResult.BOOLEAN;
    
    public Map<String, Integer> parameterMap     = new HashMap<String, Integer>();
    public List<Column>         excludes         = new ArrayList<Column>();
    public Map<Column, Integer> cloumnMap        = new HashMap<Column, Integer>();

    public String               expression;
    public transient RowJEP     rowJep;

    public String[]             defaultPools;
    public String[]             readPools;
    public String[]             writePools;
    
    public boolean isEnable;
}
