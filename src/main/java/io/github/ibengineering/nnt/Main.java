package io.github.ibengineering.nnt;

import com.jme3.app.DebugKeysAppState;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.audio.AudioListenerState;
import com.jme3.bullet.BulletAppState;

import io.github.ibengineering.nnt.tests.PoolNeuralNetworkTestState;
import io.github.ibengineering.nnt.tests.SteererState;

public class Main extends SimpleApplication {
	
	public static void main( String... args ) {
		
//		nnet.setInput(0.8,0.0,0.0, 1.0, 0.0,0.0,0.0,0.0,0.0,0.0);
//		nnet.calculate();
//		double[] output = nnet.getOutput();
//		LOG.info("Output: {}", output);
//		
//		nnet.save("src/main/resources/steering v1.nnet");
		
		Main main = new Main();
		main.start();
	}

	public Main() {
		super(
			new StatsAppState(),
			new FlyCamAppState(),
			new AudioListenerState(),
			new DebugKeysAppState(),
			new ScreenshotAppState("images/", 0),
//			new PoolNeuralNetworkTestState()
			new SteererState()
		);
	}
	
	public void simpleInitApp() {
		stateManager.attach(new BulletAppState());
	}
}


