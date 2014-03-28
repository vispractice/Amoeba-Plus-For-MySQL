/*
 * Copyright 2008-2108 amoeba.meidusa.com 
 * 
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU AFFERO GENERAL PUBLIC LICENSE for more details. 
 * 	You should have received a copy of the GNU AFFERO GENERAL PUBLIC LICENSE along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.meidusa.amoeba.mysql.filter;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.mysql.filter.FilterInvocation.Result;
import com.meidusa.amoeba.mysql.handler.MySqlCommandDispatcher;
import com.meidusa.amoeba.mysql.net.packet.MysqlPacketBuffer;
import com.meidusa.amoeba.mysql.net.packet.OkPacket;
import com.meidusa.amoeba.mysql.net.packet.QueryCommandPacket;

/**
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public class PacketIOFilter extends  AbstractIOFilter {
	protected static Logger logger = Logger.getLogger(MySqlCommandDispatcher.class);

	@Override
	protected Result packetFilter(byte[] message) {
		if(MysqlPacketBuffer.isPacketType(message, QueryCommandPacket.COM_QUIT)){
			return Result.QUIT;
		}else if(MysqlPacketBuffer.isPacketType(message, QueryCommandPacket.COM_STMT_CLOSE)){
			return Result.RETURN;
		}else if(MysqlPacketBuffer.isPacketType(message, QueryCommandPacket.COM_PING)){
			OkPacket ok = new OkPacket();
			ok.affectedRows = 0;
			ok.insertId = 0;
			ok.packetId = 1;
			ok.serverStatus = 2;
			this.setResultBuffer(ok.toByteBuffer(null).array());
			return Result.RETURN;
		}
		return null;
	}

}
