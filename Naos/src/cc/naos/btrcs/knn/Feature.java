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

import cc.naos.btrcs.knn.KnnValue;


/**
 * Representation of a feature. Contains an array of {@link KnnValue}s.
 * <p>This class should be extended by a group of custom features.</p>
 * @author Paul Lammertsma
 * Minor edits by Carlos Castellanos
 */
public class Feature
{
	final static public String ID = "";
	
	/**
	 * This is a special reserved value.
	 */
	final static public KnnValue UNKNOWN = new KnnValue(-1, "??");
	
	/**
	 * The array of values this feature can accept. It should
	 * always allow for the reserved value <em>UNKNOWN</em> (-1).
	 */
	final static public KnnValue[] VALUES = {UNKNOWN};

	protected KnnValue value;
	
	/**
	 * Assigns the {@link KnnValue} to this feature.
	 * @param value
	 */
	public Feature(KnnValue value) {
		this.value = value;
	}
	
	/**
	 * Assigns a value to the feature from a numeric value.
	 * @param value A numeric representation of the value
	 */
	public Feature(double value) {
		this(value, VALUES);
	}
	
	/**
	 * Assigns a value to the feature from a numeric value
	 * from an alternative set of values.
	 * @param value A numeric representation of the value
	 * @param values An alternative set of values
	 */
	public Feature(double value, KnnValue[] values) {
		double shortestDistance = -1.0f;
		int shortestIndex = 0;
		for (int i=0; i<values.length; i++) {
			double distance = Math.abs((values[i].getIndex() - value));
			if (distance < shortestDistance || shortestDistance<0) {
				shortestDistance = distance;
				shortestIndex = i;
			}
		}
		this.value = values[shortestIndex];
	}
	
	/**
	 * Returns the currently set value of the feature.
	 * Returns <b>NULL</b> if value is unset.
	 * @return Value
	 */
	public KnnValue getValue() {
		return this.value;
	}
	
	/**
	 * Converts the feature to a String representation.
	 */
	public String toString() {
		if (this.value == null) return UNKNOWN.getID();
		return this.value.getID();
	}
}
