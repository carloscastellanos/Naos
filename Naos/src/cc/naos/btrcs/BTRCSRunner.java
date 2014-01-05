/**
 * 
 */
package cc.naos.btrcs;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
//import java.sql.Connection;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Collections;

import cc.naos.btrcs.data.StoredData;
import cc.naos.btrcs.knn.Knn;
import cc.naos.btrcs.knn.KnnValue;
import cc.naos.btrcs.knn.Situation;
import cc.naos.btrcs.knn.scenario.Classification;
import cc.naos.btrcs.knn.scenario.EEG;
import cc.naos.btrcs.knn.scenario.EMG;
import cc.naos.btrcs.knn.scenario.GSR;
import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import de.sciss.net.OSCPacket;
import de.sciss.net.OSCReceiver;
import de.sciss.net.OSCTransmitter;

/**
 * @author carlos
 *
 */
public class BTRCSRunner extends Thread implements OSCListener
{
	// db stuff
	private static ArrayList<HashMap<String,Object>> trainingData;
	private static ArrayList<HashMap<String,Object>> stimuli;
	private static ArrayList<HashMap<String,Object>> stimTechniques;
	private static ArrayList<HashMap<String,Object>> ethnicities;
	private static ArrayList<HashMap<String,Object>> ageGroups;
	private static ArrayList<HashMap<String,Object>> genders;
	private static ArrayList<HashMap<String,Object>> questions;
	private static ArrayList<HashMap<String,Object>> classes;
	private static ArrayList<HashMap<String,Object>> participants;
	private int participantID;
	private ArrayList<String> participantClassifications;
	private ArrayList<HashMap<String,Object>> preTestStimuli;
	private ArrayList<HashMap<String,Object>> preTestStimuliCopy;
	private static final String WHAT_YES_MEANS_SAME_RACE = "1 1 1 2 2 2 1 1 1 1";
	private static final String WHAT_YES_MEANS_DIFF_RACE = "2 2 2 1 1 1 2 2 2 2";
	// knn-related stuff
	private static Situation[] dataSet;
	private Knn knn;
	// --- OSC stuff --- //
    private OSCTransmitter oscTransmitter = null;
    private OSCReceiver oscReceiver = null;
    private InetSocketAddress sendAddr;
    private InetSocketAddress rcvAddr;
    private DatagramChannel dchSend = null;
    private DatagramChannel dchRcv = null;
    private int sendPort;
    private int receivePort;
    private String host = null;
    private String pathToNaosFolder = null;
    private boolean isConnected = false;
    private boolean preTest = true; // defaults to true since it's first
    private static final int preTestCount = 30;//10;
    //private static final int naosTestCount = 30;
    
    // OSC addresses
    // ["/naos", on/off] (integer)
    // start = 1, end = 0
    private static final String oscAddress = "/naos";
    
    // file path to stimulus ["/stimpath", path] - String
    // ["/stimpath", path, question] if it's the pre-test - String, String
    private static final String stimPathAddr = "/stimpath";
    
    // pre-test, yes or no (>0 = yes, else no) ["/pretest", yes/no] - integer
    private static final String preTestAddr = "/pretest";
    
    // request for stimuli
    // ["/stimrequest']
    private static final String stimReqAddr = "/stimrequest";
    
    // request for classification
    // ["/classrequest"]
    private static final String classReqAddr = "/classrequest";
    
    // ["/class" current class, rating(e.g. loyalty rating 0.0-1.0)]  int, float
    private static final String classAddr = "/class";
    
    // message representing the biodata in the form of:
    // ["/biodata", eeg, emg, gsr, stimpath, sendstim] - all integers except stimpath (string)
    // OR ["/biodata", eeg, emg, gsr, stimpath, question, answer, sendstim]
    // sendstim, whether to send another stim after this one 1=yes, 0=no
    // all integers except stimpath & question (String), answer 1=yes, 0=no
    // the latter is for the pre-test
    // receipt of this message will also trigger the sending the next stimpath
    private static final String bioAddr = "/biodata";
	
	public BTRCSRunner(String host, int sendport, int rcvport, int participantID, String pathToNaosFolder) {
    	this.host = host;
    	this.sendPort = sendport;
    	this.receivePort = rcvport;
    	this.participantID = participantID;
    	this.pathToNaosFolder = pathToNaosFolder;
		this.participantClassifications = new ArrayList<String>(30);
    	setupTest();
	}
	
