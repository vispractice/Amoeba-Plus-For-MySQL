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
package com.meidusa.amoeba.net;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * net Event handler
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public interface NetEventHandler extends IdleChecker{
	
	/**
	 * 此时handler需要处理 when 时刻 所 handle 的网络事件。
	 * 
	 * @param when
	 * @return
	 */
	public int handleEvent (long when);
	
	public SelectionKey getSelectionKey();
	
	public void setSelectionKey(SelectionKey selkey);
	
	public boolean doWrite() throws IOException;
}
