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
package com.meidusa.amoeba.mysql.net;

import java.nio.channels.SocketChannel;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.meidusa.amoeba.net.poolable.ObjectPool;
import com.meidusa.amoeba.net.poolable.PoolableObject;
import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.mysql.context.MysqlRuntimeContext;
import com.meidusa.amoeba.mysql.io.MySqlPacketConstant;
import com.meidusa.amoeba.mysql.net.packet.AuthenticationPacket;
import com.meidusa.amoeba.mysql.net.packet.ErrorPacket;
import com.meidusa.amoeba.mysql.net.packet.HandshakePacket;
import com.meidusa.amoeba.mysql.net.packet.MysqlPacketBuffer;
import com.meidusa.amoeba.mysql.net.packet.Scramble323Packet;
import com.meidusa.amoeba.mysql.util.CharsetMapping;
import com.meidusa.amoeba.net.Connection;
import com.meidusa.amoeba.net.Sessionable;
import com.meidusa.amoeba.util.Reporter;
import com.meidusa.amoeba.util.StringUtil;

/**
 * 设计为连接mysql server的客户端Connection
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public class MysqlServerConnection extends MysqlConnection implements MySqlPacketConstant,Reporter.SubReporter,PoolableObject{
	static Logger logger = Logger.getLogger(MysqlServerConnection.class);
	
	/**
	 * 默认与mysql服务器连接采用UTF8，当mysqlServerConnection 编码与 mysqlClientConnection 编码不一致的时候，
	 * 则在query之前会发送set names charset(客户端的相应编码)
	 */
	private static int DEFAULT_CHARSET_INDEX = 33;
	
	public static enum Status{WAITE_HANDSHAKE,AUTHING,COMPLETED};
	private Status status = Status.WAITE_HANDSHAKE;
	private ObjectPool objectPool;
	
	private boolean active;
	private long serverCapabilities;

	private String serverVersion;

	private int serverMajorVersion;

	private int serverMinorVersion;

	private int serverSubMinorVersion;
	private String seed;
	
	/**
	 * query timeout (TimeUnit:second.)
	 */
	private long queryTimeout;
	
	public MysqlServerConnection(SocketChannel channel, long createStamp) {
		super(channel, createStamp);
	}
	
	public void handleMessage(Connection conn) {
		byte[] message = null;
		while((message = this.getInQueue().getNonBlocking()) != null){
			if(!isAuthenticated()){
				/**
				 * 第一次数据为 handshake packet
				 * 第二次数据为 OkPacket packet or ErrorPacket 
				 * 
				 */
				MysqlPacketBuffer buffer = new MysqlPacketBuffer(message);
				if(MysqlPacketBuffer.isErrorPacket(message)){
					setAuthenticated(false);
					ErrorPacket error = new ErrorPacket();
					error.init(message,conn);
					logger.error("handShake with "+this._channel.socket().getRemoteSocketAddress()+" error:"+error.serverErrorMessage+",hashCode="+this.hashCode());
					return;
				}
				
				if(status == Status.WAITE_HANDSHAKE){
					if(logger.isDebugEnabled()){
						logger.debug("1. handShake with "+this.getSocketId()+",hashCode="+this.hashCode());
					}
					HandshakePacket handpacket = new HandshakePacket();
					handpacket.init(buffer);
					this.serverCapabilities = handpacket.serverCapabilities;
			        this.serverVersion = handpacket.serverVersion;
			        splitVersion();
			        if (!versionMeetsMinimum(4, 1, 1) || handpacket.protocolVersion != 10){
			        	logger.error("amoeba support version minimum 4.1.1  and protocol version 10");
			        	System.exit(-1);
			        }
			        
					if(logger.isDebugEnabled()){
						logger.debug("2. receive HandshakePacket packet from server:"+this.getSocketId()+",hashCode="+this.hashCode());
					}
					
					if(ProxyRuntimeContext.getInstance() != null){
						MysqlRuntimeContext context = (MysqlRuntimeContext)ProxyRuntimeContext.getInstance().getRuntimeContext();
						if(context!=null && context.getServerCharset() == null && handpacket.serverCharsetIndex > 0){
							context.setServerCharsetIndex(handpacket.serverCharsetIndex);
							if (logger.isDebugEnabled()) {
							  logger.debug("mysql server Handshake packet= "+handpacket.toString());
                            }
						}
					}
					AuthenticationPacket authing = new AuthenticationPacket();
					authing.password = this.getPassword();
					this.seed = authing.seed = handpacket.seed+handpacket.restOfScrambleBuff;
					authing.clientParam = CLIENT_FOUND_ROWS;
					authing.charsetNumber = (byte)(DEFAULT_CHARSET_INDEX & 0xff);
					this.setCharset(CharsetMapping.INDEX_TO_CHARSET[DEFAULT_CHARSET_INDEX]);
					
					if (versionMeetsMinimum(4, 1, 0)) {
			            if (versionMeetsMinimum(4, 1, 1)) {
			            	authing.clientParam |= CLIENT_PROTOCOL_41;
			                // Need this to get server status values
			            	authing.clientParam |= CLIENT_TRANSACTIONS;
	
			                // We always allow multiple result sets
			            	authing.clientParam |= CLIENT_MULTI_RESULTS;
	
			                // We allow the user to configure whether
			                // or not they want to support multiple queries
			                // (by default, this is disabled).
			                /*if (this.connection.getAllowMultiQueries()) {
			                    this.clientParam |= CLIENT_MULTI_QUERIES;
			                }*/
			            } else {
			            	authing.clientParam |= CLIENT_RESERVED;
			            }
			        }
					
					if (handpacket.protocolVersion > 9) {
						authing.clientParam |= CLIENT_LONG_PASSWORD; // for long passwords
			        } else {
			        	authing.clientParam &= ~CLIENT_LONG_PASSWORD;
			        }
					
					if ((this.serverCapabilities & CLIENT_LONG_FLAG) != 0) {
						authing.clientParam |= CLIENT_LONG_FLAG;
			        }
					
					if ((this.serverCapabilities & CLIENT_SECURE_CONNECTION) != 0) {
						authing.clientParam |= CLIENT_SECURE_CONNECTION;
					}
					
					authing.user = this.getUser();
					authing.packetId = 1;
					
					if(this.getSchema() != null){
						authing.database = this.getSchema();
						authing.clientParam |= CLIENT_CONNECT_WITH_DB;
					}
					
					authing.maxThreeBytes = 1073741824;
					
					status = Status.AUTHING;
					if(logger.isDebugEnabled()){
						logger.debug("3. authing packet sent to server:"+this.getSocketId()+",hashCode="+this.hashCode());
					}
					this.postMessage(authing.toByteBuffer(conn));
				}else if(status == Status.AUTHING){
					if(logger.isDebugEnabled()){
						logger.debug("4. authing result packet from server:"+this.getSocketId()+",hashCode="+this.hashCode());
					}
					
					if(MysqlPacketBuffer.isOkPacket(message)){
						setAuthenticated(true);
						return;
					}else{
						if(message.length<9 && MysqlPacketBuffer.isEofPacket(message)){
							Scramble323Packet packet = new Scramble323Packet();
							packet.packetId = 3;
							packet.seed323 = this.seed.substring(0, 8);
							packet.password = this.getPassword();
							this.postMessage(packet.toByteBuffer(conn));
							if(logger.isDebugEnabled()){
								logger.debug("5. server request scrambled password in old format"+",hashCode="+this.hashCode());
							}
						}else{
							logger.warn("5. server response packet from :"+this.getSocketId()+" :\n"+StringUtil.dumpAsHex(message, message.length)+",hashCode="+this.hashCode(),new Exception());
						}
					}
				}else{
					logger.error("handShake with "+this._channel.socket().getRemoteSocketAddress()+" stat:"+status);
				}
	
			}else{
				logger.warn("server "+this._channel.socket().getRemoteSocketAddress()+" raw handler message:"+StringUtil.dumpAsHex(message, message.length));
			}
		}
		
	}
	
    public boolean isVersion(int major, int minor, int subminor) {
        return ((major == getServerMajorVersion()) &&
        (minor == getServerMinorVersion()) &&
        (subminor == getServerSubMinorVersion()));
    }
	
	public boolean versionMeetsMinimum(int major, int minor, int subminor) {
        if (getServerMajorVersion() >= major) {
            if (getServerMajorVersion() == major) {
                if (getServerMinorVersion() >= minor) {
                    if (getServerMinorVersion() == minor) {
                        return (getServerSubMinorVersion() >= subminor);
                    }

                    // newer than major.minor
                    return true;
                }

                // older than major.minor
                return false;
            }

            // newer than major  
            return true;
        }

        return false;
    }
	
    /**
     * Get the major version of the MySQL server we are talking to.
     *
     * @return DOCUMENT ME!
     */
    final int getServerMajorVersion() {
        return this.serverMajorVersion;
    }

    /**
     * Get the minor version of the MySQL server we are talking to.
     *
     * @return DOCUMENT ME!
     */
    final int getServerMinorVersion() {
        return this.serverMinorVersion;
    }

    /**
     * Get the sub-minor version of the MySQL server we are talking to.
     *
     * @return DOCUMENT ME!
     */
    final int getServerSubMinorVersion() {
        return this.serverSubMinorVersion;
    }

    /**
     * Get the version string of the server we are talking to
     *
     * @return DOCUMENT ME!
     */
    String getServerVersion() {
        return this.serverVersion;
    }
	private void splitVersion(){
		// Parse the server version into major/minor/subminor
        int point = this.serverVersion.indexOf("."); //$NON-NLS-1$

        if (point != -1) {
            try {
                int n = Integer.parseInt(this.serverVersion.substring(0, point));
                this.serverMajorVersion = n;
            } catch (NumberFormatException NFE1) {
                ;
            }

            String remaining = this.serverVersion.substring(point + 1,
                    this.serverVersion.length());
            point = remaining.indexOf("."); //$NON-NLS-1$

            if (point != -1) {
                try {
                    int n = Integer.parseInt(remaining.substring(0, point));
                    this.serverMinorVersion = n;
                } catch (NumberFormatException nfe) {
                    ;
                }

                remaining = remaining.substring(point + 1, remaining.length());

                int pos = 0;

                while (pos < remaining.length()) {
                    if ((remaining.charAt(pos) < '0') ||
                            (remaining.charAt(pos) > '9')) {
                        break;
                    }

                    pos++;
                }

                try {
                    int n = Integer.parseInt(remaining.substring(0, pos));
                    this.serverSubMinorVersion = n;
                } catch (NumberFormatException nfe) {
                    ;
                }
            }
        }
	}
	
	/**
	 * 正在处于验证的Connection Idle时间可以设置相应的少一点。
	 */
	public boolean checkIdle(long now) {
		if (isClosed()) {
            return true;
        }
		if(isAuthenticated()){
			//处于使用中的链接， 如果超过5分钟没有发生网络IO，则需要关闭该链接 
			long idleMillis = now - _lastEvent;
			if(isActive()){
				if (idleMillis > getQueryTimeout() * 1000) {
					return true;
				}
			}
			if(_handler instanceof Sessionable){
				/**
				 * 该处在高并发的情况下可能会发生ClassCastException 异常,为了提升性能,这儿将忽略这种异常.
				 */
				try{
					Sessionable session = (Sessionable)_handler;
					boolean sessionIdle = session.checkIdle(now);
					if(sessionIdle){
						logger.error("Session timeout. conn="+this.toString()+" ,idleMillis="+idleMillis+",_handler="+_handler.toString());
					}
					
					return sessionIdle;
				}catch(ClassCastException castException){
					return false;
				}
			}
			return false;
		}else{
			long idleMillis = now - _lastEvent;
			if (idleMillis < 15000) {
				return false;
			}
			return true;
		}
	}
	
	
	public void appendReport(StringBuilder buffer, long now, long sinceLast,
			boolean reset,Level level) {
		
		if(this._handler instanceof Reporter.SubReporter && this._handler != this){
			Reporter.SubReporter reporter = (Reporter.SubReporter)(this._handler);
			reporter.appendReport(buffer, now, sinceLast, reset,level);
		}
	}

	

	public ObjectPool getObjectPool() {
		return objectPool;
	}

	public synchronized void setObjectPool(ObjectPool pool) {
		if(objectPool == null && pool == null){
			if(this.isAuthenticated()){
				logger.warn("Set pool null",new Exception());
			}
		}
		this.objectPool = pool;
	}
	
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this._lastEvent = System.currentTimeMillis();
		this.active = active;
	}

	public long getQueryTimeout() {
		return queryTimeout;
	}

	public void setQueryTimeout(long queryTimeout) {
		this.queryTimeout = queryTimeout;
	}

	public boolean isRemovedFromPool() {
		return objectPool == null;
	}
	
  protected void close(Exception exception){
		super.close(exception);
		final ObjectPool tmpPool = objectPool;
		objectPool = null;
		try {
			if(tmpPool != null){
				
				/**
				 * 处于active 状态的 poolableObject，可以用ObjectPool.invalidateObject 方式从pool中销毁
				 * 否则只能等待被borrow 或者 idle time out
				 */
				if(isActive()){
					tmpPool.invalidateObject(this);
				}
				if(_handler instanceof Sessionable){
					/**
					 * TODO 该处在高并发的情况下可能会发生ClassCastException 异常,为了提升性能,这儿将忽略这种异常.
					 */
					Sessionable session = (Sessionable)_handler;
					if(!session.isEnded()){
						session.endSession(true, exception);
					}
				}
			}
		} catch (Exception e) {
			// TODO handle exception
			logger.warn(e);
		}
	}
}
