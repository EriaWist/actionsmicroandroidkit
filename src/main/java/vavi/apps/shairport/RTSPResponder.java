package vavi.apps.shairport;

import android.util.Base64;

import com.actionsmicro.airplay.AirPlayServer;
import com.actionsmicro.airplay.airtunes.daap.DaapDataParser;
import com.actionsmicro.airplay.airtunes.daap.Item;
import com.actionsmicro.airplay.crypto.EzAes;
import com.actionsmicro.airplay.crypto.FairPlay;
import com.actionsmicro.utils.Log;
import com.dd.plist.BinaryPropertyListParser;
import com.dd.plist.BinaryPropertyListWriter;
import com.dd.plist.NSArray;
import com.dd.plist.NSData;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.NSString;
import com.dd.plist.PropertyListFormatException;

import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;

import static com.actionsmicro.airplay.AirPlayServer.AIRPLAYER_VERSION_STRING;
import static com.actionsmicro.airplay.AirPlayServer.AIRPLAY_MIRROR;
import static com.actionsmicro.airplay.AirPlayServer.AIRPLAY_VIDEO_ON_MIRROR;
import static com.actionsmicro.airplay.AirPlayServer.airplayState;
import static com.actionsmicro.airplay.AirPlayServer.mEdPublicKey;
import static com.actionsmicro.airplay.AirPlayServer.mEdSecretKey;

/**
 * An primitive RTSP responder for replying iTunes
 *
 * @author bencall
 */
public class RTSPResponder extends Thread{

	private byte[] eiv;
	private byte[] fpaeskey;
	private int timingPort;
	private String model="";


	public interface AirTunesListener {

		void onDisconnected();

		void onReceiveMeta(String albumName, String artist, String title);

		void onReceiveCoverArt(byte[] byteArray);

	}


	private Socket socket;					// Connected socket
	private byte[] aesiv, aeskey;			// ANNOUNCE request infos
	private AudioPlayer serv; 				// Audio listener
	byte[] hwAddr;
	private BufferedInputStream in;
	private String fmtpString;
	private static final Pattern completedPacket = Pattern.compile("(.*)\r\n\r\n");
	private static final String TAG = "RTSPResponder";
	private AirTunesListener airTunesListener;
	private boolean shouldStop;
	public RTSPResponder(byte[] hwAddr, Socket socket) throws IOException {
		this.hwAddr = hwAddr;
		this.socket = socket;
		in = new BufferedInputStream(socket.getInputStream());
	}


