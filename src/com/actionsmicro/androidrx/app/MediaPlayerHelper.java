package com.actionsmicro.androidrx.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.VideoView;

import com.actionsmicro.utils.Log;

public class MediaPlayerHelper {

	private static final String TAG = "MediaPlayerHelper";
	private ViewGroup container;
	private PlayerListener playerListener;
	private boolean stopped;
	private MediaController mediaController;
	protected int bufferPercentage;
	protected int duration = -1;
	private VideoView videoView;
	protected MediaPlayer mediaPlayer;
	public interface PlayerListener {
		public void onDurationChange(int seconds);
		public void onLoadStart();
		public void onPlay();
		public void onTimeUpdate(int currentTime);
		public void onError(int error);
		public void onEnded();
		public void onPaused();
		public void onWaiting();
		public void onSeeked();
		public void onPlaying();
		public void onLoadedMetadata();
	}
	public MediaPlayerHelper(final Context context, final ViewGroup container, final PlayerListener playerListener) {
		this.container = container;
		this.playerListener = playerListener;
		
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				initVideoView(context, container, playerListener);				
			}
			
		});
	}
	private Handler mainHandler = new Handler(Looper.getMainLooper()) ;
	private void runOnUiThread(Runnable runnable) {
		if (runnable != null) {
			if (Looper.myLooper() == Looper.getMainLooper()) {
				runnable.run();
			} else {
				if (!mainHandler.post(runnable)) {
					Log.e(TAG, "Cannot post runnable:"+runnable);
				}
			}
		}
	}
	private void initVideoView(final Context context,
			final ViewGroup container, final PlayerListener playerListener) {
		videoView = new VideoView(context);
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
		videoView.setLayoutParams(layoutParams);
		container.addView(videoView);
		videoView.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				Log.v(TAG, "onCompletion:");
				if (playerListener != null) {
					playerListener.onTimeUpdate(videoView.getCurrentPosition()/1000);
				}
				stop();
				if (playerListener != null) {
					playerListener.onEnded();
				}
			}
			
		});
		videoView.setOnErrorListener(new OnErrorListener() {

			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				Log.v(TAG, "onError: what:"+what+", extra:"+extra);
				if (what != -38) { // MediaPlayer keeps sending -38 for unknown reason after error occurs. Let's filter it.
					if (playerListener != null) {
						playerListener.onError(convertErrorCode(what, extra));
						return true;
					}					
				}
				return false;
			}
			private int convertErrorCode(int what, int extra) {
//				http://dev.w3.org/html5/spec-author-view/video.html#error-codes
//				interface MediaError {
//					  const unsigned short MEDIA_ERR_ABORTED = 1;
//					  const unsigned short MEDIA_ERR_NETWORK = 2;
//					  const unsigned short MEDIA_ERR_DECODE = 3;
//					  const unsigned short MEDIA_ERR_SRC_NOT_SUPPORTED = 4;
//					  readonly attribute unsigned short code;
//				};
				int convertedError = 4;
				switch (extra) {
				case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
				case MediaPlayer.MEDIA_ERROR_IO:
					convertedError = 2;
					break;
				case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
				case MediaPlayer.MEDIA_ERROR_MALFORMED:
					convertedError = 4;
					break;
				default:
					if (MediaPlayer.MEDIA_ERROR_SERVER_DIED == what) {
						convertedError = 2;
						
					}
					break;
				}
				return convertedError;
			}
			
		});
