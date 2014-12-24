package com.actionsmicro.androidkit.ezcast.imp.dlna;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.fourthline.cling.controlpoint.SubscriptionCallback;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.gena.CancelReason;
import org.fourthline.cling.model.gena.GENASubscription;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.meta.StateVariable;
import org.fourthline.cling.model.meta.StateVariableAllowedValueRange;
import org.fourthline.cling.model.state.StateVariableValue;
import org.fourthline.cling.model.types.UDAServiceId;
import org.fourthline.cling.support.avtransport.callback.GetMediaInfo;
import org.fourthline.cling.support.avtransport.callback.GetPositionInfo;
import org.fourthline.cling.support.avtransport.callback.Pause;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.Seek;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.avtransport.callback.Stop;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable.CurrentMediaDuration;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.SeekMode;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.model.TransportStatus;
import org.fourthline.cling.support.model.item.AudioItem;
import org.fourthline.cling.support.model.item.VideoItem;
import org.fourthline.cling.support.renderingcontrol.callback.GetVolume;
import org.fourthline.cling.support.renderingcontrol.callback.SetVolume;
import org.seamless.util.MimeType;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApiBuilder;
import com.actionsmicro.pigeon.MediaStreamingFileDataSource;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.Utils;
import com.actionsmicro.web.SimpleContentUriHttpFileServer;

public class DlnaMediaPlayerApi extends DlnaApi implements MediaPlayerApi {

	private static final String TAG = "DlnaMediaPlayerApi";
	private Service avtransportService;
	private State state = State.UNKNOWN;
	private SubscriptionCallback avtransportSubscription;
	private MediaPlayerStateListener mediaPlayerStateListener;
	private HandlerThread timerThread;
	private Handler timerHandler;
	private SimpleContentUriHttpFileServer simpleHttpFileServer;
	
