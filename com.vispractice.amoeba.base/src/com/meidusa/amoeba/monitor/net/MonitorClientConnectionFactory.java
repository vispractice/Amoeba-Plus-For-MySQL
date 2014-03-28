package com.meidusa.amoeba.monitor.net;

import java.nio.channels.SocketChannel;

import com.meidusa.amoeba.net.AbstractConnectionFactory;
import com.meidusa.amoeba.net.Connection;

public class MonitorClientConnectionFactory extends AbstractConnectionFactory {

	@Override
	protected Connection newConnectionInstance(SocketChannel channel,
			long createStamp) {
		return new MonitorClientConnection(channel,createStamp);
	}

}
