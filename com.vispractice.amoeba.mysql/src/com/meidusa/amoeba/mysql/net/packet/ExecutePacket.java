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
package com.meidusa.amoeba.mysql.net.packet;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.mysql.handler.PreparedStatmentInfo;
import com.meidusa.amoeba.mysql.jdbc.MysqlDefs;
import com.meidusa.amoeba.net.packet.AbstractPacketBuffer;
import com.meidusa.amoeba.util.StringFillFormat;

/**
 * <pre>
 * Bytes                Name
 *  -----                ----
 *  1                    code
 *  4                    statement_id
 *  1                    flags
 *  4                    iteration_count
 *  (param_count+7)/8    null_bit_map
 *  1                    new_parameter_bound_flag 如果为1表示preparedStatment有参数绑定，否则则为0
 *  n*2                  type of parameters (only if new_params_bound = 1)
 *  
 *  code:          always COM_EXECUTE
 *  
 *  statement_id:  statement identifier
 *  
 *  flags:         reserved for future use. In MySQL 4.0, always 0.
 *                 In MySQL 5.0: 
 *                   0: CURSOR_TYPE_NO_CURSOR
 *                   1: CURSOR_TYPE_READ_ONLY
 *                   2: CURSOR_TYPE_FOR_UPDATE
 *                   4: CURSOR_TYPE_SCROLLABLE
 *  
 *  iteration_count: reserved for future use. Currently always 1.
 *  
 *  null_bit_map:  A bitmap indicating parameters that are NULL.
 *                 Bits are counted from LSB, using as many bytes
 *                 as necessary ((param_count+7)/8)
 *                 i.e. if the first parameter (parameter 0) is NULL, then
 *                 the least significant bit in the first byte will be 1.
 *  
 *  new_parameter_bound_flag:   Contains 1 if this is the first time
 *                              that &quot;execute&quot; has been called, or if
 *                              the parameters have been rebound.
 *  
 *  type:          Occurs once for each parameter that is not NULL.
 *                 The highest significant bit of this 16-bit value
 *                 encodes the unsigned property. The other 15 bits
 *                 are reserved for the type (only 8 currently used).
 *                 This block is sent when parameters have been rebound
 *                 or when a prepared statement is executed for the 
 *                 first time.
 * </pre>
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 * @author hexianmao
 */
public class ExecutePacket extends CommandPacket {

    private static Logger                    logger = Logger.getLogger(ExecutePacket.class);

    public long                              statementId;
    public byte                              flags;
    public long                              iterationCount;
    public byte                              newParameterBoundFlag;
    public BindValue[]                       values;

    protected transient int                  parameterCount;
    protected transient PreparedStatmentInfo pInfo;

    private Map<Integer, Object>             longPrameters;

    public ExecutePacket(PreparedStatmentInfo pInfo, Map<Integer, Object> longPrameters){
        this.pInfo = pInfo;
        this.parameterCount = pInfo.getParameterCount();
        this.values = new BindValue[parameterCount];
        this.longPrameters = longPrameters;
    }

    public static long readStatmentID(byte[] buffer) {
        byte[] b = buffer;
        int position = 5;
        return ((long) b[position++] & 0xff) | (((long) b[position++] & 0xff) << 8) | ((long) (b[position++] & 0xff) << 16) | ((long) (b[position++] & 0xff) << 24);
    }

    @Override
    public void init(AbstractPacketBuffer myBuffer) {
        super.init(myBuffer);
        MysqlPacketBuffer buffer = (MysqlPacketBuffer) myBuffer;
        statementId = buffer.readLong();
        flags = buffer.readByte();
        iterationCount = buffer.readLong();
        
        if(parameterCount >0){
	        int nullCount = (this.parameterCount + 7) / 8;
	        byte[] nullBitsBuffer = new byte[nullCount];
	        for (int i = 0; i < nullCount; i++) {
	            nullBitsBuffer[i] = buffer.readByte();
	        }
        
	        newParameterBoundFlag = buffer.readByte();
	
	        for (int i = 0; i < this.parameterCount; i++) {
	            if (values[i] == null) {
	                values[i] = new BindValue();
	            }
	        }
	
	        // 当newParameterBoundFlag=1时，更新pInfo.parameterTypes值
	        // 否则使用pInfo.parameterTypes作为values[i].bufferType的值
	        if (newParameterBoundFlag == (byte) 1) {
	            int[] bindTypes = new int[parameterCount];
	            for (int i = 0; i < parameterCount; i++) {
	                bindTypes[i] = buffer.readInt();
	                values[i].bufferType = bindTypes[i];
	            }
	            pInfo.setParameterTypes(bindTypes);
	        } else {
	            int[] bindTypes = pInfo.getParameterTypes();
	            for (int i = 0; i < parameterCount; i++) {
	                values[i].bufferType = bindTypes[i];
	            }
	        }
	
	        for (int i = 0; i < this.parameterCount; i++) {
	            if (longPrameters != null && longPrameters.get(i) != null) {
	                values[i].isLongData = true;
	                values[i].value = longPrameters.get(i);
	            } else {
	                if ((nullBitsBuffer[i / 8] & (1 << (i & 7))) != 0) {
	                    values[i].isNull = true;
	                } else {
	                    PacketUtil.readBindValue(buffer, values[i]);
	                }
	            }
	        }
        }
    }

