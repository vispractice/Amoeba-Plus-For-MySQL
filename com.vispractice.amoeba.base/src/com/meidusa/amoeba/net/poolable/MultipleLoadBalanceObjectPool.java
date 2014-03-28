/**
 * <pre>
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU AFFERO GENERAL PUBLIC LICENSE for more details. 
 * 	You should have received a copy of the GNU AFFERO GENERAL PUBLIC LICENSE along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * </pre>
 */
package com.meidusa.amoeba.net.poolable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.pool.PoolableObjectFactory;

import com.meidusa.amoeba.exception.InitialisationException;
import com.meidusa.amoeba.heartbeat.HeartbeatManager;
import com.meidusa.amoeba.heartbeat.Status;
import com.meidusa.amoeba.util.Initialisable;


/**
 * <pre>
 * 该Pool 提供负载均衡、failover、HA策略
 * 采用Load Balance ObjectPool，则object 必须实现{@link PoolableObject}
 * 默认提供2种负载均衡方案：
 * <li>轮询：请求将轮询分配到每个pool，每个pool的请求比较平均</li>
 * <li>繁忙程度：将所有Pool的Active Num做一个排序，最小的Active Num将优先分配请求</li>
 * &#064;author &lt;a href=mailto:piratebase@sina.com&gt;Struct chen&lt;/a&gt;
 * </pre>
 */
public class MultipleLoadBalanceObjectPool implements ObjectPool,Initialisable {
	
	public class ObjectPoolWrapper implements ObjectPool{
    	ObjectPool source;
    	public ObjectPoolWrapper(ObjectPool objectPool){
    		this.source = objectPool;
    	}

    	public boolean isEnable() {
			return this.source.isEnable();
		}

		public boolean isValid() {
			return this.source.isValid();
		}

		public void setEnable(boolean isEnabled) {
			this.source.setEnable(isEnabled);
		}

		public void setValid(boolean valid) {
			this.source.setValid(valid);
		}

		public void addObject() throws Exception, IllegalStateException,
				UnsupportedOperationException {
			this.source.addObject();
		}

		public Object borrowObject() throws Exception, NoSuchElementException,
				IllegalStateException {
			return this.source.borrowObject();
		}

		public void clear() throws Exception, UnsupportedOperationException {
			this.source.clear();			
		}

		public void close() throws Exception {
			this.source.close();
		}

		public int getNumActive() throws UnsupportedOperationException {
			return this.source.getNumActive();
		}

		public int getNumIdle() throws UnsupportedOperationException {
			return this.source.getNumIdle();
		}

		public void invalidateObject(Object obj) throws Exception {
			this.source.invalidateObject(obj);
		}

		public void returnObject(Object obj) throws Exception {
			this.source.returnObject(obj);
		}

		public void setFactory(PoolableObjectFactory factory)
				throws IllegalStateException, UnsupportedOperationException {
			this.source.setFactory(factory);
		}
		@Override
		public boolean validate() {
			return source.validate();
		}

		@Override
		public String getName() {
			return source.getName();
		}

		@Override
		public void setName(String name) {
			source.setName(name);
		}
    	
    }
	
    public static final int LOADBALANCING_ROUNDROBIN  = 1;
    public static final int LOADBALANCING_WEIGHTBASED = 2;
    public static final int LOADBALANCING_HA          = 3;
    private boolean         enable;
    private String name;
    
    
    protected static class ActiveNumComparator implements Comparator<ObjectPool> {

        public int compare(ObjectPool o1, ObjectPool o2) {
            return o1.getNumActive() - o2.getNumActive();
        }
    }



    /**
     * 负责均衡算法
     */
    private int                               loadbalance;

    private AtomicLong                        currentCount  = new AtomicLong(0);
    private ObjectPool[]                      objectPools;

    private ObjectPool[]                      runtimeObjectPools;

    private int index = 0;
    private ActiveNumComparator               comparator    = new ActiveNumComparator();
	private boolean valid;
	private MultipleHeartbeatDelayed delayed = new MultipleHeartbeatDelayed(3, TimeUnit.SECONDS, this);
	
    public MultipleLoadBalanceObjectPool(){
    }

    public MultipleLoadBalanceObjectPool(int loadbalance, ObjectPool... objectPools){
    	setObjectPools(objectPools);
        this.loadbalance = loadbalance;
    }

    public void setLoadbalance(int loadbalance) {
        this.loadbalance = loadbalance;
    }

    public void setObjectPools(ObjectPool[] objectPools) {
    	this.objectPools = new ObjectPool[objectPools.length];
    	for(int i=0;i<objectPools.length;i++){
        	this.objectPools[i] = new ObjectPoolWrapper(objectPools[i]);
        }
        this.objectPools = objectPools;
        this.runtimeObjectPools = objectPools.clone();
    }

