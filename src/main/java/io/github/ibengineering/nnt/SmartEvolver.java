package io.github.ibengineering.nnt;

import java.util.concurrent.ExecutorService;

public interface SmartEvolver extends Evolver {

	/*
	 * Settings
	 */
	public int getThreadCount();
	public void setThreadCount(int threadCount);
	
	/*
	 * Variables
	 */
	public ExecutorService getExecutorService();
	
}