	/* (non-Javadoc)
	 * @see de.sciss.net.OSCListener#messageReceived(de.sciss.net.OSCMessage, java.net.SocketAddress, long)
	 */
	public void messageReceived(OSCMessage msg, SocketAddress sender, long time) {
		// get the address pattern of the msg
		String oscMsg = msg.getName();
		
		// msg from Max indicating whether it's a pre-test or not
		if(oscMsg.equalsIgnoreCase(preTestAddr)) {
			 int preTestInt = ((Integer)msg.getArg(0)).intValue();
			 if(preTestInt > 0)
				 this.preTest = true;
			 else
				 this.preTest = false;
		// --- Max requests a stimulus filepath --- //
		} else if(oscMsg.equalsIgnoreCase(stimReqAddr)) {
			if(preTest) {
				sendPretestStimuli();
			} else { // not pre-test (thus naos test)
				sendNaostestStimuli();
			}		
		// --- Max gives bio data --- //
		// (thus we're doing classification or storing/creating training data)
		} else if(oscMsg.equalsIgnoreCase(bioAddr)) {
			System.out.println("Biodata received...");
			if(this.preTest) {
				evaluatePretestData(msg);
			} else { // we're doing the naos test
				evaluateNaostestData(msg);
			}
		// --- Max requests the classification --- //
		} else if(oscMsg.equalsIgnoreCase(classReqAddr)) {
			evaluateClassRequest();
		}
		
		InetSocketAddress addr = (InetSocketAddress) sender;
		System.out.println("=== OSC message received - " + oscMsg + 
					" received from: " + addr.getAddress() + ":" + addr.getPort() + " ===");
	}
	
	public void run() {
		System.out.println("*** BTRCSRunner started... ***");
		// Get socket via OSC/UDP
		while(!Thread.interrupted()) {
			if(!isConnected) {
				if(oscConnect() == false) {
					isConnected = false;
					System.out.println("Error making an OSC/UDP socket to " + this.host + ":" + sendPort);
					System.out.println("Will try again in 10 seconds");
					try {
						Thread.sleep(10000);  // retry in 10 secs
					} catch (InterruptedException iex) {
						System.out.println("D'oh!.  Thread " + this + " was interrupted: " + iex);
					}
				} else {
					isConnected = true;
				}
			}
		} // end while
		if(isConnected)
			oscDisconnect();
		
		System.out.println("*** BTRCSRunner stopped. ***");
	}
	
	private boolean oscConnect() {
		if(isConnected)
			return true;
		
		boolean success;
		try {
			InetAddress localhost = InetAddress.getLocalHost();
			dchRcv = DatagramChannel.open();
			dchSend = DatagramChannel.open();
			// assign an automatic local socket address
			rcvAddr = new InetSocketAddress(localhost, receivePort);
			sendAddr = new InetSocketAddress(this.host, sendPort);
			dchRcv.socket().bind(rcvAddr);
			oscReceiver = new OSCReceiver(dchRcv);
			oscReceiver.addOSCListener(this);
			oscReceiver.startListening();
			oscTransmitter = new OSCTransmitter(dchSend);
			OSCMessage connect = new OSCMessage(oscAddress, new Object[]{new Integer(1)});
			sendOSC(connect);
			System.out.println("Sending OSC message: " + oscAddress);
			System.out.println("*** OSC connection successful ***");
			success = true;
		} catch(IOException ioe) {
			System.out.println("*** OSC connection error! ***");
			System.out.println(ioe);
			success = false;
		}
		return success;
	}
	
	private void oscDisconnect() {
		// stop/close the OSC
		OSCMessage disconnect = new OSCMessage(oscAddress, new Object[]{new Integer(0)});
		sendOSC(disconnect);
		
		if(oscReceiver != null) {
			try {
				oscReceiver.stopListening();
            } catch(IOException e0) {
            }
        }
        if(dchRcv != null) {
        		try {
        			dchRcv.close();
        		} catch(IOException e1) {
        		}
        }
        if(dchSend != null) {
    		try {
    			dchSend.close();
    		} catch(IOException e2) {
    		}
        }
        System.out.println("*** OSC disconnection successful ***");
	}
	
	public void sendOSC(OSCPacket oscPacket) {
		if(oscTransmitter != null && sendAddr != null) {
			try {
				oscTransmitter.send(oscPacket, sendAddr);
				System.out.println("=== OSC Message sent:" + oscPacket.toString() + " ===");
			} catch(IOException ioe) {
				System.out.println("*** Error sending OSC/UDP message! *** " + ioe);
			}
		}
	}
	