    @Override
    public void write2Buffer(AbstractPacketBuffer myBuffer) throws UnsupportedEncodingException {
        super.write2Buffer(myBuffer);
        MysqlPacketBuffer buffer = (MysqlPacketBuffer) myBuffer;
        buffer.writeLong(statementId);
        buffer.writeByte(flags);
        buffer.writeLong(iterationCount);
        buffer.writeByte(newParameterBoundFlag);
        int nullCount = (this.parameterCount + 7) / 8;

        int nullBitsPosition = buffer.getPosition();

        for (int i = 0; i < nullCount; i++) {
            buffer.writeByte((byte) 0);
        }
        byte[] nullBitsBuffer = new byte[nullCount];

        if (newParameterBoundFlag == (byte) 1) {
            for (int i = 0; i < this.parameterCount; i++) {
                buffer.writeInt(this.values[i].bufferType);
            }
        }

        for (int i = 0; i < this.parameterCount; i++) {
            if (!this.values[i].isLongData) {
                if (!this.values[i].isNull) {
                    PacketUtil.storeBinding(buffer, this.values[i], buffer.getCharset());
                } else {
                    nullBitsBuffer[i / 8] |= (1 << (i & 7));
                }
            }
        }

        int endPosition = buffer.getPosition();
        buffer.setPosition(nullBitsPosition);
        buffer.writeBytesNoNull(nullBitsBuffer);
        buffer.setPosition(endPosition);
    }

    public Object[] getParameters() {
        Object[] result = new Object[values.length];
        int index = 0;
        for (BindValue bindValue : values) {
            if (bindValue.isNull) {
                index++;
            } else {
                switch (bindValue.bufferType) {
                    case MysqlDefs.FIELD_TYPE_TINY:
                        result[index++] = bindValue.byteBinding;
                        break;
                    case MysqlDefs.FIELD_TYPE_SHORT:
                        result[index++] = bindValue.shortBinding;
                        break;
                    case MysqlDefs.FIELD_TYPE_LONG:
                        result[index++] = bindValue.longBinding;
                        break;
                    case MysqlDefs.FIELD_TYPE_LONGLONG:
                        result[index++] = bindValue.longBinding;
                        break;
                    case MysqlDefs.FIELD_TYPE_FLOAT:
                        result[index++] = bindValue.floatBinding;
                        break;
                    case MysqlDefs.FIELD_TYPE_DOUBLE:
                        result[index++] = bindValue.doubleBinding;
                        break;
                    case MysqlDefs.FIELD_TYPE_TIME:
                        result[index++] = bindValue.value;
                        break;
                    case MysqlDefs.FIELD_TYPE_DATE:
                    case MysqlDefs.FIELD_TYPE_DATETIME:
                    case MysqlDefs.FIELD_TYPE_TIMESTAMP:
                        result[index++] = bindValue.value;
                        break;
                    case MysqlDefs.FIELD_TYPE_VAR_STRING:
                    case MysqlDefs.FIELD_TYPE_STRING:
                    case MysqlDefs.FIELD_TYPE_VARCHAR:
                    case MysqlDefs.FIELD_TYPE_DECIMAL:
                    case MysqlDefs.FIELD_TYPE_NEW_DECIMAL:
                    case MysqlDefs.FIELD_TYPE_TINY_BLOB:
                    case MysqlDefs.FIELD_TYPE_BLOB:
                        result[index++] = bindValue.value;
                        break;
                    case MysqlDefs.FIELD_TYPE_NULL:
                        index++;
                        break;
                    default: {
                        index++;
                        logger.error("error jdbc type:" + bindValue.bufferType);
                    }
                }
            }
        }
        return result;
    }

    protected int calculatePacketSize() {
        int packLength = super.calculatePacketSize();
        packLength += 10;// 4 + 1 + 4 + 1;
        return packLength;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("Length=").append(StringFillFormat.format(packetLength, 4));
        s.append(", PacketId=").append(StringFillFormat.format(packetId, 2));
        s.append(", Command=").append(StringFillFormat.format(command, 2));
        s.append(", StatementId=").append(StringFillFormat.format(statementId, 2));
        return s.toString();
    }

    public static void main(String[] args) {
        int parameterCount = 12;
        int nullCount = (parameterCount + 7) / 8;
        byte[] nullBitsBuffer = new byte[nullCount];

        for (int i = 0; i < parameterCount; i++) {
            nullBitsBuffer[i / 8] |= (1 << (i & 7));
        }
        System.out.println(Arrays.toString(nullBitsBuffer));
    }

}
