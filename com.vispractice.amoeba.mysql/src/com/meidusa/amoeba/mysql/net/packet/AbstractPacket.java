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
package com.meidusa.amoeba.mysql.net.packet;

import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.mysql.io.MySqlPacketConstant;
import com.meidusa.amoeba.net.packet.AbstractPacketBuffer;

/**
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public class AbstractPacket extends com.meidusa.amoeba.net.packet.AbstractPacket implements MySqlPacketConstant {

    protected static Logger logger = Logger.getLogger(AbstractPacket.class);

    /** 只表示数据长度，不包含包头长度 */
    public int              packetLength;

    /** 当前的数据包序列数 */
    public byte             packetId;

    @Override
    protected void init(AbstractPacketBuffer buffer) {
        buffer.setPosition(0);
        packetLength = (buffer.readByte() & 0xff) + ((buffer.readByte() & 0xff) << 8) + ((buffer.readByte() & 0xff) << 16);
        packetId = buffer.readByte();
    }

    /**
     * 估算packet的大小，估算的太大浪费内存，估算的太小会影响性能
     */
    protected int calculatePacketSize() {
        return HEADER_SIZE + 1;
    }

    @Override
    protected void write2Buffer(AbstractPacketBuffer buffer) throws UnsupportedEncodingException {
        // TODO Auto-generated method stub
    }

    @Override
    protected void afterPacketWritten(AbstractPacketBuffer buffer) {
        int position = buffer.getPosition();
        packetLength = position - HEADER_SIZE;
        buffer.setPosition(0);
        buffer.writeByte((byte) (packetLength & 0xff));
        buffer.writeByte((byte) (packetLength >>> 8));
        buffer.writeByte((byte) (packetLength >>> 16));
        buffer.writeByte((byte) (packetId & 0xff));// packet id
        buffer.setPosition(position);
    }

    @Override
    protected Class<? extends AbstractPacketBuffer> getPacketBufferClass() {
        return MysqlPacketBuffer.class;
    }

}