	private void setupTest() {
		// set up the path to Naos folder
		StoredData.pathToNaosFolder = this.pathToNaosFolder;
		
		// retrieve entire training set from db
    	trainingData = StoredData.retrieveTrainingSet();
    	stimuli = StoredData.retrieveStimuli();
    	stimTechniques = StoredData.retrieveTechniques();
    	ethnicities = StoredData.retrieveEthnicities();
    	ageGroups = StoredData.retrieveAgeGroups();
    	genders = StoredData.retrieveGenders();
    	questions = StoredData.retrieveQuestions();
    	classes = StoredData.retrieveClasses();
    	participants = StoredData.retrieveParticipants();
    	// set-up preTest stimuli
    	setupPretestStimuli();
    	// set-up Naos test stimuli
    	setupNaostestStimuli();
    	// knn set-up
		dataSet = createSituations();
		this.knn = new Knn(dataSet);
	}

	// set up situations from the training data
	private Situation[] createSituations() {
		Situation[] dataSet = new Situation[trainingData.size()];
		KnnValue[][] dataSetSituations = new KnnValue[trainingData.size()][5];
		//Situation[] sits = new ArrayList<Situation>(trainingData.size());
		
		///ListIterator<HashMap<String, Object>> iter = trainingData.listIterator();
		for(int i=0; i<trainingData.size(); i++) {
			Integer intGSR = (Integer)(trainingData.get(i).get("gsr"));
			Integer intEMG = (Integer)(trainingData.get(i).get("emg"));
			Integer intEEG = (Integer)(trainingData.get(i).get("eeg"));
			Integer intClass = (Integer)(trainingData.get(i).get("class"));
			GSR gsr = new GSR(new KnnValue(intGSR.intValue(), "GSR"));
			EMG emg = new EMG(new KnnValue(intEMG.intValue(), "EMG"));
			EEG eeg = new EEG(new KnnValue(intEEG.intValue(), "EEG"));

			// create the dataset
			dataSetSituations[i] = new KnnValue[]{gsr.getValue(), emg.getValue(), eeg.getValue(), Classification.VALUES[intClass.intValue()-1]};
			dataSet[i] = new Situation(dataSetSituations[i]);
			// give the Situation the current training data index
			// and image db id
			dataSet[i].setTrainingDataIndex(i);
			HashMap<String, Object> hm = trainingData.get(i);
			dataSet[i].setStimID(((Integer)hm.get("id")).intValue());
		}
		return dataSet;
	}
	
