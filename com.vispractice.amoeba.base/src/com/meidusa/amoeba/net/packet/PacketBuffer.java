package com.meidusa.amoeba.net.packet;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * 
 * @author struct
 *
 */
public interface PacketBuffer {
	
	/**
	 * 
	 * @return
	 */
	public ByteBuffer toByteBuffer();
	
	public byte readByte();

	public byte readByte(int postion);
	
	public int readBytes(byte[] ab, int offset, int len);
	
	public void reset();
	/**
	 * current remain， but the buffer can expands itself. when the large bytes written;
	 * @return
	 */
	int remaining();
	/**
	 * 
	 * @param bte
	 */
	public void writeByte(byte bte);
	
	/**
	 * 
	 * @param btes
	 */
	public int writeBytes(byte[] btes);
	
	/**
	 * 
	 * @return
	 */
	public int getPacketLength();
	
	
	/**
	 * 
	 * @return
	 */
	public int getPosition();
	
	/**
	 * Set the current position to write to/ read from
	 * 
	 * @param position
	 *            the position (0-based index)
	 */
	public void setPosition(int positionToSet);

	public InputStream asInputStream();

	public OutputStream asOutputStream();
	
}
