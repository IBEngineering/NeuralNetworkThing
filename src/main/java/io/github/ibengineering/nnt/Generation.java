package io.github.ibengineering.nnt;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.nnet.MultiLayerPerceptron;

@SuppressWarnings("rawtypes")
public class Generation {

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map, boolean flip) {
		return map.entrySet()
			.stream()
			.sorted(
				(flip)?
					Map.Entry.comparingByValue(Collections.reverseOrder()):
					Map.Entry.comparingByValue()
			)
			.collect(Collectors.toMap(
				Map.Entry::getKey, 
				Map.Entry::getValue, 
				(e1, e2) -> e1, 
				LinkedHashMap::new
			));
	}
	
	private NeuralNetwork[] nnets;
	private Map<NeuralNetwork, Float> scores;
	private int[] neuronLayers;
	
	public Generation(int individualCount, int... neuronLayers) {
		nnets = new NeuralNetwork[individualCount];
		scores = new HashMap<>();
		this.neuronLayers = neuronLayers;
	}
	
	public void populate() {
		for (int i = 0; i < nnets.length; i++) {
			nnets[i] = new MultiLayerPerceptron(neuronLayers);
		}
	}
	
	public Map<NeuralNetwork, Float> scoreNetwork(boolean flip) {
		return sortByValue(scores, flip);
	}
	
	public void addScore(int i, float score) {
		scores.put(nnets[i], scores.getOrDefault(nnets[i], 0f) + score);
	}
	
	public NeuralNetwork get(int i) {
		return nnets[i];
	}
	
	public void set(int i, NeuralNetwork nnet) {
		nnets[i] = nnet;
	}

	public NeuralNetwork[] getNnets() {
		return nnets;
	}

	public Map<NeuralNetwork, Float> getScores() {
		return scores;
	}

	public int[] getNeuronLayers() {
		return neuronLayers;
	}
	
}
