package com.actionsmicro.androidkit.ezcast.imp.dlna;

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
import org.fourthline.cling.support.lastchange.LastChange;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.SeekMode;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.renderingcontrol.callback.GetVolume;
import org.fourthline.cling.support.renderingcontrol.callback.SetVolume;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApiBuilder;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.Utils;

public class DlnaMediaPlayerApi extends DlnaApi implements MediaPlayerApi {

	private static final String TAG = "DlnaMediaPlayerApi";
	private Service avtransportService;
	private State state = State.UNKNOWN;
	private SubscriptionCallback avtransportSubscription;
	private MediaPlayerStateListener mediaPlayerStateListener;
	private HandlerThread timerThread;
	private Handler timerHandler;
	
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
						updateDurationIfNeeded(lastChange);
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
				org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable.TransportState eventedValue = lastChange.getEventedValue(0, AVTransportVariable.TransportState.class);
				if (eventedValue == null) return;
				TransportState transportState = eventedValue.getValue();
				if (transportState != null) {
					Log.d(TAG+".SubscriptionCallback", "transportState:"+transportState);
					if (TransportState.PLAYING == transportState) {
						state = State.PLAYING;
						startPeriodicalPoller();
						getMediaInfo();
					} else if (TransportState.PAUSED_PLAYBACK == transportState) {
						state = State.PAUSED;
					} else if (TransportState.STOPPED == transportState) {
						state = State.STOPPED;
						stopPeriodicalPoller();
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
//		LastChange das;
//		SubscriptionCallback dsa;
		super.connect();
	}
	@Override
	public void disconnect() {
		super.disconnect();
		stopPeriodicalPoller();
		
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
        UpnpService.getUpnpService().execute(new Stop(avtransportService) {
        	@Override
			public void success(ActionInvocation invocation) {
        		stopPeriodicalPoller();
				if (mediaPlayerStateListener != null) {
					mediaPlayerStateListener.mediaPlayerDidStop(DlnaMediaPlayerApi.this);
				}
			}
            @Override
            public void failure(ActionInvocation invocation, UpnpResponse response, String defaultMsg) {
            	Log.e(TAG, defaultMsg);
            }
        });
        
		return true;
	}

	@Override
	public boolean play(Context context, String url, String userAgentString,
			Long mediaContentLength, String title) throws Exception {
		if (avtransportService == null) return false;
		UpnpService.getUpnpService().execute(new SetAVTransportURI(avtransportService, url, null) {
			@Override
			public void failure(ActionInvocation invocation,
					UpnpResponse response, String defaultMsg) {
				Log.e(TAG+".SetAVTransportURI", defaultMsg);
			}
			@Override
			public void success(ActionInvocation invocation) {
				if (avtransportService == null) return;
				if (mediaPlayerStateListener != null) {
					mediaPlayerStateListener.mediaPlayerDidStart(DlnaMediaPlayerApi.this);
				}
				playImp();
				getMediaInfo();
			}
		});

		return true;
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
	private static int parseFormattedTimeString(String durationStr) {
		String time[] = durationStr.split(":");
		int duration = 0;
		for (int i = 0; i < time.length ; i++) {
			duration = duration * 60 + Integer.valueOf(time[i]);
		}
		return duration;
	}

}
