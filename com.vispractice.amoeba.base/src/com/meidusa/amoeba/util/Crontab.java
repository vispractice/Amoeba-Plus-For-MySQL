package com.meidusa.amoeba.util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

public class Crontab {
	protected static Logger logger = Logger .getLogger(Crontab.class);

	// 启动定时任务，提供简单的重试
 	public static void startRuleConfigUpdateSchedule(final ScheduledExecutorService service, 
 													   final Runnable task,
 													   final AtomicInteger retryTimesCounter,
 													   final Long RELOAD_DELAY,
 													   final int MAX_RETRY_TIMES,
 													   final boolean isStartRigthNow) {
 		
 		ScheduledFuture<?> delayFuture = null;
 		if (isStartRigthNow) {
			delayFuture = service.scheduleWithFixedDelay(task, 0, RELOAD_DELAY, TimeUnit.MILLISECONDS);
		} else {
			delayFuture = service.scheduleWithFixedDelay(task, RELOAD_DELAY, RELOAD_DELAY, TimeUnit.MILLISECONDS);
		}
 				
 		final ScheduledFuture<?> future = delayFuture;
		
		Runnable watchdog = new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						future.get();
						// 请不要在这里添加其他代码，
						// 因为会future会将控制权转给ScheduledExecutorService
						// 这里永远不会被执行到
					} catch (Exception e) {
						logger.error("Exception thrown in Cron task, try to recover, task name: " + (task == null ? "null" : task.getClass()) );
				        future.cancel(true);
				        int i = retryTimesCounter.incrementAndGet();
				        
				        // 重试
				        if (i <= MAX_RETRY_TIMES) {
				        	if (!service.isShutdown() && !service.isTerminated()) {
				        		boolean isStartRightNow = false;	// 不要马上重试
				        		startRuleConfigUpdateSchedule(service, task, retryTimesCounter, RELOAD_DELAY, MAX_RETRY_TIMES, isStartRightNow);
							}
				        } else {
				        	logger.error("I have try my best to recover, but it doesn't work, task " + (task == null ? "null" : task.getClass()) + " fail");
				        	service.shutdown();
				        } 
				        return;		// 一定不要删了return，否则出异常的时候，不能结束这个watchdog线程
				    }
				}
			}
		};
		new Thread(watchdog).start();
 	}
}
