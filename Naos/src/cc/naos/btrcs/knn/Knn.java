/*
 * Copyright © 2007 Paul Lammertsma
 * 
 * This file is part of jKNN.
 * 
 * jKNN is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * jKNN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */

package cc.naos.btrcs.knn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import logging.com.bitfactoryinc.logging.FileLogger;


/**
 * This is the root class of jKNN and includes the core
 * of the algorithm.
 * @author Paul Lammertsma
 * minor edits by Carlos Castellanos
 */

public class Knn implements KnnConstants
{

	private static final long serialVersionUID = -1660494050190503761L;

	public static boolean doEuclidean = KNN_DISTANCE_EUCLIDEAN;
	public static boolean doRescale = KNN_DISTANCE_SCALED;
	public static boolean doScaleOutputs = KNN_SCALE_OUTPUTS;
	private Situation[] dataSet;
	
	// logging
	public static FileLogger logger = new FileLogger("log/btrcs.log");
	
	/**
	 * Default constructor for jKNN.
	 */
	public Knn(Situation[] dataSet) {
		this.dataSet = dataSet;
	}
	
	public Knn() {
		
	}
	
	public void setDataSet(Situation[] ds) {
		this.dataSet = ds;
	}
	
	/**
	 * Evaluates a situation to classify any unknown variables.
	 * @param qisituation A {@link Situation} containing one or
	 * more unknown {@link Value}s.
	 * i.e. - the query instance
	 * @return int representing the index in the raining data
	 * it corresponds to (so we can get the image to show next
	 */
	public HashMap<String, Object> evaluate(Situation qisituation) {
		clear();
		
		busy(true);
		
		int trainingDataIndex = -1;
		//Logger.out.println("Gathering " + KNN_NEIGHBORS + " nearest neighbors to:\n  " + situation + "\n");
		System.out.println("Gathering " + KNN_NEIGHBORS + " nearest neighbors to:\n  " + qisituation + "\n");
		logger.logInfo("Starting KNN algorithm\n");
		logger.logInfo("Nearest Neighbors=" + KNN_NEIGHBORS + " Query Instance=" + qisituation);
		Situation[] nearest = getNearest(qisituation, KNN_NEIGHBORS);
		String currClass = "";
		
		//Logger.out.println();
		
		// we have the k-nearest, now classify
		KnnValue[] properties = qisituation.getProperties();
		KnnValue[] newProperties = new KnnValue[properties.length];
		for (int i=0; i<properties.length; i++) {
			if (properties[i].isUnknown()) {
				double sum = 0;
				Map<Double, Integer> votes = new HashMap<Double, Integer>();
				int maxVotes = 0;
				double maxVoteIndex = 0.0f;
				String set1 = "", set2 = "";
				// to make sure maxVoteIndex represents 
				// "the nearest of the nearest" (if there is a tie)
				// start from end of array and go backwards
				for (int j=nearest.length-1; j>-1; j--) {
					if (doScaleOutputs) {
						sum += nearest[j].getProperty(i).getIndex();
					} else {
						// the array index of the value
						double thisVote = nearest[j].getProperty(i).getIndex();
						int value = 0;
						if (votes.get(thisVote) != null) value = votes.get(thisVote) + 1;
						votes.put(thisVote, value);
						if (maxVotes < value) {
							maxVotes = value;
							maxVoteIndex = thisVote;
							// get the index from the training data so we can get the image
							trainingDataIndex = nearest[j].getTrainingDataIndex();
						}
					}
					//if (j>0) {
					//	set1 += ", ";
					//	set2 += " + ";
					//}
					set1 += nearest[j].getProperty(i).toString() + " ";
					set2 += nearest[j].getProperty(i).getIndex() + " ";
				}
				//char chars[] = new char[KNN_FEATURE_LIST[i].length()];
				//Arrays.fill(chars, ' ');
				String placeholder = new String(" ");
				Feature feature;
				if (doScaleOutputs) {
					//Logger.out.println(KNN_FEATURE_LIST[i] + " = { " + set1 + " } / " + nearest.length);
					System.out.println(KNN_FEATURE_LIST[i] + " = { " + set1 + " } / " + nearest.length);
					logger.logInfo(KNN_FEATURE_LIST[i] + "={" + set1 + "}/" + nearest.length + " ");
					//Logger.out.println(placeholder + " = (" + set2 + ") / " + nearest.length);
					System.out.println(placeholder + " = (" + set2 + ") / " + nearest.length);
					logger.logInfo(KNN_FEATURE_LIST[i] + "=(" + set1 + ")/" + nearest.length + " ");
					sum /= nearest.length;
					feature = new Feature(sum, KNN_CLASSIFICATION_VALUE_LIST[0]);
					//Logger.out.println(placeholder + " = " + sum);
					logger.logInfo(placeholder + "=" + sum + "\n");
				} else {
					//Logger.out.println(KNN_FEATURE_LIST[i] + " = MAX( " + set1 + " )");
					System.out.println(KNN_FEATURE_LIST[i] + " MAX( " + set1 + " )");
					logger.logInfo(KNN_FEATURE_LIST[i] + " MAX( " + set1 + " )\n");
					feature = new Feature(maxVoteIndex, KNN_CLASSIFICATION_VALUE_LIST[0]);
				}
				currClass = feature.toString();
				//Logger.out.println(placeholder + " = " + feature);
				System.out.println("Classification" + " = " + currClass);
				logger.logInfo("Classification" + "=" + currClass + "\n");
			} else {
				newProperties[i] = properties[i];
			}
		}
		
		Situation newSituation = new Situation(newProperties);
		
		//Logger.out.println("\nExpected situation result:\n  " + newSituation);
		System.out.println("\nExpected situation result:\n " + newSituation);
		logger.logInfo("Expected situation result:" + newSituation + "\n***\n");
		
		busy(false);
		
		HashMap<String, Object> returnVal = new HashMap<String, Object>(2);
		returnVal.put("trainingDataIndex", new Integer(trainingDataIndex));
		returnVal.put("class", currClass);
		
		return returnVal;
	}

