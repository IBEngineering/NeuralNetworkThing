package io.github.ibengineering.tests;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.nnet.MultiLayerPerceptron;

import com.jme3.app.Application;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Sphere;

import io.github.mistercavespider.lina.color.GradientColorController;
import io.github.mistercavespider.lina.ctrl.TimeTracer;

public class PoolNeuralNetworkTestState extends BaseAppState {

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
	    return map.entrySet()
	              .stream()
	              .sorted(Map.Entry.comparingByValue(/*Collections.reverseOrder()*/))
	              .collect(Collectors.toMap(
	                Map.Entry::getKey, 
	                Map.Entry::getValue, 
	                (e1, e2) -> e1, 
	                LinkedHashMap::new
	              ));
	}
	
	private Node node;
	private Spatial[] balls;
	private Node propNode;
	
	private Material ballmat, linemat;
	
	private ExecutorService pool;
	private Future<?>[] futures;
	
	private int nnetCount = 500;
	@SuppressWarnings("rawtypes")
	private NeuralNetwork[] nnets;
	private Vector3f goal;
	private Vector3f lastGoalDirection;
	private double originalDistance;
	private double maxSpeed = 1d;
	
	private HashMap<NeuralNetwork, Float> scores;
	
	private int runCounter = 0;
	
	@Override
	protected void initialize(Application app) {
		getState(BulletAppState.class).getPhysicsSpace().setGravity(Vector3f.ZERO);
		getState(FlyCamAppState.class).getCamera().setMoveSpeed(65f);
		node = new Node("Node");
		((SimpleApplication)app).getRootNode().attachChild(node);
		propNode = new Node("Props");
		((SimpleApplication)app).getRootNode().attachChild(propNode);
		
		balls = new Spatial[nnetCount];
		
		pool = Executors.newFixedThreadPool(5);
		futures = new Future<?>[nnetCount];
		
		nnets = new NeuralNetwork[nnetCount];
		
		ballmat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/UnshadedNodes.j3md");
		ballmat.setColor("Color", ColorRGBA.Green);
		linemat = ballmat.clone();
		linemat.setTransparent(true);
		linemat.setBoolean("VertexColor", true);
		
		createGeneration(true);
		populateProps();
	}
	
	private void populateProps() {
		Material propmat = ballmat.clone();
		propmat.setColor("Color", new ColorRGBA(0,0.4f,1,1));
		
		Material arrowmat = ballmat.clone();
		arrowmat.setColor("Color", ColorRGBA.White);
		
		Geometry grid = new Geometry("Grid", new Grid(24, 24, 1f));
		grid.setLocalTranslation(-12f, 0f, -12f);
		grid.setMaterial(propmat);
		propNode.attachChild(grid);
		
		Geometry goalArrow = new Geometry("Goal Pointer", new Arrow(goal));
		goalArrow.setMaterial(arrowmat);
		propNode.attachChild(goalArrow);
	}

	public void update(float tpf) {
		if(runCounter >= 300) {
			concludeGeneration();
			deletePreviousGeneration();
			createGeneration(false);
			
			propNode.detachAllChildren();
			populateProps();
			runCounter = 0;
		}
		
		runGeneration(tpf);
		
	}

	private void createGeneration(boolean createNnets) {
		if(createNnets) createNeuralNetworks();
		createSpatials();
		createScores();
		
		goal = new Vector3f(
			FastMath.nextRandomFloat() * 12 - 6,
			FastMath.nextRandomFloat() * 12 - 6,
			FastMath.nextRandomFloat() * 12 - 6
		);
		getApplication().getCamera().lookAt(goal, Vector3f.UNIT_Y);
		lastGoalDirection = new Vector3f(
			FastMath.nextRandomFloat() * 12 - 6,
			FastMath.nextRandomFloat() * 12 - 6,
			FastMath.nextRandomFloat() * 12 - 6
		);
		
		originalDistance = goal.distance(Vector3f.ZERO);
	}
	
	private void createNeuralNetworks() {
		for (int i = 0; i < nnets.length; i++) {
			nnets[i] = new MultiLayerPerceptron(10, 8 , 4);
		}
	}

	private void createSpatials() {
		for (int i = 0; i < nnetCount; i++) {
			Spatial s = createSpatial(i);
			balls[i] = s;
		}
	}
	
	private void createScores() {
		scores = new HashMap<>();
	}
	
	private Spatial createSpatial(int i) {
		//Geom
		Geometry g = new Geometry("Ball #"+i, new Sphere(8,8, 0.5f));
		g.setMaterial(ballmat);
		node.attachChild(g);
		
		//Trace
		GradientColorController cc = new GradientColorController();
		cc.setBaseColor(ColorRGBA.Cyan);
		cc.setSecondaryColor(ColorRGBA.Green);
		g.addControl(new TimeTracer(linemat, 30, 256, cc));
		
		//Physics
		RigidBodyControl phy = new RigidBodyControl(new SphereCollisionShape(0.5f));
		getState(BulletAppState.class).getPhysicsSpace().add(phy);
		g.addControl(phy);
		
		return g;
	}

	private void runGeneration(float tpf) {
		//set inputs ...
		for(int i = 0; i < nnetCount; i++) {
			Vector3f currPosition = balls[i].getControl(RigidBodyControl.class).getPhysicsLocation();
			
			Vector3f goalDirection = goal.subtract(currPosition).normalizeLocal().addLocal(1, 1, 1).divide(2);
			double relativeDistance = goal.distance(currPosition) / originalDistance;
			
			nnets[i].setInput(new double[] {
				goalDirection.x,
				goalDirection.y,
				goalDirection.z,
				
				Math.min(relativeDistance, 0.99),
				
//				0.001, 0.001, 0.001, 0.001, 0.001, 0.001
				0, 0, 0, 0, 0, 0
			});
		}
		
		// invoke
		for(int i = 0; i < nnetCount; i++) {
			// Executes calculate
			Future<?> future = pool.submit((Runnable)nnets[i]::calculate);
			futures[i] = future;
		}
		//wait
		for(Future<?> f : futures) {
			try {
				f.get();	//Blocks until done
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		//get outputs ...
		for(int i = 0; i < nnetCount; i++) {
			double[] output = nnets[i].getOutput();
			
			Vector3f outDirection = new Vector3f(
				(float) ((output[0] -0.5d) * 2d),
				(float) ((output[1] -0.5d) * 2d),
				(float) ((output[2] -0.5d) * 2d)
			).normalize();
			double speedMultiplier = output[3];
			
			Vector3f movement = outDirection.mult((float) (speedMultiplier * maxSpeed));
			RigidBodyControl rbc = balls[i].getControl(RigidBodyControl.class);
			rbc.setPhysicsLocation(rbc.getPhysicsLocation().add(movement));
			
			//Add score
			float iterScore = 0f;
			iterScore += goal.distance(rbc.getPhysicsLocation());
			scores.put(nnets[i], scores.getOrDefault(nnets[i], 0f) + iterScore);
		}
		
		runCounter++;
	}
	
	@SuppressWarnings("rawtypes")
	private void concludeGeneration() {
		int chunksize = 5;
		int survivorRatio = 10;
		
		Map<NeuralNetwork, Float> sortedScores = sortByValue(scores);
		
//		NeuralNetwork[] newgeneration = new NeuralNetwork[nnetCount];
		
		NeuralNetwork[] sortedNnets = sortedScores.keySet().toArray(new NeuralNetwork[nnetCount]);
		for(int i = 0; i < nnetCount/survivorRatio; i+=chunksize) {
			NeuralNetwork current = sortedNnets[i];
			NeuralNetwork next = sortedNnets[i+1];
			Double[] currentWeights = current.getWeights();
			Double[] nextWeights = next.getWeights();
			
			for(int j = 0; j < survivorRatio; j++) {
				NeuralNetwork output = new MultiLayerPerceptron(10, 8, 4);
				Double[] weights = output.getWeights();
				for (int k = 0; k < weights.length; k++) {
					weights[k] = (currentWeights[k] + nextWeights[k]) / chunksize;
					weights[k] += (Math.random()*2d-1d) / 100d;
				}
				output.setWeights(Stream.of(weights).mapToDouble(Double::doubleValue).toArray());
				
				nnets[i*survivorRatio/chunksize+j] = output;
			}
			
			System.err.println("i=" + i);
		}
		
		
//		float smallestDistance = Float.POSITIVE_INFINITY;
//		for(int i = 0; i < nnetCount; i++) {
//			Vector3f pos = balls[i].getControl(RigidBodyControl.class).getPhysicsLocation();
//			float distance = pos.distance(goal);
//			if(distance < smallestDistance) {
//				smallestDistance = distance;
//			}
//		}
//		System.out.println("Smallest distance from goal: " + smallestDistance);
	}
	
	private void deletePreviousGeneration() {
		for(Spatial s : balls) {
			RigidBodyControl phy = s.getControl(RigidBodyControl.class);
			if(phy != null) {
				//Remove physics
				getState(BulletAppState.class).getPhysicsSpace().remove(phy);
			}
			
			//Remove spatial
			s.removeControl(RigidBodyControl.class);
			s.removeControl(TimeTracer.class);
			s.removeFromParent();
		}
		
		// Just in case
		node.detachAllChildren();
		
		scores = null;
	}
	
	@Override
	protected synchronized void cleanup(Application app) {
		deletePreviousGeneration();
		
		pool.shutdown();
		while(!pool.isTerminated()) {
			try {
				wait(1);
			} catch (InterruptedException e) {
				
			}
		}
	}

	@Override
	protected void onEnable() {
	}

	@Override
	protected void onDisable() {
	}

}
