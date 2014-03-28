package com.meidusa.amoeba.monitor;

public interface MonitorConstant {
	String APPLICATION_NAME = ".Amoeba";
    int    HEADER_SIZE               = 5;
    
    byte   FUN_TYPE_OBJECT           = 1;
    byte   FUN_TYPE_PING             = 2;
    byte   FUN_TYPE_OK               = 3;

    byte   FUN_TYPE_DBSERVER_ADD     = 4;
    byte   FUN_TYPE_DBSERVER_DELETE  = 5;
    byte   FUN_TYPE_DBSERVER_UPDATE  = 6;
    byte   FUN_TYPE_DBSERVER_DSIABLE = 7;
    byte   FUN_TYPE_DBSERVER_ENABLE  = 8;
    
    byte   FUN_TYPE_RULE_UPDATE      = 9;
    byte   FUN_TYPE_RULE_ADD         = 10;
    byte   FUN_TYPE_RULE_DELETE      = 11;
    byte   FUN_TYPE_AMOEBA_RELOAD    = 21;
    byte   FUN_TYPE_AMOEBA_SHUTDOWN  = 22;

    byte[] HEADER_PAD                = new byte[HEADER_SIZE];
}