//		setOnInfoListener(playerListener);
		videoView.setOnPreparedListener(new OnPreparedListener() {

			@Override
			public void onPrepared(MediaPlayer mp) {
				Log.v(TAG, "onPrepared:");
				mediaPlayer = mp;
				if (mediaPlayer != null) {
					mediaPlayer.setOnInfoListener(onInfoListener);
				}
				if (playerListener != null) {
					playerListener.onLoadedMetadata();
				}
				scheduleInfoPoller();
			}
			
		});
		mediaController = new MediaController(context);
		videoView.setMediaController(mediaController);
	}

	@SuppressLint("NewApi")
	private void setOnInfoListener(final PlayerListener playerListener) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			videoView.setOnInfoListener(onInfoListener);
		}
	}
	private OnInfoListener onInfoListener = new OnInfoListener() {

		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra) {
			Log.v(TAG, "onInfo: what:"+what+", extra:"+extra);
			Log.v(TAG, "onInfo: duration:"+mp.getDuration()/1000);
			switch (what) {
			case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
				if (playerListener != null) {
					playerListener.onPlaying();
				}
				setDuration(mp.getDuration()/1000);					
				break;
			case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
				Log.v(TAG, "onInfo: duration:"+mp.getDuration()/1000);
				setDuration(mp.getDuration()/1000);					
				break;
			}
			return false;
		}

	};

	private void setDuration(int duration) {
		if (this.duration != duration) {
			this.duration = duration;
			if (playerListener != null) {
				playerListener.onDurationChange(duration);
			}
		}
	}

	private void scheduleInfoPoller() {
		mainHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (!stopped) {
					if (videoView != null) {
						if (playerListener != null) {
							playerListener.onTimeUpdate(videoView.getCurrentPosition()/1000);
						}
						setDuration(videoView.getDuration()/1000);
						scheduleInfoPoller();
					}
				}
			}

		}, 1000);
	}

	public void load(final String url) {
		Log.v(TAG, "load:"+url);
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				loadImp(url);
			}
			
		});
	}
	private void loadImp(String url) {
		if (videoView != null) {
			try {
				videoView.setVideoPath(url);
				if (playerListener != null) {
					playerListener.onLoadStart();
				}
				showControl(0);

			} catch (Throwable e) {
				if (playerListener != null) {
					playerListener.onError(4);
				}
				e.printStackTrace();
			}
		}
	}

	public void play(final int startpos) {
		Log.v(TAG, "play:"+startpos);
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				playImp(startpos);
			}
			
		});
	}
	private void playImp(int startpos) {
		if (videoView != null) {
			try {
				if (startpos != 0) {
					videoView.seekTo(startpos);
				}
				videoView.start();
				showControl();
				if (playerListener != null) {
					playerListener.onPlay();
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	private void showControl(final int timeout) {
		if (videoView != null) {
			videoView.post(new Runnable() {

				@Override
				public void run() {
					try {
						if (mediaController != null) {
							mediaController.show(timeout);
						}
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}

			});
		}
	}
	private void showControl() {
		showControl(3000);
	}
	public void stop() {
		Log.v(TAG, "stop:");
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				stopImp();
			}
			
		});
	}
	private void stopImp() {
		if (videoView != null) {
			try {
				if (videoView.isPlaying()) {
					videoView.stopPlayback();
				}
				container.removeView(videoView);					
				videoView = null;
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		stopped = true;
		duration = -1;
	}

	public void pause() {
		Log.v(TAG, "pause:");
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				pauseImp();
			}
			
		});

	}
	private void pauseImp() {
		if (videoView != null) {
			try {
				videoView.pause();
				showControl(0);
				if (playerListener != null) {
					playerListener.onPaused();
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	public void resume() {
		Log.v(TAG, "resume:");
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				resumeImp();
			}
			
		});
	}
	private void resumeImp() {
		if (videoView != null) {
			try {
				videoView.start();
				showControl();
				if (playerListener != null) {
					playerListener.onPlaying();
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	public void setVolume(float volume) {
		Log.v(TAG, "setVolume:"+volume);
		if (mediaPlayer != null) {
			try {
				mediaPlayer.setVolume(volume, volume);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	public void seek(final int msec) {
		Log.v(TAG, "seek:"+msec);
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				seekImp(msec);
			}
			
		});
	}
	private void seekImp(int msec) {
		if (videoView != null) {
			try {
				if (videoView.isPlaying()) {
					showControl();
				} else {
					showControl(0);					
				}
				videoView.seekTo(msec);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

}
