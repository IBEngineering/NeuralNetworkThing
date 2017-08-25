package io.github.ibengineering;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.exceptions.NeurophException;
import org.neuroph.nnet.Perceptron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme3.app.SimpleApplication;

public class Main extends SimpleApplication {
	private static Logger LOG = LoggerFactory.getLogger(Main.class);
	
	@SuppressWarnings("rawtypes")
	public static void main( String... args ) {
		NeuralNetwork nnet = null;
		try {
			nnet = NeuralNetwork.createFromFile("src/main/resources/steering v1.nnet");
		} catch (NeurophException ne) {
			LOG.info("Could not read");
			nnet = new Perceptron(10, 4);
		}
		
		nnet.setInput(0.8,0.0,0.0, 1.0, 0.0,0.0,0.0,0.0,0.0,0.0);
		nnet.calculate();
		double[] output = nnet.getOutput();
		LOG.info("Output: {}", output);
		
		nnet.save("src/main/resources/steering v1.nnet");
		
		Main main = new Main();
		main.start();
	}

	public void simpleInitApp() {
		
	}
}


