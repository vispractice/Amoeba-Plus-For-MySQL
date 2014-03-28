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
import com.meidusa.amoeba.util.StringFillFormat;

/**
 * From client to server whenever the client wants the server to do something.
 * 
 * <pre>
 * Bytes          Name
 *  -----         ----
 *  1             command
 *  n             arg
 *  
 *  command:      The most common value is 03 COM_QUERY, because
 *                INSERT UPDATE DELETE SELECT etc. have this code.
 *                The possible values at time of writing (taken
 *                from /include/mysql_com.h for enum_server_command) are:
 *  
 *                #      Name                    Associated client function
 *                -      ----                    --------------------------
 *                0x00   COM_SLEEP               (none, this is an internal thread state)
 *                0x01   COM_QUIT                mysql_close
 *                0x02   COM_INIT_DB             mysql_select_db 
 *                0x03   COM_QUERY               mysql_real_query
 *                0x04   COM_FIELD_LIST          mysql_list_fields
 *                0x05   COM_CREATE_DB           mysql_create_db (deprecated)
 *                0x06   COM_DROP_DB             mysql_drop_db (deprecated)
 *                0x07   COM_REFRESH             mysql_refresh
 *                0x08   COM_SHUTDOWN            mysql_shutdown
 *                0x09   COM_STATISTICS          mysql_stat
 *                0x0a   COM_PROCESS_INFO        mysql_list_processes
 *                0x0b   COM_CONNECT             (none, this is an internal thread state)
 *                0x0c   COM_PROCESS_KILL        mysql_kill
 *                0x0d   COM_DEBUG               mysql_dump_debug_info
 *                0x0e   COM_PING                mysql_ping
 *                0x0f   COM_TIME                (none, this is an internal thread state)
 *                0x10   COM_DELAYED_INSERT      (none, this is an internal thread state)
 *                0x11   COM_CHANGE_USER         mysql_change_user
 *                0x12   COM_BINLOG_DUMP         (used by slave server / mysqlbinlog)
 *                0x13   COM_TABLE_DUMP          (used by slave server to get master table)
 *                0x14   COM_CONNECT_OUT         (used by slave to log connection to master)
 *                0x15   COM_REGISTER_SLAVE      (used by slave to register to master)
 *                0x16   COM_STMT_PREPARE        mysql_stmt_prepare
 *                0x17   COM_STMT_EXECUTE        mysql_stmt_execute
 *                0x18   COM_STMT_SEND_LONG_DATA mysql_stmt_send_long_data
 *                0x19   COM_STMT_CLOSE          mysql_stmt_close
 *                0x1a   COM_STMT_RESET          mysql_stmt_reset
 *                0x1b   COM_SET_OPTION          mysql_set_server_option
 *                0x1c   COM_STMT_FETCH          mysql_stmt_fetch
 *  
 *  arg:          The text of the command is just the way the user typed it, there is no processing
 *                by the client (except removal of the final ';').
 *                This field is not a null-terminated string; however,
 *                the size can be calculated from the packet size,
 *                and the MySQL client appends '\0' when receiving.
 * </pre>
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 * @author hexianmao
 */
public class QueryCommandPacket extends CommandPacket {

    public String query;

    @Override
    public void init(AbstractPacketBuffer myBuffer) {
        super.init(myBuffer);
        MysqlPacketBuffer buffer = (MysqlPacketBuffer) myBuffer;
        String charset = buffer.getCharset();
        this.query = (charset == null ? buffer.readString() : buffer.readString(charset));
    }

    @Override
    public void write2Buffer(AbstractPacketBuffer myBuffer) throws UnsupportedEncodingException {
        super.write2Buffer(myBuffer);
        MysqlPacketBuffer buffer = (MysqlPacketBuffer) myBuffer;
        buffer.writeString(query);
    }

    @Override
    protected int calculatePacketSize() {
        int packLength = super.calculatePacketSize();
        packLength += (query == null ? 0 : query.length() * 2);
        return packLength;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("[Length=").append(StringFillFormat.format(packetLength, 4));
        s.append(", PacketId=").append(StringFillFormat.format(packetId, 2));
        s.append(", Command=").append(StringFillFormat.format(command, 2));
        s.append(", Query=").append(query).append("]");
        return s.toString();
    }

}
