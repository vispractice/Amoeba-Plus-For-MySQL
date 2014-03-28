package com.meidusa.amoeba.net;

import java.io.IOException;

/**
 * 作为前端数据库连接工厂
 * @author struct
 *
 */
public abstract class FrontendConnectionFactory extends AbstractConnectionFactory {
	
	protected void initConnection(Connection connection) throws IOException{
		super.initConnection(connection);
	}
	

}
