/*
 * Copyright amoeba.meidusa.com
 * 
 *  This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 *  This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU AFFERO GENERAL PUBLIC LICENSE for more details. 
 *  You should have received a copy of the GNU AFFERO GENERAL PUBLIC LICENSE along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.meidusa.amoeba.server;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.meidusa.amoeba.config.BeanObjectEntityConfig;
import com.meidusa.amoeba.config.ConfigUtil;
import com.meidusa.amoeba.config.loader.AmoebaContextLoader;
import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.monitor.MonitorConstant;
import com.meidusa.amoeba.monitor.ShutdownClient;
import com.meidusa.amoeba.monitor.packet.MonitorCommandPacket;
import com.meidusa.amoeba.net.ConnectionManager;
import com.meidusa.amoeba.runtime.PriorityShutdownHook;
import com.meidusa.amoeba.service.Service;
import com.meidusa.amoeba.util.Reporter;
import com.meidusa.amoeba.util.StringUtil;

/**
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 * 
 */
public class AmoebaProxyServer {
    private static Logger log = Logger.getLogger(AmoebaProxyServer.class);
    private static Logger repoterLog = Logger.getLogger("report");
    /** Used to generate "state of server" reports. */
    protected static ArrayList<Reporter> reporters = new ArrayList<Reporter>();
    /** The time at which the server was started. */
    protected static long serverStartTime = System.currentTimeMillis();

    /** The last time at which {@link #generateReport} was run. */
    protected static long lastReportStamp = serverStartTime;
    
    public static void registerReporter(Reporter reporter) {
        reporters.add(reporter);
    }

    /**
     * Generates a report for all system services registered as a
     * {@link Reporter}.
     */
    public static String generateReport() {
        return generateReport(System.currentTimeMillis(), false);
    }

    /**
     * Generates and logs a "state of server" report.
     */
    protected static String generateReport(long now, boolean reset) {
        long sinceLast = now - lastReportStamp;
        long uptime = now - serverStartTime;
        StringBuilder report = new StringBuilder(" State of server report:"+StringUtil.LINE_SEPARATOR);

        report.append("- Uptime: ").append(StringUtil.intervalToString(uptime)).append(StringUtil.LINE_SEPARATOR);
        report.append("- Report period: ").append(StringUtil.intervalToString(sinceLast)).append(StringUtil.LINE_SEPARATOR);

        // report on the state of memory
        Runtime rt = Runtime.getRuntime();
        long total = rt.totalMemory(), max = rt.maxMemory();
        long used = (total - rt.freeMemory());
        report.append("- Memory: ").append(used / 1024).append("k used, ");
        report.append(total / 1024).append("k total, ");
        report.append(max / 1024).append("k max").append(StringUtil.LINE_SEPARATOR);
        
        for (int ii = 0; ii < reporters.size(); ii++) {
            Reporter rptr = reporters.get(ii);
            try {
                rptr.appendReport(report, now, sinceLast, reset,repoterLog.getLevel());
            } catch (Throwable t) {
                log.error("Reporter choked [rptr=" + rptr + "].", t);
            }
        }

        // only reset the last report time if this is a periodic report
        if (reset) {
            lastReportStamp = now;
        }

        return report.toString();
    }

    protected static void logReport(String report) {
        repoterLog.info(report);
    }

