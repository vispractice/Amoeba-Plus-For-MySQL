/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.meidusa.amoeba.net.poolable;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.heartbeat.HeartbeatDelayed;
import com.meidusa.amoeba.heartbeat.Status;

/**
 * 
 * @author struct
 *
 */
public interface ObjectPool extends org.apache.commons.pool.ObjectPool {
	static Logger logger = Logger.getLogger(ObjectPool.class); 

    public static class ActiveNumComparator implements Comparator<ObjectPool> {

        public int compare(ObjectPool o1, ObjectPool o2) {
            return o1.getNumActive() - o2.getNumActive();
        }
    }

	public static class ObjectPoolHeartbeatDelayed extends HeartbeatDelayed {

        private ObjectPool pool;
        
        public boolean isCycle(){
        	return false;
        }
        
        public ObjectPool getPool() {
			return pool;
		}

		public ObjectPoolHeartbeatDelayed(long nsTime, TimeUnit timeUnit, ObjectPool pool){
			super(nsTime,timeUnit);
            this.pool = pool;
        }

	    public boolean equals(Object obj) {
	    	if(obj instanceof ObjectPoolHeartbeatDelayed){
	    		ObjectPoolHeartbeatDelayed other = (ObjectPoolHeartbeatDelayed)obj;
	    		return other.pool == this.pool && this.getClass() == obj.getClass();
	    	}else{
	    		return false;
	    	}
        }
	    
	    public int hashCode(){
	    	return pool == null?this.getClass().hashCode():this.getClass().hashCode() + pool.hashCode();
	    }
	    
        
        public Status doCheck() {
			if(pool.validate()){
				pool.setValid(true);
				return Status.VALID;
			}else{
				pool.setValid(false);
				return Status.INVALID;
			}
        }

		@Override
		public String getName() {
			return this.pool.getName();
		}
	}

	/**
	 * return this pool enabled/disabled status
	 * 
	 * @return
	 */
	boolean isEnable();

	void setEnable(boolean isEnabled);

	boolean isValid();
	
	void setValid(boolean valid);
	
	public boolean validate();
	
	public String getName();
	
	public void setName(String name);
}