	/**
	 * Determines nearest <em>n</em> neighbors of a given situation.
	 * @param situation {@link Situation} to compare
	 * @param knn_neighbors Number of neighbors to return
	 * @return Nearest neighboring situations
	 */
	@SuppressWarnings("unchecked")
	private Situation[] getNearest(Situation qisituation, int knn_neighbors) {
		Situation[] nearest = new Situation[knn_neighbors];
		Map<Integer, Double> distances = new HashMap<Integer, Double>();
		System.out.println("Distance from " + qisituation + " to...");
		for (int i=0; i<dataSet.length; i++) {
			distances.put(i, measureDistance(qisituation, dataSet[i]));
			//Logger.out.println("..." + KnnSituation.getInput(i) + ": " + distances.get(i));
			System.out.println("... " + Situation.getInput(i) + ":" + distances.get(i));
		}
		
		// first one (n=0) is the overall nearest
		for (int n=0; n<knn_neighbors; n++) {
			double shortestDistance = -1.0f;
			int shortestIndex = 0;
			// Cycle through all distances
			Iterator it = distances.entrySet().iterator();
			while (it.hasNext()) {
				Entry<Integer, Double> distance = (Entry<Integer, Double>) it.next();
				if (distance.getValue() < shortestDistance || shortestDistance<0) {
					shortestDistance = distance.getValue();
					shortestIndex = distance.getKey();
				}
			}
			//highlight(shortestIndex);
			if (KNN_LOG_NEAREST) {
			//	Logger.out.println("Neighbor #" + (n+1) + ": " + Situation.getInput(shortestIndex) + ":\n  distance = " + shortestDistance + "\n  " + Knn.dataSet[shortestIndex].toString());
				System.out.println("Neighbor #" + (n+1) + ":" + Situation.getInput(shortestIndex) +
						":distance=" + shortestDistance + ":" + dataSet[shortestIndex].toString() +
						"STIM DB ID:" + dataSet[shortestIndex].getStimID());
				logger.logInfo("Neighbor #" + (n+1) + ":" + Situation.getInput(shortestIndex) +
						":distance=" + shortestDistance + ":" + dataSet[shortestIndex].toString() +
						"STIM DB ID:" + dataSet[shortestIndex].getStimID());
			}
			// Remove the current distance from the Map
			nearest[n] = dataSet[shortestIndex];
			distances.remove(shortestIndex);
		}
		return nearest;
	}

	/**
	 * Calculates either the Euclidean or absolute distance
	 * between two situations by calculating the distances
	 * between the individual values.
	 * @param situation1
	 * @param situation2
	 * @return Distance between the situations
	 */
	private static double measureDistance(Situation situation1, Situation situation2) {
		double result = 0.0f;
		
		for (int i=0; i<situation1.getProperties().length; i++) {
			if (!situation1.getProperty(i).isUnknown()) {
				if (doEuclidean) {
					// Euclidean distance
					result += Math.pow((situation1.getProperty(i).getIndex() - situation2.getProperty(i).getIndex(doRescale)), 2);
				} else {
					// Absolute distance
					result += Math.abs((situation1.getProperty(i).getIndex() - situation2.getProperty(i).getIndex(doRescale)));
				}
			}
		}
		if (doEuclidean) result = Math.pow(result, 0.5);
		return result;
	}

	/**
	 * Notifies jKNN that the application is busy.
	 * @param isBusy Whether or not the process is busy
	 */
	public void busy(boolean isBusy) {
		//Logger.out.progressSetVisible(isBusy);
		//System.out.println("Busy!");
	}

	/**
	 * Notifies jKNN that the application is busy.
	 * @param progress Progress of the process (0 to 100)
	 */
	public void busy(int progress) {
		//Logger.out.updateProgress(progress);
		System.out.println("Busy! - progress = " + progress);
	}

	/**
	 * Clears any log information.
	 */
	public void clear() {
		//Logger.out.clear();
		System.out.print("clear...");
	}

	/**
	 * Highlights a situation for visual comparison.
	 * @param index Index of situation in the dataset
	 */
	//public void highlight(int index) {}

}
