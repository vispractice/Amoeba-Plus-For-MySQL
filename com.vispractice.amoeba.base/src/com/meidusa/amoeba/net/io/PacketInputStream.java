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
package com.meidusa.amoeba.net.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import com.meidusa.amoeba.util.StringUtil;

/**
 * 
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public abstract class PacketInputStream {
	
	/**
     * 用于读取可读通道的缓存对象，初始化容量={@link #INITIAL_BUFFER_CAPACITY}，不够时候每次增长上一次的一倍
     */
    protected ByteBuffer _buffer;

    /** 整个封包长度，包括包头 */
    protected int _length = -1;

    /**
     * 当前buffer 中的字节长度
     */
    protected int _have = 0;

    /**
     * 初始化buffer 容量,默认32字节
     */
    protected static final int INITIAL_BUFFER_CAPACITY = 32;

    /** 最大容量 */
    protected static final int MAX_BUFFER_CAPACITY = 1024 * 1024 * 2;
    private int maxPacketSize = MAX_BUFFER_CAPACITY;
    
    
    public int getMaxPacketSize() {
		return maxPacketSize;
	}

	public void setMaxPacketSize(int maxPacketSize) {
		this.maxPacketSize = maxPacketSize;
	}
	private byte[] tmp = new byte[4096]; 
    /**
     * Creates a new framed input stream.
     */
    public PacketInputStream ()
    {
        _buffer = ByteBuffer.allocate(INITIAL_BUFFER_CAPACITY);
    }

    /**
     * Reads a packet from the provided channel, appending to any partially
     * read packet. If the entire packet data is not yet available,
     * <code>readPacket</code> will return false, otherwise true.
     *
     * <p> <em>Note:</em> when this method returns true, it is required
     * that the caller read <em>all</em> of the packet data from the stream
     * before again calling {@link #readPacket} as the previous packet's
     * data will be elimitated upon the subsequent call.
     *
     * @return byte[] if the entire packet has been read, null if the buffer contains only a partial packet. 
     */
    public byte[] readPacket(ReadableByteChannel source)
        throws IOException
    {
        if (checkForCompletePacket()) {
            return readPacket();
        }

        // read whatever data we can from the source
        do {
            int got = source.read(_buffer);
            if (got == -1) {
                throw new EOFException();
            }
            if(got == 0){
            	return null;
            }
            _have += got;

            if (_length == -1) {
                // if we didn't already have our length, see if we now
                // have enough data to obtain it
                _length = decodeLength();
            }
            
            if(_length < -1){
            	_buffer.flip();
            	byte[] bts = _buffer.array();
            	throw new IOException("decodeLength error:_length="+_length+"\r\n"+StringUtil.dumpAsHex(bts, bts.length));
            }

            // if there's room remaining in the buffer, that means we've
            // read all there is to read, so we can move on to inspecting
            // what we've got
            if (_buffer.remaining() > 0) {
                break;
            }

            // additionally, if the buffer happened to be exactly as long
            // as we needed, we need to break as well
            if ((_length > 0) && (_have >= _length)) {
                break;
            }

            // otherwise, we've filled up our buffer as a result of this
            // read, expand it and try reading some more
            int newSize = _buffer.capacity() << 1;
            newSize = newSize>_length ? newSize:_length+16;
            if(newSize > maxPacketSize){
            	throw new IOException("packet over MAX_BUFFER_CAPACITY size="+newSize);
            }
            ByteBuffer newbuf = ByteBuffer.allocate(newSize);
            newbuf.put((ByteBuffer)_buffer.flip());
            _buffer = newbuf;

            // don't let things grow without bounds
        } while (_buffer.capacity() < maxPacketSize);

        if (checkForCompletePacket()) {
            return readPacket();
        }else{
        	return null;
        }
    }

    protected abstract byte[] readPacket();
    /**
     * only for bio
     * @param source
     * @return
     * @throws IOException
     */
    public byte[] readPacket (InputStream source)
    throws IOException
	{
	    // we may already have the next frame entirely in the buffer from
	    // a previous read
	    if (checkForCompletePacket()) {
	    	return readPacket();
	    }
	   
	    // read whatever data we can from the source
	    do {
	    	int got = source.read(tmp);
	    	expandCapacity(got);
	    	if(got ==0) return null;
	    	if(got >0){
	    		
	    		//got = source.read(byt);
	    		this._buffer.put(tmp, 0, got);
	    	}
	    	
	        if (got == -1) {
	            throw new EOFException();
	        }
	        _have += got;
	        if (_length == -1) {
	            // if we didn't already have our length, see if we now
	            // have enough data to obtain it
	            _length = decodeLength();
	        }
	        if(_length<-1 || _length > MAX_BUFFER_CAPACITY){
	        	throw new IOException("over max packet limit");
	        }
	        // don't let things grow without bounds
	    } while (_buffer.capacity() < MAX_BUFFER_CAPACITY &&  !checkForCompletePacket());
	
	    if (checkForCompletePacket()) {
            return readPacket();
        }else{
        	return null;
        }
	    
	}
    
    private void expandCapacity(int needSize){
    	if(_buffer.remaining()<needSize){
    		int newSize = _buffer.capacity() << 1;
            newSize = newSize>_length ? newSize:_length+16;
            ByteBuffer newbuf = ByteBuffer.allocate(newSize>needSize?newSize:needSize);
            newbuf.put((ByteBuffer)_buffer.flip());
            _buffer = newbuf;
    	}
    }
    /**
     * Decodes and returns the length of the current packet from the buffer
     * if possible. Returns -1 otherwise.
     */
    protected abstract int decodeLength ();

    /**
     * Returns true if a complete frame is in the buffer, false otherwise.
     * If a complete packet is in the buffer, the buffer will be prepared
     * to deliver that frame via our {@link InputStream} interface.
     */
    protected boolean checkForCompletePacket ()
    {
        if (_length <= -1 || _have < _length || _length < getHeaderSize()) {
            return false;
        }
        return true;
    }


    public String toString(){
    	StringBuffer buffer = new StringBuffer();
    	buffer.append("buffer:").append(_buffer).append(",length:").append(_length).append(",have:").append(_have);
    	return buffer.toString();
    }
    /**
     *  
     * @return packet header size
     */
    public abstract int getHeaderSize();
    
}
