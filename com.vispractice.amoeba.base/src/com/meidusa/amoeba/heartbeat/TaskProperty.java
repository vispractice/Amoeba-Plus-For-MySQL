package com.meidusa.amoeba.heartbeat;


public final class TaskProperty {
	//时间单位为毫秒
	
	// IP 过滤地址刷新时间和重试次数
    public final static int IPRULE_MAX_RETRY_TIMES = 10;
    public final static Long IPRULE_RELOAD_DELAY = 8000l;
    
    private TaskProperty(){
    	
    }
    
}
