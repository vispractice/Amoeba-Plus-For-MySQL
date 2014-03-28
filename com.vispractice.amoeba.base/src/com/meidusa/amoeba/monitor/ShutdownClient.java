package com.meidusa.amoeba.monitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import com.meidusa.amoeba.config.ConfigUtil;
import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.monitor.io.MonitorPacketInputStream;
import com.meidusa.amoeba.monitor.packet.MonitorCommandPacket;
import com.meidusa.amoeba.net.io.PacketInputStream;
import com.meidusa.amoeba.util.StringUtil;

public class ShutdownClient implements MonitorConstant {
	private File socketInfoFile;
	private String appplicationName;
	private int port;
	private String host;
	public ShutdownClient(String appplicationName) {
		this.appplicationName = appplicationName;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * 
	 * @param command
	 * @return false if server not running
	 */
	public boolean run(MonitorCommandPacket command) {
		if(port <=0){
			socketInfoFile = new File(ProxyRuntimeContext.getInstance().getAmoebaHomePath(),appplicationName+".shutdown.port");
			if(!socketInfoFile.exists()){
				return false;
			}
			
			try {
				BufferedReader reader = new BufferedReader(new FileReader(socketInfoFile));
				String sport = reader.readLine();
				String tmp[] = StringUtil.split(sport, ":");
				if(tmp.length <=1){
					return false;
				}
				this.port = Integer.parseInt(tmp[1]);
				this.host = tmp[0];
				reader.close();
			}catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		
		try {
			Socket socket = null;
			try{
				if(host == null){
					socket = new Socket(InetAddress.getLocalHost(),port);
				}else{
					socket = new Socket(host, port);
				}
			}catch(IOException e){
				return false;
			}
			socket.getOutputStream().write(command.toByteBuffer(null).array());
			socket.getOutputStream().flush();
			PacketInputStream pis = new MonitorPacketInputStream();
			
			byte[] message = pis.readPacket(socket.getInputStream());
			MonitorCommandPacket response = new MonitorCommandPacket();
			response.init(message, null);
			if(response.funType == MonitorConstant.FUN_TYPE_OK){
				System.out.println("remote application= "+ appplicationName+":"+port+" response OK");
			}
			
			socket.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File socketId = new File(ProxyRuntimeContext.getInstance().getAmoebaHomePath(),"amoeba.shutdown.port");
		ShutdownClient client = new ShutdownClient(socketId.getAbsolutePath());
	}

}
