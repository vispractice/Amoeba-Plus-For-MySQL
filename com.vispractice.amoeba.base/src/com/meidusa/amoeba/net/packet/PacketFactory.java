package com.meidusa.amoeba.net.packet;

import com.meidusa.amoeba.net.Connection;

/**
 * 由Packet 工厂负责创建 Packet
 * @author struct
 *
 */
public interface PacketFactory<T extends Packet> {
	
	/**
	 * 
	 * @param conn 数据包的来源Connection
	 * @param buffer 数据包字节
	 * @return Packet 目标数据包
	 */
	T createPacket(Connection conn,byte[] buffer);

}
