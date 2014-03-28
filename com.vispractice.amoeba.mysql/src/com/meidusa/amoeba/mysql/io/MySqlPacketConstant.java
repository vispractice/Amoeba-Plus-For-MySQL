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
package com.meidusa.amoeba.mysql.io;

/**
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public interface MySqlPacketConstant {
	public static final int HEADER_SIZE = 4;
	public static final byte[] HEADER_PAD = new byte[HEADER_SIZE];
	public static final int AUTH_411_OVERHEAD = 33;
	
	/** latin1 charset */
	public static final String CODE_PAGE_1252 = "Cp1252";
	
	
	public static final int    CLIENT_LONG_PASSWORD	=1;	/* new more secure passwords */
	public static final int    CLIENT_FOUND_ROWS	=2;	/* Found instead of affected rows */
	public static final int    CLIENT_LONG_FLAG	=4;	/* Get all column flags */
	public static final int    CLIENT_CONNECT_WITH_DB	=8;	/* One can specify db on connect */
	public static final int    CLIENT_NO_SCHEMA	=16;	/* Don't allow database.table.column */
	public static final int    CLIENT_COMPRESS		=32;	/* Can use compression protocol */
	public static final int    CLIENT_ODBC		=64;	/* Odbc client */
	public static final int    CLIENT_LOCAL_FILES	=128;	/* Can use LOAD DATA LOCAL */
	public static final int    CLIENT_IGNORE_SPACE	=256;	/* Ignore spaces before '(' */
	public static final int    CLIENT_PROTOCOL_41	=512;	/* New 4.1 protocol */
	public static final int    CLIENT_INTERACTIVE	=1024;	/* This is an interactive client */
	public static final int    CLIENT_SSL              =2048;	/* Switch to SSL after handshake */
	public static final int    CLIENT_IGNORE_SIGPIPE   =4096;    /* IGNORE sigpipes */
	public static final int    CLIENT_TRANSACTIONS	=8192;	/* Client knows about transactions */
	public static final int    CLIENT_RESERVED         =16384;   /* Old flag for 4.1 protocol  */
	public static final int    CLIENT_SECURE_CONNECTION =32768;  /* New 4.1 authentication */
	public static final int    CLIENT_MULTI_STATEMENTS =65536;   /* Enable/disable multi-stmt support */
	public static final int    CLIENT_MULTI_RESULTS    =131072;  /* Enable/disable multi-results */
}
