package com.meidusa.amoeba.monitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.Random;

import org.apache.log4j.Level;

import com.meidusa.amoeba.config.ConfigUtil;
import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.exception.InitialisationException;
import com.meidusa.amoeba.net.ServerableConnectionManager;

public class MonitorServer extends ServerableConnectionManager{

	public MonitorServer() throws IOException {
		super();
	}

	protected File socketInfoFile;
	protected String appplicationName = MonitorConstant.APPLICATION_NAME;
	public void init() throws InitialisationException {
		super.init();
		socketInfoFile = new File(ProxyRuntimeContext.getInstance().getAmoebaHomePath(),appplicationName+".shutdown.port");
	}
	
	protected void initServerSocket(){
		Random random = new Random();
        try {
            // create a listening socket and add it to the select set
            ssocket = ServerSocketChannel.open();
            ssocket.configureBlocking(false);
            InetSocketAddress isa = null;
            int times = 0;
    		do{
	    		try {
	    			if(port <=0){
	    				port = random.nextInt(65535);
	    			}

	    			if (ipAddress != null) {
		                isa = new InetSocketAddress(ipAddress, port);
		            } else {
		                isa = new InetSocketAddress(port);
		            }
		
		            ssocket.socket().bind(isa);
		            break;
	    		} catch (IOException e) {
		    			if(times >100){
		    				System.out.println("cannot create shutdownServer socket,System exit now!");
		    				e.printStackTrace();
		    				System.exit(-1);
		    			}
		    	}
    		}while(true);
            
            
            registerServerChannel(ssocket);

            Level level = log.getLevel();
            log.setLevel(Level.INFO);
            
            if (log.isInfoEnabled()) {
              log.info(this.getName()+" listening on " + isa + ".");
            }
            
            log.setLevel(level);
    		} catch (IOException ioe) {
                log.error("Failure listening to socket on port '" + port + "'.", ioe);
                System.err.println("Failure listening to socket on port '" + port + "'.");
                ioe.printStackTrace();
                System.exit(-1);
            }
    		
    		try {
	    		FileWriter writer = new FileWriter(socketInfoFile);
				writer.write((ipAddress==null?"0.0.0.0":ipAddress)+":"+port);
				writer.flush();
				writer.close();
    		} catch (IOException e) {
				System.out.println("cannot create shutdownServer socket,System exit now!");
				e.printStackTrace();
				System.exit(-1);
    		}
    }
	
	public void shutdown(){
		super.shutdown();
		if(socketInfoFile.exists()){
			socketInfoFile.delete();
		}
	}
}
