/*
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU AFFERO GENERAL PUBLIC LICENSE for more details. 
 * 	You should have received a copy of the GNU AFFERO GENERAL PUBLIC LICENSE along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.meidusa.amoeba.server;

import java.util.StringTokenizer;

import com.meidusa.amoeba.context.ContextChangedListener;
import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.exception.InitialisationException;
import com.meidusa.amoeba.exception.UpdateDBServerRuntimeException;
import com.meidusa.amoeba.net.poolable.MultipleLoadBalanceObjectPool;
import com.meidusa.amoeba.net.poolable.ObjectPool;
import com.meidusa.amoeba.util.Initialisable;
import com.meidusa.amoeba.util.StringUtil;

/**
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public class MultipleServerPool extends MultipleLoadBalanceObjectPool implements Initialisable,ContextChangedListener {
	private String poolNames;
	public String getPoolNames() {
		return poolNames;
	}
	
	public void setPoolNames(String poolNames) {
		this.poolNames = poolNames;
	}
	
	public void init() throws InitialisationException {
		if(!StringUtil.isEmpty(this.poolNames)){
			StringTokenizer tokenizer = new StringTokenizer(poolNames," ,	");
			ObjectPool[] objectPools = new ObjectPool[tokenizer.countTokens()];
			int index = 0;
			while(tokenizer.hasMoreTokens()){
				String poolName = tokenizer.nextToken().trim();
				ObjectPool pool = ProxyRuntimeContext.getInstance().getPoolMap().get(poolName);
				if(pool == null){
					throw new InitialisationException("cannot found dbserver="+poolName);
				}
				objectPools[index++] = pool;
			}
			this.setObjectPools(objectPools);
		}
		super.init();
	}

	@Override
	public void doChange() {
		try {
			this.init();
		} catch (InitialisationException e) {
		  String poolName = this.getName();
		  String msg = String.format("pool %s context do change error!", poolName);
		  
		  throw new UpdateDBServerRuntimeException(poolName, msg, e);
		}
	}
}
