package com.meidusa.amoeba.net.packet;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.net.Connection;

public abstract class AbstractPacketFactory<T extends AbstractPacket,V extends AbstractPacketBuffer> implements PacketFactory<T> {
	private static final Logger logger = Logger.getLogger(AbstractPacketFactory.class);
	public T createPacket(Connection conn, byte[] buffer) {
		try {
			T packet = getPacketClass(buffer).newInstance();
			V packetBuffer = getPacketBufferClass(buffer).newInstance();
			packet.init(packetBuffer);
			return packet;
		} catch (Exception e) {
			logger.error("instance of packet Error",e);
			return null;
		}
	}
	
	protected abstract Class<T> getPacketClass(byte[] buffer);
	
	protected abstract Class<V> getPacketBufferClass(byte[] buffer);
}
