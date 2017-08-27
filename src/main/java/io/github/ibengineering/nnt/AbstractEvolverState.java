package io.github.ibengineering.nnt;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.nnet.MultiLayerPerceptron;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public abstract class AbstractEvolverState extends BaseAppState implements SmartEvolver {

	/*
	 * Settings
	 */
	private int individualCount = 256;
	private int iterationLimit = 300;
	private int threadCount = 5;
	
	/*
	 * Variables
	 */
	protected int iterationCount = 0;
	
	protected ExecutorService executorService;
	
	protected Generation currentGeneration;
	
	/*
	 * Concurrent
	 */
	private Future<?>[] futures;
	
	/*
	 * jME
	 */
	protected Node baseNode;
	protected Node propNode;
	protected Spatial[] spatials;
	
	@Override
	protected void initialize(Application app) {
		executorService = Executors.newFixedThreadPool(threadCount);
		
		currentGeneration = new Generation(individualCount, 4,6,4);
		
		futures = new Future<?>[individualCount];
		
		baseNode = new Node("BaseNode of AbstractEvolverState");
		((SimpleApplication)app).getRootNode().attachChild(baseNode);
		propNode = new Node("PropNode of AbstractEvolverState");
		((SimpleApplication)app).getRootNode().attachChild(propNode);
		spatials = new Spatial[individualCount];
		
		createMaterials(app);
		
		createGeneration(true);
		populateProps();
	}
	
	protected void createMaterials(Application app) {}
	protected abstract void populateProps();

	@Override
	public void processIteration() {
		if(iterationCount >= iterationLimit) {
			cleanGeneration();
			createGeneration(false);
			
			propNode.detachAllChildren();
			populateProps();
			
			iterationCount = 0;
		}
		
		runGeneration();
	}
	
	@Override
	protected void cleanup(Application app) {
		cleanGeneration();
		
		executorService.shutdown();
		while(!executorService.isTerminated()) {
		}
	}

	@Override
	public void createGeneration(boolean newWeights) {
		if(newWeights) {
			currentGeneration.populate();
		} else {
			concludeGeneration();
		}
		
		createSpatials();
		onCreateGeneration();
	}
	
	protected void createSpatials() {
		for (int i = 0; i < individualCount; i++) {
			Spatial s = createSpatial(i);
			baseNode.attachChild(s);
			addControls(i, s);
			spatials[i] = s;
		}
	}
	
	public abstract Spatial createSpatial(int i);
	public void addControls(int i, Spatial s) {}
	
	public abstract void onCreateGeneration();
	
	@Override
	public void runGeneration() {
		preInputs();
		
		for (int i = 0; i < individualCount; i++) {
			preInput(i);
			double[] in = bakeInput(i);
			NeuralNetwork n = currentGeneration.get(i);
			n.setInput(in);
		}
		
		//invoke
		for (int i = 0; i < individualCount; i++) {
			Future<?> f = executorService.submit((Runnable)currentGeneration.get(i)::calculate);
			futures[i] = f;
		}
		/* ! wait ! */
		for (Future<?> f : futures) {
			try {
				f.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		for (int i = 0; i < individualCount; i++) {
			double[] out = currentGeneration.get(i).getOutput();
			processOutput(i, out);
			postOutput(i);
		}
		
		postOutputs();
		
		iterationCount++;
	}

	protected void preInputs() {}
	protected void preInput(int i) {}
	protected abstract double[] bakeInput(int i);
	protected abstract void processOutput(int i, double[] outputs);
	protected void postOutput(int i) {}
	protected void postOutputs() {}
	
	@Override
	public void concludeGeneration() {
		concludeGeneration(2, 2);
	}

	public void concludeGeneration(int pairSize, int survivorDivider) {
		Generation nextGeneration = new Generation(individualCount, 4, 6, 4);
		
		Map<NeuralNetwork, Float> sortedScores = currentGeneration.scoreNetwork(false);
		NeuralNetwork[] sortedNnets = sortedScores.keySet().toArray(new NeuralNetwork[individualCount]);
		
		for(int i = 0; i < individualCount/survivorDivider; i+=pairSize) {
			NeuralNetwork current = sortedNnets[i];
			NeuralNetwork next = sortedNnets[i+1];
			Double[] currentWeights = current.getWeights();
			Double[] nextWeights = next.getWeights();
			
			for(int j = 0; j < survivorDivider*pairSize; j++) {
				NeuralNetwork output = new MultiLayerPerceptron(4, 6, 4);
				Double[] weights = output.getWeights();
				for (int k = 0; k < weights.length; k++) {
					//get from current and next
					weights[k] = (Math.random() < 0.5d) ? currentWeights[k] : nextWeights[k];
					//random mutation rate?
					weights[k] += (Math.random() < 0.2d) ? (Math.random()*2d-1d) / 1d : 0d;
				}
				output.setWeights(Stream.of(weights).mapToDouble(Double::doubleValue).toArray());
				
				nextGeneration.set(i*survivorDivider+j, output);
			}
		}
		
		saveGeneration(currentGeneration);
		currentGeneration = null;
		currentGeneration = nextGeneration;
	}
	
	protected void saveGeneration(Generation g) {}
	

	@Override
	public void cleanGeneration() {
		for(Spatial s : spatials) {
			removeControls(s);
			s.removeFromParent();
		}
		baseNode.detachAllChildren();
	}
	
	public void removeControls(Spatial s) {}
	
	@Override
	protected void onEnable() {}

	@Override
	protected void onDisable() {}

	@Override
	public int getIndividualCount() {
		return individualCount;
	}

	@Override
	public void setIndividualCount(int individualCount) {
		this.individualCount = individualCount;
	}

	@Override
	public int getIterationLimit() {
		return iterationLimit;
	}

	@Override
	public void setIterationLimit(int iterationLimit) {
		this.iterationLimit = iterationLimit;
	}

	@Override
	public int getIterationCount() {
		return iterationCount;
	}

	@Override
	public ExecutorService getExecutorService() {
		return executorService;
	}
	
	@Override
	public int getThreadCount() {
		return threadCount;
	}

	@Override
	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}
}
