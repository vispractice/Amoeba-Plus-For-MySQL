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
package com.meidusa.amoeba.mysql.net;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.meidusa.amoeba.net.io.PacketInputStream;
import com.meidusa.amoeba.net.io.PacketOutputStream;
import com.meidusa.amoeba.mysql.io.MysqlFramedInputStream;
import com.meidusa.amoeba.mysql.io.MysqlFramingOutputStream;
import com.meidusa.amoeba.net.DatabaseConnection;

/**
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public abstract class MysqlConnection extends DatabaseConnection {
    //是否需要xa
    private volatile boolean isXaActive = false;
    
	public MysqlConnection(SocketChannel channel, long createStamp) {
		super(channel, createStamp);
	}
	
	@Override
	protected PacketInputStream createPacketInputStream() {
		return new MysqlFramedInputStream(true);
	}
	
	@Override
	protected PacketOutputStream createPacketOutputStream() {
		return new MysqlFramingOutputStream(true);
	}
	
	/**
	 * 为了提升性能，由于mysql数据包写到目的地的时候已经包含了包头，则不需要经过PacketOutputStream处理
	 */
	public void postMessage(byte[] msg)
    {
		_outQueue.append(ByteBuffer.wrap(msg));
        writeMessage();
    }
	
	// 非自动提交且标记了xa，那么就需要发送xa start/xa prepare/ xa commit序列语句
    public boolean isXaActive() {
      return isXaActive && !isAutoCommit();
    }
  
    public void setIsXaActive(boolean isXaActive) {
      this.isXaActive = isXaActive;
    }
    
}