    public void addObject() throws Exception {
        throw new UnsupportedOperationException("addObject is not supported in class="+ this.getClass().getName());

    }
    
    public Object borrowObject() throws Exception {
    	return selectPool().borrowObject();
    }
    
    public ObjectPool selectPool() throws Exception{
    	ObjectPool pool = null;
        ObjectPool[] poolsTemp = null;
        
        while(true){
	        poolsTemp = runtimeObjectPools;
	        if (poolsTemp.length == 0) {
	            throw new Exception("poolName="+name+", no valid pools");
	        }
	
	        if (loadbalance == LOADBALANCING_ROUNDROBIN) {
	            long current = currentCount.getAndIncrement();
	            pool = poolsTemp[(int) (current % poolsTemp.length)];
	        } else if (loadbalance == LOADBALANCING_WEIGHTBASED) {
	            if (poolsTemp.length > 1) {
	                ObjectPool[] objectPoolsCloned = poolsTemp.clone();
	                Arrays.sort(objectPoolsCloned, comparator);
	                pool = objectPoolsCloned[0];
	            } else if (poolsTemp.length == 1) {
	                pool = poolsTemp[0];
	            }
	        } else if (loadbalance == LOADBALANCING_HA) {
	            // HA,只要有效的pool
	        	if(index < poolsTemp.length){
	        		pool = poolsTemp[index];
	        	}else{
	        		pool = poolsTemp[0];
	        	}
	        } else {
	            throw new Exception("poolName="+name+" loadbalance parameter error,parameter loadbalance in [1,2,3]");
	        }
	        
	        if(!pool.isValid()){
	        	validate();
	        	continue;
	        }else{
	        	break;
	        }
        }
        
        return pool;
    }
    
	public void initAllPools() {
        /*for (ObjectPool pool : this.objectPools) {
        	HeartbeatManager.addPooltoHeartbeat(new HeartbeatDelayed(2, TimeUnit.SECONDS, pool));
        }*/
    }

    public void clear() throws Exception, UnsupportedOperationException {
    	//do not clear internal pools
    	/*for (ObjectPool pool : objectPools) {
            pool.clear();
        }*/

    }

    public void close() throws Exception {
    	//do not close internal pools
        /*for (ObjectPool pool : objectPools) {
            pool.close();
        }*/
    	HeartbeatManager.removeHeartbeat(delayed);
    }

    public int getNumActive() throws UnsupportedOperationException {
        int active = 0;
        for (ObjectPool pool : objectPools) {
            active += pool.getNumActive();
        }
        return active;
    }

    public int getNumIdle() throws UnsupportedOperationException {
        int idle = 0;
        for (ObjectPool pool : objectPools) {
            idle += pool.getNumIdle();
        }
        return idle;
    }

    public void invalidateObject(Object obj) throws Exception {
        PoolableObject poolableObject = (PoolableObject) obj;
        ObjectPool pool = poolableObject.getObjectPool();
        pool.invalidateObject(obj);
    }

    public void returnObject(Object obj) throws Exception {
        PoolableObject poolableObject = (PoolableObject) obj;
        ObjectPool pool = poolableObject.getObjectPool();
        pool.returnObject(obj);
    }

    public void setFactory(PoolableObjectFactory factory) throws IllegalStateException, UnsupportedOperationException {
        throw new UnsupportedOperationException("setFactory is not supported in class="+ this.getClass().getName());
    }

    public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean isEnabled) {
        this.enable = isEnabled;
    }

	public boolean isValid() {
		return this.valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public static class MultipleHeartbeatDelayed extends ObjectPoolHeartbeatDelayed {

		public MultipleHeartbeatDelayed(long nsTime, TimeUnit timeUnit,
				MultipleLoadBalanceObjectPool pool) {
			super(nsTime, timeUnit, pool);
		}
		
		public boolean isCycle(){
			return true;
		}
		
		public Status doCheck() {
			return super.doCheck();
		}
	}

	@Override
	public void init() throws InitialisationException {
		HeartbeatManager.addHeartbeat(delayed);
	}

	@Override
	public boolean validate() {
		List<ObjectPool> poolList = new ArrayList<ObjectPool>();
		for(ObjectPool object :this.objectPools){
			if(object.isValid()){
				poolList.add(object);
			}
		}
		ObjectPool[] poolsTemp = runtimeObjectPools = poolList.toArray(new ObjectPool[poolList.size()]);
        if (poolsTemp.length == 0) {
            return false;
        }else{
        	return true;
        }
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + loadbalance;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + Arrays.hashCode(objectPools);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MultipleLoadBalanceObjectPool other = (MultipleLoadBalanceObjectPool) obj;
		if (loadbalance != other.loadbalance)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (!Arrays.equals(objectPools, other.objectPools))
			return false;
		return true;
	}
}
