package com.meidusa.amoeba.mysql.net.packet;

import java.io.UnsupportedEncodingException;

import com.meidusa.amoeba.mysql.util.Util;
import com.meidusa.amoeba.net.packet.AbstractPacketBuffer;

/**
 * <pre>
 * By sending this very specific reply server asks us to send scrambled 
 * password in old format. The reply contains scramble_323.
 *</pre>
 * 
 * @author Struct
 */
public class Scramble323Packet extends AbstractPacket {

    public String password;
    public String seed323;

    public void write2Buffer(AbstractPacketBuffer mybuffer) throws UnsupportedEncodingException {
        super.write2Buffer(mybuffer);
        MysqlPacketBuffer buffer = (MysqlPacketBuffer) mybuffer;
        buffer.writeString(Util.newCrypt(password, seed323));
    }

}