	public DlnaMediaPlayerApi(MediaPlayerApiBuilder apiBuilder) {
		super(apiBuilder);
		mediaPlayerStateListener = apiBuilder.getMediaPlayerStateListener();
	}
	@Override
	public void connect() {
		DlnaDeviceInfo dlnaDevice = (DlnaDeviceInfo)getDevice();
		avtransportService = dlnaDevice.getDevice().findService(new UDAServiceId("AVTransport"));
		avtransportSubscription = new SubscriptionCallback(avtransportService) {

			@Override
			protected void ended(GENASubscription arg0, CancelReason arg1,
					UpnpResponse arg2) {
				Log.d(TAG+".SubscriptionCallback", "ended");
				if (avtransportSubscription != null) {
					avtransportSubscription.end();
					avtransportSubscription = null;
				}
			}

			@Override
			protected void established(GENASubscription arg0) {
				Log.d(TAG+".SubscriptionCallback", "established:"+arg0.toString());
				
			}

			@Override
			protected void eventReceived(GENASubscription sub) {
				Map<String, StateVariableValue<?>> values = sub.getCurrentValues();
				Log.d(TAG+".SubscriptionCallback", "eventReceived:"+sub.toString()+"\n values:"+values);
				if (values.containsKey("LastChange")) {
					try {
						LastChange lastChange = new LastChange(new AVTransportLastChangeParser(), values.get("LastChange").toString());
						updateStateIfNeeded(lastChange);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			private void updateDurationIfNeeded(LastChange lastChange) {
				CurrentMediaDuration eventedValue = lastChange.getEventedValue(0, AVTransportVariable.CurrentMediaDuration.class);
				if (eventedValue == null) return;
				String durationStr = eventedValue.getValue();
				if (durationStr != null) {
					int duration = parseFormattedTimeString(durationStr);
					Log.d(TAG, "CurrentMediaDuration:"+durationStr+"("+duration+")");
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDurationIsReady(DlnaMediaPlayerApi.this, duration);
					}
				}
			}


			private void updateStateIfNeeded(LastChange lastChange) {
				org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable.TransportState stateValue = lastChange.getEventedValue(0, AVTransportVariable.TransportState.class);
				if (stateValue == null) return;
				TransportState transportState = stateValue.getValue();
				org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable.TransportStatus statusValue = lastChange.getEventedValue(0, AVTransportVariable.TransportStatus.class);
				
				if (transportState != null) {
					Log.d(TAG+".SubscriptionCallback", "transportState:"+transportState);
					if (TransportState.PLAYING == transportState) {
						state = State.PLAYING;
						startPeriodicalPoller();
						getMediaInfo();
						if (mediaPlayerStateListener != null) {
							mediaPlayerStateListener.mediaPlayerDidStart(DlnaMediaPlayerApi.this);
						}
					} else if (TransportState.PAUSED_PLAYBACK == transportState) {
						state = State.PAUSED;
					} else if (TransportState.STOPPED == transportState) {
						state = State.STOPPED;
						stopPeriodicalPoller();
						if (statusValue != null) {
							TransportStatus transportStatus = statusValue.getValue();
							if (transportStatus != null && transportStatus.getValue().equals("ERROR_OCCURRED")) {
								if (mediaPlayerStateListener != null) {
									// TODO mapping error
									mediaPlayerStateListener.mediaPlayerDidFailed(DlnaMediaPlayerApi.this, AV_RESULT_ERROR_GENERIC);
								}
							}
						}
						if (mediaPlayerStateListener != null) {
							mediaPlayerStateListener.mediaPlayerDidStop(DlnaMediaPlayerApi.this);
						}
					}
				}
			}

			@Override
			protected void eventsMissed(GENASubscription arg0, int arg1) {
				Log.d(TAG+".SubscriptionCallback", "eventsMissed:"+arg0.toString());
				
			}

			@Override
			protected void failed(GENASubscription arg0, UpnpResponse arg1,
					Exception arg2, String arg3) {
				Log.d(TAG+".SubscriptionCallback", "failed:"+arg3);
				
			}
			
		};
		UpnpService.getUpnpService().execute(avtransportSubscription);
		super.connect();
	}
	@Override
	public void disconnect() {
		super.disconnect();
		stopPeriodicalPoller();
		stopHttpFileServer();
		if (avtransportSubscription != null) {
			avtransportSubscription.end();
			avtransportSubscription = null;
		}
		avtransportService = null;
	}
	@Override
	public void uploadSubtitle(InputStream is, String fileType)
			throws Exception {

	}

	@Override
	public State getState() {
		return state;
	}

	@Override
	public boolean pause() {
		if (avtransportService == null) return false;
        UpnpService.getUpnpService().execute(new Pause(avtransportService) {
            @Override
            public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
            	Log.e(TAG, defaultMsg);
            }
        });
        
		return true;
	}

	@Override
	public boolean resume() {
		if (avtransportService == null) return false;
        playImp();
		return true;
	}

	@Override
	public boolean increaseVolume() {
		return adjustVolumeRelatively(1);
	}
	private boolean adjustVolumeRelatively(final int factor) {
		DlnaDeviceInfo dlnaDevice = (DlnaDeviceInfo)getDevice();
		final Service renderingControl = dlnaDevice.getDevice().findService(new UDAServiceId("RenderingControl"));
		if (renderingControl != null) {
			StateVariable volState = renderingControl.getStateVariable("Volume");
			final StateVariableAllowedValueRange volRange = volState.getTypeDetails().getAllowedValueRange();
			UpnpService.getUpnpService().execute(new GetVolume(renderingControl) {

				@Override
				public void received(ActionInvocation arg0, int arg1) {
					Log.d(TAG+".GetVolume", "volume:"+arg1);
					long newVol = arg1 + volRange.getStep() * factor;
					if (newVol >= volRange.getMinimum() && newVol <= volRange.getMaximum()) {
						UpnpService.getUpnpService().execute(new SetVolume(renderingControl, newVol) {

							@Override
							public void failure(ActionInvocation arg0,
									UpnpResponse arg1, String defaultMsg) {
								Log.e(TAG+".SetVolume", defaultMsg);
							}

						});
					}
				}

				@Override
				public void failure(ActionInvocation arg0, UpnpResponse arg1,
						String defaultMsg) {
					Log.e(TAG+".GetVolume", defaultMsg);
				}
				
			});
			return true;
		}
		return false;
	}

	@Override
	public boolean decreaseVolume() {
		return adjustVolumeRelatively(-1);
	}

	@Override
	public boolean seek(int position) {
		if (avtransportService == null) return false;
        String relTime = Utils.makeTimeString("%1$d:%3$02d:%5$02d", position);
    	Log.d(TAG+".seek", "REL_TIME:"+relTime);
		UpnpService.getUpnpService().execute(new Seek(avtransportService, SeekMode.REL_TIME, relTime) {

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
            	Log.e(TAG+".seek", defaultMsg);
            }
        });
        
		return true;
	}

