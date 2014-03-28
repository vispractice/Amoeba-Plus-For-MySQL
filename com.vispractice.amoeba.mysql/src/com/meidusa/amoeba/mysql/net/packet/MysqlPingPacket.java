package com.meidusa.amoeba.mysql.net.packet;

/**
 * 
 * @author struct
 *
 */
public class MysqlPingPacket extends CommandPacket {
	public MysqlPingPacket(){
		command = COM_PING;
	}
}
