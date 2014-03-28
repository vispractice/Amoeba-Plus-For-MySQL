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
package com.meidusa.amoeba.net.packet;

import java.nio.ByteBuffer;

import com.meidusa.amoeba.net.Connection;

/**
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public interface Packet extends Cloneable {

    /**
     * 从buffer(含包头) 中初始化数据包。
     * 
     * @param buffer buffer是从socketChannel的流读取头n个字节计算数据包长度 并且读取相应的长度所形成的buffer
     */
    public void init(byte[] buffer, Connection conn);

    /**
     * 将数据包转化成ByteBuffer,byteBuffer中包含有包头信息
     * 
     * @return
     */
    public ByteBuffer toByteBuffer(Connection conn);
}
