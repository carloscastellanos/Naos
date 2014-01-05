/**
 * 
 */
package cc.naos;

/**
 * @author carlos
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

/**
 * @author carlos
 *
 */

// Class for reading from Thought Technology ProComp/FlexComp devices

public class ProComp implements Runnable
{
	private String host = null;
	private static final int port = 20248;
	private Thread runner = null; // we're running this class as a thread
	private boolean running = false;
	private Socket socket;
    private BufferedReader in;
    //private BufferedWriter out;
    
    // biofeedback values
    private static int gsr = 0;
    private static int eeg = 0;
    private static int emg = 0;
    
    // baseLine values
	private static int gsrBaseline = 0;
	private static int emgBaseline = 0;
	private static int eegBaseline = 0;
	
	// deviation values
	/*
	private static int gsrDev = 0;
	private static int emgDev = 0;
	private static int eegDev = 0;
    */
	
	// ranges and minimums
	private static final int EMG_RANGE = 8000;
	private static final int EEG_RANGE = 17000;
	private static final int GSR_RANGE = 1000;
	private static final int EIGHT_BIT = 255;
	
	private static final int GSR_MIN = -3605;
	
    private static boolean userConnected = false;
    private static boolean baselinesEstablished = false;
    
    // store a list of all the current event listeners for this class (will probably only be one)
	static ArrayList<ProCompConnectionListener> listeners = new ArrayList<ProCompConnectionListener>();
    // store the bio-data in a HashMap
	static HashMap<String, Integer> bioData = new HashMap<String, Integer>(1);
	
    // constructors
	public ProComp(String host) {
		this.host = host;
    }
    
