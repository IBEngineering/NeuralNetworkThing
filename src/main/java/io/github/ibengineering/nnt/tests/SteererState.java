package io.github.ibengineering.nnt.tests;

import java.sql.Time;

import com.jme3.app.Application;
import com.jme3.app.FlyCamAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Sphere;

import io.github.ibengineering.nnt.AbstractEvolverState;
import io.github.mistercavespider.lina.color.TransparentColorControl;
import io.github.mistercavespider.lina.ctrl.TimeTracer;

public final class SteererState extends AbstractEvolverState {

	/*
	 * Evolver
	 */
	private Vector3f goal;
	private Vector3f lastGoalDirection;
	private float originalDistance;
	private float maxSpeed = 0.08f;
	
	/*
	 * jME
	 */
	private Material ballmat;
	private Material linemat;

	/*
	 * props
	 */
	private Arrow a;
	private Geometry goalg;

	@Override
	protected void initialize(Application app) {
		super.initialize(app);
		
		getState(BulletAppState.class).getPhysicsSpace().setGravity(Vector3f.ZERO);
		getState(FlyCamAppState.class).getCamera().setMoveSpeed(55f);
		getState(FlyCamAppState.class).getCamera().setDragToRotate(true);
	}

	@Override
	public void update(float tpf) {
		processIteration();
	}

	@Override
	protected void createMaterials(Application app) {
		ballmat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/UnshadedNodes.j3md");
		ballmat.setColor("Color", ColorRGBA.Green);
		linemat = ballmat.clone();
		linemat.setTransparent(true);
		linemat.setBoolean("VertexColor", true);
	}

	@Override
	protected void populateProps() {
		ColorRGBA propColor = new ColorRGBA(0,0.4f,1,1);
		
		Material propmat = ballmat.clone();
		propmat.setColor("Color", propColor);
		propmat.getAdditionalRenderState().setWireframe(true);
		
		Material arrowmat = ballmat.clone();
		arrowmat.setColor("Color", ColorRGBA.White);
		
		Material proplinemat = linemat.clone();
		proplinemat.setColor("Color", propColor);
		
		Geometry grid = new Geometry("Grid", new Grid(24, 24, 1f));
		grid.setLocalTranslation(-12f, 0f, -12f);
		grid.setMaterial(propmat);
		propNode.attachChild(grid);
		
		a = new Arrow(goal);
		Geometry goalArrow = new Geometry("Goal Pointer", a);
		goalArrow.setMaterial(arrowmat);
		propNode.attachChild(goalArrow);
		
		goalg = new Geometry("Goal", new Sphere(8,8, 0.5f));
		goalg.setMaterial(propmat);
		propNode.attachChild(goalg);
		TransparentColorControl cc = new TransparentColorControl();
		cc.setBaseColor(propColor);
		goalg.addControl(new TimeTracer(proplinemat, 30, 128, cc));
	}

	@Override
	public Spatial createSpatial(int i) {
		Geometry r = new Geometry("Ball", new Sphere(8,8, 0.5f));
		r.setMaterial(ballmat);
		return r;
	}
	
	@Override
	public void addControls(int i, Spatial s) {
		TransparentColorControl cc = new TransparentColorControl();
		cc.setBaseColor(ColorRGBA.Green);
		s.addControl(new TimeTracer(linemat, 30, 128, cc));
		
		//Physics
		RigidBodyControl phy = new RigidBodyControl(new SphereCollisionShape(0.5f));
		getState(BulletAppState.class).getPhysicsSpace().add(phy);
		s.addControl(phy);
	}

	@Override
	public void onCreateGeneration() {
		goal = new Vector3f(
			FastMath.nextRandomFloat() * 12 - 6,
			FastMath.nextRandomFloat() * 12 - 6,
			FastMath.nextRandomFloat() * 12 - 6
		);
		lastGoalDirection = new Vector3f(
			FastMath.nextRandomFloat() * 2 - 1,
			FastMath.nextRandomFloat() * 2 - 1,
			FastMath.nextRandomFloat() * 2 - 1
		).normalizeLocal();
		
		originalDistance = goal.distance(Vector3f.ZERO);
	}

	@Override
	protected void preInputs() {
		Vector3f newDirection = new Vector3f(
			FastMath.nextRandomFloat() * 2 - 1,
			FastMath.nextRandomFloat() * 2 - 1,
			FastMath.nextRandomFloat() * 2 - 1
		).normalizeLocal();
		newDirection.multLocal(0.05f).addLocal(lastGoalDirection.mult(0.95f));
		goal.addLocal(newDirection.mult(0.5f));
		lastGoalDirection = newDirection;
		originalDistance = goal.distance(Vector3f.ZERO);
		/** ! **/
		goal.normalizeLocal().multLocal(12f);
		
		//props
		a.setArrowExtent(goal);
		goalg.setLocalTranslation(goal);
	}

	@Override
	protected double[] bakeInput(int i) {
		Vector3f currPosition = spatials[i].getControl(RigidBodyControl.class).getPhysicsLocation();
		
		Vector3f goalDirection = goal.subtract(currPosition).normalizeLocal().addLocal(1, 1, 1).divide(2);
//		double relativeDistance = goal.distance(currPosition) / originalDistance;
		
		return new double[] {
			goalDirection.x,
			goalDirection.y,
			goalDirection.z,
			
			1d	// TODO: realistic value
		};
	}

	@Override
	protected void processOutput(int i, double[] outputs) {
		double[] output = currentGeneration.get(i).getOutput();
		
		Vector3f outDirection = new Vector3f(
			(float) ((output[0] -0.5d) * 2d),
			(float) ((output[1] -0.5d) * 2d),
			(float) ((output[2] -0.5d) * 2d)
		).normalize();
		double speedMultiplier = output[3];
		
		Vector3f movement = outDirection.mult((float) (speedMultiplier * maxSpeed));
		RigidBodyControl rbc = spatials[i].getControl(RigidBodyControl.class);
		rbc.setPhysicsLocation(rbc.getPhysicsLocation().add(movement));
		
		//Add score
		float iterScore = 0f;
		iterScore += goal.distance(rbc.getPhysicsLocation());
		/* add for penalties ... */
		
		currentGeneration.addScore(i, iterScore);
	}

	@Override
	public void removeControls(Spatial s) {
		RigidBodyControl phy = s.getControl(RigidBodyControl.class);
		if(phy!=null) {
			getState(BulletAppState.class).getPhysicsSpace().remove(phy);
		}
		
		s.removeControl(RigidBodyControl.class);
		s.removeControl(TimeTracer.class);
	}

}
