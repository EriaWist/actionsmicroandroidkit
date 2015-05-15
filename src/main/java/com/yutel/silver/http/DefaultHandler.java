package com.yutel.silver.http;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.actionsmicro.utils.Log;
import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;
import com.yutel.silver.exception.AirplayException;
import com.yutel.silver.util.AirplayUtil;
import com.yutel.silver.util.StringUtil;
import com.yutel.silver.vo.AirplayState;

public class DefaultHandler {
	private static final String TAG = "HttpDefaultHandler";
	protected AirplayServer server;
	protected HttpWrap wrap;

	public DefaultHandler(AirplayServer server, HttpWrap wrap) {
		this.server = server;
		this.wrap = wrap;
	}

	public void process() {
		try {
			if ("/reverse".equals(wrap.getContext())) {
				wrap.setResponseCode(101);
			} else if ("/server-info".equals(wrap.getContext())) {
				wrap.setResponseCode(200);
				wrap.getResponseHeads().put("Content-Type", "text/x-apple-plist+xml");
				String res = AirplayUtil.getServerInfo(server.getDevice());
				wrap.setBodys(res);
			} else if ("/rate".equals(wrap.getContext())) {
				wrap.setResponseCode(200);
				String rate = wrap.getRequestParameters().get("value");
				if (rate != null) {
					float ratef = StringUtil.toFloat(rate);
					int ratei = (int) ratef;
					if (ratei == 1) {
						server.getProxy().videoResume();
					} else {
						server.getProxy().videoPause();
					}					
				}
			} else if ("/stop".equals(wrap.getContext())) {
				wrap.setResponseCode(200);
				server.getProxy().videoStop();
			} else if ("/scrub".equals(wrap.getContext())) {
				wrap.setResponseCode(200);
				if ("GET".equals(wrap.getContext())) {
					wrap.setResponse(true);
					StringBuffer sb = new StringBuffer();
					sb.append("duration: ")
							.append(server.getProxy().videoDuration())
							.append(HttpProtocol.CRLF);
					sb.append("position: ")
							.append(server.getProxy().videoPostion())
							.append(HttpProtocol.CRLF);
					wrap.setBodys(sb.toString());
				} else {
					String position = wrap.getRequestParameters().get("position");
					server.getProxy().videoSeek((int)Float.valueOf(position).longValue());
				}
			} else if ("/playback-info".equals(wrap.getContext())) {
				playbackInfo();
			} else if ("/play".equals(wrap.getContext())) {
				play();
			} else if ("/setProperty".equals(wrap.getContext())) {
				setProperty(wrap.getRequestParameters());
			} else if ("/photo".equals(wrap.getContext())) {
				Log.d(TAG, "photo");
				String assetKey = wrap.getRequestHeads().get("X-Apple-AssetKey");				
				String action = wrap.getRequestHeads().get("X-Apple-AssetAction");
				String transition = wrap.getRequestHeads().get("X-Apple-Transition");
				wrap.setResponseCode(200);
				if (action == null) {
					server.getProxy().displayPhoto(wrap.getRequestBody(), assetKey, transition);
				} else if (action.equalsIgnoreCase("cacheOnly")) {
					server.getProxy().cachePhoto(assetKey, wrap.getRequestBody());					
				} else if (action.equalsIgnoreCase("displayCached")) {
					if (!server.getProxy().displayCached(assetKey, transition)) {
						wrap.setResponseCode(412); //Precondition Failed
					}
				} else {
					Log.e(TAG, "unhanled photo action:" + assetKey);					
				}
			} else if ("/volume".equals(wrap.getContext())) {
				if (wrap.getRequestParameters().containsKey("volume")) {
					server.getProxy().setVolume((float)Float.valueOf(wrap.getRequestParameters().get("volume")));
				}
				wrap.setResponseCode(200);				
			} else {
				Log.e(TAG, "unhanled request:"+wrap.getContext());
				wrap.setReverse(false);
				wrap.setResponseCode(404);
			}
		} catch (AirplayException ae) {
			ae.printStackTrace();
		}
	}

	private void setProperty(Map<String, String> requestParameters) {
		wrap.setResponseCode(200);
		wrap.getResponseHeads().put(HttpProtocol.ContentType,
				AirplayState.txtPLIST);
		wrap.setBodys(AirplayUtil.getSetProperty(0));
	}

	private void play() {
		try {
			if (wrap.getRequestBody() != null
					&& wrap.getRequestBody().length > 0) {
				String conType = wrap.getRequestHeads().get(
						HttpProtocol.ContentType);
				System.out.println("ContentType=" + conType);
				String url = null;
				String rate = "0f";
				String pos = "0f";
				if (AirplayState.binPLIST.equals(conType)) {
					NSDictionary rootDict = (NSDictionary) PropertyListParser
							.parse(wrap.getRequestBody());
					if (rootDict.containsKey("Content-Location")) {
						url = rootDict.objectForKey("Content-Location").toString();						
					} else if (rootDict.containsKey("host")) {
						url = "http://"+rootDict.objectForKey("host").toString()+rootDict.objectForKey("path").toString();						
					}
					if (rootDict.containsKey("rate")) {
						rate = rootDict.objectForKey("rate").toString();
					}
					if (rootDict.containsKey("Start-Position")) {
						pos = rootDict.objectForKey("Start-Position").toString();
					}
				} else {
					ByteArrayInputStream bin = new ByteArrayInputStream(wrap.getRequestBody());
					BufferedReader in = new BufferedReader(new InputStreamReader(bin));
					Pattern headerPattern = Pattern.compile(":\\ *(.*)");
					String line = in.readLine();
					while (line != null && line.length()>0) {
						Matcher matcher = headerPattern.matcher(line);
						if (matcher.find()) {
							if (line.startsWith("Content-Location")) {
								url = matcher.group(1);
							}
							if (line.startsWith("Start-Position:")) {
								pos = matcher.group(1);
							}
						}
						line  = in.readLine();						
					};					
				}
				Log.d(TAG, "video url=" + url);
				if (url != null) {
					server.getProxy().video(url, rate, pos);
					wrap.setResponseCode(200);
				}

			} else {
				System.out.println("body is null");
			}
		} catch (AirplayException ae) {
			ae.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void playbackInfo() {
		try {
			wrap.setResponseCode(200);
			wrap.setResponse(true);
			int state = server.getProxy().videoStatus();
			int duration = server.getProxy().videoDuration();
			int position = server.getProxy().videoPostion();
			wrap.getResponseHeads().put(HttpProtocol.ContentType,
					AirplayState.txtPLIST);
			if (state != AirplayState.STOPPED) {
				wrap.setBodys(AirplayUtil.getPlaybackInfo(duration, position,
						state == AirplayState.PLAYING?1.0f:0.0f));
			} else {
				wrap.setBodys(AirplayUtil.getPlaybackInfoNotReady());
			}
			switch (state) {
			case AirplayState.STOPPED:
				wrap.setReverseEvent(AirplayState.EVENT_STOPPED);
				break;
			case AirplayState.CACHING:
				wrap.setReverseEvent(AirplayState.EVENT_LOADING);
				break;
			case AirplayState.PAUSING:
				wrap.setReverseEvent(AirplayState.EVENT_PAUSED);
				break;
			case AirplayState.PLAYING:
				wrap.setReverseEvent(AirplayState.EVENT_PLAYING);
				break;
			}
		} catch (AirplayException ae) {
			ae.printStackTrace();
		}
	}

}
