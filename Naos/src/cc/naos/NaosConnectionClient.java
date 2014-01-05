/**
 * 
 */
package cc.naos;

import java.util.HashMap;

import de.sciss.net.OSCBundle;
import de.sciss.net.OSCMessage;



/**
 * @author carlos
 *
 * Connects ProComp to Max/MSP (or any UDP/OSC compatible client)
 */
public class NaosConnectionClient implements ProCompConnectionListener
{
	private static ProComp proComp;
	private String proCompHost = null; // host of app that reads from ProComp device
	//private int proCompPort = 20248; //default port num for above app
	private OSCHandler oscHandler;
	private String oscHost = null;
	private static final String gsrMsg = "/gsr";
	private static final String eegMsg = "/eeg";
	private static final String emgMsg = "/emg";
	//private int count = 0;
	
	public NaosConnectionClient(String pchost, String ohost) {
		this.proCompHost = pchost;
		this.oscHost = ohost;
	}
	
	public void proCompConnectionEvent(HashMap<String, Integer> data) {
		OSCBundle bundle = new OSCBundle();
			
		//OSCMessage gsr = new OSCMessage(gsrMsg, new Object[] {data.get("gsr")});
		OSCMessage eeg = new OSCMessage(eegMsg, new Object[] {data.get("eeg")});
		//OSCMessage emg = new OSCMessage(emgMsg, new Object[] {data.get("emg")});
			
		//bundle.addPacket(gsr);
		bundle.addPacket(eeg);
		//bundle.addPacket(emg);
	}
	
    private void init() {
    	// start ProComp
    	proComp = new ProComp(this.proCompHost);
    	//proComp.addProCompConnectionListener(batrcs);
		proComp.start();
		
		// add this object to the ProCompConnectionListner list
		// to trap ProComp events (e.g. network data)
		proComp.addProCompConnectionListener(this);
		
		// start the OSCHandler thread
		oscHandler = new OSCHandler(this.oscHost);
		oscHandler.setDaemon(true);
		oscHandler.start();
    }
    
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String pchost, ohost;
		if((args.length != 2)) {
			throw new IllegalArgumentException("Usage: NaosConnectionClient <procomp host> <osc host>");
		} else {
			pchost = args[0];
			ohost = args[1];
        }

		// NaosConectionClient
		NaosConnectionClient app;
		app = new NaosConnectionClient(pchost, ohost);
	
		// init the ProComp and OSC start reading bio-data
		System.out.println("Initializing...");
		app.init();
	}

}
