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
public class GSR extends Feature
{
	final static public String ID = "GSR";
	
	// default values
	private static KnnValue GSR = new KnnValue(0, "GSR");

	public GSR(KnnValue value) {
		super(value);
	}

	public GSR(double value) {
		super(new KnnValue(value, "GSR"));
	}
	
	public GSR() {
		super(GSR);
	}
	
}
