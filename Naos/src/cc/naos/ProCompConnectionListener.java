/**
 * 
 */
package cc.naos;

import java.util.EventListener;
import java.util.HashMap;

/**
 * @author carlos
 *
 */
public interface ProCompConnectionListener extends EventListener
{
	public abstract void proCompConnectionEvent(HashMap<String, Integer> data);
}
