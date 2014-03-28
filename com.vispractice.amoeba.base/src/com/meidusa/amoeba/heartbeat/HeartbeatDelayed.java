package com.meidusa.amoeba.heartbeat;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * @author Struct
 *
 */
public abstract class HeartbeatDelayed implements Delayed {

        private long                          time;
        /** Sequence number to break ties FIFO */
        private final long                    sequenceNumber;
        private long                          nano_origin = System.nanoTime();
        private static final AtomicLong       sequencer   = new AtomicLong(0);
        private long nextFireTime = nano_origin;
        
        public boolean isCycle(){
        	return false;
        }
        
		public HeartbeatDelayed(long nsTime, TimeUnit timeUnit){
            this.time = TimeUnit.NANOSECONDS.convert(nsTime, timeUnit);
            this.sequenceNumber = sequencer.getAndIncrement();
            nextFireTime = time + nano_origin;
        }

		public abstract String getName();
		
        public void setDelayedTime(long time, TimeUnit timeUnit) {
            nano_origin = System.nanoTime();
            this.time = TimeUnit.NANOSECONDS.convert(time, timeUnit);
            nextFireTime = time + nano_origin;
        }
        
        /**
         * this method will be invoked when cancel from heart beat manager
         */
        public void cancel(){
        	
        }
        
        public void reset(){
        	nano_origin = System.nanoTime();
        	nextFireTime = time + nano_origin;
        }

        public long getDelay(TimeUnit unit) {
            long d = unit.convert(time - now(), TimeUnit.NANOSECONDS);
            return d;
        }
        
        public abstract Status doCheck();

        public int compareTo(Delayed other) {
            if (other == this) // compare zero ONLY if same object
            return 0;
            HeartbeatDelayed x = (HeartbeatDelayed) other;
            long diff = nextFireTime - x.nextFireTime;
            if (diff < 0) return -1;
            else if (diff > 0) return 1;
            else if (sequenceNumber < x.sequenceNumber) return -1;
            else return 1;
        }

        /**
         * Returns nanosecond time offset by origin
         */
        final long now() {
            return System.nanoTime() - nano_origin;
        }

	}