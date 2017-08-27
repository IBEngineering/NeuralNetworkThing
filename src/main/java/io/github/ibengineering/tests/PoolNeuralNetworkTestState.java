package io.github.ibengineering.tests;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Sphere;

import io.github.mistercavespider.lina.color.TransparentColorControl;
import io.github.mistercavespider.lina.ctrl.TimeTracer;

public class PoolNeuralNetworkTestState extends BaseAppState {

	private Node node;
	private Spatial[] balls;
	private Node propNode;
	
	private Material ballmat, linemat;
	
	private ExecutorService pool;
	
	private int nnetCount = 250;
	@SuppressWarnings("rawtypes")
	private NeuralNetwork[] nnets;
	private Vector3f goal = new Vector3f(-12, -3, 4);
	private double originalDistance;
	private double maxSpeed = 10;
	
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
		
		nnets = new NeuralNetwork[nnetCount];
		
		ballmat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/UnshadedNodes.j3md");
		ballmat.setColor("Color", ColorRGBA.Green);
		linemat = ballmat.clone();
		linemat.setTransparent(true);
		linemat.setBoolean("VertexColor", true);
		
		createGeneration();
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
//		deletePreviousGeneration();
//		createGeneration();
		runGeneration(tpf);
	}

	private void createGeneration() {
		createNeuralNetworks();
		createSpatials();
		
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
	
	private Spatial createSpatial(int i) {
		//Geom
		Geometry g = new Geometry("Ball #"+i, new Sphere(8,8, 0.5f));
		g.setMaterial(ballmat);
		node.attachChild(g);
		
		//Trace
//		GradientColorController cc = new GradientColorController();
//		cc.setBaseColor(ColorRGBA.Cyan);
//		cc.setSecondaryColor(ColorRGBA.Green);
		TransparentColorControl cc = new TransparentColorControl();
		cc.setBaseColor(ColorRGBA.Green);
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
			
			Vector3f goalDirection = goal.subtract(currPosition).normalizeLocal();
			double relativeDistance = goal.distance(currPosition) / originalDistance;
			
			nnets[i].setInput(new double[] {
				goalDirection.x,
				goalDirection.y,
				goalDirection.z,
				
				Math.min(relativeDistance, 0.99),
				
				0.001, 0.001, 0.001, 0.001, 0.001, 0.001
			});
		}
		
		// invoke
		for(int i = 0; i < nnetCount; i++) {
			// Executes calculate
			pool.execute(nnets[i]::calculate);
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
			rbc.setPhysicsLocation(rbc.getPhysicsLocation().add(movement.mult(tpf)));
		}
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
