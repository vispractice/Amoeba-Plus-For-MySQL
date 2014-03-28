package com.meidusa.amoeba.config.loader;

import java.util.List;
import java.util.Map;

import com.meidusa.amoeba.parser.dbobject.Table;
import com.meidusa.amoeba.route.TableRule;

public interface TableRuleLoader {
	Map<Table, TableRule> loadRule();
	Map<Table, TableRule> reLoadRule();

	boolean needLoad();
	Map<Table, TableRule> loadRule(List<Long> ids);
}
