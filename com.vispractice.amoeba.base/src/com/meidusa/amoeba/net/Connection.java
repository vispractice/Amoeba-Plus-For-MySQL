/**
 * <pre>
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU AFFERO GENERAL PUBLIC LICENSE for more details. 
 * 	You should have received a copy of the GNU AFFERO GENERAL PUBLIC LICENSE along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * </pre>
 */
package com.meidusa.amoeba.net;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.net.io.PacketInputStream;
import com.meidusa.amoeba.net.io.PacketOutputStream;
import com.meidusa.amoeba.net.packet.Packet;
import com.meidusa.amoeba.net.packet.PacketFactory;
import com.meidusa.amoeba.util.Queue;

/**
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public abstract class Connection implements NetEventHandler {

    private static Logger       logger        = Logger.getLogger(Connection.class);
    public static final long    PING_INTERVAL = 90 * 1000L;
    protected static final long LATENCY_GRACE = 30 * 1000L;

    private final Object writeLock = new Object();
    protected ConnectionManager _cmgr;
    protected SelectionKey      _selkey;
    protected SocketChannel     _channel;
    protected long              _lastEvent;
    protected MessageHandler    _handler;
    protected final Lock        closeLock     = new ReentrantLock(false);
    protected final Lock        postCloseLock = new ReentrantLock(false);
    protected long _createTime = System.currentTimeMillis();
    protected boolean           closePosted   = false;
    private PacketInputStream   _fin;

    private PacketOutputStream  _fout;
    protected Queue<byte[]> _inQueue     = new Queue<byte[]>();
    protected Queue<ByteBuffer> _outQueue     = new Queue<ByteBuffer>();
    private boolean             socketClosed  = false;
    protected String            host;
    protected int               port;
    protected boolean 		  isAdmin;
    
    // 为了支持事务，客户端和后端连接建立了粘性，这些粘起来的连接是不可关闭的
    protected boolean closeable;

    private final UUID cid = UUID.randomUUID();  
    
    public PacketFactory<? extends Packet> getPacketFactory() {
        return packetFactory;
    }

    public Queue<byte[]> getInQueue(){
    	return _inQueue;
    }
    public void setPacketFactory(PacketFactory<? extends Packet> packetFactory) {
        this.packetFactory = packetFactory;
    }

    protected PacketFactory<? extends Packet> packetFactory;

    public Connection(SocketChannel channel, long createStamp){
        _channel = channel;
        try {
            host = channel.socket().getInetAddress().getHostAddress();
            port = channel.socket().getPort();
        } catch (Exception e) {
            logger.error("socket not connect", e);
        }
        _lastEvent = createStamp;
    }

    /**
     * when connection registed to ConnectionManager, {@link #init()} will invoked.
     * 
     * @see <code> {@link ConnectionManager#registerConnection(Connection, int)}</code>
     */
    protected void init() {
    	if(_outQueue.size()>0){
    		_selkey.interestOps(_selkey.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    public void setConnectionManager(ConnectionManager cmgr) {
        this._cmgr = cmgr;
    }

    /**
     * 设置与 SocketChannel 相关的 SelectionKey
     */
    public synchronized void setSelectionKey(SelectionKey selkey) {
        this._selkey = selkey;
    }

    /**
     * 返回与SocketChannel 相关的 Selection Key
     */
    public SelectionKey getSelectionKey() {
        return _selkey;
    }

    /**
     * 返回一个非阻塞的SocketChannel
     * 
     * @return
     */
    public SocketChannel getChannel() {
        return _channel;
    }

    public InetAddress getInetAddress() {
        return (_channel == null) ? null : _channel.socket().getInetAddress();
    }

    public void setMessageHandler(MessageHandler handler) {
        _handler = handler;
    }

    public MessageHandler getMessageHandler() {
        return _handler;
    }

    protected void inheritStreams(Connection other) {
        _fin = other._fin;
        _fout = other._fout;
    }

    /**
     * 判断 连接 是否关闭
     * 
     * @return
     */
    public synchronized boolean isClosed() {
        return socketClosed;

    }

    /**
     * 关闭当前连接，并且从ConnectionManager中删除该连接。
     */
    protected void close(Exception exception) {
            // we shouldn't be closed twice
        if (isClosed()) {
            return;
        }
        
        synchronized (this) {
        	if (isClosed()) {
        		return;
        	}
        	socketClosed = true;
		}
        
        if (_handler instanceof Sessionable) {
        	try{
	            Sessionable session = (Sessionable) _handler;
	            logger.error(this + ",closeSocket,and endSession,handler=" + session);
	            session.endSession(true, exception);
        	}catch(Exception e){
        		logger.warn("Error endSession [conn=" + toString() + "]",e);
        	}
        }
    
        try{
	        if (_selkey != null) {
	            _selkey.attach(null);
	            Selector selector = _selkey.selector();
	            _selkey.cancel();
	            // wake up again to trigger thread death
	            selector.wakeup();
	
	            _selkey = null;
	        }
        }catch(Exception e){
        	logger.warn("Error cancel connection selectkey [conn=" + toString() + "] error=" + e + "].",e);
        }
        
        if(logger.isDebugEnabled()){
        	logger.debug("Closing channel " + this + ".",exception);
        }
        try {
            _channel.close();
        } catch (IOException ioe) {
            logger.warn("Error closing connection ["+ toString() + "], error=" + ioe + "].");
        }
        try{
	        if (exception != null) {
	            _cmgr.connectionFailed(this, exception);
	        } else {
	            _cmgr.connectionClosed(this);
	        }
        }catch(Exception e){
        	logger.warn("notify ConnectionManager closing connection ["+ toString() + "], error=" + e + "].",e);
        }
    }

    /**
     * POST-->Queue->close->(_cmgr.connectionClosed( notify observer))-->closeSocket()
     * 可以提供给外界调用，这儿只是递交关闭该连接得请求。具体关闭将由Connection Manager处理。
     */
    public void postClose(Exception exception) {
        if (closePosted) {
            return;
        }
        postCloseLock.lock();
        try {
            if (closePosted) return;
            closePosted = true;
            this._cmgr.closeConnection(this, exception);
        } finally {
            postCloseLock.unlock();
        }
    }

    /**
     * 当连接处理数据或者遇到其他异常而需要关闭连接的情况下调用次方法。
     * 
     * @param ioe
     */
    public void handleFailure(Exception ioe) {
        // 如果已经关闭
        if (isClosed()) {
            logger.warn("Failure reported on closed connection " + this + ".", ioe);
            //reclose socket
            socketClosed = false;
            this.close(ioe);
            return;
        }
        postClose(ioe);
    }

    public int handleEvent(long when) {
        int bytesInTotal = 0;
        try {
            if (_fin == null) {
                _fin = createPacketInputStream();
            }
            byte[] msg = null;
            while (_channel != null && _channel.isOpen() && (msg = _fin.readPacket(_channel)) != null) {
                // 记录最后一次发生时间
                _lastEvent = when;
                bytesInTotal +=msg.length;
                doReceiveMessage(msg);
            }
        	messageProcess();
        } catch (EOFException eofe) {
            // close down the socket gracefully
    		messageProcess();
            handleFailure(eofe);
        } catch (IOException ioe) {
            // don't log a warning for the ever-popular "the client dropped the
            // connection" failure
            String msg = ioe.getMessage();

            if (msg == null || msg.indexOf("reset by peer") == -1) {
                logger.error("Error reading message from connection ["+ toString() + "], error=" + ioe + "].");
            }
            // deal with the failure
            handleFailure(ioe);
        } catch (Exception exception) {
        	logger.error("Error reading message from connection ["+ toString() + "], error=" + exception + "].");
            handleFailure(exception);
        }

        return bytesInTotal;
    }

    
    protected void doReceiveMessage(byte[] message){
    	_inQueue.append(message);
    }
    
    protected void messageProcess() {
		_handler.handleMessage(this);
    }

    public boolean doWrite() throws IOException {
        synchronized (writeLock) {
            ByteBuffer buffer = null;
            int wrote = 0;
            int message = 0;
            while ((buffer = _outQueue.getNonBlocking()) != null) {
                wrote += this.getChannel().write(buffer);
                if (buffer.remaining() > 0) {
                    _outQueue.prepend(buffer);
                    return false;
                } else {
                    // buffer.clear();
                    message++;
                }
            }
            return true;

        }
    }

    public void postMessage(byte[] msg) {
        PacketOutputStream _framer = getPacketOutputStream();
        ByteBuffer buffer = null;
		synchronized (_framer) {
	        _framer.resetPacket();
	        try {
	            _framer.write(msg);
	            buffer = _framer.returnPacketBuffer();
	        } catch (IOException e) {
	            this._cmgr.connectionFailed(this, e);
	            return;
	        }
		}
		
		if(buffer != null){
			postMessage(buffer);
		}
    }

    public void postMessage(ByteBuffer msg) {
        _outQueue.append(msg);
        writeMessage();
    }

    public int getInQueueSize(){
    	return _outQueue.size();
    }
    protected void writeMessage() {
        // 已关闭的连接时不可能再写了
        if (isClosed()) {
            handleFailure(new IOException("connection " + this +" has been closed"));
            return;
        }
        
        try {
            SelectionKey key = getSelectionKey();
            if (key!= null && !key.isValid()) {
                handleFailure(new java.nio.channels.CancelledKeyException());
                return;
            }
            if (key != null && (key.interestOps() & SelectionKey.OP_WRITE) == 0) {
                /**
                 * 发送数据，如果返回false，则表示socket send buffer 已经满了。则Selector 需要监听 Writeable event
                 */
                boolean finished = doWrite();
                
                if (!finished) {
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                }
            }else{
            	if(key == null){
            		if(logger.isDebugEnabled()){
            			logger.debug("writeMessage socketId="+this.getSocketId()+" hascode="+hashCode()+" but key="+key);
            		}
            	}
            }
        } catch (IOException ioe) {
            handleFailure(ioe);
        }catch(CancelledKeyException ce){
        	handleFailure(ce);
        }
    }
    
    public boolean checkIdle(long now) {
        long idleMillis = now - _lastEvent;
        if (idleMillis < PING_INTERVAL + LATENCY_GRACE) {
            return false;
        }
        if (isClosed()) {
            return true;
        }
        return true;
    }

    protected abstract PacketInputStream createPacketInputStream();

    protected abstract PacketOutputStream createPacketOutputStream();

    protected PacketOutputStream getPacketOutputStream() {
        if (_fout == null) {
            _fout = createPacketOutputStream();
        }
        return this._fout;

    }

    public ConnectionManager getConnectionManager(){
    	return this._cmgr;
    }
    
    protected PacketInputStream getPacketInputStream() {
        if (_fin == null) {
            _fin = createPacketInputStream();
        }
        return this._fin;

    }

    public String getSocketId(){
    	return this.host+":"+this.port;
    }
    
    public String toString(){
    	StringBuffer buffer = new StringBuffer();
    	buffer.append(this.getClass().getCanonicalName());
    	buffer.append("@").append(this.host).append(":").append(this.port);
    	buffer.append(",hashcode=").append(this.hashCode());
    	
    	return buffer.toString();
    	
    }
    
    public String cid() {
      return cid.toString();
    }

	public boolean isAdmin() {
		return isAdmin;
	}

	public void setIsAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
	
    public boolean isCloseable() {
      return closeable;
    }
  
    public void setCloseable(boolean closeable) {
      this.closeable = closeable;
    }
}
