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
import com.meidusa.amoeba.mysql.net.MysqlServerConnection;
import com.meidusa.amoeba.mysql.net.packet.MysqlPacketBuffer;
import com.meidusa.amoeba.mysql.net.packet.OKforPreparedStatementPacket;
import com.meidusa.amoeba.mysql.net.packet.QueryCommandPacket;
import com.meidusa.amoeba.net.Connection;


public class PreparedStatmentConnectionStatuts extends QueryCommandMessageHandler.QueryConnectionStatus {
	static class PreparedStatmentSessionStatus extends SessionStatus {
        public static final int PREPAED_PARAMETER_EOF = 2048;
        public static final int PREPAED_FIELD_EOF     = 4096;
    }
	
    OKforPreparedStatementPacket ok = null;

    public PreparedStatmentConnectionStatuts(Connection conn, PreparedStatmentInfo preparedStatmentInfo){
        super(conn);
    }

    /**
     * packet step: 1:OKforPreparedStatementPacket, (parameters ==0,columns==0) end; parameters>0 [
     * n*parameterFieldPacket,PREPAED_PARAMETER_EOF] columns>0 [n * columnPacket,PREPAED_FIELD_EOF] mysql version
     * 5.0.0 no parameter field packet
     */
    @Override
    public boolean isCompleted(byte[] buffer) {
        if (this.commandType == QueryCommandPacket.COM_STMT_PREPARE) {
            MysqlServerConnection connection = (MysqlServerConnection) conn;
            if (MysqlPacketBuffer.isEofPacket(buffer)) {
                if (ok.parameters > 0 && ok.columns > 0) {
                    if ((this.statusCode & PreparedStatmentSessionStatus.PREPAED_PARAMETER_EOF) > 0) {
                        this.statusCode |= PreparedStatmentSessionStatus.PREPAED_FIELD_EOF;
                        this.statusCode |= PreparedStatmentSessionStatus.COMPLETED;
                        lastStatusCode = PreparedStatmentSessionStatus.PREPAED_FIELD_EOF;
                        return true;
                    } else {
                        if (connection.isVersion(5, 0, 0)) {
                            if (ok.columns == 0) {
                                this.statusCode |= PreparedStatmentSessionStatus.PREPAED_FIELD_EOF;
                                this.statusCode |= PreparedStatmentSessionStatus.COMPLETED;
                                lastStatusCode = PreparedStatmentSessionStatus.PREPAED_FIELD_EOF;
                                return true;
                            }
                        }
                        this.statusCode |= PreparedStatmentSessionStatus.PREPAED_PARAMETER_EOF;
                        lastStatusCode = PreparedStatmentSessionStatus.PREPAED_PARAMETER_EOF;
                        return false;
                    }
                } else {
                    this.statusCode |= PreparedStatmentSessionStatus.PREPAED_FIELD_EOF;
                    lastStatusCode = PreparedStatmentSessionStatus.PREPAED_FIELD_EOF;
                    this.statusCode |= PreparedStatmentSessionStatus.COMPLETED;
                    return true;
                }

            } else if ((packetIndex == 0 || lastStatusCode == PreparedStatmentSessionStatus.PREPAED_FIELD_EOF) && MysqlPacketBuffer.isErrorPacket(buffer)) {
                this.statusCode |= PreparedStatmentSessionStatus.ERROR;
                lastStatusCode = PreparedStatmentSessionStatus.ERROR;
                this.statusCode |= PreparedStatmentSessionStatus.COMPLETED;
                setErrorPacket(buffer);
                return true;
            } else if (packetIndex == 0 && MysqlPacketBuffer.isOkPacket(buffer)) {
                ok = new OKforPreparedStatementPacket();
                ok.init(buffer, null);
                if (ok.parameters == 0 && ok.columns == 0) {
                    this.statusCode |= PreparedStatmentSessionStatus.OK;
                    this.statusCode |= PreparedStatmentSessionStatus.COMPLETED;
                    lastStatusCode = PreparedStatmentSessionStatus.OK;
                    return true;
                } else {
                    if (connection.isVersion(5, 0, 0)) {
                        if (ok.columns == 0) {
                            this.statusCode |= PreparedStatmentSessionStatus.OK;
                            this.statusCode |= PreparedStatmentSessionStatus.COMPLETED;
                            lastStatusCode = PreparedStatmentSessionStatus.OK;
                            return true;
                        }
                    }
                    return false;
                }
            }
            return false;
        } else if (this.commandType == QueryCommandPacket.COM_STMT_CLOSE) {
            if (packetIndex == 0 && MysqlPacketBuffer.isErrorPacket(buffer)) {
                this.statusCode |= PreparedStatmentSessionStatus.ERROR;
                this.statusCode |= PreparedStatmentSessionStatus.COMPLETED;
                setErrorPacket(buffer);
                return true;
            } else {
                this.statusCode |= PreparedStatmentSessionStatus.COMPLETED;
                return true;
            }
        } else {
            return super.isCompleted(buffer);
        }
    }
}