package io.github.ibengineering.tests;

import java.util.Arrays;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.exceptions.NeurophException;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.util.TransferFunctionType;

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
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Sphere;

import io.github.mistercavespider.lina.ctrl.TimeTracer;

public class SingleNeuralNetworkTestState extends BaseAppState {

	private RigidBodyControl rbc;
	private Geometry g;
	@SuppressWarnings("rawtypes")
	private NeuralNetwork nnet = null;

	private Vector3f goal = new Vector3f(12f, -3f, 4f);
	private float originalDistance;
	private float maxSpeed = 10; // m/s
	
	private Node node;
	
	private Line l;
	
	@Override
	protected void initialize(Application app) {
		getState(BulletAppState.class).getPhysicsSpace().setGravity(Vector3f.ZERO);
		getState(FlyCamAppState.class).getCamera().setMoveSpeed(65f);
		
		g = new Geometry("g", new Sphere(16, 16, .5f));
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", ColorRGBA.Red);
		g.setMaterial(mat);
		((SimpleApplication)app).getRootNode().attachChild(g);
		
		//Physics
		rbc = new RigidBodyControl(new SphereCollisionShape(.5f));
		getState(BulletAppState.class).getPhysicsSpace().add(rbc);
		g.addControl(rbc);
		originalDistance = rbc.getPhysicsLocation().distance(goal);
		
		//Lina
		TimeTracer tt = new TimeTracer(mat.clone(), 100, 512);
		g.addControl(tt);
		//Line
		l = new Line(goal, rbc.getPhysicsLocation());
		Geometry gl = new Geometry("l", l);
		Material matc = mat.clone();
		matc.setColor("Color", ColorRGBA.Cyan);
		gl.setMaterial(matc);
		((SimpleApplication)app).getRootNode().attachChild(gl);
		
		try {
			nnet = NeuralNetwork.createFromFile("src/main/resources/steering v1.nnet");
		} catch (NeurophException ne) {
			System.out.println("Creating new nnet");
			nnet = new MultiLayerPerceptron(TransferFunctionType.SIGMOID, 10, 8, 4);
			
			System.out.println("Weights: " + Arrays.toString(nnet.getWeights()));
		}
		//nnet.save("src/main/resources/steering v1.nnet");
		
		node = new Node("n");
		((SimpleApplication)app).getRootNode().attachChild(node);
	}

	@Override
	public void update(float tpf) {
		/*
		 * Step 1: get all the inputs
		 * 
		 * Step 2: calculate
		 * 
		 * Step 3: get all the outputs
		 */
		
		Vector3f inDirection = goal.subtract(rbc.getPhysicsLocation()).normalize();
		double inDistance = Math.min(0.99, rbc.getPhysicsLocation().distance(goal) / originalDistance);
		
		double[] inputs = bakeInputs(inDirection, inDistance);
		System.out.println(Arrays.toString(inputs));
		nnet.setInput(inputs);
		nnet.calculate();
		double[] output = nnet.getOutput();
		
		System.out.println(Arrays.toString(output));
		
		Vector3f outDirection = new Vector3f(
			(float) output[0],
			(float) output[1],
			(float) output[2]
		).normalize();
		double speedMultiplier = output[3];
		
		Vector3f movement = outDirection.mult((float) (speedMultiplier * maxSpeed));
		System.out.println("Output move vector : " + movement);
		rbc.setPhysicsLocation(rbc.getPhysicsLocation().add(movement.mult(tpf)));
//		g.setLocalTranslation(outDirection.mult((float) (speedMultiplier * maxSpeed * tpf)));
		
		
	}		

	private double[] bakeInputs(Vector3f direction, double distance) {
		return new double[] {
			direction.x,
			direction.y,
			direction.z,
			distance,
			
			0.5, 0.5, 0.5, 0.5, 0.5, 0.5
		};
	}
	
	@Override
	protected void cleanup(Application app) {
	}

	@Override
	protected void onEnable() {
	}

	@Override
	protected void onDisable() {
	}

}
