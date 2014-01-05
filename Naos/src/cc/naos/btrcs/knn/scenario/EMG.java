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
public class EMG extends Feature
{
	final static public String ID = "EMG";
	
	// default values
	private static KnnValue EMG = new KnnValue(0, "EMG");

	public EMG(KnnValue value) {
		super(value);
	}
	
	public EMG(double value) {
		super(new KnnValue(value, "EMG"));
	}
	
	public EMG() {
		super(EMG);
	}
}
