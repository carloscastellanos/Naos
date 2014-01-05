/**
 * 
 */
package cc.naos;
/**
 * @author carlos
 *
 */


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;
import de.sciss.net.OSCPacket;
import de.sciss.net.OSCReceiver;
import de.sciss.net.OSCTransmitter;
/**
 * @author carlos
 *
 */
class OSCHandler extends Thread
{	
	
    private OSCTransmitter oscTransmitter = null; // for sending messages to TEMP server (on port 12000)
    private OSCReceiver oscReceiver = null; // for receiving messages from TEMP
    private InetSocketAddress sendAddr;
    private InetSocketAddress rcvAddr;
    private DatagramChannel dchSend = null;
    private DatagramChannel dchRcv = null;
    private static final int sendPort = 44000;
    private static final int receivePort = 44001;
    //private static final String oscAddress = "/naos";
    private static final String tempConnect = "/connect" + Integer.toString(receivePort);
    private static final String tempDisconnect = "/disconnect" + Integer.toString(receivePort);
    private boolean isConnected = false;
    private String host = null;
    
    public OSCHandler(String host) {
    	this.host = host;
    }
    
	public void run() {
		System.out.println("*** OSCHandler started... ***");
		// Get socket to TEMP server via OSC/UDP
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
		
		System.out.println("*** OSCHandler stopped. ***");
	}
	
	public void sendOSC(OSCPacket oscPacket)
	{
		if(oscTransmitter != null && sendAddr != null) {
			try {
				oscTransmitter.send(oscPacket, sendAddr);
				System.out.println("=== OSC Message sent:" + oscPacket.toString() + " ===");
			} catch(IOException ioe) {
				System.out.println("*** Error sending OSC/UDP message! *** " + ioe);
			}
		}
	}
	
	private boolean oscConnect()
	{
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
			oscReceiver.addOSCListener(new OSCListener() {
				// listen for and accept incoming OSC messages
				public void messageReceived(OSCMessage msg, SocketAddress sender, long time)
				{
					// get the address pattern of the msg
					String oscMsg = msg.getName();
					InetSocketAddress addr = (InetSocketAddress) sender;
					System.out.println("=== OSC message received - " + oscMsg + 
							" received from: " + addr.getAddress() + ":" + addr.getPort() + " ===");

					System.out.println("OSC Message:"+oscMsg);
				}
			});
			oscReceiver.startListening();
			oscTransmitter = new OSCTransmitter(dchSend);
			OSCMessage connect = new OSCMessage(tempConnect, OSCMessage.NO_ARGS);
			sendOSC(connect);

			System.out.println("*** OSC connection successful ***");
			success = true;
		} catch(IOException ioe) {
			System.out.println("*** OSC connection error! ***");
			System.out.println(ioe);
			success = false;
		}
		return success;
	}
	
	private void oscDisconnect()
	{
		// stop/close the OSC
		OSCMessage disconnect = new OSCMessage(tempDisconnect, OSCMessage.NO_ARGS);
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
}

