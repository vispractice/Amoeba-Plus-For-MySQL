package com.meidusa.amoeba.net;

import com.meidusa.amoeba.net.packet.Packet;

public interface PacketHandler<T extends Packet> extends MessageHandler {
	public void handlePacket(T packet);
}
