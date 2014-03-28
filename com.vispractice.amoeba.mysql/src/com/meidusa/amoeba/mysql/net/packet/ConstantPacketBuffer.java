package com.meidusa.amoeba.mysql.net.packet;

import com.meidusa.amoeba.mysql.net.packet.OkPacket;

public class ConstantPacketBuffer {
  public static byte[]   STATIC_OK_BUFFER;
  static {
      OkPacket ok = new OkPacket();
      ok.affectedRows = 0;
      ok.insertId = 0;
      ok.packetId = 1;
      ok.serverStatus = 2;
      STATIC_OK_BUFFER = ok.toByteBuffer(null).array();
  }
  
  public static final String _READ_UNCOMMITTED = "SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED";
  
  public static final String _READ_COMMITTED = "SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED";
  
  public static final String _REPEATED_READ = "SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ";
  
  public static final String _SERIALIZABLE = "SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE";
}
