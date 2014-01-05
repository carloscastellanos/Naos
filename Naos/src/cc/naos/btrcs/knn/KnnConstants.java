/**
 * 
 */
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

import cc.naos.btrcs.knn.scenario.Classification;
import cc.naos.btrcs.knn.scenario.EEG;
import cc.naos.btrcs.knn.scenario.EMG;
import cc.naos.btrcs.knn.scenario.GSR;



/**
 * This interface contains the default structure and data,
 * including several default options. 
 * @author Paul Lammertsma
 * minor edits by Carlos Castellanos
 */

public interface KnnConstants
{
	public final static int KNN_NEIGHBORS = 5; 
	public final static boolean KNN_DISTANCE_EUCLIDEAN = true;
	public final static boolean KNN_DISTANCE_SCALED = true;
	public final static boolean KNN_SCALE_OUTPUTS = false;

	public final static boolean KNN_LOG_NEAREST = true;
	
	// Feature structure
	
	/**
	 * KnnSituation.id represents the array of input IDs
	 */
	public final static String KNN_DATASET_INPUTS = "STIMULI";
	
	public final static String KNN_DATASET_INPUT = "STIM - ";

	/**
	 * Format of feature list as it should be understood
	 * by KNN.
	 */
	public final static String[] KNN_FEATURE_LIST = {
		GSR.ID,
		EMG.ID,
		EEG.ID,
		Classification.ID
	};
	
	/**
	 * Format of classification list as it should be understood
	 * by KNN, containing arrays of {@link KnnValue}s.
	 */
	public final static KnnValue[][] KNN_CLASSIFICATION_VALUE_LIST = {
		Classification.VALUES
	};
	

}
