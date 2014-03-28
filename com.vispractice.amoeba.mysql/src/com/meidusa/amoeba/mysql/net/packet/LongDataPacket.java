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
public class LongDataPacket extends CommandPacket {

    public long   statementId;
    public int    parameterIndex;
    //public int    type;
    public byte[] data;

    @Override
    public void init(AbstractPacketBuffer myBuffer) {
        super.init(myBuffer);
        MysqlPacketBuffer buffer = (MysqlPacketBuffer) myBuffer;
        statementId = buffer.readLong();
        parameterIndex = buffer.readInt();
        //type = buffer.readInt();
        if(buffer.hasRemaining()){
        	data = buffer.getBytes(buffer.getPosition(), buffer.getBufLength() - buffer.getPosition());
        }
    }

    @Override
	public void write2Buffer(AbstractPacketBuffer myBuffer) throws UnsupportedEncodingException {
        super.write2Buffer(myBuffer);
        MysqlPacketBuffer buffer = (MysqlPacketBuffer) myBuffer;
        buffer.writeLong(statementId);
        buffer.writeInt(parameterIndex);
        //buffer.writeInt(type);
        if(data != null){
        	buffer.writeBytesNoNull(data);
        }
    }

}
