package com.yutel.silver;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.JmmDNS;
import javax.jmdns.ServiceInfo;

import com.actionsmicro.androidrx.Bonjour;
import com.yutel.silver.exception.AirplayException;
import com.yutel.silver.http.AirplayServer;
import com.yutel.silver.http.HttpProtocol;
import com.yutel.silver.http.handler.HttpHandler;
import com.yutel.silver.util.AirplayUtil;
import com.yutel.silver.vo.AirplayState;
import com.yutel.silver.vo.Device;

public class AikaImpl extends Aika {
	private static Logger logger = Logger.getLogger(AikaImpl.class.getName());
	private int mPort;
	private AirplayServer as;
	private JmDNS mJmDNS;
	private InetAddress mInetAddress;
	private String mType = "_airplay._tcp.local.";
	private String mName;
	private Device mDevice;
	private HashMap<String, HttpHandler> mHandlers;
	private AikaProxy mProxy;
	private HashMap<String, String> mConfig;
	private ServiceInfo airPlayService;

	public AikaImpl(InetAddress inetAddress, int port, String name) {
		mInetAddress = inetAddress;
		mPort = port < 7000 ? 7000 : port;
		mName = name == null ? "aika" : name;
		mHandlers = new HashMap<String, HttpHandler>();
		mProxy = new AikaProxy();
	}

	@Override
	public void createContext(String key, HttpHandler handler) {
		mHandlers.put(key, handler);
	}

	@Override
	public AikaProxy getAikaProxy() {
		return mProxy;
	}

	@Override
	public void setConnectListener(AikaConnectListener listener) {
		mProxy.setConnectListener(listener);
	}

	@Override
	public AikaConnectListener getConnectListener() {
		return mProxy.getConnectListener();
	}

	@Override
	public void setControlListener(AikaControlListener listener) {
		mProxy.setControlListener(listener);
	}

	@Override
	public AikaControlListener getControlListener() {
		return mProxy.getControlListener();
	}

	@Override
	public void config(Device device) {
		mDevice = device;
		mConfig = new HashMap<String, String>();
		mConfig.put("deviceid", device.getDeviceid().toUpperCase());
		mConfig.put("model", device.getModel());
		mConfig.put("features", device.getFeatures());
		mConfig.put("srcvers", device.getSrcvers());
		mConfig.put("rmodel", "EZAir1,1");
		mConfig.put("pw", "0");
	}

	@Override
	public boolean start() {
		try {
			// http server
			as = new AirplayServer(mPort);
			as.setProxy(this.getAikaProxy());
			as.setDevice(mDevice);
			// as.setHandlers(mHandlers);
			as.start();
			// jmdns server
			mJmDNS = Bonjour.getInstance(mInetAddress);
			logger.log(Level.INFO, "Opened JmDNS!");
			airPlayService = ServiceInfo.create(mType, mName, mPort,
					0, 0, mConfig);
			mJmDNS.registerService(airPlayService);
			logger.log(Level.INFO, "Registered Service as " + airPlayService);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			stop();
			return false;
		}
	}

	@Override
	public void stop() {
		try {
			// jmdns
			final JmDNS mJmDNS2 = mJmDNS;
			final ServiceInfo airPlayService = this.airPlayService;
			this.airPlayService = null;
			new Thread(new Runnable() {

				@Override
				public void run() {
					cleanUpJmDNS(mJmDNS2,  airPlayService);					
				}
				
			}).start();
			mJmDNS = null;
			// http
			if (as != null) {
				as.forceStop();
				as = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void cleanUpJmDNS(JmDNS mJmDNS2, ServiceInfo airPlayService) {
		if (mJmDNS2 != null) {
			mJmDNS2.unregisterService(airPlayService);
			logger.log(Level.INFO, "JmDNS unregisterService:"+airPlayService);
		}
	}

	@Override
	public void sendEvent() {
		try {
			int eventId = AirplayState.EVENT_STOPPED;
			int videoStatus = mProxy.getControlListener().videoStatus();
			switch (videoStatus) {
			case AirplayState.STOPPED:
				eventId = AirplayState.EVENT_STOPPED;
				break;
			case AirplayState.CACHING:
				eventId = AirplayState.EVENT_LOADING;
				break;
			case AirplayState.PAUSING:
				eventId = AirplayState.EVENT_PAUSED;
				break;
			case AirplayState.PLAYING:
				eventId = AirplayState.EVENT_PLAYING;
				break;
			}
			
			String event = HttpProtocol.getEvent(eventId);
			logger.log(Level.SEVERE, "sendEvent:"+event+"(videoStatus:"+videoStatus+")");
			String reverse = AirplayUtil.getEventInfo(event);
			as.sendReverseResponse(reverse);
		} catch (AirplayException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
