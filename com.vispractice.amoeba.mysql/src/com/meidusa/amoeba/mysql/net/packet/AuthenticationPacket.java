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
import java.security.NoSuchAlgorithmException;

import com.meidusa.amoeba.mysql.util.Security;
import com.meidusa.amoeba.mysql.util.Util;
import com.meidusa.amoeba.net.packet.AbstractPacketBuffer;

/**
 * From client to server during initial handshake.
 * 
 * <pre>
 * 
 *  &lt;b&gt;VERSION 4.0&lt;/b&gt;
 *  Bytes                        Name
 *  -----                        ----
 *  2                            client_flags
 *  3                            max_packet_size
 *  n  (Null-Terminated String)  user
 *  8                            scramble_buff
 *  1                            (filler) always 0x00
 *  
 *  &lt;b&gt;VERSION 4.1&lt;/b&gt;
 *  Bytes                        Name
 *  -----                        ----
 *  4                            client_flags
 *  4                            max_packet_size
 *  1                            charset_number
 *  23                           (filler) always 0x00...
 *  n (Null-Terminated String)   user
 *  n (Length Coded Binary)      scramble_buff (1 + x bytes)
 *  1                            (filler) always 0x00
 *  n (Null-Terminated String)   databasename
 *  
 *  client_flags:            CLIENT_xxx options. The list of possible flag
 *                           values is in the description of the Handshake
 *                           Initialisation Packet, for server_capabilities.
 *                           For some of the bits, the server passed &quot;what
 *                           it's capable of&quot;. The client leaves some of the
 *                           bits on, adds others, and passes back to the server.
 *                           One important flag is: whether compression is desired.
 *  
 *  max_packet_size:         the maximum number of bytes in a packet for the client
 *  
 *  charset_number:          in the same domain as the server_language field that
 *                           the server passes in the Handshake Initialization packet.
 *  
 *  user:                    identification
 *  
 *  scramble_buff:           the password, after encrypting using the scramble_buff
 *                           contents passed by the server (see &quot;Password functions&quot;
 *                           section elsewhere in this document)
 *                           if length is zero, no password was given
 *  
 *  databasename:            name of schema to use initially
 * 
 * </pre>
 * 
 * 该数据包只支持mysql 4.1版本以后,mysql协议版本10，并且是非安全连接
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public class AuthenticationPacket extends AbstractPacket {

    public long   clientParam;
    public long   maxThreeBytes;

    // charset, JDBC will connect as 'latin1',
    // and use 'SET NAMES' to change to the desired
    // charset after the connection is established.
    public byte   charsetNumber = 8;

    public String user;

    public String seed;

    public String password;

    // 16字节
    public byte[] encryptedPassword;

    public String database;

    public void init(AbstractPacketBuffer mybuffer) {
        super.init(mybuffer);
        MysqlPacketBuffer buffer = (MysqlPacketBuffer) mybuffer;
        int position = buffer.getPosition();
        clientParam = buffer.readInt();
        
        if((clientParam & CLIENT_PROTOCOL_41) == 0){
        	//protocol version 4.0
        	//we will reject version 4.0 ,so ignore decode this packet 
        }else{
        	//protocol version 4.1
        	buffer.setPosition(position);
	        clientParam = buffer.readLong();
	        maxThreeBytes = buffer.readLong();
	        charsetNumber = buffer.readByte();
	        // 跳过23个填充字节
	        buffer.setPosition(buffer.getPosition() + 23);
	        user = buffer.readString();
	        // buffer.read
	        long passwordLength = buffer.readFieldLength();
	
	        encryptedPassword = buffer.getBytes(buffer.getPosition(), (int) passwordLength);
	        // encryptedPassword = buffer.readString(CODE_PAGE_1252);
	
	        buffer.setPosition(buffer.getPosition() + (int) passwordLength);
	        if ((clientParam & CLIENT_CONNECT_WITH_DB) != 0) {
	            if (buffer.getPosition() < buffer.getBufLength()) {
	                database = buffer.readString();
	            }
	        }
        }
    }

    public void write2Buffer(AbstractPacketBuffer mybuffer) throws UnsupportedEncodingException {
        super.write2Buffer(mybuffer);
        MysqlPacketBuffer buffer = (MysqlPacketBuffer) mybuffer;

        if ((this.clientParam & CLIENT_SECURE_CONNECTION) != 0) {
            buffer.writeLong(clientParam);
            buffer.writeLong(maxThreeBytes);
            buffer.writeByte(charsetNumber);
            buffer.writeBytesNoNull(new byte[23]);

            if (user == null) {
                user = "";
            }
            buffer.writeString(user);

            if (this.password != null && password.length() != 0) {
                try {
                    encryptedPassword = Security.scramble411(password, this.seed);
                } catch (NoSuchAlgorithmException e) {
                    logger.error("encrypt Password error", e);
                }

                if (encryptedPassword != null && encryptedPassword.length != 0) {
                    buffer.writeFieldLength(encryptedPassword.length);
                    buffer.writeBytesNoNull(encryptedPassword);
                } else {
                    buffer.writeByte((byte) 0);
                }
            } else {
                buffer.writeByte((byte) 0);
            }

            if ((clientParam & CLIENT_CONNECT_WITH_DB) != 0) {
                if (database != null) {
                    buffer.writeString(database);
                }
            }

        } else {
            buffer.writeLong(this.clientParam);
            buffer.writeLong(this.maxThreeBytes);

            // charset, JDBC will connect as 'latin1',
            // and use 'SET NAMES' to change to the desired
            // charset after the connection is established.
            buffer.writeByte(charsetNumber);

            // Set of bytes reserved for future use.
            buffer.writeBytesNoNull(new byte[23]);

            buffer.writeString(user, CODE_PAGE_1252);
            buffer.writeString(Util.newCrypt(password, this.seed), CODE_PAGE_1252);

            if ((clientParam & CLIENT_CONNECT_WITH_DB) != 0) {
                if (database != null) {
                    buffer.writeString(database, CODE_PAGE_1252);
                }
            }

        }

    }

    protected int calculatePacketSize() {
        int packLength = super.calculatePacketSize();
        int passwordLength = 16;
        int userLength = (user != null) ? user.length() : 0;
        int databaseLength = (database != null) ? database.length() : 0;
        packLength += ((userLength + passwordLength + databaseLength) * 2) + 7 + HEADER_SIZE + AUTH_411_OVERHEAD;
        return packLength;
    }

    public static void main(String[] args) {
        AuthenticationPacket auth = new AuthenticationPacket();
        auth.charsetNumber = 8;
        auth.password = "hello12323";
        auth.seed = "12345678912345678901";
        auth.packetId = 1;
        auth.database = "test";
        auth.clientParam = 63487;
        auth.user = "testamoeba";
        AuthenticationPacket other = new AuthenticationPacket();
        other.init(auth.toByteBuffer(null).array(), null);
        System.out.println(other);

        System.out.println(41516 & CLIENT_SECURE_CONNECTION);
    }

}