	@Override
	public boolean stop() {
		if (avtransportService == null) return false;
		final SimpleContentUriHttpFileServer detachedFileServer = simpleHttpFileServer; // since it's asynchronous, we need to detach the file server first to prevent stop wrong file server.
		simpleHttpFileServer = null;
		UpnpService.getUpnpService().execute(new Stop(avtransportService) {
        	@Override
			public void success(ActionInvocation invocation) {
        		detachedFileServer.stop();
        		stopPeriodicalPoller();
				if (mediaPlayerStateListener != null) {
					mediaPlayerStateListener.mediaPlayerDidStop(DlnaMediaPlayerApi.this);
				}
			}
            @Override
            public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
            	Log.e(TAG, defaultMsg);
            	detachedFileServer.stop();
            }
        });
        
		return true;
	}
	private void stopHttpFileServer() {
		if (simpleHttpFileServer != null) {
			simpleHttpFileServer.stop();
			simpleHttpFileServer = null;
		}
	}
	@Override
	public boolean play(Context context, String url, String userAgentString,
			Long mediaContentLength, String title) throws Exception {
		if (avtransportService == null) return false;
		
		stopHttpFileServer();
		Uri mediaUri = null;
		try {
			mediaUri = Uri.parse(url);
			if (mediaUri.getScheme() == null) {
				mediaUri = mediaUri.buildUpon().scheme("file").build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			mediaUri = Uri.fromFile(new File(url));
		}
		String mediaUriString = url;
		boolean isAudio = false;
		if (mediaUri.getScheme().equalsIgnoreCase(ContentResolver.SCHEME_CONTENT) || 
				mediaUri.getScheme().equalsIgnoreCase("file")) {
			simpleHttpFileServer = new SimpleContentUriHttpFileServer(context, mediaUri, 0) {
				@Override
				protected long getContentLengthForByteRangeResponse(final long fileLen, final long dataLen) {
					return fileLen;
				}
			};
			try {
				simpleHttpFileServer.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
			String mimeType = simpleHttpFileServer.getMimeType();
			if ((mimeType != null && mimeType.startsWith("audio")) || 
					MediaStreamingFileDataSource.isAudioFileExt(Utils.getFileExtension(mediaUriString.toString()))) {
				isAudio = true;
			}
			mediaUriString = simpleHttpFileServer.getServerUrl();
		}
		setAvTransportUri(mediaUriString, title, isAudio);

		return true;
	}
	private void setAvTransportUri(String mediaUriString, String title, boolean isAudio) throws Exception {
		Log.d(TAG+".SetAVTransportURI", "play: "+mediaUriString);
		DIDLContent didl = new DIDLContent();
		if (isAudio)  {
			MimeType mimeType = new MimeType("audio", "*");			
			didl.addItem(new AudioItem("1", "0", title, null, new Res(mimeType, 0l, null, null, mediaUriString)));			
		} else {
			MimeType mimeType = new MimeType("video", "*");			
			didl.addItem(new VideoItem("1", "0", null, null, new Res(mimeType, 0l, null, null, mediaUriString)));
		}
		String metadata = new DIDLParser().generate(didl);
		UpnpService.getUpnpService().execute(new SetAVTransportURI(avtransportService, mediaUriString, metadata) {
			@Override
			public void failure(ActionInvocation invocation,
					UpnpResponse response, String defaultMsg) {
				Log.e(TAG+".SetAVTransportURI", defaultMsg);
				if (mediaPlayerStateListener != null) {
					// TODO mapping error
					mediaPlayerStateListener.mediaPlayerDidFailed(DlnaMediaPlayerApi.this, AV_RESULT_ERROR_GENERIC);
				}
			}
			@Override
			public void success(ActionInvocation invocation) {
				if (avtransportService == null) return;
				if (mediaPlayerStateListener != null) {
					mediaPlayerStateListener.mediaPlayerDidStart(DlnaMediaPlayerApi.this);
				}
				playImp();
			}
		});
	}
	private void playImp() {
		if (avtransportService == null) return;
		
		UpnpService.getUpnpService().execute(new Play(avtransportService) {
			@Override
			public void success(ActionInvocation invocation) {
			}
			@Override
			public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
				// Something was wrong
				Log.e(TAG, defaultMsg);
				if (mediaPlayerStateListener != null) {
					// TODO mapping error
					mediaPlayerStateListener.mediaPlayerDidFailed(DlnaMediaPlayerApi.this, AV_RESULT_ERROR_GENERIC);
				}
			}
		});
	}
	private void startPeriodicalPoller() {
		if (timerThread != null) return;
		timerThread = new HandlerThread("DlnaTimerThread");
		timerThread.start();
		timerHandler = new Handler(timerThread.getLooper());
		schedulePlaybackInfoPoller();
	}
	private void schedulePlaybackInfoPoller() {
		timerHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				getPositionInfo();
			}
			
		}, 1000);
	}
	private void stopPeriodicalPoller() {
		if (timerThread != null) {
			timerThread.quit();
			timerThread = null;
		}
	}
	private void getPositionInfo() {
		UpnpService.getUpnpService().execute(new GetPositionInfo(avtransportService) {

			@Override
			public void received(ActionInvocation arg0,
					PositionInfo arg1) {
				schedulePlaybackInfoPoller();
				int currentPosition = parseFormattedTimeString(arg1.getRelTime());
				Log.d(TAG, "GetPositionInfo received:"+currentPosition);
				if (mediaPlayerStateListener != null) {
					mediaPlayerStateListener.mediaPlayerTimeDidChange(DlnaMediaPlayerApi.this, currentPosition);
				}
			}

			@Override
			public void failure(ActionInvocation arg0,
					UpnpResponse arg1, String arg2) {
				Log.e(TAG, "GetPositionInfo failed:"+arg2);
				getPositionInfo();
			}
			
		});
	}
	private void getMediaInfo() {
		UpnpService.getUpnpService().execute(new GetMediaInfo(avtransportService) {

			@Override
			public void received(ActionInvocation arg0, MediaInfo mediaInfo) {
				String durationStr = mediaInfo.getMediaDuration();
				int duration = parseFormattedTimeString(durationStr);
				Log.d(TAG, "GetMediaInfo:received: duration="+durationStr+"("+duration+")");
				if (duration != 0) {
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDurationIsReady(DlnaMediaPlayerApi.this, duration);
					}
				}
			}

			@Override
			public void failure(ActionInvocation arg0,
					UpnpResponse arg1, String arg2) {
				if (mediaPlayerStateListener != null) {
					// TODO mapping error
					mediaPlayerStateListener.mediaPlayerDidFailed(DlnaMediaPlayerApi.this, AV_RESULT_ERROR_GENERIC);
				}
			}
			
		});
	}

}
