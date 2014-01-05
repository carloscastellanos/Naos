/**
 * 
 */
package cc.naos.btrcs.knn.scenario;

import cc.naos.btrcs.knn.Feature;
import cc.naos.btrcs.knn.KnnValue;

/**
 * @author carlos
 *
 */

public class EEG extends Feature
{
	final static public String ID = "EEG";
	
	// default values
	private static KnnValue EEG = new KnnValue(0, "EEG");

	public EEG(KnnValue value) {
		super(value);
	}

	public EEG(double value) {
		super(new KnnValue(value, "EEG"));
	}
	
	public EEG() {
		super(EEG);
	}
}