	private void setupPretestStimuli() {
	   	// the list of pre-Test stimuli (capacity is preTestCount)
    	this.preTestStimuli = new ArrayList<HashMap<String,Object>>(preTestCount);
    	// the stimuli
    	ArrayList<HashMap<String,Object>> stimuliCopy = 
			new ArrayList<HashMap<String,Object>>(stimuli);
    	Collections.shuffle(stimuliCopy); // randomize so we get diff order each time
		// the questions
		ArrayList<HashMap<String,Object>> questionsCopy = 
			new ArrayList<HashMap<String,Object>>(questions);
		Collections.shuffle(questionsCopy); // randomize so we get diff order each time
		for(int i=0; i<preTestCount; i++) {
			HashMap<String,Object> tempH = stimuliCopy.get(i);
			HashMap<String,Object> tempQH;
			// add the question to the hashmap
			if(i<10) {
				tempQH = questionsCopy.get(i);
			} else {
				int rand = (int)((Math.random() *10) - 1);
				tempQH = questionsCopy.get(rand);
			}
			tempH.put("question", (String)tempQH.get("question"));
			this.preTestStimuli.add(new HashMap<String,Object>(tempH));
		}

		/* --- for when we have multiple visual techniques
		// load 10 stimuli filepath randomly from db (w/diff techniques)
		ListIterator<HashMap<String,Object>> stimIterator = stimuliCopy.listIterator();
		int prevTechnique = -1;
		int currIdx = 0;
		// we want 10 stimuli w/alternating techniques (e.g. 1 then 2 then 1, etc)
		// these loaded stimuli will not be used on the naos test
		while(stimIterator.hasNext() && this.preTestStimuli.size() < preTestCount) {
			HashMap<String,Object> tempH = stimIterator.next();
			int currTechnique = ((Integer)tempH.get("technique")).intValue();
			if(currTechnique == prevTechnique) {
				continue;
			} else {
				// add the question to the hashmap
				tempH.put("question", questionsCopy.get(currIdx++));
				// add hashmap to the arraylist
				this.preTestStimuli.add(new HashMap<String,Object>(tempH));
				prevTechnique = currTechnique;
			}
		}
		*/
		// preTestStimuliCopy will be used to send to Max
		// items will be removed as they are sent
		// (which is why we're making a copy)
		this.preTestStimuliCopy = new ArrayList<HashMap<String,Object>>(this.preTestStimuli);

		
		/*
		String queryTechnique1 = "SELECT * FROM stimuli WHERE stimtechnique=1 ORDER BY Random() LIMIT 5";
		String queryTechnique2 = "SELECT * FROM stimuli WHERE stimtechnique=2 ORDER BY Random() LIMIT 5";
		Connection connection = StoredData.getConnection();
		ArrayList<HashMap<String,Object>> tempList1 = new ArrayList<HashMap<String,Object>>(5);
		ArrayList<HashMap<String,Object>> tempList2 = new ArrayList<HashMap<String,Object>>(5);
		
		try {
			Statement statement = connection.createStatement();
			// technique 1 query
			ResultSet rs1 = statement.executeQuery(queryTechnique1);
			// technique 2 query
			ResultSet rs2 = statement.executeQuery(queryTechnique2);			
			// add the results to the ArrayLists
			while(rs1.next()) {
				HashMap<String, Object> hash = new HashMap<String, Object>(6);
				hash.put("id", new Integer(rs1.getInt("id")));
				hash.put("stimpath", rs1.getString("stimpath"));
				hash.put("technique", new Integer(rs1.getInt("stimtechnique")));
				hash.put("ethnicity", new Integer(rs1.getInt("ethnicity")));
				hash.put("age", new Integer(rs1.getInt("age")));
				hash.put("gender", new Integer(rs1.getInt("gender")));
				tempList1.add(hash);
			}
			while(rs2.next()) {
				HashMap<String, Object> hash = new HashMap<String, Object>(6);
				hash.put("id", new Integer(rs2.getInt("id")));
				hash.put("stimpath", rs2.getString("stimpath"));
				hash.put("technique", new Integer(rs2.getInt("stimtechnique")));
				hash.put("ethnicity", new Integer(rs2.getInt("ethnicity")));
				hash.put("age", new Integer(rs2.getInt("age")));
				hash.put("gender", new Integer(rs2.getInt("gender")));
				tempList2.add(hash);
			}
			
		} catch(SQLException se) {
			System.out.println(se.getMessage());
			se.printStackTrace();
		}
		
		// load 10 stimuli filepath randomly from db (w/diff techniques)
		int count = 0;
		int currTechnique = 1;
		// we want 10 stimuli w/alternating techniques
		// these loaded stimuli will not be used on the naos test
		while(count<5) {
			HashMap<String,Object> hash1 = tempList1.get(count);
			HashMap<String,Object> hash2 = tempList2.get(count);
			if(currTechnique == 1) { // if the techniques match
				this.preTestStimuli.add(new HashMap<String,Object>(hash1));
				// change the technique
				currTechnique = 2;
			} else {
				this.preTestStimuli.add(new HashMap<String,Object>(hash2));
				// change the technique
				currTechnique = 1;
				count++;
			}
		}
		*/
	}
	
	private void setupNaostestStimuli() {
		// remove from the training data and stimuli list
		// any stimuli that were in the pretest
		// if the stimpaths match remove it
		ListIterator<HashMap<String,Object>> ptsiter = this.preTestStimuli.listIterator();
		ListIterator<HashMap<String,Object>> tditer = trainingData.listIterator();
		while(ptsiter.hasNext()) {
			HashMap<String,Object> h = ptsiter.next();
			String sp = (String)h.get("stimpath");
			while(tditer.hasNext()) {
				HashMap<String, Object> ht = tditer.next();
				// if there is a match remove it from the training set
				if(((String)ht.get("stimpath")).equalsIgnoreCase(sp))
					if(trainingData.remove(ht))
						System.out.println();
					else
						System.out.println("*** Could not remove element form the training set ***");
			}
		}
		
	}
	
