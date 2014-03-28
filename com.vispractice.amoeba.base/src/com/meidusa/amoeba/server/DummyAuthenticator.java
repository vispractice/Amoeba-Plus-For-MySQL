/*
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
package com.meidusa.amoeba.server;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.net.AuthResponseData;
import com.meidusa.amoeba.net.Authenticator;
import com.meidusa.amoeba.net.AuthingableConnection;
import com.meidusa.amoeba.net.packet.AbstractPacket;

/**
 * 一个相当简单的身份验证者.
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public  class DummyAuthenticator<T extends AbstractPacket> extends Authenticator<T> {
	protected static Logger logger = Logger.getLogger(DummyAuthenticator.class);
	
	public DummyAuthenticator() {
		
	}

	@Override
	protected void processAuthentication(AuthingableConnection conn,
			AbstractPacket authenPacket, AuthResponseData rdata) {
	  
	    if (logger.isInfoEnabled()) {
	      logger.info("Accepting request: conn=" + conn);
        }
	    
		rdata.code = AuthResponseData.SUCCESS;
	}

}
