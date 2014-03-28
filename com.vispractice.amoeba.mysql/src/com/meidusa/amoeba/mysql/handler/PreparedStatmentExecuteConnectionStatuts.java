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

package com.meidusa.amoeba.mysql.handler;

import com.meidusa.amoeba.mysql.handler.session.SessionStatus;
import com.meidusa.amoeba.mysql.net.packet.MysqlPacketBuffer;
import com.meidusa.amoeba.mysql.net.packet.QueryCommandPacket;
import com.meidusa.amoeba.net.Connection;

public class PreparedStatmentExecuteConnectionStatuts extends PreparedStatmentConnectionStatuts{
		public PreparedStatmentExecuteConnectionStatuts(Connection conn,PreparedStatmentInfo preparedStatmentInfo){
			super(conn,preparedStatmentInfo);
		}
		
		@Override
		public boolean isCompleted(byte[] buffer) {
			if(this.commandType == QueryCommandPacket.COM_STMT_EXECUTE){
				if (packetIndex == 0){
                	if(MysqlPacketBuffer.isErrorPacket(buffer)){
                		this.statusCode |= PreparedStatmentSessionStatus.ERROR;
    					this.statusCode |= PreparedStatmentSessionStatus.COMPLETED;
    					 lastStatusCode = SessionStatus.ERROR;
    					this.setErrorPacket(buffer);
                        return true;
                	}else if(MysqlPacketBuffer.isOkPacket(buffer)){
                		this.statusCode |= PreparedStatmentSessionStatus.OK;
    					this.statusCode |= PreparedStatmentSessionStatus.COMPLETED;
                        lastStatusCode = SessionStatus.OK;
                         return true;
                	}else {
                        if (statusCode == SessionStatus.QUERY) {
                            statusCode |= SessionStatus.RESULT_HEAD;
                        }
                        return false;
                    }
                }else{
                	if(lastStatusCode == SessionStatus.EOF_FIELDS && MysqlPacketBuffer.isErrorPacket(buffer)){
                		statusCode |= SessionStatus.ERROR;
                        statusCode |= SessionStatus.COMPLETED;
                        lastStatusCode = SessionStatus.ERROR;
                        return true;
                	}else if((isCall && (lastStatusCode == SessionStatus.EOF_ROWS)) && MysqlPacketBuffer.isOkPacket(buffer)){
                		statusCode |= SessionStatus.OK;
                        statusCode |= SessionStatus.COMPLETED;
                        lastStatusCode = SessionStatus.OK;
                        return true;
                	}else if (MysqlPacketBuffer.isEofPacket(buffer) ) {
                        if ((statusCode & SessionStatus.EOF_FIELDS) > 0) {
                            statusCode |= SessionStatus.EOF_ROWS;
                            lastStatusCode = SessionStatus.EOF_ROWS;
                            if(!isCall){
                            	statusCode |= SessionStatus.COMPLETED;
                            	return true;
                            }
                        } else {
                            statusCode |= SessionStatus.EOF_FIELDS;
                            lastStatusCode = SessionStatus.EOF_FIELDS;
                            return false;
                        }
                    } 
                	return false;
                }
				
				/*if(MysqlPacketBuffer.isEofPacket(buffer)){
					if((this.statusCode & PreparedStatmentSessionStatus.EOF_FIELDS)==0){
						this.statusCode |= PreparedStatmentSessionStatus.EOF_FIELDS;
						return false;
					}else{
						this.statusCode |= PreparedStatmentSessionStatus.EOF_ROWS;
						this.statusCode |= PreparedStatmentSessionStatus.COMPLETED;
						return true;
					}
				}else if(packetIndex == 0 && MysqlPacketBuffer.isErrorPacket(buffer)){
					this.statusCode |= PreparedStatmentSessionStatus.ERROR;
					this.statusCode |= PreparedStatmentSessionStatus.COMPLETED;
					this.setErrorPacket(buffer);
					return true;
				}else if(packetIndex == 0 && MysqlPacketBuffer.isOkPacket(buffer)){
					this.statusCode |= PreparedStatmentSessionStatus.OK;
					this.statusCode |= PreparedStatmentSessionStatus.COMPLETED;
					return true;
				}
				return false;*/
			}else if(this.commandType == QueryCommandPacket.COM_STMT_SEND_LONG_DATA){
				return true;
			}else{
				return super.isCompleted(buffer);
			}
		}
	}