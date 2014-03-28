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
 * From server to client in response to command, if no error and no result set.
 * 
 * <pre>
 * VERSION 4.0
 *  Bytes                       Name
 *  -----                       ----
 *  1   (Length Coded Binary)   field_count, always = 0
 *  1-9 (Length Coded Binary)   affected_rows
 *  1-9 (Length Coded Binary)   insert_id
 *  2                           server_status
 *  n   (until end of packet)   message
 *  
 *  VERSION 4.1
 *  Bytes                       Name
 *  -----                       ----
 *  1   (Length Coded Binary)   field_count, always = 0
 *  1-9 (Length Coded Binary)   affected_rows
 *  1-9 (Length Coded Binary)   insert_id
 *  2                           server_status
 *  2                           warning_count
 *  n   (until end of packet)   message
 *  
 *  field_count:     always = 0
 *  
 *  affected_rows:   = number of rows affected by INSERT/UPDATE/DELETE
 *  
 *  insert_id:       If the statement generated any AUTO_INCREMENT number, 
 *                   it is returned here. Otherwise this field contains 0.
 *                   Note: when using for example a multiple row INSERT the
 *                   insert_id will be from the first row inserted, not from
 *                   last.
 *  
 *  server_status:   = The client can use this to check if the
 *                   command was inside a transaction.
 *  
 *  warning_count:   number of warnings
 *  
 *  message:         For example, after a multi-line INSERT, message might be
 *                   &quot;Records: 3 Duplicates: 0 Warnings: 0&quot;
 * ========================================================================
 *</pre>
 * 
 *<pre>
 *  The message field is optional. 
 *  Alternative terms: OK Packet is also known as &quot;okay packet&quot; or &quot;ok packet&quot; or &quot;OK-Packet&quot;. 
 *  field_count is also known as &quot;number of rows&quot; or &quot;marker for ok packet&quot;. 
 *  message is also known as &quot;Messagetext&quot;. 
 *  OK Packets (and result set packets) are also called &quot;Result packets&quot;.
 *</pre>
 * 
 *<pre>
 * ===============================================================
 * Example OK Packet
 *                     Hexadecimal                ASCII
 *                     -----------                -----
 * field_count         00                         .
 * affected_rows       01                         .
 * insert_id           00                         .
 * server_status       02 00                      ..
 * warning_count       00 00                      ..
 *</pre>
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public class OkPacket extends AbstractResultPacket {

    public long   affectedRows;
    public long   insertId;
    public int    serverStatus;
    public int    warningCount;
    public String message;

    @Override
    public void init(AbstractPacketBuffer buffer) {
        super.init(buffer);
        MysqlPacketBuffer myPacketBuffer = (MysqlPacketBuffer) buffer;
        affectedRows = myPacketBuffer.readFieldLength();
        
        if(buffer.hasRemaining()){
        	insertId = myPacketBuffer.readFieldLength();
        }
        
        if(buffer.remaining() >= 2){
        	serverStatus = myPacketBuffer.readInt();
        }
        
        if(buffer.remaining() >= 2){
        	warningCount = myPacketBuffer.readInt();
        }

        if (buffer.getPosition() < myPacketBuffer.getBufLength()) {
            message = myPacketBuffer.readString();
        }
    }

    @Override
    public void write2Buffer(AbstractPacketBuffer buffer) throws UnsupportedEncodingException {
        super.write2Buffer(buffer);
        MysqlPacketBuffer myPacketBuffer = (MysqlPacketBuffer) buffer;
        myPacketBuffer.writeFieldLength(affectedRows);
        myPacketBuffer.writeFieldLength(insertId);
        myPacketBuffer.writeInt(serverStatus);
        myPacketBuffer.writeInt(warningCount);
        if (message != null) {
            myPacketBuffer.writeString(message);
        }
    }

    @Override
    protected int calculatePacketSize() {
        int packLength = super.calculatePacketSize();
        packLength += 4 + 4 + 2 + 2 + (message == null ? 0 : message.length()) * 2;
        return packLength;
    }
}
