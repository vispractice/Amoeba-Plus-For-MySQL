package com.meidusa.amoeba.service;

import com.meidusa.amoeba.exception.InitialisationException;
import com.meidusa.amoeba.runtime.Shutdowner;
import com.meidusa.amoeba.util.Initialisable;
import com.meidusa.amoeba.util.Reporter;

/**
 * 
 * @author Struct
 *
 */
public interface Service extends Initialisable,Shutdowner,Reporter{
	
	public int getPriority();
	/**
	 * after Properties setted 
	 */
	public void init() throws InitialisationException;
	
	/**
	 * start Service
	 */
	public void start();
	
	/**
	 * destroy service
	 */
	public void shutdown();
}
