package com.meidusa.amoeba.mysql.net.packet;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.mysql.jdbc.MysqlDefs;
import com.meidusa.amoeba.util.StaticString;
import com.meidusa.amoeba.util.ThreadLocalMap;

public class PacketUtil {
	private static Logger logger = Logger.getLogger(PacketUtil.class);
	public static void resultToBindValue(BindValue bindValue,int columnIndex,ResultSet rs, FieldPacket fieldPacket) throws SQLException{
		switch (bindValue.bufferType & 0xff) {
		case MysqlDefs.FIELD_TYPE_TINY:
			bindValue.byteBinding = rs.getByte(columnIndex);
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_SHORT:
			bindValue.shortBinding =  rs.getShort(columnIndex);
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_LONG:
			bindValue.longBinding = rs.getLong(columnIndex);
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_LONGLONG:
			bindValue.longBinding = rs.getLong(columnIndex);
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_FLOAT:
			bindValue.floatBinding = rs.getFloat(columnIndex);
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_DOUBLE:
			bindValue.doubleBinding = rs.getDouble(columnIndex);
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_TIME:
			bindValue.value = rs.getTime(columnIndex);
			if(bindValue.value == null) {
				bindValue.isNull = true;
			}
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_DATE:
		case MysqlDefs.FIELD_TYPE_DATETIME:
		case MysqlDefs.FIELD_TYPE_TIMESTAMP:
			Timestamp time = rs.getTimestamp(columnIndex);
			bindValue.value = time;
			if(bindValue.value == null) {
				bindValue.isNull = true;
			}
			
			/**
			 * 
			 */
			if(time != null && (bindValue.bufferType & 0xff) == MysqlDefs.FIELD_TYPE_DATE){
				Calendar sessionCalendar = (Calendar)ThreadLocalMap.get(StaticString.CALENDAR);
				if(sessionCalendar == null){
					sessionCalendar = Calendar.getInstance();
					ThreadLocalMap.put(StaticString.CALENDAR, sessionCalendar);
				}
				sessionCalendar.setTime(time);
				byte hour =((byte) sessionCalendar.get(Calendar.HOUR_OF_DAY));
				byte minute = ((byte) sessionCalendar
						.get(Calendar.MINUTE));
				byte second = ((byte) sessionCalendar.get(Calendar.SECOND));
				
				if(hour>0 || minute>0||second>0){
					fieldPacket.type = MysqlDefs.FIELD_TYPE_TIMESTAMP;
				}
			}
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_VAR_STRING:
		case MysqlDefs.FIELD_TYPE_STRING:
		case MysqlDefs.FIELD_TYPE_VARCHAR:
			bindValue.value = rs.getString(columnIndex);
			if(bindValue.value == null){
				bindValue.isNull= true; 
			}
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_DECIMAL:
		case MysqlDefs.FIELD_TYPE_NEW_DECIMAL:
			bindValue.value = rs.getBigDecimal(columnIndex);
			if(bindValue.value == null) {
				bindValue.isNull = true;;
			}
			bindValue.isSet =true;
			return;
		default:{
			logger.error("error type=" + bindValue.bufferType);
		}
		}
	}
	
