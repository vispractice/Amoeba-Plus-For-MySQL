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
 * From server to client during initial handshake.
 * 
 * <pre>
 *  Bytes                        Name
 *  -----                        ----
 *  1                            protocol_version
 *  n (Null-Terminated String)   server_version
 *  4                            thread_id
 *  8                            scramble_buff
 *  1                            (filler) always 0x00
 *  2                            server_capabilities
 *  1                            server_language
 *  2                            server_status
 *  13                           (filler) always 0x00 ...
 *  13                           rest of scramble_buff (4.1)
 *  
 *  protocol_version:    The server takes this from PROTOCOL_VERSION
 *                       in /include/mysql_version.h. Example value = 10.
 *  
 *  server_version:      The server takes this from MYSQL_SERVER_VERSION
 *                       in /include/mysql_version.h. Example value = &quot;4.1.1-alpha&quot;.
 *  
 *  thread_number:       ID of the server thread for this connection.
 *  
 *  scramble_buff:       The password mechanism uses this. The second part are the
 *                       last 13 bytes.
 *                       (See &quot;Password functions&quot; section elsewhere in this document.)
 *  
 *  server_capabilities: CLIENT_XXX options. The possible flag values at time of
 *  writing (taken from  include/mysql_com.h):
 *   CLIENT_LONG_PASSWORD	1		// new more secure passwords 
 *   CLIENT_FOUND_ROWS	2			// Found instead of affected rows 
 *   CLIENT_LONG_FLAG	4			// Get all column flags 
 *   CLIENT_CONNECT_WITH_DB	8		// One can specify db on connect 
 *   CLIENT_NO_SCHEMA	16			// Don't allow database.table.column 
 *   CLIENT_COMPRESS		32		// Can use compression protocol 
 *   CLIENT_ODBC		64			// Odbc client 
 *   CLIENT_LOCAL_FILES	128			// Can use LOAD DATA LOCAL 
 *   CLIENT_IGNORE_SPACE	256		// Ignore spaces before '(' 
 *   CLIENT_PROTOCOL_41	512			// New 4.1 protocol 
 *   CLIENT_INTERACTIVE	1024		// This is an interactive client 
 *   CLIENT_SSL              2048	// Switch to SSL after handshake 
 *   CLIENT_IGNORE_SIGPIPE   4096   // IGNORE sigpipes 
 *   CLIENT_TRANSACTIONS	8192	// Client knows about transactions 
 *   CLIENT_RESERVED         16384  // Old flag for 4.1 protocol  
 *   CLIENT_SECURE_CONNECTION 32768 // New 4.1 authentication 
 *   CLIENT_MULTI_STATEMENTS 65536  // Enable/disable multi-stmt support 
 *   CLIENT_MULTI_RESULTS    131072 // Enable/disable multi-results 
 *  
 *  server_language:     current server character set number
 *  
 *  server_status:       SERVER_STATUS_xxx flags: e.g. SERVER_STATUS_AUTOCOMMIT
 *  &#064;see http://forge.mysql.com/wiki/MySQL_Internals_ClientServer_Protocol#Handshake_Initialization_Packet
 * </pre>
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public class HandshakePacket extends AbstractPacket {

    public byte   protocolVersion;

    /** n个字节 */
    public String serverVersion;

    public long   threadId;

    /** 8个字节 */
    public String seed;

    public int    serverCapabilities;

    public byte   serverCharsetIndex;

    public int    serverStatus;

    /** 13个字节 */
    public String restOfScrambleBuff;

    @Override
    public void init(AbstractPacketBuffer myBuffer) {
        super.init(myBuffer);
        MysqlPacketBuffer buffer = (MysqlPacketBuffer) myBuffer;
        this.protocolVersion = buffer.readByte();
        this.serverVersion = buffer.readString();
        threadId = buffer.readLong();

        this.seed = buffer.readString();
        this.serverCapabilities = 0;

        if (buffer.getPosition() < buffer.getBufLength()) {
            this.serverCapabilities = buffer.readInt();
        }
        /* New protocol with 16 bytes to describe server characteristics */
        this.serverCharsetIndex = buffer.readByte();
        this.serverStatus = buffer.readInt();
        buffer.setPosition(buffer.getPosition() + 13);
        restOfScrambleBuff = buffer.readString();
    }

    @Override
    public void write2Buffer(AbstractPacketBuffer mysqlpacketBuffer) throws UnsupportedEncodingException {
        super.write2Buffer(mysqlpacketBuffer);
        MysqlPacketBuffer buffer = (MysqlPacketBuffer) mysqlpacketBuffer;
        buffer.writeByte(protocolVersion);
        buffer.writeString(serverVersion, CODE_PAGE_1252);
        buffer.writeLong(threadId);
        buffer.writeString(seed);
        buffer.writeInt(serverCapabilities);
        buffer.writeByte(serverCharsetIndex);
        buffer.writeInt(serverStatus);
        buffer.writeBytesNoNull(new byte[13]);
        buffer.writeString(restOfScrambleBuff);
    }

    protected int calculatePacketSize() {
        int packLength = super.calculatePacketSize();
        int serverVersionLength = (serverVersion != null) ? serverVersion.length() : 0;
        packLength += 1 + (serverVersionLength + 8 + 13) * 2 + 8 + 2 + 1 + 2;
        return packLength;
    }

    protected void showServerCapabilities() {
        if (logger.isDebugEnabled()) {
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            builder.append("==============================Server Flag===============================\n");
            builder.append("CLIENT_LONG_PASSWORD:" + (serverCapabilities & CLIENT_LONG_PASSWORD) + "\n");
            builder.append("CLIENT_FOUND_ROWS:" + (serverCapabilities & CLIENT_FOUND_ROWS) + "\n");
            builder.append("CLIENT_LONG_FLAG:" + (serverCapabilities & CLIENT_LONG_FLAG) + "\n");
            builder.append("CLIENT_CONNECT_WITH_DB:" + (serverCapabilities & CLIENT_CONNECT_WITH_DB) + "\n");
            builder.append("CLIENT_NO_SCHEMA:" + (serverCapabilities & CLIENT_NO_SCHEMA) + "\n");
            builder.append("CLIENT_COMPRESS:" + (serverCapabilities & CLIENT_COMPRESS) + "\n");
            builder.append("CLIENT_ODBC:" + (serverCapabilities & CLIENT_ODBC) + "\n");
            builder.append("CLIENT_LOCAL_FILES:" + (serverCapabilities & CLIENT_LOCAL_FILES) + "\n");
            builder.append("CLIENT_IGNORE_SPACE:" + (serverCapabilities & CLIENT_IGNORE_SPACE) + "\n");
            builder.append("CLIENT_PROTOCOL_41:" + (serverCapabilities & CLIENT_PROTOCOL_41) + "\n");
            builder.append("CLIENT_INTERACTIVE:" + (serverCapabilities & CLIENT_INTERACTIVE) + "\n");
            builder.append("CLIENT_SSL:" + (serverCapabilities & CLIENT_SSL) + "\n");
            builder.append("CLIENT_IGNORE_SIGPIPE:" + (serverCapabilities & CLIENT_IGNORE_SIGPIPE) + "\n");
            builder.append("CLIENT_TRANSACTIONS:" + (serverCapabilities & CLIENT_TRANSACTIONS) + "\n");
            builder.append("CLIENT_RESERVED:" + (serverCapabilities & CLIENT_RESERVED) + "\n");
            builder.append("CLIENT_SECURE_CONNECTION:" + (serverCapabilities & CLIENT_SECURE_CONNECTION) + "\n");
            builder.append("CLIENT_MULTI_STATEMENTS:" + (serverCapabilities & CLIENT_MULTI_STATEMENTS) + "\n");
            builder.append("CLIENT_MULTI_RESULTS:" + (serverCapabilities & CLIENT_MULTI_RESULTS) + "\n");
            builder.append("==========================END Server Flag===============================\n");
            logger.debug(builder.toString());
        }
    }

}
