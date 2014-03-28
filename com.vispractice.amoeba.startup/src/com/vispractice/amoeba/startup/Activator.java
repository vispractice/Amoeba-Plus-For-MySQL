package com.vispractice.amoeba.startup;


import org.apache.log4j.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.meidusa.amoeba.exception.AmoebaRuntimeException;
import com.meidusa.amoeba.server.AmoebaProxyServer;


public class Activator implements BundleActivator {
    private static Logger logger = Logger.getLogger(Activator.class);

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(final BundleContext context) throws Exception {
	  String[] args = {"start"};
      try {
          AmoebaProxyServer.run(args, context);
      } catch (Exception e) {
        logger.error("start amoeba error", e);
        throw new AmoebaRuntimeException(e);
      }
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
	}

}