	/**
	 * read packetBuffer to stroe in BindValue
	 * @param packet
	 * @param bindValue
	 */
	public static void readBindValue(MysqlPacketBuffer packet, BindValue bindValue) {

		//
		// Handle primitives first
		//
		switch (bindValue.bufferType & 0xff) {

		case MysqlDefs.FIELD_TYPE_TINY:
			bindValue.byteBinding = packet.readByte();
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_SHORT:
			bindValue.shortBinding =  (short)packet.readInt();
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_LONG:
			bindValue.longBinding = packet.readLong();
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_LONGLONG:
			bindValue.longBinding = packet.readLongLong();
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_FLOAT:
			bindValue.floatBinding = packet.readFloat();
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_DOUBLE:
			bindValue.doubleBinding = packet.readDouble();
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_TIME:
			bindValue.value = readTime(packet);
			if(bindValue.value == null)bindValue.isNull = true;
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_DATE:
		case MysqlDefs.FIELD_TYPE_DATETIME:
		case MysqlDefs.FIELD_TYPE_TIMESTAMP:
			bindValue.value = readDate(packet);
			if(bindValue.value == null)bindValue.isNull = true;
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_VAR_STRING:
		case MysqlDefs.FIELD_TYPE_STRING:
		case MysqlDefs.FIELD_TYPE_VARCHAR:
			String charset = packet.getCharset();
			bindValue.value = packet.readLengthCodedString(charset);
			if(bindValue.value == null)bindValue.isNull = true;
			bindValue.isSet =true;
			return;
		case MysqlDefs.FIELD_TYPE_DECIMAL:
		case MysqlDefs.FIELD_TYPE_NEW_DECIMAL:
			charset = packet.getCharset();
			bindValue.value = new BigDecimal(packet.readLengthCodedString(charset));
			if(bindValue.value == null)bindValue.isNull = true;
			bindValue.isSet =true;
			return;
		default:{
			logger.error("error type=" + bindValue.bufferType);
		}
		}
	}
	
	/**
	 * Method storeBinding.
	 * 
	 * @param packet
	 * @param bindValue
	 * @param mysql
	 *            DOCUMENT ME!
	 * 
	 * @throws SQLException
	 *             DOCUMENT ME!
	 */
	public static void storeBinding(MysqlPacketBuffer packet, BindValue bindValue,String encoding){
			Object value = bindValue.value;

			//
			// Handle primitives first
			//
		switch (bindValue.bufferType & 0xff) {

			case MysqlDefs.FIELD_TYPE_TINY:
				packet.writeByte(bindValue.byteBinding);
				return;
			case MysqlDefs.FIELD_TYPE_SHORT:
				packet.writeInt(bindValue.shortBinding);
				return;
			case MysqlDefs.FIELD_TYPE_LONG:
				packet.writeLong(bindValue.longBinding);
				return;
			case MysqlDefs.FIELD_TYPE_LONGLONG:
				packet.writeLongLong(bindValue.longBinding);
				return;
			case MysqlDefs.FIELD_TYPE_FLOAT:
				packet.writeFloat(bindValue.floatBinding);
				return;
			case MysqlDefs.FIELD_TYPE_DOUBLE:
				packet.writeDouble(bindValue.doubleBinding);
				return;
			case MysqlDefs.FIELD_TYPE_TIME:
				storeTime(packet, (Time) value);
				return;
			case MysqlDefs.FIELD_TYPE_DATE:
			case MysqlDefs.FIELD_TYPE_DATETIME:
			case MysqlDefs.FIELD_TYPE_TIMESTAMP:
				storeDateTime(packet, (java.util.Date) value);
				return;
			case MysqlDefs.FIELD_TYPE_VAR_STRING:
			case MysqlDefs.FIELD_TYPE_STRING:
			case MysqlDefs.FIELD_TYPE_VARCHAR:{
				if (value instanceof byte[]) {
					packet.writeLenBytes((byte[]) value);
				}else{
					packet.writeLengthCodedString((String)value,encoding);
				}
				return;
			}
			case MysqlDefs.FIELD_TYPE_DECIMAL:
			case MysqlDefs.FIELD_TYPE_NEW_DECIMAL:{
				if (value instanceof byte[]) {
					packet.writeLenBytes((byte[]) value);
				}else{
					packet.writeLengthCodedString(value != null?value.toString():null,encoding);
				}
				return;
			}
			default:{
				logger.error("error type=" + bindValue.bufferType);
			}
		}
	}
			
	
	public static void storeDateTime(MysqlPacketBuffer intoBuf, Date dt) {
		if(dt == null){
			intoBuf.writeByte((byte)0);
			return;
		}
		Calendar sessionCalendar = (Calendar)ThreadLocalMap.get(StaticString.CALENDAR);
		if(sessionCalendar == null){
			sessionCalendar = Calendar.getInstance();
			ThreadLocalMap.put(StaticString.CALENDAR, sessionCalendar);
		}
		
		java.util.Date oldTime = sessionCalendar.getTime();
		try {
			sessionCalendar.setTime(dt);
			
			if (dt instanceof java.sql.Date) {
				sessionCalendar.set(Calendar.HOUR_OF_DAY, 0);
				sessionCalendar.set(Calendar.MINUTE, 0);
				sessionCalendar.set(Calendar.SECOND, 0);
			}

			byte length = (byte) 7;

			if (dt instanceof java.sql.Timestamp) {
				length = (byte) 11;
			}

			intoBuf.writeByte(length); // length

			int year = sessionCalendar.get(Calendar.YEAR);
			int month = sessionCalendar.get(Calendar.MONTH) + 1;
			int date = sessionCalendar.get(Calendar.DAY_OF_MONTH);
			
			intoBuf.writeInt(year);
			intoBuf.writeByte((byte) month);
			intoBuf.writeByte((byte) date);

			if (dt instanceof java.sql.Date) {
				intoBuf.writeByte((byte) 0);
				intoBuf.writeByte((byte) 0);
				intoBuf.writeByte((byte) 0);
			} else {
				intoBuf.writeByte((byte) sessionCalendar
						.get(Calendar.HOUR_OF_DAY));
				intoBuf.writeByte((byte) sessionCalendar
						.get(Calendar.MINUTE));
				intoBuf.writeByte((byte) sessionCalendar
						.get(Calendar.SECOND));
			}

			if (length == 11) {
				intoBuf.writeLong(((java.sql.Timestamp) dt).getNanos());
			}
		
		} finally {
			sessionCalendar.setTime(oldTime);
		}
	}

