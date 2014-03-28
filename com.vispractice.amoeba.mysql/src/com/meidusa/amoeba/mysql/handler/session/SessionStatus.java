package com.meidusa.amoeba.mysql.handler.session;

/**
 * 表示服务器返回的数据包所表示当前会话状态
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public class SessionStatus{
	public static final int QUERY = 1;
	public static final int RESULT_HEAD  = 2;
	public static final int EOF_FIELDS  = 4;
	public static final int EOF_ROWS  = 8;
	public static final int OK  = 16;
	public static final int ERROR  = 32;
	public static final int COMPLETED  = 64;
}