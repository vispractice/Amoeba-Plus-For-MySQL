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
public class FieldPacket extends AbstractPacket {

    public String           catalog;
    public String           db;
    public String           table;
    public String           orgTable;
    public String           name;
    public String           orgName;
    public int              character;
    public long             length;

    public transient String charsetName;

    /** mysql Type */
    public byte             type;

    /** javaType */
    public transient int    javaType;
    public int              flags;
    public byte             decimals;
    public String           definition;

    @Override
    public void init(AbstractPacketBuffer myBuffer) {
        super.init(myBuffer);
        MysqlPacketBuffer buffer = (MysqlPacketBuffer) myBuffer;
        catalog = buffer.readLengthCodedString(CODE_PAGE_1252);
        db = buffer.readLengthCodedString(CODE_PAGE_1252);
        table = buffer.readLengthCodedString(CODE_PAGE_1252);
        orgTable = buffer.readLengthCodedString(CODE_PAGE_1252);
        name = buffer.readLengthCodedString(CODE_PAGE_1252);
        orgName = buffer.readLengthCodedString(CODE_PAGE_1252);
        buffer.readByte();// only for move position
        character = buffer.readInt();
        length = buffer.readLong();
        type = buffer.readByte();
        flags = buffer.readInt();
        decimals = buffer.readByte();
        buffer.readByte();
        buffer.readByte();
        if (buffer.getPosition() < buffer.getBufLength()) {
            definition = buffer.readLengthCodedString(CODE_PAGE_1252);
        }
    }

    @Override
    protected void write2Buffer(AbstractPacketBuffer myBuffer) throws UnsupportedEncodingException {
        super.write2Buffer(myBuffer);
        MysqlPacketBuffer buffer = (MysqlPacketBuffer) myBuffer;
        buffer.writeLengthCodedString(catalog, CODE_PAGE_1252);

        buffer.writeLengthCodedString(db, CODE_PAGE_1252);
        buffer.writeLengthCodedString(table, CODE_PAGE_1252);
        buffer.writeLengthCodedString(orgTable, CODE_PAGE_1252);
        buffer.writeLengthCodedString(name, CODE_PAGE_1252);
        buffer.writeLengthCodedString(orgName, CODE_PAGE_1252);
        buffer.writeByte((byte) 0x0c);// write filler
        buffer.writeInt(character);
        buffer.writeLong(length);
        buffer.writeByte(type);
        buffer.writeInt(flags);
        buffer.writeByte(decimals);
        buffer.writeByte((byte) 0x00);
        buffer.writeByte((byte) 0x00);
        buffer.writeLengthCodedString(definition, CODE_PAGE_1252);

    }

    protected int calculatePacketSize() {
        int packLength = super.calculatePacketSize();

        int length = (catalog == null ? 0 : catalog.length());
        length += (db == null ? 0 : db.length());
        length += (table == null ? 0 : table.length());
        length += (orgTable == null ? 0 : orgTable.length());
        length += (name == null ? 0 : name.length());
        length += (orgName == null ? 0 : orgName.length());
        length = length * 2;
        length += 2 + 4 + 1 + 2 + 1 + (definition == null ? 0 : definition.length()) * 2;

        packLength += length;
        return packLength;
    }

}
