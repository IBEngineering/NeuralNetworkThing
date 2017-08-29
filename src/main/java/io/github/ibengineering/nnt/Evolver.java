package io.github.ibengineering.nnt;

public interface Evolver {

//	/**
//	 * What to save to files.
//	 * 
//	 * @author MisterCavespider
//	 */
//	public static enum SaveScope {
//		BEST,
//		MEDIAN,
//		WORST,
//		
//		NONE,
//		SURVIVORS,
//		DEATHS,
//		ALL
//	}
	
	/*
	 * Settings
	 */
	public int getIndividualCount();
	public void setIndividualCount(int individualCount);

	public int getIterationLimit();
	public void setIterationLimit(int iterationLimit);

	/*
	 * Variables
	 */
	public int getIterationCount();
	
	/*
	 * Methods
	 */
	public void processIteration();
	
	public void createGeneration(boolean newWeights);
	public void runGeneration();
	public void concludeGeneration();
	public void cleanGeneration(boolean removeSpatials);
	
}
