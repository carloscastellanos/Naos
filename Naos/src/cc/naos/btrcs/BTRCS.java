/**
 * 
 */
package cc.naos.btrcs;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.IOException;


/**
 * @author carlos
 *
 */
public class BTRCS
{
	private static BTRCSRunner runner;

    public static void main(String[] args) throws IOException {
		// Setup custom event trapping (to trap escape key globally)
		// NOTE: this won't work with MS Jview (comment out if running in that JRE)
		// NOTE: this interferes with Alt-F4 key (OS key to close window)
		// thanks to Mark Napier for this code
		EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();
		eq.push(new BTRCSEventQueue());
		
		String host;
		int sendport;
		int rcvport;
		int participantID;
		String pathToNaos = "";
		if((args.length != 5)) {
			throw new IllegalArgumentException
			("Usage: BTRCS <host> <send port> <receive port> <participantID> <path to Naos folder>");
		} else {
			host = args[0];
			sendport = Integer.parseInt(args[1]);
			rcvport = Integer.parseInt(args[2]);
			participantID = Integer.parseInt(args[3]);
			pathToNaos = args[4];
        }
		runner = new BTRCSRunner (host, sendport, rcvport, participantID, pathToNaos);
		runner.start();
    }
}