    /**
     * @param args
     * @throws IOException
     * @throws Exception 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public static void run(String[] args, BundleContext bundleContext) throws Exception {
    	final Logger logger = Logger.getLogger(AmoebaProxyServer.class);
        
        String backendBundleName = bundleContext.getProperty("amoeba.context.backend");
        Bundle backendBundle = null;
        
        if (backendBundleName != null) {
            for(Bundle bundle : bundleContext.getBundles()) {
                if (bundle.getSymbolicName().equalsIgnoreCase(backendBundleName)) {
                    backendBundle = bundle;
                    break;
                }
            }
        }
        
        if (backendBundle == null) {
            logger.error("could not find backend bundle " + backendBundleName);
            System.exit(-1);
        }
        
        // Proxy Runtime Context最先建立
        String contextClass = bundleContext.getProperty("amoeba.context.class");
        if(contextClass != null){
            ProxyRuntimeContext proxyRuntimeContext = (ProxyRuntimeContext)backendBundle.loadClass(contextClass).newInstance();
            ProxyRuntimeContext.setInstance(proxyRuntimeContext);
        }
        // 默认是ProxyRuntimeContext
        else {
            ProxyRuntimeContext proxyRuntimeContext = (ProxyRuntimeContext)backendBundle.loadClass(ProxyRuntimeContext.class.getName()).newInstance();
            ProxyRuntimeContext.setInstance(proxyRuntimeContext);
        }
        ProxyRuntimeContext.getInstance().setBackendBundle(backendBundle);
    	
        String level = bundleContext.getProperty("benchmark.level");
        if (!StringUtil.isEmpty(level)) {
          System.setProperty("benchmark.level", level);
        }
        
        // 设置Amoeba home路径，考虑相对路径
        String amoebaHomeProp = bundleContext.getProperty("amoeba.home");
        String relatedPath = new File(amoebaHomeProp).getCanonicalPath();
        File amoebaHome = new File(relatedPath);
        
        if (amoebaHome == null || !amoebaHome.exists()) {
        	logger.error("could not setup amoeba home path :"+amoebaHomeProp);
            System.exit(-1);
		}
        String amoebaHomePath = amoebaHome.getAbsolutePath();
        ProxyRuntimeContext.getInstance().setAmoebaHomePath(amoebaHomePath);
        
        if(args.length>=1){
            ShutdownClient client = new ShutdownClient(MonitorConstant.APPLICATION_NAME);
            MonitorCommandPacket packet = new MonitorCommandPacket();
            if("start".equalsIgnoreCase(args[0])){
                packet.funType = MonitorCommandPacket.FUN_TYPE_PING;
                if(client.run(packet)){
                    System.out.println("amoeba server is running with port="+client.getPort());
                    System.exit(-1);
                }
            }else{
                packet.funType = MonitorCommandPacket.FUN_TYPE_AMOEBA_SHUTDOWN;
                if(client.run(packet)){
                    System.out.println("amoeba server shutting down with port="+client.getPort());
                }else{
                    System.out.println("amoeba server not running with port="+client.getPort());
                }
                System.exit(0);
            }
        }else{
            System.out.println("amoeba start|stop");
            System.exit(0);
        }
        
        ServiceReference<AmoebaContextLoader> ref = backendBundle.getBundleContext().getServiceReference(AmoebaContextLoader.class);
        AmoebaContextLoader loader = backendBundle.getBundleContext().getService(ref);
        loader.setAmoebaContext(ProxyRuntimeContext.getInstance());
        ProxyRuntimeContext.getInstance().setAmoebaLoader(loader);
        ProxyRuntimeContext.getInstance().initConfig();
        ProxyRuntimeContext.getInstance().init();
        
        registerReporter(ProxyRuntimeContext.getInstance());
        for(ConnectionManager connMgr :ProxyRuntimeContext.getInstance().getConnectionManagerList().values()){
            registerReporter(connMgr);
        }
        
        Map<String,Object> context = new HashMap<String,Object>();
        context.putAll(ProxyRuntimeContext.getInstance().getConnectionManagerList());
        
        List<BeanObjectEntityConfig> serviceConfigList = ProxyRuntimeContext.getInstance().getConfig().getServiceConfigList();
        
        for(BeanObjectEntityConfig serverConfig : serviceConfigList){
            Service service = (Service)serverConfig.createBeanObject(false, context);
            
            service.init();
            service.start();
            PriorityShutdownHook.addShutdowner(service);
            registerReporter(service);
        }
        new Thread(){
            {
                this.setDaemon(true);
                this.setName("Amoeba Report Thread");
            }
            public void run(){
                while(true){
                    try {
                        Thread.sleep(60*1000);
                    } catch (InterruptedException e) {
                    }
                    try{
                    logReport(generateReport());
                    }catch(Exception e){
                        logger.error("report error",e);
                    }
                }
            }
        }.start();
    }
}
