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
package com.meidusa.amoeba.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public class ProxyServerConfig {

	private List<BeanObjectEntityConfig> serviceConfigList = new ArrayList<BeanObjectEntityConfig>();
	private BeanObjectEntityConfig threadPoolConfig;
	private BeanObjectEntityConfig authenticatorConfig;
	private BeanObjectEntityConfig connectionFactoryConfig;
	private BeanObjectEntityConfig runtimeConfig;
	
    /**
     * 默认是没有值的
     */
    private Map<String, BeanObjectEntityConfig> managers                 = new ConcurrentHashMap<String, BeanObjectEntityConfig>();

    private Map<String, DBServerConfig>         dbServers                = new ConcurrentHashMap<String, DBServerConfig>();

    private BeanObjectEntityConfig              queryRouterConfig;

    public List<BeanObjectEntityConfig> getServiceConfigList() {
		return serviceConfigList;
	}

	public void addServerConfig(BeanObjectEntityConfig serverConfig) {
		this.serviceConfigList.add(serverConfig);
	}

	public BeanObjectEntityConfig getThreadPoolConfig() {
		return threadPoolConfig;
	}

	public void setThreadPoolConfig(BeanObjectEntityConfig threadPoolConfig) {
		this.threadPoolConfig = threadPoolConfig;
	}

	public BeanObjectEntityConfig getAuthenticatorConfig() {
		return authenticatorConfig;
	}

	public void setAuthenticatorConfig(BeanObjectEntityConfig authenticatorConfig) {
		this.authenticatorConfig = authenticatorConfig;
	}

	public BeanObjectEntityConfig getConnectionFactoryConfig() {
		return connectionFactoryConfig;
	}

	public void setConnectionFactoryConfig(
			BeanObjectEntityConfig connectionFactoryConfig) {
		this.connectionFactoryConfig = connectionFactoryConfig;
	}
	
    public BeanObjectEntityConfig getRuntimeConfig() {
		return runtimeConfig;
	}

	public void setRuntimeConfig(BeanObjectEntityConfig runtimeConfig) {
		this.runtimeConfig = runtimeConfig;
	}

	public void addManager(String name, BeanObjectEntityConfig managerConfig) {
        managers.put(name, managerConfig);
    }

    public Map<String, BeanObjectEntityConfig> getManagers() {
        return managers;
    }

    public Map<String, DBServerConfig> getDbServers() {
        return dbServers;
    }

    public void addServer(String name, DBServerConfig serverConfig) {
        dbServers.put(name, serverConfig);
    }

	public void putAllServers(Map<String, DBServerConfig> dbServers) {
		this.dbServers.putAll(dbServers);
	}

	public BeanObjectEntityConfig getQueryRouterConfig() {
        return queryRouterConfig;
    }

    public void setQueryRouterConfig(BeanObjectEntityConfig queryRouterConfig) {
        this.queryRouterConfig = queryRouterConfig;
    }

}
