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
 * </pre.
 */
package com.meidusa.amoeba.mysql.net.packet;

import java.io.UnsupportedEncodingException;

import com.meidusa.amoeba.net.packet.AbstractPacketBuffer;

/**
 * <pre>
 * A &quot;result packet&quot; is a packet that goes from the server to the client in response to a Client Authentication Packet or Command Packet. 
 * To distinguish between the types of result packets,
 *  a client must look at the first byte in the packet. 
 *  We will call this byte &quot;field_count&quot; in the description of each individual package, 
 *  although it goes by several names.
 * </pre>
 * 
 *<pre>
 * Type Of Result Packet       Hexadecimal Value Of First Byte (field_count)
 *  ---------------------       ---------------------------------------------
 *  
 *  OK Packet                   00
 *  Error Packet                ff
 *  Result Set Packet           1-250 (first byte of Length-Coded Binary)
 *  Field Packet                1-250 (&quot;&quot;)
 *  Row Data Packet             1-250 (&quot;&quot;)
 *  EOF Packet                  fe
 * </pre>
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public abstract class AbstractResultPacket extends AbstractPacket {

    public static final byte PACKET_TYPE_OK    = 0x00;
    public static final byte PACKET_TYPE_ERROR = (byte) 0xff;
    public static final byte PACKET_TYPE_EOF   = (byte) 0xfe;

    public byte              resultPacketType;

    @Override
    public void init(AbstractPacketBuffer buffer) {
        super.init(buffer);
        resultPacketType = buffer.readByte();
    }

    @Override
    protected void write2Buffer(AbstractPacketBuffer buffer) throws UnsupportedEncodingException {
        super.write2Buffer(buffer);
        buffer.writeByte(resultPacketType);
    }

    @Override
    protected int calculatePacketSize() {
        int packLength = super.calculatePacketSize();
        packLength += 1;
        return packLength;
    }
}
