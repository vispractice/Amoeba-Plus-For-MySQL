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

import com.meidusa.amoeba.net.packet.AbstractPacketBuffer;

/**
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public class EOFPacket extends AbstractResultPacket {

    public int serverStatus;
    public int warningCount;

    public EOFPacket(){
        this.resultPacketType = PACKET_TYPE_EOF;
    }

    public void init(AbstractPacketBuffer buffer) {
        super.init(buffer);
        MysqlPacketBuffer myBuffer = (MysqlPacketBuffer) buffer;
        warningCount = myBuffer.readInt();
        serverStatus = myBuffer.readInt();
    }

    protected int calculatePacketSize() {
        int packLength = super.calculatePacketSize();
        packLength += 4;
        return packLength;
    }

    public void write2Buffer(AbstractPacketBuffer buffer) throws UnsupportedEncodingException {
        super.write2Buffer(buffer);
        MysqlPacketBuffer myBuffer = (MysqlPacketBuffer) buffer;
        myBuffer.writeInt(warningCount);
        myBuffer.writeInt(serverStatus);
    }

}