	private void sendPretestStimuli() {
	    // ["/stimpath", path, question]
		// make sure list is not empty
		if(this.preTestStimuliCopy.isEmpty() == false) {
			// remove an element from list and send it
			HashMap<String,Object> h = this.preTestStimuliCopy.remove(0);
			String stimFilePath = (String)(h.get("stimpath"));
			String question = (String)(h.get("question"));
			System.out.println("Sending pre-test stimuli...");
			// remove it from the stimuli list so it doesn't appear on Naos test
			stimuli.remove(h);

			OSCMessage stimPathMsg = 
				new OSCMessage(stimPathAddr, new Object[] {stimFilePath, question});
			sendOSC(stimPathMsg);
			System.out.println("Pre-test stimuli sent.");
		} else {
			System.out.println("*** No more pre-test stimuli left to send! ***");
			System.out.println("Is pre-test over?");
		}
	}
	
	// randomly selects a stimpath
	// only used for select first stimpath
	private void sendNaostestStimuli() {
		// ["/stimpath", path]
		int rand = (int)(Math.random() * (stimuli.size() - 1));
		// load 1 stimulus filepath randomly from db
		HashMap<String, Object> h = stimuli.get(rand);
		String stimFilePath = (String)h.get("stimpath");

		// send OSC message to Max to load stimulus 
		OSCMessage stimPathMsg = new OSCMessage(stimPathAddr, new Object[] {stimFilePath});
		sendOSC(stimPathMsg);
	}
	
	private void sendNaostestStimuli(String path) {
		// ["/stimpath", path]
		// load 1 stimulus filepath from db

		// send OSC message to Max to load stimulus 
		OSCMessage stimPathMsg = new OSCMessage(stimPathAddr, new Object[] {path});
		sendOSC(stimPathMsg);
	}
	
	private void evaluatePretestData(OSCMessage msg) {
		// ["/biodata", eeg, emg, gsr, stimpath, question, answer, sendstim]
		int eeg = ((Integer)(msg.getArg(0))).intValue();
		int emg = ((Integer)(msg.getArg(1))).intValue();
		int gsr = ((Integer)(msg.getArg(2))).intValue();
		String stimpath = (String)msg.getArg(3);
		String question = (String)msg.getArg(4); // text of the question
		int answer = ((Integer)(msg.getArg(5))).intValue();
		int sendstim = ((Integer)(msg.getArg(6))).intValue();
		
		// --- determine whether answer to question makes them loyal or disloyal --- //
		// get the question index
		ListIterator<HashMap<String, Object>> qiter = questions.listIterator();
		int qidx = 0; 
		while(qiter.hasNext()) {
			int idx = qiter.nextIndex();
			HashMap<String, Object> qh = qiter.next();
			String q = (String)qh.get("question");
			if(q.equalsIgnoreCase(question)) {
				qidx = idx; // this value goes in teh db (+1)
				break;
			}
		}
		// get the stimpath
		ListIterator<HashMap<String, Object>> pathiter = stimuli.listIterator();
		//String answersSameRace = "";
		//String answersDiffRace = "";
		int age = 0, ethnicity = 0, gender = 0;
		while(pathiter.hasNext()) {
			HashMap<String, Object> ph = pathiter.next();
			String path = (String)ph.get("stimpath");
			if(path.equalsIgnoreCase(stimpath)) {
				// get the answers strings
				//answersSameRace = (String)ph.get("answersSameRace");
				//answersDiffRace = (String)ph.get("answersDifferentRace");
				// get the age, ethnicity and gender of the stim
				age = ((Integer)ph.get("age")).intValue();
				ethnicity = ((Integer)ph.get("ethnicity")).intValue();
				gender = ((Integer)ph.get("gender")).intValue();
				break;
			}
		}
		
		// now get the participant's age, ethnicity & gender
		int participantAge = 0, participantEthnicity = 0, participantGender = 0;
		ListIterator<HashMap<String, Object>> partiter = participants.listIterator();
		while(partiter.hasNext()) {
			HashMap<String, Object> h = partiter.next();
			int pid = ((Integer)h.get("id")).intValue();
			if(pid == this.participantID) {
				participantAge = ((Integer)h.get("age")).intValue();
				participantEthnicity = ((Integer)h.get("ethnicity")).intValue();
				participantGender = ((Integer)h.get("gender")).intValue();
				break;
			}
		}
		// --------------------------------------------------------------------------- //
		// each answer to each question for each stimuli is separated by a space.
		// e.g. - 1 1 2 1 2 2 2 1 1 2 where 1 = loyal & 2 = disloyal
		// (which corresponds to DB id)
		// Use String.split() to get each answer
		// index in resulting array corresponds to the index in the questions ArrayList
		// --------------------------------------------------------------------------- //
		
		// convert to int - this is what the yes answer means (1 = loyal, 2 = disloyal)
		int yesAnswer = 0;
		int classification = 0;
		// get the answer to the question we want
		// if participant & stim are of same ethnicity
		if(participantEthnicity == ethnicity) {
			String[] resultSameRace = WHAT_YES_MEANS_SAME_RACE.split("\\s"); // regex signifying a space
			yesAnswer = Integer.parseInt(resultSameRace[qidx]);
		} else {
			// participant & stim are NOT of same ethnicity
			String[] resultDiffRace = WHAT_YES_MEANS_DIFF_RACE.split("\\s"); // regex signifying a space
			yesAnswer = Integer.parseInt(resultDiffRace[qidx]);
		}
		
		if(answer > 0) { // if answer is yes
			classification = yesAnswer;
		} else { // answer is no
			if(yesAnswer == 1) // yes answer means loyal
				classification = 2; // disloyal 'cause answer is no
			else // yesAnswer is 2, thus yes means disloyal
				classification = 1; // loyal 'cause answer is no
		}

		int stimloc = stimpath.lastIndexOf("/N") - 2;
		String newStimpath = stimpath.substring(stimloc, stimpath.length()-1);
		
		// --- write to the db (store all of this data) --- //
		String sql = "INSERT INTO data (stimpath, EEG, EMG, GSR, class, question, participant) VALUES " +
				"('" + newStimpath + "', " + "'" + eeg + "', " + "'" + emg + "', " + "'" + gsr + "', " 
				+ "'" + classification + "', " + "'" + question + "', " + "'" + this.participantID + "')";
		StoredData.write(sql);
		
		// send the next stim if necessary
		if(sendstim > 0)
			sendPretestStimuli();
	}
	
