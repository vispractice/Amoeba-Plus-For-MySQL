package com.meidusa.amoeba.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.exception.AmoebaRuntimeException;
import com.meidusa.amoeba.seq.fetcher.SeqFetchService;

/**
 * 具有优先级的程序关闭钩子,需要在服务器关闭的时候进行相关资源释放的操作，都必须实现 {@link Shutdowner} 接口,
 * 并且需要调用 {@link #addShutdowner(Shutdowner)} 注册到这个钩子程序里面
 * 
 * @author Struct
 *
 */
public class PriorityShutdownHook extends Thread{
	private static Logger logger = Logger.getLogger(PriorityShutdownHook.class);
	
	private static PriorityShutdownHook instance = new PriorityShutdownHook();
	
	static{
		Runtime.getRuntime().addShutdownHook(instance);
	}
	
	private List<Shutdowner> shutdowners = Collections.synchronizedList(new ArrayList<Shutdowner>());
	
	
	private PriorityShutdownHook(){}
	
	/**
	 * register a shutdowner 
	 * @param shutdowner 
	 */
	public static void addShutdowner(Shutdowner shutdowner){
		synchronized(instance){
			instance.shutdowners.add(shutdowner);
		}
	}
	
	/**
	 * remove a shutdowner
	 * @param shutdowner
	 */
	public static void removeShutdowner(Shutdowner shutdowner){
		synchronized(instance){
			instance.shutdowners.remove(shutdowner);
		}
	}
	
	public synchronized void run(){
		List<Shutdowner> shutDownTmp = new ArrayList<Shutdowner>();
		shutDownTmp.addAll(shutdowners);
		
		Collections.sort(shutDownTmp, new Comparator<Shutdowner>(){
			public int compare(final Shutdowner o1, final Shutdowner o2) {
				return  o2.getShutdownPriority() - o1.getShutdownPriority();
			}
		});
		
		for(Shutdowner shutdowner : shutDownTmp){
			try{
				shutdowner.shutdown();
				if(logger.isInfoEnabled()){
					logger.info("shutdowner :" + shutdowner +" shutdown completed!");
				}
			}catch(Exception e){
				logger.error("shutdowner invoke shutdown method error",e);
			}
		}
		
	    // 关闭全局序列服务
	    try {
	      SeqFetchService.stop();
	      if (logger.isInfoEnabled()) {
	        logger.info("sequence service is stopped");
	      }
	    } catch (Exception e) {
	      logger.error("stop sequence service error!");
	      throw new AmoebaRuntimeException(e);
	    }
	}
}
