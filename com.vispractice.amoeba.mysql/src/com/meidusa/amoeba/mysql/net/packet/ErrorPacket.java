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
 * From server to client in response to command, if error.
 * 
 * <pre>
 *  VERSION 4.0
 *  Bytes                       Name
 *  -----                       ----
 *  1                           field_count, always = 0xff
 *  2                           errno
 *  n                           message
 *  
 *  VERSION 4.1
 *  Bytes                       Name
 *  -----                       ----
 *  1                           field_count, always = 0xff
 *  2                           errno
 *  1                           (sqlstate marker), always '#'
 *  5                           sqlstate (5 characters)
 *  n                           message
 *  
 *  field_count:       Always 0xff (255 decimal).
 *  
 *  errno:             The possible values are listed in the manual, and in
 *                     the MySQL source code file /include/mysqld_error.h.
 *  
 *  sqlstate marker:   This is always '#'. It is necessary for distinguishing
 *                     version-4.1 messages.
 *  
 *  sqlstate:          The server translates errno values to sqlstate values
 *                     with a function named mysql_errno_to_sqlstate(). The
 *                     possible values are listed in the manual, and in the
 *                     MySQL source code file /include/sql_state.h.
 *  
 *  message:           The error message is a string which ends at the end of
 *                     the packet, that is, its length can be determined from
 *                     the packet header. The MySQL client (in the my_net_read()
 *                     function) always adds '\0' to a packet, so the message
 *                     may appear to be a Null-Terminated String.
 *                     Expect the message to be between 0 and 512 bytes long.
 * ===========================================================================
 * </pre>
 * 
 * <pre>
 * Example of Error Packet
 *                     Hexadecimal                ASCII
 *                     -----------                -----
 * field_count         ff                         .
 * errno               1b 04                      ..
 * (sqlstate marker)   23                         #
 * sqlstate            34 32 53 30 32             42S02
 * message             55 63 6b 6e 6f 77 6e 20    Unknown
 *                     74 61 62 6c 6c 65 20 27    table '
 *                     71 27                      q'
 * </pre>
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public class ErrorPacket extends AbstractResultPacket {

    public int    errno;

    /**
     * 5个字节
     */
    public String sqlstate;

    /**
     * 错误信息
     */
    public String serverErrorMessage;

    public ErrorPacket(){
        resultPacketType = PACKET_TYPE_ERROR;
    }

    @Override
    public void init(AbstractPacketBuffer buffer) {
        super.init(buffer);
        MysqlPacketBuffer myBuffer = (MysqlPacketBuffer) buffer;
        errno = myBuffer.readInt();
        serverErrorMessage = myBuffer.readString(CODE_PAGE_1252);

        if (serverErrorMessage.charAt(0) == '#') { //$NON-NLS-1$
            // we have an SQLState
            if (serverErrorMessage.length() > 6) {
                sqlstate = serverErrorMessage.substring(1, 6);
                serverErrorMessage = serverErrorMessage.substring(6);
            }
        }
    }

    @Override
    public void write2Buffer(AbstractPacketBuffer buffer) throws UnsupportedEncodingException {
        super.write2Buffer(buffer);
        MysqlPacketBuffer myBuffer = (MysqlPacketBuffer) buffer;
        myBuffer.writeInt(errno);
        myBuffer.writeString('#' + sqlstate + serverErrorMessage);
    }

    @Override
    protected int calculatePacketSize() {
        int packLength = super.calculatePacketSize();
        packLength += ((sqlstate == null ? 0 : sqlstate.length()) + (serverErrorMessage == null ? 0 : serverErrorMessage.length())) * 2 + 3;
        return packLength;
    }

}