	private void evaluateNaostestData(OSCMessage msg) {
		// [ "/biodata", eeg, emg, gsr, stimpath, sendstim]
		int eeg = ((Integer)(msg.getArg(0))).intValue();
		int emg = ((Integer)(msg.getArg(1))).intValue();
		int gsr = ((Integer)(msg.getArg(2))).intValue();
		//String stimpath = (String)msg.getArg(3);
		int sendstim = ((Integer)(msg.getArg(4))).intValue();
		
		// read from training set (db) and do the KNN classification
		EEG qeeg = new EEG(new KnnValue(eeg, "EEG"));
		GSR qgsr = new GSR(new KnnValue(gsr, "GSR"));
		EMG qemg = new EMG(new KnnValue(emg, "EMG"));
							
		// ---------------- KNN Algorithm -------------------- //
		// give the Knn object the query instance situation
		// and run the evaluation
		HashMap<String, Object> vals = knn.evaluate(new Situation(
				new KnnValue[]{qgsr.getValue(), qemg.getValue(), qeeg.getValue(), Classification.UNKNOWN}));
		HashMap<String, Object> hm = 
			trainingData.get(((Integer)vals.get("trainingDataIndex")).intValue());
		
		// record the resulting classification
		this.participantClassifications.add((String)vals.get("class"));
		
		System.out.println("KNN Classification complete.");
		
		// send next stimpath (make sure it's not the current one?)
		// send the next stim if necessary
		if(sendstim > 0) {
			// determine next stim
			String nextStimpath = (String)hm.get("stimpath");
			sendNaostestStimuli(nextStimpath);
		}
		
	}
	
	// send back the current class as well as the loyalty rating
	private void evaluateClassRequest() {
		// get the current class
		String currClass = 
			this.participantClassifications.get(this.participantClassifications.size()-1);
		int loyalty = -1;
		// greater than five chracters means it's disloyal
		if(currClass.length() > 5)
			loyalty = 2;
		else
			loyalty = 1;
		
		// get the loyalty rating
		int loyalCount = 0;
		ListIterator<String> iter = this.participantClassifications.listIterator();
		while(iter.hasNext()) {
			String currentClass = iter.next();
			// if it's loyal
			if(!(currentClass.length() > 5))
				loyalCount++;
		}
		float loyaltyRating = (float)(loyalCount/this.participantClassifications.size());
		
		// send the current class and the overall loyalty rating
		OSCMessage classMsg = 
			new OSCMessage(classAddr, new Object[] {new Integer(loyalty), new Float(loyaltyRating)});
		sendOSC(classMsg);
		
	}
}
