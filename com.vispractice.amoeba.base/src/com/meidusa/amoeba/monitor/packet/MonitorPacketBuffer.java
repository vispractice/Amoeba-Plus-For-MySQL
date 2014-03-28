package com.meidusa.amoeba.monitor.packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.meidusa.amoeba.net.packet.AbstractPacketBuffer;

/**
 * 
 * @author struct
 *
 */
public class MonitorPacketBuffer extends AbstractPacketBuffer {
	
	public MonitorPacketBuffer(int size) {
		super(size);
	}
	
	public int readInt() {
		byte[] b = this.buffer; // a little bit optimization
		return 	   (b[this.position++] & 0xff) 
				| ((b[this.position++] & 0xff) << 8)
				| ((b[this.position++] & 0xff) << 16)
				| ((b[this.position++] & 0xff) << 24);
	}

	public void writeInt(int x) {
		ensureCapacity(4);
		writeByte((byte) (0xFF & x ));
		writeByte((byte) (0xFF & (x >> 8)));
		writeByte((byte) (0xFF & (x >> 16)));
		writeByte((byte) (0xFF & (x >> 24)));
	}

	public void writeInt(int pos, int x) {
		final int save = getPosition();
		setPosition(pos);
		writeInt(x);
		setPosition(save);
	}

	public long readLong() {
		byte[] b = this.buffer; // a little bit optimization
		return (b[this.position++] & 0xffL)
				| ((b[this.position++] & 0xffL) << 8)
				| ((b[this.position++] & 0xffL) << 16)
				| ((b[this.position++] & 0xffL) << 24)
				| ((b[this.position++] & 0xffL) << 32)
				| ((b[this.position++] & 0xffL) << 40)
				| ((b[this.position++] & 0xffL) << 48)
				| ((b[this.position++] & 0xffL) << 56);
	}

	public void writeLong(long x) {
		ensureCapacity(8);
		writeByte((byte) (0xFFL & (x >> 0)));
		writeByte((byte) (0xFFL & (x >> 8)));
		writeByte((byte) (0xFFL & (x >> 16)));
		writeByte((byte) (0xFFL & (x >> 24)));
		writeByte((byte) (0xFFL & (x >> 32)));
		writeByte((byte) (0xFFL & (x >> 40)));
		writeByte((byte) (0xFFL & (x >> 48)));
		writeByte((byte) (0xFFL & (x >> 56)));
	}
	
	
	public MonitorPacketBuffer(byte[] buf) {
		super(buf);
	}
	
	public Object readObject(){
		try {
			ObjectInputStream ois = new ObjectInputStream(this.asInputStream());
			return ois.readObject();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void writeObject(Object object){
		try {
			ObjectOutputStream oos = new ObjectOutputStream(this.asOutputStream());
			oos.writeObject(object);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
