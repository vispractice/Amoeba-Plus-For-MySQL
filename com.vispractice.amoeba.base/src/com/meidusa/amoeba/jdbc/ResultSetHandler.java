package com.meidusa.amoeba.jdbc;

import com.meidusa.amoeba.net.DatabaseConnection;

public interface ResultSetHandler {
	
	public boolean needHandle(int jdbcType);
	
	public <T> T serverToClient(DatabaseConnection conn, T object);
	
	public <T> T clientToServer(DatabaseConnection conn, T object);
}
