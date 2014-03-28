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
package com.meidusa.amoeba.runtime;

/**
 * 实现该接口，并且需要调用 {@link PriorityShutdownHook#addShutdowner(Shutdowner)} 当系统shutdown的时候将会被通知到
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 * 
 */
public interface Shutdowner {
	
	/**
	 * 获取组件关闭的时候优先级别，该定义跟线程优先级别一致
	 * @see Thread#getPriority()
	 * 
	 * @return
	 */
	public int getShutdownPriority();
	
	/**
	 * Called when the server is shutting down.
	 */
	public void shutdown();
	
}
