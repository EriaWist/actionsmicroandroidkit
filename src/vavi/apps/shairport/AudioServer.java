/**
 * The class that process audio data
 * 
 * @author bencall
 */

package vavi.apps.shairport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.commons.net.ntp.TimeStamp;

import com.actionsmicro.debug.DumpBinaryFile;
import com.actionsmicro.utils.Log;


/**
 * Main class that listen for new packets.
 * 
 * @author bencall
 */
public class AudioServer implements UDPDelegate, AudioPlayer{
	// Constantes
	public static final int BUFFER_FRAMES = 512;	// Total buffer size (number of frame)
	public static final int START_FILL = 282;		// Alac will wait till there are START_FILL frames in buffer
	public static final int MAX_PACKET = 2048;		// Also in UDPListener (possible to merge it in one place?)
	private static final boolean DEBUG_LOG = false;
	private static final String TAG = "AudioServer";
	
	// Sockets
	private DatagramSocket sock, csock;
	private UDPListener l1;
   
	// client address
	private InetAddress rtpClient;

	// Audio infos and datas
	private AudioSession session;
	private AudioBuffer audioBuf;

    // The audio player
	private PCMPlayer player;
	private UDPListener controlPortListener;
	protected long rtpTimestamp;
	protected long ntpTimestamp;
	private DumpBinaryFile debugFile;
    
    /**
     * Constructor. Initiate instances vars
     * @param aesiv
     * @param aeskey
     * @param fmtp
     * @param controlPort
     * @param timingPort
     */
	public AudioServer(AudioSession session){		
		// Init instance var
		this.session = session;
		
		// Init functions
		audioBuf = new AudioBuffer(session, this);
		this.initRTP();
		player = new PCMPlayer(session, audioBuf);
		player.start();
//		try {
//			debugFile = new DumpBinaryFile("/sdcard/audio.debug");
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
	}

	public void stop(){
		player.stopThread();
		l1.stopThread();
		if (controlPortListener != null) {
			controlPortListener.stopThread();
		}
		if (sock != null) {
			sock.close();
		}
		if (csock != null) {
			csock.close();
		}
		if (debugFile != null) {
			try {
				debugFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			debugFile = null;
		}
	}
	
	public void setVolume(double vol){
		player.setVolume(vol);
	}
	
	/**
	 * Return the server port for the bonjour service
	 * @return
	 */
	public int getServerPort() {
		return sock.getLocalPort();
	}
	
	/**
	 * Opens the sockets and begin listening
	 */
	private void initRTP(){
		try {
			sock = new DatagramSocket();
			l1 = new UDPListener(sock, this);
			csock = new DatagramSocket();
			controlPortListener = new UDPListener(csock, new UDPDelegate() {

				@Override
				public void packetReceived(DatagramSocket socket,
						DatagramPacket packet) {					
					ByteBuffer packetBuffer = ByteBuffer.wrap(packet.getData());
					packetBuffer.order(ByteOrder.BIG_ENDIAN);
					packetBuffer.position(1);
					byte type = (byte) (packetBuffer.get()&~0x80);
					debugLog("control port: data type:"+type);
					if (type == 84) { //Sync packets
						packetBuffer.position(4);						
						long timestamp = (packetBuffer.getInt() & 0xffffffffL);
						long ntpTime = packetBuffer.getLong();
						long nextTimestamp = (packetBuffer.getInt() & 0xffffffffL);
						synchronized(AudioServer.this) {
							rtpTimestamp = timestamp;
							ntpTimestamp = TimeStamp.getTime(ntpTime);
						}
						debugLog("control port: timestamp:"+timestamp+", ntpTime:"+ntpTimestamp+", nextTimestamp:"+nextTimestamp);
					} else if (type == 86) {
						packetBuffer.position(6);						
						int seqno = (packetBuffer.getShort() & 0xffff);
						int payloadSize = packet.getLength() - 16;
						byte[] pktp = new byte[payloadSize];
						packetBuffer.position(16);						
						packetBuffer.get(pktp, 0, payloadSize);
						audioBuf.putPacketInBuffer(seqno, pktp);
						Log.d(TAG, "control port: retransmit reply: seqno:"+seqno+", remaining:"+packetBuffer.remaining());
						if (debugFile != null) {
							try {
								debugFile.writeToFile(packet.getData());
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						
					} else {
						Log.w(TAG, "control port: unhandled control packet type:"+type);
					}
				}
				
			});
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * When udpListener gets a packet
	 */
	public void packetReceived(DatagramSocket socket, DatagramPacket packet) {
		this.rtpClient = packet.getAddress();		// The client address
		
		int type = packet.getData()[1] & ~0x80;
		if (type == 0x60 || type == 0x56) { 	// audio data / resend
			// Decale de 4 bytes supplementaires
			int off = 0;
			if(type==0x56){
				off = 4;
			}
			
			//seqno is on two byte
			int seqno = ((packet.getData()[2+off] & 0xff)*256 + (packet.getData()[3+off] & 0xff));
			if (type==0x56){
				Log.d(TAG, "retransmit reply: seqno:"+seqno);
			}
			// + les 12 (cfr. RFC RTP: champs a ignorer)
			byte[] pktp = new byte[packet.getLength() - off - 12];
			for(int i=0; i<pktp.length; i++){
				pktp[i] = packet.getData()[i+12+off];
			}
			debugLog("packetReceived:"+seqno+", size:"+pktp.length);
			
			audioBuf.putPacketInBuffer(seqno, pktp);
		}
	}
	
	
	/**
	 * Ask iTunes to resend packet
	 * FUNCTIONAL??? NO PROOFS
	 * @param first
	 * @param last
	 */
	public void request_resend(int first, int last) {
		Log.d(TAG, "Resend Request: " + first + "::" + last);
		if(last<first){
			return;
		}
		
		int len = last - first + 1;
	    byte[] request = new byte[] { (byte) 0x80, (byte) (0x55|0x80), 0x01, 0x00, (byte) ((first & 0xFF00) >> 8), (byte) (first & 0xFF), (byte) ((len & 0xFF00) >> 8), (byte) (len & 0xFF)};
	    
		try {
			DatagramPacket temp = new DatagramPacket(request, request.length, rtpClient, session.getControlPort());
			csock.send(temp);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	/**
	 * Flush the audioBuffer
	 */
	public void flush(){
		audioBuf.flush();
	}

	@Override
	public int getControlPort() {
		return csock.getLocalPort();
	}
	private void debugLog(String msg) {
		if (DEBUG_LOG) {
			Log.d(TAG, msg);
		}
	}
	private void debugLogW(String msg) {
		if (DEBUG_LOG) {
			Log.w(TAG, msg);
		}
	}
}