    // open an input and output stream and start a new Thread
    public synchronized void start() {
    	if(runner == null) {
            try
            {
            	socket = new Socket(host, port);
            	in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            	//out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            }
            catch (IOException ex) 
            {
            	System.out.println("Socket or other I/O error!");
            	if(!(socket == null)) {
            		try {
            			socket.close();
            		} catch(IOException ioe) {
            			System.out.println(ioe);
            		}
            		socket = null;
    			}
            }
    		runner = new Thread(this);
    		runner.setPriority(Thread.MAX_PRIORITY);
    		running = true;
    		runner.start();
    		//userConnected = true;
    		System.out.println("*** ProComp client started. ***");
    	}
    }
    
    // interrupt the runner Thread and close the i/o streams
    public synchronized void stop() {
    	if(runner != null) {
    		if(runner != Thread.currentThread())
    			runner.interrupt();
    		runner = null;
    	}
    	try {
    		socket.close();
    	} catch(IOException ignored) {
    		
    	}
		running = false;
		userConnected = false;
    	System.out.println("*** ProComp client stopped. ***");
    }
    
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			while(running && socket.isConnected()) {
				// read the data from the socket
				String line = in.readLine();
				System.out.println("line="+line);
				if(line != null && line.indexOf("IP") < 0) {
					
					// get the channel
					String chan = line.substring(0, 1);
					// get the absolute value of the numeric portion of the string
					int val = Integer.parseInt(line.substring(1));
					// assign the values
					if(chan.equalsIgnoreCase("A")) {
						// A = EEG
						//eeg = Math.abs(val);
						eeg = val;
						// put the bio-data in the HashMap and send the event notification
						bioData.put("eeg", new Integer(val));
						notifyProCompConnectionListeners();
						System.out.println("EEG="+eeg);
					} else if(chan.equalsIgnoreCase("B")) {
						// B = EMG
						//emg = Math.abs(val);
						emg = val;
						// put the bio-data in the HashMap and send the event notification
						bioData.put("emg", new Integer(val));
						notifyProCompConnectionListeners();
						System.out.println("EMG="+emg);
					} else {
						// C - H = GSR
						gsr = val; // this can't be an abs value
						// put the bio-data in the HashMap and send the event notification
						bioData.put("gsr", new Integer(val));
						notifyProCompConnectionListeners();
						System.out.println("GSR="+gsr);
					}
					
					//eeg = Integer.parseInt(line);
					//System.out.println("EEG="+eeg);
				
					/*
					// -------- Baseline readings & User connection -------- //
					// establish baseline readings before telling the rest of
					// the app that the user is connected
					// ----------------------------------------------------- //
					if(!baselinesEstablished) 
						establishBaselines();
				
					// if user is connected check for disconnection
					// else check for connection
					if(userConnected) {
						if(gsr <= GSR_MIN) {
							userConnected = false;
							baselinesEstablished = false;
							notifyProCompConnectionListeners();
							System.out.println("*** User disconnected from ProComp ***");
						}
					} else {
						if(gsr > GSR_MIN + 5) {
							if(baselinesEstablished) {
								userConnected = true;
								notifyProCompConnectionListeners();
								System.out.println("*** ProComp connection established. ***");
							}
						}
					}
					*/
				} //end if(line != null)
			} // end while
		} catch(IOException e) {
			this.stop();
			System.out.println(e);
		}
		//stop();
	}
	
    // --- GSR --- //
    /**
     * return "raw" gsr
     * @return gsr "raw" gsr
     */
    public static int getGSR() {
    	return gsr;
    }
    
    /**
     * return gsr deviation from baseline reading (i.e. - peaks)
     * @return gsrDev gsr deviation from baseline reading (i.e. - peaks)
     */
    private static int[] gsrDevReadings = new int[20];
    public static int getGSRDeviation() {
    	while(!baselinesEstablished) {
    		System.out.println("Baselines not yet established! Trying again...");
    	}

    	for(int i = 0; i < gsrDevReadings.length; i++) {
    		gsrDevReadings[i] = gsr;
    	}
    	
    	double avg = averageArray(gsrDevReadings, gsrDevReadings.length);
    	double map = ( (avg - gsrBaseline) / (GSR_RANGE - (gsrBaseline - GSR_MIN)) ) * EIGHT_BIT;
    	if(map < 0)
    		return 0;
    	return (int)(Math.round(map));
    }
  
    // --- EEG --- //
    /**
     * return "raw" eeg
     * @return eeg "raw" eeg
     */
    public static int getEEG() {
    	return eeg;
    }

    /**
     * return eeg deviation from baseline reading (i.e. - peaks)
     * @return eegDev eeg deviation from baseline reading (i.e. - peaks)
     */
    private static int[] eegDevReadings = new int[20];
    public static int getEEGDeviation() {
    	while(!baselinesEstablished) {
    		System.out.println("Baselines not yet established! Trying again...");
    	}

    	for(int i = 0; i < eegDevReadings.length; i++) {
    		eegDevReadings[i] = eeg;
    	}
    	
    	// have to map the ProComp's 11-bit readings (0-2047) to 8-bit (0-255)
    	// (and also subtract the baseline)
    	double avg = averageArray(eegDevReadings, eegDevReadings.length);
    	double map = ( (Math.abs(avg - eegBaseline)) / (EEG_RANGE - eegBaseline) ) * EIGHT_BIT;
    	return (int)(Math.round(map));
    }
    
    // --- EMG --- //
    /**
     * returns "raw" emg
     * @return emg "raw" emg
     */
    public static int getEMG() {
    	return emg;
    }
    
    /**
     * return emg deviation from baseline reading (i.e. - peaks)
     * @return emgDev emg deviation from baseline reading (i.e. - peaks)
     */
    private static int[] emgDevReadings = new int[20];
    public static int getEMGDeviation() {
    	while(!baselinesEstablished) {
    		System.out.println("Baselines not yet established! Trying again...");
    	}

    	for(int i = 0; i < emgDevReadings.length; i++) {
    		emgDevReadings[i] = emg;
    	}
    	
    	// have to map the ProComp's 11-bit readings (0-2047) to 8-bit (0-255)
    	// (and also subtract the baseline)
    	double avg = averageArray(emgDevReadings, emgDevReadings.length);
    	double map = ( (Math.abs(avg - emgBaseline)) / (EMG_RANGE - emgBaseline) ) * EIGHT_BIT;
    	return (int)(Math.round(map));
    }
    
    public boolean isRunning() {
    	return running;
    }
    
    // establish baseline readings from which to measure differences (i.e. - peaks)
    private void establishBaselines() {
    	if(establishGSRBaseline() && establishEMGBaseline() && establishEEGBaseline()) {
    		baselinesEstablished = true;
    		System.out.println("--- Baselines established! ---");
    	}
    }
    
    private static int gsrCount = 0;
    private static int[] gsrReadings = new int[100];
	private static boolean gsrFlag = false;
    private boolean establishGSRBaseline() {
    	if(gsr <= GSR_MIN)
    		return false;
    		
    	gsrReadings[gsrCount] = gsr;
    	gsrCount++;
    	
    	// if the array is filled up
    	if(gsrCount >= gsrReadings.length) {
    		gsrCount = 0;
    		gsrFlag = true; // can now calculate
    	}
    	
    	if(gsrFlag) {
    		gsrBaseline = (int)(Math.round(averageArray(gsrReadings, gsrReadings.length)));
    		return true;
    	} else {
    		return false;
    	}
    }
    
    private static int emgCount = 0;
    private static int[] emgReadings = new int[100];
	private static boolean emgFlag = false;
    private boolean establishEMGBaseline() {
    	if(emg <= 1)
    		return false;
    	
    	emgReadings[emgCount] = emg;
    	emgCount++;
    	
    	// if the array is filled up
    	if(emgCount >= emgReadings.length) {
    		emgCount = 0;
    		emgFlag = true; // can now calculate
    	}
    	
    	if(emgFlag) {
    		emgBaseline = (int)(Math.round(averageArray(emgReadings, emgReadings.length)));
    		return true;
    	} else {
    		return false;
    	}
    }
    
    private static int eegCount = 0;
    private static int[] eegReadings = new int[100];
	private static boolean eegFlag = false;
    private boolean establishEEGBaseline() {
    	if(eeg <= 1)
    		return false;
    	
    	eegReadings[eegCount] = eeg;
    	eegCount++;
    	
    	// if the array is filled up
    	if(eegCount >= eegReadings.length) {
    		eegCount = 0;
    		eegFlag = true; // can now calculate
    	}
    	
    	if(eegFlag) {
    		eegBaseline = (int)(Math.round(averageArray(eegReadings, eegReadings.length)));
    		return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * adds a listener to trap data events from this class
     */
    public synchronized void addProCompConnectionListener(ProCompConnectionListener pccl) {
        if(pccl != null && listeners.indexOf(pccl) == -1) {
            listeners.add(pccl);
            System.out.println("[+ ProCompConnectionListener] " + pccl);
        }
    }

    /**  
     * removes a listener from this class
     */
    public synchronized void removeProCompConnectionListener(ProCompConnectionListener pccl) {
        if(listeners.contains(pccl)) {
            listeners.remove(listeners.indexOf(pccl));
            System.out.println("[- ProCompConnectionListener] " + pccl);
        }
    }
    
	/**
	 * let everyone know a ProComp connection event was received
	 */
	private void notifyProCompConnectionListeners() {
	    if(listeners == null) {
	        return;
        } else {
            ListIterator<ProCompConnectionListener> iter = listeners.listIterator();
            while(iter.hasNext()) {
                iter.next().proCompConnectionEvent(bioData);
            }
        }
    }
	
	// average the values in an array: 
	private static double averageArray(int[] intArray, int divisor) {
	  int total = 0;
	  double average = 0.0;
	  for (int i = 0; i < intArray.length; i++) { 
	    total = total + intArray[i];
	  }
	  average = total/divisor;
	  return average;
	}
	
}
