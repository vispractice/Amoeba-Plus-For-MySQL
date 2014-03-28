package com.meidusa.amoeba.monitor.packet;

import java.io.UnsupportedEncodingException;

import com.meidusa.amoeba.monitor.MonitorConstant;
import com.meidusa.amoeba.net.packet.AbstractPacket;

/**
 * @author struct
 */
public class MonitorCommandPacket extends AbstractPacket<MonitorPacketBuffer> implements MonitorConstant {

    public int  lenght;
    public byte funType;
    public Object[] objects;
    
    @Override
    protected void init(MonitorPacketBuffer buffer) {
        buffer.setPosition(0);
        lenght = buffer.readInt();
        funType = buffer.readByte();
        if(funType<100 ){
        	
        }else{
        	//
        	return ;
        }
        int count = buffer.readInt();
        objects = new Object[count];
        for(int i=0;i<count;i++){
        	objects[i] = buffer.readObject();
        }
    }

    @Override
    protected void write2Buffer(MonitorPacketBuffer buffer) throws UnsupportedEncodingException {
        buffer.setPosition(HEADER_SIZE);
        
        if(objects != null && objects.length >0){
        	buffer.writeInt(objects.length);
        	for(Object object : objects){
        		buffer.writeObject(object);
        	}
        }else{
        	buffer.writeInt(0);
        }
    }

    @Override
    protected void afterPacketWritten(MonitorPacketBuffer buffer) {
        int position = buffer.getPosition();
        lenght = position;
        buffer.setPosition(0);
        buffer.writeInt(lenght);
        buffer.writeByte((byte) funType);
        buffer.setPosition(position);
    }

    @Override
    protected int calculatePacketSize() {
        return 12;
    }

	@Override
	protected Class<MonitorPacketBuffer> getPacketBufferClass() {
		return MonitorPacketBuffer.class;
	}
}
