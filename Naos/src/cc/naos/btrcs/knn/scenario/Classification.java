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
public class Classification extends Feature
{
	final static public String ID = "Classification";
	
	final static public KnnValue LOYAL = new KnnValue(0, "Loyal");
	final static public KnnValue DISLOYAL = new KnnValue(1, "Disloyal");
	
	final static public KnnValue[] VALUES = {
		LOYAL,
		DISLOYAL,
		UNKNOWN
	};
	
	public Classification(KnnValue value) {
		super(value);
	}
	
}
