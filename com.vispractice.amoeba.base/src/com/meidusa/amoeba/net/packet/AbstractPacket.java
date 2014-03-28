/**
 * <pre>
 * copy right meidusa.com
 * 
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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.meidusa.amoeba.net.Connection;

/**
 * @author struct
 */
public abstract class AbstractPacket<T extends AbstractPacketBuffer> implements Packet {
    public void init(byte[] buffer, Connection conn) {
        T packetBuffer = constractorBuffer(buffer);
        packetBuffer.init(conn);
        init(packetBuffer);
        afterRead(packetBuffer);
    }

    /**
	 * 分析数据包(分析包头+数据区域,分析完包头以后应该将Buffer的postion设置到数据区)
	 */
	protected void init(T buffer){
		readHead(buffer);
		readBody(buffer);
	}
	
	protected void readHead(T buffer){
		
	}

	protected void readBody(T buffer){
		
	}
	
    /**
     * 做完初始化以后
     */
    protected void afterRead(T buffer) {
    }

    public ByteBuffer toByteBuffer(Connection conn) {
        try {
            int bufferSize = calculatePacketSize();
            T packetBuffer = constractorBuffer(bufferSize);
            packetBuffer.init(conn);
            return toBuffer(packetBuffer).toByteBuffer();
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private T constractorBuffer(int bufferSize) {
        T buffer = null;
        try {
            Constructor<T> constractor = getPacketBufferClass().getConstructor(int.class);
            buffer = constractor.newInstance(bufferSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer;
    }

    /**
     * <pre>
     *  该方法调用了{@link #write2Buffer(PacketBuffer)} 写入到指定的buffer， 
     *  并且调用了{@link #afterPacketWritten(PacketBuffer)}
     * </pre>
     */
    private T toBuffer(T buffer) throws UnsupportedEncodingException {
        write2Buffer(buffer);
        afterPacketWritten(buffer);
        return buffer;
    }

    /**
     * 包含头的消息封装
     */
    protected void write2Buffer(T buffer) throws UnsupportedEncodingException{
    	writeHead(buffer);
    	writeBody(buffer);
	}

    protected void writeBody(T buffer) throws UnsupportedEncodingException {
    	
    }
    protected void writeHead(T buffer){
    	
    }

    /**
     * <pre>
     * 写完之后一定需要调用这个方法，buffer的指针位置指向末尾的下一个位置（包总长度位置）。
     * 这儿一般是计算数据包总长度,或者其他需要数据包写完才能完成的数据
     * </pre>
     */
    protected abstract void afterPacketWritten(T buffer);

    /**
     * 估算packet的大小，估算的太大浪费内存，估算的太小会影响性能
     */
    protected abstract int calculatePacketSize();

    private T constractorBuffer(byte[] buffer) {
        T packetbuffer = null;
        try {
            Constructor<T> constractor = getPacketBufferClass().getConstructor(byte[].class);
            packetbuffer = constractor.newInstance(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return packetbuffer;
    }

    protected abstract Class<T> getPacketBufferClass();

    public String toString() {
		return ToStringBuilder.reflectionToString(this);
    }

}
