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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.net.packet.AbstractPacketBuffer;

/**
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public class RowDataPacket extends AbstractPacket {

    public List<Object> columns;
    private boolean     isBinaryEncoded;

    public RowDataPacket(boolean isBinaryEncoded){
        this.isBinaryEncoded = isBinaryEncoded;
    }

    @Override
    public void init(AbstractPacketBuffer myBuffer) {
        super.init(myBuffer);
        MysqlPacketBuffer buffer = (MysqlPacketBuffer) myBuffer;
        if (columns == null) {
            columns = new ArrayList<Object>();
        }

        while (buffer.getPosition() < this.packetLength + HEADER_SIZE) {
        	//encoding = ProxyRuntimeContext.getInstance().getRuntimeContext().getServerCharset()
            columns.add(buffer.readLengthCodedString(null));
        }
    }

    @Override
    public void write2Buffer(AbstractPacketBuffer myBuffer) throws UnsupportedEncodingException {
        super.write2Buffer(myBuffer);
        MysqlPacketBuffer buffer = (MysqlPacketBuffer) myBuffer;

        if (!isBinaryEncoded) {

            if (columns == null) return;
            Iterator<Object> it = columns.iterator();
            while (it.hasNext()) {
                Object obj = it.next();
                buffer.writeLengthCodedString(obj != null ? obj.toString() : null, ProxyRuntimeContext.getInstance().getRuntimeContext().getServerCharset());
            }
        } else {
            myBuffer.writeByte((byte) 0);
            if (columns == null) return;
            int numFields = columns.size();
            /* Reserve place for null-marker bytes */
            int nullCount = (numFields + 9) / 8;
            byte[] nullBitMask = new byte[nullCount];

            int nullPosition = buffer.getPosition();
            buffer.writeBytes(nullBitMask);

            int nullMaskPos = 0;
            int bit = 4;
            int i = 0;
            for (Object colum : columns) {
                if (colum == null) {
                    nullBitMask[nullMaskPos] |= bit;
                } else {
                    if (colum instanceof BindValue) {
                        BindValue value = (BindValue) colum;
                        if (!((BindValue) colum).isNull) {
                            PacketUtil.storeBinding(buffer, value, value.charset);
                        } else {
                            nullBitMask[nullMaskPos] |= bit;
                        }
                    } else {

                    }
                }
                if (((bit <<= 1) & 255) == 0) {
                    bit = 1; /* To next byte */
                    nullMaskPos++;
                }
                i++;
            }

            int position = buffer.getPosition();
            buffer.setPosition(nullPosition);
            buffer.writeBytes(nullBitMask);
            buffer.setPosition(position);
        }
    }

}