	public RTSPResponse handlePacket(RTSPPacket packet, ByteArrayBuffer requestBodyBuffer){
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
        			fmtpString = m.group(2);        			
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
        } else if (REQ.contentEquals("RECORD")){
        	response.append("Audio-Jack-Status", "connected; type=analog");
    		
//        	Headers	
//        	Range: ntp=0-
//        	RTP-Info: seq={Note 1};rtptime={Note 2}
//        	Note 1: Initial value for the RTP Sequence Number, random 16 bit value
//        	Note 2: Initial value for the RTP Timestamps, random 32 bit value

        } else if (REQ.contentEquals("FLUSH")){
        	if (serv != null) {
        		serv.flush();
        	}
        } else if (REQ.contentEquals("TEARDOWN")){
        	response.append("Connection", "close");
        	
        } else if (REQ.contentEquals("SET_PARAMETER")){
        	String contentType = packet.valueOfHeader("Content-Type");
        	if (contentType != null) {
        		if (contentType.equalsIgnoreCase("text/parameters")) {
        			if (packet.getContent() != null) {
        				Pattern p = Pattern.compile("volume: (.+)");
        				Matcher m = p.matcher(packet.getContent());
        				if(m.find()){
        					//Audio volume can be changed using a SET_PARAMETER request. 
        					//The volume is a float value representing the audio attenuation in dB. A value of –144 means the audio is muted. 
        					//Then it goes from –30 to 0.
        					double volume = Double.parseDouble(m.group(1));
        					if (volume == -144) {
        						if (serv != null) {
        				        	serv.setVolume(0);
        						}
        					} else {
        						if (serv != null) {
        				        	serv.setVolume((volume+30)/30);
        						}
        					}
        				}
        			}
        		} else if (contentType.equalsIgnoreCase("application/x-dmap-tagged")) {
        			byte[] daapData = requestBodyBuffer.toByteArray();
        			Item daapItem = DaapDataParser.parse(daapData);
        			String albumName = daapItem.getChildDataAsString(0x6173616C);
        			Log.d(TAG, "daapItem:album name:"+albumName);
        			String artist = daapItem.getChildDataAsString(0x61736172);
        			Log.d(TAG, "daapItem:artist:"+artist);
        			String title = daapItem.getChildDataAsString(0x6D696E6D);
        			Log.d(TAG, "daapItem:song name:"+title);
        			if (airTunesListener != null) {
        				airTunesListener.onReceiveMeta(albumName, artist, title);
        			}
        		} else if (contentType.equalsIgnoreCase("image/jpeg")) {
        			if (airTunesListener != null) {
        				airTunesListener.onReceiveCoverArt(requestBodyBuffer.toByteArray());
        			}
        		} else if (contentType.equalsIgnoreCase("image/none")) {
        			if (airTunesListener != null) {
        				airTunesListener.onReceiveCoverArt(null);
        			}
        		}
        	}
        }  else if (REQ.contentEquals("GET_PARAMETER")){
        	response.appendBody("volume", "-15");
        }
//        else if (REQ.contentEquals("POST") && packet.getDirectory().equals("/fp-setup")) {
//        	String content = packet.getContent();
//        	Log.d(TAG, "length:"+content.length());
//        	
//        } 
        else {
			Log.e(TAG, "REQUEST(" + REQ + "): Not Supported Yet!");
//			Log.d(TAG, packet.getRawPacket());
		}
		response.append("Server", "AirTunes/" + AIRPLAYER_VERSION_STRING);
		
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
			ByteArrayBuffer requestBodyBuffer = new ByteArrayBuffer(655360);			
			do {
				Log.d(TAG, "listening packets ... ");
				requestBodyBuffer.clear();
				StringBuilder packet = new StringBuilder();
				int ret = readRequestHeader(packet);
				int contentLength = 0;
				if (ret != -1) {
					Log.d(TAG, "read body ... ");
					Matcher m = requestHeaderPattern.matcher(packet.toString());
				    contentLength = getContentLength(m);
				    ret = readRequestBody(contentLength, packet, requestBodyBuffer);
				} else {
					Log.d(TAG, "corrupt packet:"+packet.toString());
				}
				if (ret!=-1) {
					// We handle the packet
					RTSPPacket request = new RTSPPacket(packet.toString());
					Log.d(TAG, "raw " + request.getRawPacket());
					if (request.getReq().equals("POST")){
						String dir = request.getDirectory();
						if (dir.equals("/fp-setup")) {
							Log.d(TAG, "requestBodyBuffer.length:" + requestBodyBuffer.length());
							byte packageData[] = requestBodyBuffer.toByteArray();
							if (requestBodyBuffer.length() == 16) {
								FairPlay.init();
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								byte responseData[] = FairPlay.setupPhase1(requestBodyBuffer.buffer(), requestBodyBuffer.length(), true);
								Log.d("fairplay", "responseData.length:" + responseData.length);
								RTSPResponse response = new RTSPResponse("RTSP/1.0 200 OK");
								response.append("Content-Type", "application/octet-stream");
								response.append("Content-Length", String.valueOf(responseData.length));
								response.append("Server", "AirTunes/" + AIRPLAYER_VERSION_STRING);
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
									shouldStop = true;
								}
								Log.d(TAG, sb.toString());

							} else if (requestBodyBuffer.length() == 164) {
								byte responseData[] = FairPlay.setupPhase2(requestBodyBuffer.buffer(), requestBodyBuffer.length(), true);
								RTSPResponse response = new RTSPResponse("RTSP/1.0 200 OK");
								response.append("Content-Type", "application/octet-stream");
								response.append("Content-Length", String.valueOf(responseData.length));
								response.append("Server", "AirTunes/" + AIRPLAYER_VERSION_STRING);
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
									shouldStop = true;
								}
								Log.d(TAG, sb.toString());
							}
						} else if (dir.equals("/pair-setup")) {
							Log.d(TAG, "dir 11" + dir);
							RTSPResponse response = new RTSPResponse("RTSP/1.0 200 OK");
							response.append("Content-Type", "application/octet-stream");
							response.append("Content-Length", "32");
							response.append("Server", "AirTunes/" + AIRPLAYER_VERSION_STRING);
							response.append("CSeq", request.valueOfHeader("CSeq"));
							response.finalize();
							StringBuilder sb = new StringBuilder();
							sb.append(response.getRawPacket());
							// Write the response to the wire
							try {
								socket.getOutputStream().write(sb.toString().getBytes());
								socket.getOutputStream().write(AirPlayServer.mEdPublicKey);
								socket.getOutputStream().flush();
							} catch (IOException e) {
								e.printStackTrace();
								shouldStop = true;
							}
							Log.d(TAG,"sb string" + sb.toString());

						} else if (dir.equals("/pair-verify")) {
							if(readBuffer[0] == 1)
							{
								/*struct {
								uint8_t publicKey[ 32 ];
								uint8_t secretKey[ 32 ];
								uint8_t sharedKey[ 32 ];
								uint8_t controllerPublicKey[ 32 ];
								uint8_t controllerSignature[ 32 ];

								uint8_t edSecret[ 32 ];
								uint8_t edPubKey[ 32 ];

								uint8_t enKey[ 4 ];
								} pair_setup;*/
								int length = 32;
								byte[] controllerPublicKey = new byte[length];
								byte[] controllerSignature = new byte[length];
								byte[] out = new byte[96];
								for (int i = 0; i < length; i++) {
									controllerPublicKey[i] = readBuffer[i + 4];
									controllerSignature[i] = readBuffer[i + 4 + 32];
								}
								EzAes.pairVerify(mEdPublicKey, mEdSecretKey, controllerPublicKey, controllerSignature, out);
								RTSPResponse response = new RTSPResponse("RTSP/1.0 200 OK");
								response.append("Content-Type", "application/octet-stream");
								response.append("Content-Length", "96");
								response.append("Server", "AirTunes/" + AIRPLAYER_VERSION_STRING);
								response.append("CSeq", request.valueOfHeader("CSeq"));
								response.finalize();
								StringBuilder sb = new StringBuilder();
								sb.append(response.getRawPacket());
								// Write the response to the wire
								try {
									socket.getOutputStream().write(sb.toString().getBytes());
									socket.getOutputStream().write(out);
									socket.getOutputStream().flush();
								} catch (IOException e) {
									e.printStackTrace();
									shouldStop = true;
								}
								Log.d(TAG, sb.toString());
							} else {
								RTSPResponse response = new RTSPResponse("RTSP/1.0 200 OK");
								response.append("Content-Type", "application/octet-stream");
								response.append("Content-Length", "0");
								response.append("Server", "AirTunes/" + AIRPLAYER_VERSION_STRING);
								response.append("CSeq", request.valueOfHeader("CSeq"));
								response.finalize();
								StringBuilder sb = new StringBuilder();
								sb.append(response.getRawPacket());
								try {
									socket.getOutputStream().write(sb.toString().getBytes());
									socket.getOutputStream().flush();
								} catch (IOException e) {
									e.printStackTrace();
									shouldStop = true;
								}
								Log.d(TAG, sb.toString());

							}
//
						} else if (dir.equals("/feedback")) {
							RTSPResponse response = new RTSPResponse("RTSP/1.0 200 OK");
							response.append("Content-Type", "application/octet-stream");
							response.append("Content-Length", "0");
							response.append("Server", "AirTunes/" + AIRPLAYER_VERSION_STRING);
							response.append("CSeq", request.valueOfHeader("CSeq"));
							response.finalize();
							StringBuilder sb = new StringBuilder();
							sb.append(response.getRawPacket());
							try {
								socket.getOutputStream().write(sb.toString().getBytes());
								socket.getOutputStream().flush();
							} catch (IOException e) {
								e.printStackTrace();
								shouldStop = true;
							}
							Log.d(TAG, sb.toString());

						} else{
							//TODO
							Log.d(TAG, "unhandled request post dir ====== " + dir);
						}

					} else if (request.getReq().equals("SETUP")){
						RTSPResponse response = new RTSPResponse("RTSP/1.0 200 OK");
						String contentType = request.valueOfHeader("Content-Type");
						String userAgent = request.valueOfHeader("User-Agent");
						ByteArrayOutputStream binaryPlist = new ByteArrayOutputStream();
						String airplayFullVersion = userAgent.split("/")[1];
						int airplayMajorVersion = Integer.valueOf(airplayFullVersion.split("\\.")[0]);
						int airplayMinorVersion = Integer.valueOf(airplayFullVersion.split("\\.")[1]);
						if (request.valueOfHeader("Transport") != null) {
							/*
							SETUP rtsp://fe80::217:f2ff:fe0f:e0f6/3413821438 RTSP/1.0
							CSeq: 4
							Transport: RTP/AVP/UDP;unicast;interleaved=0-1;mode=record;control_port=6001;timing_port=6002
							User-Agent: iTunes/10.6 (Macintosh; Intel Mac OS X 10.7.3) AppleWebKit/535.18.5
							Client-Instance: 56B29BB6CB904862
							DACP-ID: 56B29BB6CB904862
							Active-Remote: 1986535575
							---
							RTSP/1.0 200 OK
							Transport: RTP/AVP/UDP;unicast;mode=record;server_port=53561;control_port=63379;timing_port=50607
							Session: 1
							Audio-Jack-Status: connected
							Server: AirTunes/130.14
							CSeq: 4
							*/
							String value = request.valueOfHeader("Transport");
							response.append("Audio-Jack-Status", "connected; type=analog");
							int controlPort = 0;
							int timingPort = 0;

							// Control port
							Pattern p = Pattern.compile(";control_port=(\\d+)");
							Matcher m = p.matcher(value);
							if (m.find()) {
								controlPort = Integer.valueOf(m.group(1));
							}

							// Timing port
							p = Pattern.compile(";timing_port=(\\d+)");
							m = p.matcher(value);
							if (m.find()) {
								timingPort = Integer.valueOf(m.group(1));
							}

							// Launching audioserver
							releaseAudioServer();
							//TODO depends on RTSP meta
							AudioSession session = new AudioSession(aesiv, aeskey, fmtpString, controlPort, timingPort, socket.getInetAddress());
							if (session.isAacEldEncoding()) {
								serv = new com.actionsmicro.airplay.airtunes.AudioPlayer(session);
							} else if (session.isAppleLosslessEncoding()) {
								serv = new AudioServer(session);
							}
							// TODO
							// ??? Why ???
							response.append("Session", "DEADBEEF");
							if (serv != null) {
								response.append("Transport", String.format("RTP/AVP/UDP;unicast;mode=record;events;control_port=%d;server_port=%d", serv.getControlPort(), serv.getServerPort()));
							}
						}

						if (contentType.equalsIgnoreCase("application/x-apple-binary-plist")) {
							try {
								NSDictionary pDict = (NSDictionary) BinaryPropertyListParser.parse(requestBodyBuffer.toByteArray());
								if (null != pDict.get("eiv")) {
									eiv = ((NSData) pDict.get("eiv")).bytes();
								}

								if (null != pDict.get("ekey")) {
									byte[] ekey = ((NSData) pDict.get("ekey")).bytes();
									fpaeskey = FairPlay.decrypt(ekey, 0x48);
								}

								if (null != pDict.get("timingPort")) {
									timingPort = ((NSNumber) pDict.get("timingPort")).intValue();
								}
								if (null != pDict.get("model")) {
									model = pDict.get("model").toString().toLowerCase();
								}
								if(null != pDict.get("streams")) {
									NSDictionary estream = (NSDictionary) ((NSArray) pDict.get("streams")).getArray()[0];
									int type = ((NSNumber) estream.get("type")).intValue();

									NSNumber streamConnectionID = (NSNumber)estream.get("streamConnectionID");
									if (streamConnectionID == null) {
										streamConnectionID = new NSNumber(0);
									}

									byte[] temp8 = new byte[64];
									byte[] temp9 = new byte[64];
									byte[] temp10 = new byte[64];
									if (streamConnectionID != null) {
										EzAes.setup(airplayMajorVersion, eiv, fpaeskey, streamConnectionID.longValue(), temp8, temp9, temp10);
									}
									if (type == 110) {
										byte[] videoAesKey = new byte[16];
										byte[] videoAesIV = new byte[16];
										System.arraycopy(temp9,0,videoAesKey,0,16);
										System.arraycopy(temp10, 0, videoAesIV, 0, 16);
										if (streamConnectionID != null) {
											EzAes.init(videoAesKey, videoAesIV);
										}
										initAirplaySetup(request, response, binaryPlist, airplayMajorVersion, type);

									} else if (type == 96) {
										// TODO start RTSP
										int controlPort = ((NSNumber) estream.get("controlPort")).intValue();
										int audioFormat = ((NSNumber) estream.get("audioFormat")).intValue();
										int spf = ((NSNumber) estream.get("spf")).intValue();

										if (audioFormat == 0x40000) {
											fmtpString = "96 352 0 16 40 10 14 2 255 0 0 44100";
										} else {
											fmtpString = "96 mode=AAC-eld; constantDuration=480";
										}
										Log.d(TAG, "SETUP RTP..................");
										// Launching audioserver
										releaseAudioServer();
										byte[] pair_aesKey = new byte[16];
										byte[] pair_aesiv = new byte[16];
										if (streamConnectionID != null || airplayMajorVersion < 230) {
											if (streamConnectionID != null) {
												System.arraycopy(temp8, 0, pair_aesKey, 0, 16);
											} else {
												// ios8
												System.arraycopy(fpaeskey, 0, pair_aesKey, 0, 16);
											}
											System.arraycopy(eiv, 0, pair_aesiv, 0, 16);
										}

										AudioSession session = new AudioSession(pair_aesiv, pair_aesKey, fmtpString, controlPort, timingPort, socket.getInetAddress());
										if (session.isAacEldEncoding()) {
											serv = new com.actionsmicro.airplay.airtunes.AudioPlayer(session);
										} else if (session.isAppleLosslessEncoding()) {
											serv = new AudioServer(session);
										}

										if(airplayMajorVersion >= 230) {
										/*const char fmt_content_96[] = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"\
										"<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\r\n"\
										"<plist version=\"1.0\">\r\n"\
										"<dict>\r\n"\
										"<key>streams</key>\r\n"\
										"<array>\r\n"\
										"<dict>\r\n"\
										"<key>dataPort</key>\r\n"\
										"<integer>%d</integer>\r\n"\
										"<key>controlPort</key>\r\n"\
										"<integer>%d</integer>\r\n"\
										"<key>type</key>\r\n"\
										"<integer>96</integer>\r\n"\
										"</dict>\r\n"\
										"</array>\r\n"\
										"<key>timingPort</key>\r\n"\
										"<integer>%d</integer>\r\n"\
										"</dict>\r\n"\
										"</plist>\r\n";*/

											NSDictionary setupInfo = new NSDictionary();

											NSArray streamArray = new NSArray(1);
											NSDictionary streamDict = new NSDictionary();
											streamDict.put("dataPort", serv.getServerPort());
											streamDict.put("controlPort", serv.getControlPort());
											streamDict.put("type", type);
											streamArray.setValue(0, streamDict);
											setupInfo.put("streams", streamArray);
											setupInfo.put("timingPort", timingPort);

											BinaryPropertyListWriter.write(binaryPlist, setupInfo);

											response.append("Content-Length", String.valueOf(binaryPlist.size()));
											response.append("Server", "AirTunes/" + AIRPLAYER_VERSION_STRING);
											response.append("CSeq", request.valueOfHeader("CSeq"));
											response.finalize();
											StringBuilder sb = new StringBuilder();
											sb.append(response.getRawPacket());
											// Write the response to the wire
											try {
												socket.getOutputStream().write(sb.toString().getBytes());
												socket.getOutputStream().write(binaryPlist.toByteArray());
												socket.getOutputStream().flush();
											} catch (IOException e) {
												e.printStackTrace();
												shouldStop = true;
											}
											Log.d(TAG,"sb string" + sb.toString());
											Log.d(TAG, "SETUP ios9 rtp..................");
										} else{
											//TODO ios8
										/*const char fmt_content_96_ios8[] = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"\
										"<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\r\n"\
										"<plist version=\"1.0\">\r\n"\
										"<dict>\r\n"\
										"<key>streams</key>\r\n"\
										"<array>\r\n"\
										"<dict>\r\n"\
										"<key>dataPort</key>\r\n"\
										"<integer>%d</integer>\r\n"\
										"<key>controlPort</key>\r\n"\
										"<integer>%d</integer>\r\n"\
										"<key>type</key>\r\n"\
										"<integer>96</integer>\r\n"\
										"</dict>\r\n"\
										"</array>\r\n"\
										"<key>eventPort</key>\r\n"\
										"<integer>%d</integer>\r\n"\
										"<key>timingPort</key>\r\n"\
										"<integer>%d</integer>\r\n"\
										"</dict>\r\n"\
										"</plist>\r\n";*/

											NSDictionary setupInfo = new NSDictionary();

											NSArray streamArray = new NSArray(1);
											NSDictionary streamDict = new NSDictionary();
											streamDict.put("dataPort", serv.getServerPort());
											streamDict.put("controlPort", serv.getControlPort());
											streamDict.put("type", type);
											streamArray.setValue(0, streamDict);
											NSNumber eventPort = new NSNumber(AirPlayServer.eventPort);

											setupInfo.put("streams", streamArray);
											setupInfo.put("eventPort", eventPort);
											setupInfo.put("timingPort", timingPort);

											BinaryPropertyListWriter.write(binaryPlist, setupInfo);

											response.append("Content-Length", String.valueOf(binaryPlist.size()));
											response.append("Server", "AirTunes/" + AIRPLAYER_VERSION_STRING);
											response.append("CSeq", request.valueOfHeader("CSeq"));
											response.finalize();
											StringBuilder sb = new StringBuilder();
											sb.append(response.getRawPacket());
											// Write the response to the wire
											try {
												socket.getOutputStream().write(sb.toString().getBytes());
												socket.getOutputStream().write(binaryPlist.toByteArray());
												socket.getOutputStream().flush();
											} catch (IOException e) {
												e.printStackTrace();
												shouldStop = true;
											}
											Log.d(TAG,"sb string" + sb.toString());
											Log.d(TAG, "SETUP ios8 rtp..................");

										}
									}
								} else if (airplayMajorVersion > 300){
									// for iOS10
									airplay10SetupPhase1(request, response);
								}
							} catch (IOException e) {
								e.printStackTrace();
							} catch (PropertyListFormatException e) {
								e.printStackTrace();
							} catch (OutOfMemoryError e) {
								StringWriter stringWriter = new StringWriter();
								e.printStackTrace(new PrintWriter(stringWriter));
								String errorMsg = e.getLocalizedMessage() + "\n" + stringWriter.toString();
								Log.e(TAG, errorMsg);
							}
						}


					}
					else if (request.getReq().equals("GET")){
						RTSPResponse response = new RTSPResponse("RTSP/1.0 200 OK");
						ByteArrayOutputStream binaryPlist = new ByteArrayOutputStream();
						String dir = request.getDirectory();
						if (dir.equals("/info")) {
							/*const char fmt_headers[] = "Content-Type: application/x-apple-binary-plist\r\n"\
							"Server: AirTunes/%s\r\n";
							const char fmt_content[] = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"\
							"<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\r\n"\
							"<plist version=\"1.0\">\r\n"\
							"<dict>\r\n"\
							"<key>pk</key>\r\n"\
							"<data>\r\n"\
							"%s\r\n"\
							"</data>\r\n"\
							"<key>name</key>\r\n"\
							"<string>%s</string>\r\n"\
							"<key>vv</key>\r\n"\
							"<integer>%d</integer>\r\n"\
							"<key>statusFlags</key>\r\n"\
							"<integer>%d</integer>\r\n"\
							"<key>keepAliveLowPower</key>\r\n"\
							"<integer>1</integer>\r\n"\
							"<key>keepAliveSendStatsAsBody</key>\r\n"\
							"<integer>1</integer>\r\n"\
							"<key>pi</key>\r\n"\
							"<string>%s</string>\r\n"\
							"<key>sourceVersion</key>\r\n"\
							"<string>%s</string>\r\n"\
							"<key>deviceID</key>\r\n"\
							"<string>%02X:%02X:%02X:%02X:%02X:%02X</string>\r\n"\
							"<key>macAddress</key>\r\n"\
							"<string>%02X:%02X:%02X:%02X:%02X:%02X</string>\r\n"\
							"<key>model</key>\r\n"\
							"<string>%s</string>\r\n"\
							"<key>audioFormats</key>\r\n"\
							"<array>\r\n"\
							"<dict>\r\n"\
							"<key>audioInputFormats</key>\r\n"\
							"<integer>67108860</integer>\r\n"\
							"<key>audioOutputFormats</key>\r\n"\
							"<integer>67108860</integer>\r\n"\
							"<key>type</key>\r\n"\
							"<integer>100</integer>\r\n"\
							"</dict>\r\n"\
							"<dict>\r\n"\
							"<key>audioInputFormats</key>\r\n"\
							"<integer>67108860</integer>\r\n"\
							"<key>audioOutputFormats</key>\r\n"\
							"<integer>67108860</integer>\r\n"\
							"<key>type</key>\r\n"\
							"<integer>101</integer>\r\n"\
							"</dict>\r\n"\
							"</array>\r\n"\
							"<key>audioLatencies</key>\r\n"\
							"<array>\r\n"\
							"<dict>\r\n"\
							"<key>audioType</key>\r\n"\
							"<string>default</string>\r\n"\
							"<key>inputLatencyMicros</key>\r\n"\
							"<false/>\r\n"\
							"<key>outputLatencyMicros</key>\r\n"\
							"<false/>\r\n"\
							"<key>type</key>\r\n"\
							"<integer>100</integer>\r\n"\
							"</dict>\r\n"\
							"<dict>\r\n"\
							"<key>audioType</key>\r\n"\
							"<string>default</string>\r\n"\
							"<key>inputLatencyMicros</key>\r\n"\
							"<false/>\r\n"\
							"<key>outputLatencyMicros</key>\r\n"\
							"<false/>\r\n"\
							"<key>type</key>\r\n"\
							"<integer>101</integer>\r\n"\
							"</dict>\r\n"\
							"</array>\r\n"\
							"<key>features</key>\r\n"\
							"<integer>%s</integer>\r\n"\
							"<key>displays</key>\r\n"\
							"<array>\r\n"\
							"<dict>\r\n"\
							"<key>height</key>\r\n"\
							"<integer>%d</integer>\r\n"\
							"<key>width</key>\r\n"\
							"<integer>%d</integer>\r\n"\
							"<key>rotation</key>\r\n"\
							"<false/>\r\n"\
							"<key>widthPhysical</key>\r\n"\
							"<false/>\r\n"\
							"<key>heightPhysical</key>\r\n"\
							"<false/>\r\n"\
							"<key>widthPixels</key>\r\n"\
							"<integer>%d</integer>\r\n"\
							"<key>heightPixels</key>\r\n"\
							"<integer>%d</integer>\r\n"\
							"<key>refreshRate</key>\r\n"\
							"<integer>%d</integer>\r\n"\
							"<key>features</key>\r\n"\
							"<integer>14</integer>\r\n"\
							"<key>overscanned</key>\r\n"\
							"<false/>\r\n"\
							"<key>uuid</key>\r\n"\
							"<string>%s</string>\r\n"\
							"</dict>\r\n"\
							"</array>\r\n"\
							"</dict>\r\n"\
							"</plist>\r\n";*/

							NSDictionary info = new NSDictionary();
							String pkString = Base64.encodeToString(mEdPublicKey, Base64.NO_WRAP);
							pkString+="\r\n";
//							for (int i = 0; i < 32; i++) {
//								pkString += String.format("%02X", mEdPublicKey[i]);
//							}

							info.put("pk",pkString);
							NSString name = new NSString("Apple TV");
							info.put("name",name);
							info.put("vv",2);
							NSNumber statusFlags = new NSNumber(0x4);
							info.put("statusFlags",statusFlags);
							NSNumber keepAliveLowPower = new NSNumber(1);
							info.put("keepAliveLowPower",keepAliveLowPower);
							NSNumber keepAliveSendStatsAsBody = new NSNumber(1);
							info.put("keepAliveSendStatsAsBody",keepAliveSendStatsAsBody);
							info.put("pi","b08f5a79-db29-4384-b456-a4784d9e6055");
							info.put("sourceVersion",AIRPLAYER_VERSION_STRING);
							String hwAddrStr ="";
							for (int i = 0; i < 6; i++) {
								hwAddrStr += String.format("%02X", hwAddr[i]);
								if (i != 5) {
									hwAddrStr += ":";
								}
							}
							hwAddrStr = hwAddrStr.toUpperCase();
							info.put("deviceID",hwAddrStr);
							info.put("macAddress",hwAddrStr);
							info.put("model",AirPlayServer.AIRPLAY_MODEL);

							NSArray audioFormats = new NSArray(2);
							NSDictionary audioFormats1 = new NSDictionary();
							audioFormats1.put("audioInputFormats",new NSNumber(67108860));
							audioFormats1.put("audioOutputFormats",new NSNumber(67108860));
							audioFormats1.put("type",new NSNumber(100));

							NSDictionary audioFormats2 = new NSDictionary();
							audioFormats2.put("audioInputFormats",new NSNumber(67108860));
							audioFormats2.put("audioOutputFormats",new NSNumber(67108860));
							audioFormats2.put("type",new NSNumber(101));
							audioFormats.setValue(0, audioFormats1);
							audioFormats.setValue(1,audioFormats2);

							info.put("audioFormats",audioFormats);

							NSArray audioLatencies = new NSArray(2);

							NSDictionary audioLatency1 = new NSDictionary();
							audioLatency1.put("audioType",new NSString("default"));
							audioLatency1.put("inputLatencyMicros", false);
							audioLatency1.put("outputLatencyMicros", false);
							audioLatency1.put("type", new NSNumber(100));

							NSDictionary audioLatency2 = new NSDictionary();
							audioLatency2.put("audioType", new NSString("default"));
							audioLatency2.put("inputLatencyMicros", false);
							audioLatency2.put("outputLatencyMicros", false);
							audioLatency2.put("type", new NSNumber(101));
							audioLatencies.setValue(0, audioLatency1);
							audioLatencies.setValue(1,audioLatency2);

							info.put("audioLatencies", audioLatencies);

//							NSNumber features = new NSNumber(130367356919l);
							NSNumber features = new NSNumber(176156663);
							info.put("features", features);


							int height = 720;
							int width = 1280;
							if (isIphone5Series()) {
								height = 768;
								width = 1366;
							}
							NSArray displays = new NSArray(1);
							NSDictionary display1 = new NSDictionary();
							display1.put("height",height);
							display1.put("width",width);
							display1.put("rotation",false);
							display1.put("widthPhysical",false);
							display1.put("heightPhysical",false);
							display1.put("widthPixels",width);
							display1.put("heightPixels",height);
							display1.put("refreshRate",24);
							display1.put("features",14);
							display1.put("overscanned",false);
							display1.put("uuid",UUID.randomUUID().toString());
							displays.setValue(0,display1);
							info.put("displays", displays);

							BinaryPropertyListWriter.write(binaryPlist, info);
							response.append("Content-Type", "application/x-apple-binary-plist");
							response.append("Content-Length", String.valueOf(binaryPlist.size()));
							response.append("Server", "AirTunes/" + AIRPLAYER_VERSION_STRING);
							response.append("CSeq", request.valueOfHeader("CSeq"));
							response.finalize();
							StringBuilder sb = new StringBuilder();
							sb.append(response.getRawPacket());
							// Write the response to the wire
							try {
								socket.getOutputStream().write(sb.toString().getBytes());
								socket.getOutputStream().write(binaryPlist.toByteArray());
								socket.getOutputStream().flush();
							} catch (IOException e) {
								e.printStackTrace();
								shouldStop = true;
							}
							Log.d(TAG,"sb string" + sb.toString());
							Log.d(TAG, "GET info..................");


						} else if (dir.equals("/stream.xml")) {
							Log.d(TAG, "GET stream..................");
						}


					}
					else {
						if ("TEARDOWN".equals(request.getReq())) {
							Log.d(TAG, "TEARDOWN:airplayState = " + airplayState);
						}

						RTSPResponse response = this.handlePacket(request, requestBodyBuffer);

						// Write the response to the wire
						try {
							BufferedWriter oStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
							oStream.write(response.getRawPacket());
							oStream.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}
						Log.d(TAG, "raw " + response.getRawPacket());

						if ((airplayState == AIRPLAY_MIRROR || airplayState == AIRPLAY_VIDEO_ON_MIRROR) && "TEARDOWN".equals(request.getReq())) {
							Log.d(TAG, "don't close server since still in MIRRORING STATE ");
							continue;
						}
						if("TEARDOWN".equals(request.getReq())){
							releaseAudioServer();
							socket.close();
							socket = null;
			    			shouldStop = true;
						}
					}
				} else {
					Log.d(TAG, "raw " + "return == -1");
	    			socket.close();
	    			socket = null;
	    			shouldStop = true;
				}
			} while (!shouldStop);
			
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
			if (airTunesListener != null) {
				airTunesListener.onDisconnected();
			}
		}
		Log.d(TAG, "connection ended.");
	}

	private void airplay10SetupPhase1(RTSPPacket request, RTSPResponse response) {
		response.append("Content-Length", "0");
		response.append("Server", "AirTunes/" + AIRPLAYER_VERSION_STRING);
		response.append("CSeq", request.valueOfHeader("CSeq"));
		response.finalize();
		StringBuilder sb = new StringBuilder();
		sb.append(response.getRawPacket());
		// Write the response to the wire
		try {
            socket.getOutputStream().write(sb.toString().getBytes());
            socket.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
            shouldStop = true;
        }
		Log.d(TAG, "sb string" + sb.toString());
	}

	private boolean isIphone5Series() {
		//iPhone(5c/5s/SE)
		if(model.startsWith("iphone5,") || model.startsWith("iphone6,") || model.startsWith("iphone8,4")) {
			return true;
		}
		return false;
	}

	private void initAirplaySetup(RTSPPacket request, RTSPResponse response, ByteArrayOutputStream binaryPlist, int airplayMajorVersion, int type) throws IOException {
		// ios9 format
		/*const char fmt_content_110[] = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"\
		"<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\r\n"\
		"<plist version=\"1.0\">\r\n"\
		"<dict>\r\n"\
		"<key>streams</key>\r\n"\
		"<array>\r\n"\
		"<dict>\r\n"\
		"<key>dataPort</key>\r\n"\
		"<integer>%d</integer>\r\n"\
		"<key>type</key>\r\n"\
		"<integer>110</integer>\r\n"\
		"</dict>\r\n"\
		"</array>\r\n"\
		"<key>timingPort</key>\r\n"\
		"<integer>%d</integer>\r\n"\
		"</dict>\r\n"\
		"</plist>\r\n";*/

		// ios 8 format
		/*const char fmt_content_110_ios8[] = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"\
		"<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\r\n"\
		"<plist version=\"1.0\">\r\n"\
		"<dict>\r\n"\
		"<key>streams</key>\r\n"\
		"<array>\r\n"\
		"<dict>\r\n"\
		"<key>dataPort</key>\r\n"\
		"<integer>%d</integer>\r\n"\
		"<key>type</key>\r\n"\
		"<integer>110</integer>\r\n"\
		"</dict>\r\n"\
		"</array>\r\n"\
		"<key>eventPort</key>\r\n"\
		"<integer>%d</integer>\r\n"\
		"<key>timingPort</key>\r\n"\
		"<integer>%d</integer>\r\n"\
		"</dict>\r\n"\
		"</plist>\r\n";*/
		NSDictionary setupInfo = new NSDictionary();

		NSArray streamArray = new NSArray(1);
		NSNumber dataPort = new NSNumber(7100);
		NSDictionary streamDict = new NSDictionary();
		streamDict.put("dataPort", dataPort);
		streamDict.put("type", type);
		streamArray.setValue(0, streamDict);
		setupInfo.put("streams", streamArray);
		if (airplayMajorVersion < 230) {
			setupInfo.put("eventPort", AirPlayServer.eventPort);
		}
		setupInfo.put("timingPort", timingPort);

		BinaryPropertyListWriter.write(binaryPlist, setupInfo);

		response.append("Content-Length", String.valueOf(binaryPlist.size()));
		response.append("Server", "AirTunes/" + AIRPLAYER_VERSION_STRING);
		response.append("CSeq", request.valueOfHeader("CSeq"));
		response.finalize();
		StringBuilder sb = new StringBuilder();
		sb.append(response.getRawPacket());
		// Write the response to the wire
		try {
			socket.getOutputStream().write(sb.toString().getBytes());
			socket.getOutputStream().write(binaryPlist.toByteArray());
			socket.getOutputStream().flush();
		} catch (IOException e) {
			e.printStackTrace();
			shouldStop = true;
		}
		Log.d(TAG, "sb string" + sb.toString());
	}

	private final byte[] readBuffer = new byte[100*1024];

	private int readRequestBody(int contentLength, StringBuilder packet,
			ByteArrayBuffer requestBodyBuffer) throws IOException {
		requestBodyBuffer.clear();
		int ret = 0;
		int readMore = contentLength;
		while (readMore > 0) {
			Log.d(TAG, "readMore:"+readMore);
			ret = in.read(readBuffer, 0, Math.min(readMore, readBuffer.length));
			if (ret!=-1) {
				Log.d(TAG, "readMore:ret:"+ret);
				packet.append(new String(readBuffer, 0, ret));
				requestBodyBuffer.append(readBuffer, 0, ret);
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


	public void stopThread() {
		shouldStop = true;
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
			}
		}
		this.interrupt();
	}


	public void setAirTunesListener(RTSPResponder.AirTunesListener listener) {
		airTunesListener = listener;
	}
		
}