	public static void storeTime(MysqlPacketBuffer intoBuf, Time tm){
		if(tm == null){
			intoBuf.writeByte((byte)0);
			return;
		}
		intoBuf.writeByte((byte) 8); // length
		intoBuf.writeByte((byte) 0); // neg flag
		intoBuf.writeLong(0); // tm->day, not used

		Calendar cal = (Calendar)ThreadLocalMap.get(StaticString.CALENDAR);
		
		synchronized (cal) {
			cal.setTime(tm);
			intoBuf.writeByte((byte) cal.get(Calendar.HOUR_OF_DAY));
			intoBuf.writeByte((byte) cal.get(Calendar.MINUTE));
			intoBuf.writeByte((byte) cal.get(Calendar.SECOND));

			// intoBuf.writeLongInt(0); // tm-second_part
		}
	}
	
	public static Time readTime(MysqlPacketBuffer intoBuf){
		intoBuf.readByte();
		intoBuf.readByte();
		intoBuf.readLong();
		int hour = intoBuf.readByte();
		int minute = intoBuf.readByte();
		int second = intoBuf.readByte();
		Calendar cal = (Calendar)ThreadLocalMap.get(StaticString.CALENDAR);
		cal.set(0, 0, 0, hour, minute, second);
		return new Time(cal.getTimeInMillis());
	}
	
	public static Date readDate(MysqlPacketBuffer intoBuf){
		byte length = intoBuf.readByte(); // length
		int year = intoBuf.readInt();
		byte month = (byte)(intoBuf.readByte()-1); //数据包中月份范围是1-12，而 java对象中月份是0-11
		byte date = intoBuf.readByte();
		int hour = intoBuf.readByte();
		int minute = intoBuf.readByte();
		int second = intoBuf.readByte();
		if (length == 11) {
			long nanos = intoBuf.readLong();
			Calendar cal = (Calendar)ThreadLocalMap.get(StaticString.CALENDAR);
			if (cal == null) {
                cal = Calendar.getInstance();
                ThreadLocalMap.put(StaticString.CALENDAR, cal);
            }
			cal.set(year, month, date, hour, minute, second);
			Timestamp time = new Timestamp(cal.getTimeInMillis());
			time.setNanos((int)nanos);
			return time;
		}else{
			Calendar cal = Calendar.getInstance();
			cal.set(year, month, date, hour, minute, second);
			return cal.getTime();
		}
	}
	
}
