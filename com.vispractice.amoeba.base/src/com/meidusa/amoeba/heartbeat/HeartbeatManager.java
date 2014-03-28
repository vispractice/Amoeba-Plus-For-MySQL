package com.meidusa.amoeba.heartbeat;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;

import org.apache.log4j.Logger;

/**
 * 
 * @author Struct
 *
 */
public class HeartbeatManager {
	static Logger logger = Logger.getLogger(HeartbeatManager.class);
	protected static final BlockingQueue<HeartbeatDelayed> HEART_BEAT_QUEUE = new DelayQueue<HeartbeatDelayed>();
	static {
		new Thread() {
			{
				this.setDaemon(true);
				this.setName("HeartbeatManagerThread");
			}

			public void run() {
				HeartbeatDelayed delayed = null;
				while (true) {
					try {
						delayed = HEART_BEAT_QUEUE.take();
						Status status = delayed.doCheck();
						
						if (logger.isDebugEnabled()) {
							logger.debug("checked task taskName="+ delayed.getName() + " ,Status=" + status);
						}
						
						if (delayed.isCycle()) {
							delayed.reset();
							HeartbeatManager.addHeartbeat(delayed);
						}else{
							if (status == Status.INVALID) {
								// delayed.setDelayedTime(5, TimeUnit.SECONDS);
								delayed.reset();
								HeartbeatManager.addHeartbeat(delayed);
							}else{
								delayed.cancel();
							}
						}
					} catch (Exception e) {
						logger.error("check task= " + delayed.getName() + " error");
					}
				}
			}
		}.start();
	}

	public static void addHeartbeat(HeartbeatDelayed delay) {
		if (!HEART_BEAT_QUEUE.contains(delay)) {
			HEART_BEAT_QUEUE.offer(delay);
		}
	}

	public static void removeHeartbeat(HeartbeatDelayed delay) {
		HEART_BEAT_QUEUE.remove(delay);
	}
}