package com.yutel.silver;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jmdns.ServiceInfo;

import com.actionsmicro.bonjour.BonjourServiceAdvertiser;
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
	private BonjourServiceAdvertiser bonjourServiceAdvertiser;
	private InetAddress mInetAddress;
	private String mType = "_airplay._tcp.local.";
	private String mName;
	private Device mDevice;
	private HashMap<String, HttpHandler> mHandlers;
	private AikaProxy mProxy;
	private HashMap<String, String> mConfig;

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
		mConfig.put("features", "0x"+Long.toHexString(Long.valueOf(device.getFeatures())));
		mConfig.put("srcvers", device.getSrcvers());
		mConfig.put("rmodel", "EZAir1,1");
		mConfig.put("pw", "0");
	}

	@Override
	public boolean start() {
		try {
			// http server
			as = new AirplayServer(mPort);
			as.setConnectionListener(new AirplayServer.ConnectionListener() {
				
				@Override
				public void onAirPlayStop() {
					
					AikaImpl.this.getAikaProxy().onAirPlayStop();
				}

				@Override
				public void onAirPlayStart() {
					AikaImpl.this.getAikaProxy().onAirPlayStart();
				}
			});
			as.setProxy(this.getAikaProxy());
			as.setDevice(mDevice);
			// as.setHandlers(mHandlers);
			as.start();
			bonjourServiceAdvertiser = new BonjourServiceAdvertiser(ServiceInfo.create(mType, mName, mPort,
					0, 0, mConfig));
			bonjourServiceAdvertiser.register();
			return true;
		} catch (IOException e) {
			// TODO pass exception out to client
			e.printStackTrace();
			stop();
			return false;
		}
	}

	@Override
	public void stop() {
		try {
			if (bonjourServiceAdvertiser != null) {
				final BonjourServiceAdvertiser bonjour = bonjourServiceAdvertiser;
				new Thread(new Runnable() {

					@Override
					public void run() {
						bonjour.unregister();	
						bonjour.close();
					}

				}).start();
				bonjourServiceAdvertiser = null;
			}
			// http
			if (as != null) {
				as.forceStop();
				as = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sendEvent() {
		try {
			int eventId = AirplayState.EVENT_STOPPED;
			int videoStatus = mProxy.videoStatus();
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
			logger.log(Level.INFO, "sendEvent:"+event+"(videoStatus:"+videoStatus+")");
			String reverse = AirplayUtil.getEventInfo(event);
			
            if (null != as) {
                as.sendReverseResponse(reverse);
            }
		} catch (AirplayException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void closeCurrentConnection() {
		if (as != null) {
			as.closeCurrentConnections();
		}
	}
}
