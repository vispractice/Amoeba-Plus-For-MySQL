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
package com.meidusa.amoeba.config;

public class DBServerConfig extends ConfigEntity implements Cloneable{
	private static final long serialVersionUID = 1L;
	private String name;
	private Boolean abstractive = false;
	private Boolean virtual = false;
	private String parent;
	private BeanObjectEntityConfig factoryConfig;
	private BeanObjectEntityConfig poolConfig;
	private Boolean isEnable = false;
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Boolean getVirtual() {
		return virtual;
	}

	public void setVirtual(Boolean virtual) {
		this.virtual = virtual;
	}

	public Boolean getAbstractive() {
		return abstractive;
	}

	public void setAbstractive(Boolean abstractive) {
		this.abstractive = abstractive;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}
	

	public BeanObjectEntityConfig getFactoryConfig() {
		return factoryConfig;
	}

	public void setFactoryConfig(BeanObjectEntityConfig factoryConfig) {
		this.factoryConfig = factoryConfig;
	}

	public BeanObjectEntityConfig getPoolConfig() {
		return poolConfig;
	}

	public void setPoolConfig(BeanObjectEntityConfig poolConfig) {
		this.poolConfig = poolConfig;
	}
	
	public Boolean isEnable() {
		return isEnable;
	}

	public void setIsEnable(Boolean isEnable) {
		this.isEnable = isEnable;
	}

	public Object clone(){
		DBServerConfig config = new DBServerConfig();
		config.virtual = (virtual != null? Boolean.valueOf(virtual):null);
		config.abstractive = (abstractive != null? Boolean.valueOf(abstractive):null);
		config.name = name;
		config.parent = parent;
		config.isEnable = (isEnable != null ? Boolean.valueOf(isEnable): null);
		
		if(factoryConfig != null){
			config.factoryConfig = (BeanObjectEntityConfig)factoryConfig.clone();
		}
		
		if(poolConfig != null){
			config.poolConfig = (BeanObjectEntityConfig)poolConfig.clone();
		}
		
		return config;
	}

}
