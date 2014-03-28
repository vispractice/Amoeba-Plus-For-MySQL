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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import com.meidusa.amoeba.mysql.jdbc.Messages;

/**
 * 阻塞模式读取packet
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public class BlockedPacketIO {

    public static final int readFully(InputStream in, byte[] b, int off, int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = 0;
        while (n < len) {
            int count = in.read(b, off + n, len - n);
            if (count < 0) {
                throw new EOFException(Messages.getString("MysqlIO.EOF", new Object[] { new Integer(len), new Integer(n) }));
            }
            n += count;
        }
        return n;
    }

    /**
     * @return the packet from the server.
     * @throws SQLException DOCUMENT ME!
     * @throws IOException
     */
    public static final MysqlPacketBuffer readFullyPacketBuffer(InputStream mysqlInput) throws IOException {

        byte[] packetHeaderBuf = new byte[4];
        int lengthRead = readFully(mysqlInput, packetHeaderBuf, 0, 4);

        if (lengthRead < 4) {
            throw new IOException(Messages.getString("MysqlIO.1")); //$NON-NLS-1$
        }

        // 数据包长度
        int packetLength = (packetHeaderBuf[0] & 0xff) + ((packetHeaderBuf[1] & 0xff) << 8) + ((packetHeaderBuf[2] & 0xff) << 16);

        // 缓冲区长度
        int bufferLength = packetLength + 4;

        // Read data
        byte[] buffer = new byte[bufferLength + 1];
        System.arraycopy(packetHeaderBuf, 0, buffer, 0, 4);

        int numBytesRead = readFully(mysqlInput, buffer, 4, packetLength);

        if (numBytesRead != packetLength) {
            throw new IOException("Short read, expected " + packetLength + " bytes, only read " + numBytesRead);
        }

        buffer[bufferLength] = 0;
        MysqlPacketBuffer packet = new MysqlPacketBuffer(buffer);
        packet.setBufLength(bufferLength + 1);

        return packet;
    }

}
