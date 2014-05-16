package vavi.apps.shairport;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;

import org.apache.http.util.ByteArrayBuffer;

import android.util.Base64;
import android.util.Log;

import com.actionsmicro.airplay.crypto.FairPlay;


/**
 * An primitive RTSP responder for replying iTunes
 * 
 * @author bencall
 */
public class RTSPResponder extends Thread{

	private Socket socket;					// Connected socket
	private int[] fmtp;
	private byte[] aesiv, aeskey;			// ANNOUNCE request infos
	private AudioPlayer serv; 				// Audio listener
	byte[] hwAddr;
	private BufferedInputStream in;
	private static final Pattern completedPacket = Pattern.compile("(.*)\r\n\r\n");

	public RTSPResponder(byte[] hwAddr, Socket socket) throws IOException {
		this.hwAddr = hwAddr;
		this.socket = socket;
		in = new BufferedInputStream(socket.getInputStream());
	}


	public RTSPResponse handlePacket(RTSPPacket packet){
		// We init the response holder
		RTSPResponse response = new RTSPResponse("RTSP/1.0 200 OK");
		response.append("CSeq", packet.valueOfHeader("CSeq"));

		// Apple Challenge-Response field if needed
    	String challenge;
    	if( (challenge = packet.valueOfHeader("Apple-Challenge")) != null){
    		// BASE64 DECODE
    		byte[] decoded = Base64.decode(challenge, Base64.DEFAULT);

    		// IP byte array
    		//byte[] ip = socket.getLocalAddress().getAddress();
    		SocketAddress localAddress = socket.getLocalSocketAddress(); //.getRemoteSocketAddress();
    		    		
    		byte[] ip =  ((InetSocketAddress) localAddress).getAddress().getAddress();
    		
    		ByteArrayOutputStream out = new ByteArrayOutputStream();
    		// Challenge
    		try {
				out.write(decoded);
				// IP-Address
				out.write(ip);
				// HW-Addr
				out.write(hwAddr);

				// Pad to 32 Bytes
				int padLen = 32 - out.size();
				for(int i = 0; i < padLen; ++i) {
					out.write(0x00);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

    		 
    		// RSA
    		byte[] crypted = this.encryptRSA(out.toByteArray());
    		
    		// Encode64
    		String ret = Base64.encodeToString(crypted, Base64.DEFAULT);
    		
    		// On retire les ==
	        ret = ret.replace("=", "").replace("\r", "").replace("\n", "");

    		// Write
        	response.append("Apple-Response", ret);
    	} 
    	
		// Paquet request
		String REQ = packet.getReq();
        if(REQ.contentEquals("OPTIONS")){
        	// The response field
        	response.append("Public", "ANNOUNCE, SETUP, RECORD, PAUSE, FLUSH, TEARDOWN, OPTIONS, GET_PARAMETER, SET_PARAMETER, POST, GET");
        } else if (REQ.contentEquals("ANNOUNCE")){
        	response.append("Audio-Jack-Status", "connected; type=analog");
    		// Nothing to do here. Juste get the keys and values
        	Pattern p = Pattern.compile("^a=([^:]+):(.+)", Pattern.MULTILINE);
        	Matcher m = p.matcher(packet.getContent());
        	while(m.find()){
        		if(m.group(1).contentEquals("fmtp")){
        			// Parse FMTP as array
        			String[] temp = m.group(2).split(" ");
        			fmtp = new int[temp.length];
        			for (int i = 0; i< temp.length; i++){
        				try {
        					fmtp[i] = Integer.valueOf(temp[i]);
        				} catch (NumberFormatException e) {
        					e.printStackTrace();
        				}
        			}
        			
        		} else if(m.group(1).contentEquals("rsaaeskey")){
        			aeskey = this.decryptRSA(Base64.decode(m.group(2), Base64.DEFAULT));
        		} else if(m.group(1).contentEquals("aesiv")){
        			aesiv = Base64.decode(m.group(2), Base64.DEFAULT);        			
        		} else if (m.group(1).contentEquals("fpaeskey")) {
        			byte fpaeskey[] = Base64.decode(m.group(2), Base64.DEFAULT);
        			aeskey = new byte[16];
        			fpaeskey = FairPlay.decrypt(fpaeskey, fpaeskey.length);
        			System.arraycopy(fpaeskey, 0, aeskey, 0, 16);        			
        		}
        	}
        	
        } else if (REQ.contentEquals("SETUP")){
        	response.append("Audio-Jack-Status", "connected; type=analog");
    		int controlPort = 0;
        	int timingPort = 0;
        	
        	String value = packet.valueOfHeader("Transport");        	
        	
        	// Control port
        	Pattern p = Pattern.compile(";control_port=(\\d+)");
        	Matcher m = p.matcher(value);
        	if(m.find()){
        		controlPort =  Integer.valueOf(m.group(1));
        	}
        	
        	// Timing port
        	p = Pattern.compile(";timing_port=(\\d+)");
        	m = p.matcher(value);
        	if(m.find()){
        		timingPort =  Integer.valueOf(m.group(1));
        	}
            
        	// Launching audioserver
        	releaseAudioServer();
        	//TODO depends on RTSP meta
//			serv = new AudioServer(new AudioSession(aesiv, aeskey, fmtp, controlPort, timingPort));
        	serv = new com.actionsmicro.airplay.airtunes.AudioPlayer(new AudioSession(aesiv, aeskey, fmtp, controlPort, timingPort, socket.getInetAddress()));
			// ??? Why ???
        	response.append("Session", "DEADBEEF");

//        	response.append("Transport", packet.valueOfHeader("Transport") + ";server_port=" + serv.getServerPort());
        	response.append("Transport", String.format("RTP/AVP/UDP;unicast;mode=record;events;control_port=%d;server_port=%d", serv.getControlPort(), serv.getServerPort()));
        			
        } else if (REQ.contentEquals("RECORD")){
        	response.append("Audio-Jack-Status", "connected; type=analog");
    		
//        	Headers	
//        	Range: ntp=0-
//        	RTP-Info: seq={Note 1};rtptime={Note 2}
//        	Note 1: Initial value for the RTP Sequence Number, random 16 bit value
//        	Note 2: Initial value for the RTP Timestamps, random 32 bit value

        } else if (REQ.contentEquals("FLUSH")){
        	serv.flush();
        
        } else if (REQ.contentEquals("TEARDOWN")){
        	response.append("Connection", "close");
        	
        } else if (REQ.contentEquals("SET_PARAMETER")){
        	if (packet.getContent() != null) {
            	Pattern p = Pattern.compile("volume: (.+)");
        		Matcher m = p.matcher(packet.getContent());
        		if(m.find()){
        			//Audio volume can be changed using a SET_PARAMETER request. 
        			//The volume is a float value representing the audio attenuation in dB. A value of –144 means the audio is muted. 
        			//Then it goes from –30 to 0.
        			double volume = Double.parseDouble(m.group(1));
        			if (volume == -144) {
        				serv.setVolume(0);
        			} else {
        				serv.setVolume((volume+30)/30);
        			}
        		}
        	}
        }  else if (REQ.contentEquals("GET_PARAMETER")){
        	response.append("volume", "0");
        }
//        else if (REQ.contentEquals("POST") && packet.getDirectory().equals("/fp-setup")) {
//        	String content = packet.getContent();
//        	Log.d("ShairPort", "length:"+content.length());
//        	
//        } 
        else {
        	Log.e("ShairPort", "REQUEST(" + REQ + "): Not Supported Yet!");
//        	Log.d("ShairPort", packet.getRawPacket());
        }
        response.append("Server", "AirTunes/150.33");
		
    	// We close the response
    	response.finalize();
    	return response;
	}


	private void releaseAudioServer() {
		if (serv != null) {
			serv.stop();
			serv = null;
		}
	}

	/**
	 * Crypts with private key
	 * @param array	data to encrypt
	 * @return encrypted data
	 */
	public byte[] encryptRSA(byte[] array){
		try{

	        // Encrypt
	        Cipher cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding"); 
//	        cipher.init(Cipher.ENCRYPT_MODE, MainActivity.pk);
	        return cipher.doFinal(array);

		}catch(Exception e){
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Decrypt with RSA priv key
	 * @param array
	 * @return
	 */
	public byte[] decryptRSA(byte[] array){
		try{
			// La clef RSA

	        // Encrypt
	        Cipher cipher = Cipher.getInstance("RSA/NONE/OAEPPadding"); 
//	        cipher.init(Cipher.DECRYPT_MODE, MainActivity.pk);
	        return cipher.doFinal(array);
			
//			return FairPlay.fp_decrypt(array, array.length);

		}catch(Exception e){
			e.printStackTrace();
		}

		return null;
	}
	private static final Pattern requestHeaderPattern = Pattern.compile("^([\\w-]+):\\W(.+)\r\n", Pattern.MULTILINE);
    
    /**
     * Thread to listen packets
     */
	public void run() {
		try {
			ByteArrayBuffer requestBodyBuffer = new ByteArrayBuffer(4096);			
			do {
				Log.d("ShairPort", "listening packets ... ");
				requestBodyBuffer.clear();
				StringBuilder packet = new StringBuilder();
				int ret = readRequestHeader(packet);
				int contentLength = 0;
				if (ret != -1) {
					Log.d("ShairPort", "read body ... ");
					Matcher m = requestHeaderPattern.matcher(packet.toString());
				    contentLength = getContentLength(m);
				    ret = readRequestBody(contentLength, packet, requestBodyBuffer);
				} else {
					Log.d("ShairPort", "corrupt packet:"+packet.toString());
				}
				if (ret!=-1) {
					// We handle the packet
					RTSPPacket request = new RTSPPacket(packet.toString());
					Log.d("ShairPort", request.getRawPacket());	
					if (request.getReq().equals("POST") && 
							request.getDirectory().equals("/fp-setup")) {
						Log.d("ShairPort", "requestBodyBuffer.length:"+requestBodyBuffer.length());
						byte packageData[] = requestBodyBuffer.toByteArray();
						if (packageData[6] == 1) {
							FairPlay.init();
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							byte responseData[] = FairPlay.setupPhase1(requestBodyBuffer.buffer(), requestBodyBuffer.length(), true);
							Log.d("fairplay", "responseData.length:"+responseData.length);	
							RTSPResponse response = new RTSPResponse("RTSP/1.0 200 OK");
							response.append("Content-Type", "application/octet-stream");
							response.append("Content-Length", String.valueOf(responseData.length));
							response.append("Server", "AirTunes/150.33");
							response.append("CSeq", request.valueOfHeader("CSeq"));
							response.finalize();
							StringBuilder sb = new StringBuilder();
							sb.append(response.getRawPacket());
							// Write the response to the wire
							try {			
								socket.getOutputStream().write(sb.toString().getBytes());
								socket.getOutputStream().write(responseData);
								socket.getOutputStream().flush();
							} catch (IOException e) {
								e.printStackTrace();
							}
							Log.d("ShairPort", sb.toString());

						} else if (packageData[6] == 3) {
							byte responseData[] = FairPlay.setupPhase2(requestBodyBuffer.buffer(), requestBodyBuffer.length(), true);
							RTSPResponse response = new RTSPResponse("RTSP/1.0 200 OK");
							response.append("Content-Type", "application/octet-stream");
							response.append("Content-Length", String.valueOf(responseData.length));
							response.append("Server", "AirTunes/150.33");
							response.append("CSeq", request.valueOfHeader("CSeq"));
							response.finalize();
							StringBuilder sb = new StringBuilder();
							sb.append(response.getRawPacket());
//							CharArrayBuffer responseChars = new CharArrayBuffer(responseData.length);
//							responseChars.append(responseData, 0, responseData.length);
//							sb.append(responseChars);
							// Write the response to the wire
							try {			
//								BufferedWriter oStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//								oStream.write(sb.toString());
//								oStream.flush();
								socket.getOutputStream().write(sb.toString().getBytes());
								socket.getOutputStream().write(responseData);
								socket.getOutputStream().flush();
							} catch (IOException e) {
								e.printStackTrace();
							}
							Log.d("ShairPort", sb.toString());
						}
					} else {
						RTSPResponse response = this.handlePacket(request);		
						Log.d("ShairPort", response.getRawPacket());

						// Write the response to the wire
						try {			
							BufferedWriter oStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
							oStream.write(response.getRawPacket());
							oStream.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}

						if("TEARDOWN".equals(request.getReq())){
							releaseAudioServer();
							socket.close();
							socket = null;
						}
					}
				} else {
	    			socket.close();
	    			socket = null;
				}
			} while (socket!=null);
			
		} catch (IOException e) {
			e.printStackTrace();
			
		} finally {
			releaseAudioServer();
			
			try {
				if (in!=null) in.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (socket!=null) socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		Log.d("ShairPort", "connection ended.");
	}


	private int readRequestBody(int contentLength, StringBuilder packet,
			ByteArrayBuffer requestBodyBuffer) throws IOException {
		requestBodyBuffer.clear();
		int ret = 0;
		int readMore = contentLength;
		ByteBuffer buffer = ByteBuffer.allocate(512);
		while (readMore > 0) {
			buffer.clear();
			Log.d("ShairPort", "readMore:"+readMore);
			ret = in.read(buffer.array(), 0, Math.min(readMore, buffer.capacity()));
			if (ret!=-1) {
				Log.d("ShairPort", "readMore:ret:"+ret);
				packet.append(new String(buffer.array(), 0, ret));
				requestBodyBuffer.append(buffer.array(), 0, ret);
				readMore -= ret;
			} else {
				break;
			}
		}
		return ret;
	}


	private int getContentLength(Matcher m) {
		int contentLength = 0;
		while(m.find()){
			if (m.group(1).equalsIgnoreCase("Content-Length")) {
				contentLength = Integer.valueOf(m.group(2));
			}
		}
		return contentLength;
	}


	private int readRequestHeader(StringBuilder packet) throws IOException {
		int ret = 0;
		do {
			ret = in.read();
			if (ret != -1) {
				packet.append((char)ret);
			}
		} while (ret!=-1 && !completedPacket.matcher(packet.toString()).find());
		return ret;
	}
		
}