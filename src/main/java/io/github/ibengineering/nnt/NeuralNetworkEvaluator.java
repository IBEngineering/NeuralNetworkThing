package io.github.ibengineering.nnt;

import org.neuroph.core.NeuralNetwork;

@FunctionalInterface
public interface NeuralNetworkEvaluator {

	public float evaluate(NeuralNetwork<?> nn);
	
}
