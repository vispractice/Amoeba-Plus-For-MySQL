package com.meidusa.amoeba.net;

public interface IdleChecker {
	
	/**
	 * 检查是否以及处于idle，如果返回true，则需要关闭.
	 * @param now
	 * @return
	 */
	public boolean checkIdle(long now);
}
