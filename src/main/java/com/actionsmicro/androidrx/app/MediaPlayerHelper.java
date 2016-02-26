package com.actionsmicro.androidrx.app;

import android.content.Context;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.VideoView;

import com.actionsmicro.utils.Log;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import java.util.ArrayList;

public class MediaPlayerHelper implements IVLCVout.Callback, LibVLC.HardwareAccelerationError{

	private static final String TAG = "MediaPlayerHelper";
	private ViewGroup container;
	private PlayerListener playerListener;
	private boolean stopped;
//	private MediaController mediaController;
	protected int duration = -1;
	private VideoView videoView;
	protected MediaPlayer mediaPlayer;
	private Context mCtx;

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
		mCtx = context;
		this.container = container;
		this.playerListener = playerListener;
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				initSurfaceView(context,container,playerListener);
//				initVideoView(context, container, playerListener);
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

	private void initSurfaceView(final Context context,
							   final ViewGroup container, final PlayerListener playerListener) {
		mSurface = new SurfaceView(context);
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
		mSurface.setLayoutParams(layoutParams);
		container.addView(mSurface);
		createMediaPlayerifNeeded();
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
//		mediaController = new MediaController(context);
//		videoView.setMediaController(mediaController);
	}

//	@SuppressLint("NewApi")
//	private void setOnInfoListener(final PlayerListener playerListener) {
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//			videoView.setOnInfoListener(onInfoListener);
//		}
//	}
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
		setMediaPath(url);
		if (playerListener != null) {
			playerListener.onLoadStart();
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
		playVideoAtPath();
		if (playerListener != null) {
			playerListener.onPlay();
		}

//		if (videoView != null) {
//			try {
//				if (startpos != 0) {
//					videoView.seekTo(startpos);
//				}
//				videoView.start();
////				showControl();
//				if (playerListener != null) {
//					playerListener.onPlay();
//				}
//			} catch (Throwable e) {
//				e.printStackTrace();
//			}
//		}
	}

	private void setMediaPath(String media) {
		Media m;
		if (media.contains("http")) {
			m = new Media(libvlc, Uri.parse(media));
		} else {
			m = new Media(libvlc, media);
		}

		if (media.contains("mov") || media.contains("mts")) {
			m.setHWDecoderEnabled(false, false);
		}

		mMediaPlayer.setMedia(m);
	}

	private void playVideoAtPath() {
		try {
			mMediaPlayer.play();
		} catch (Exception e) {
			Log.e(TAG, "Error creating player!", e);
		}
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
		if(mMediaPlayer!=null) {
			mMediaPlayer.stop();
			container.removeView(mSurface);
			releasePlayer();
		}
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
		if (mMediaPlayer != null) {
			mMediaPlayer.pause();
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
		if (mMediaPlayer != null) {
			mMediaPlayer.play();
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
		if(null != mMediaPlayer) {
			long leng = mMediaPlayer.getLength();
			if (0l == leng) {
				mMediaPlayer.setTime(msec);
			} else {
				mMediaPlayer.setPosition(msec * 1f / mMediaPlayer.getLength());
			}
		}
	}

	private SurfaceHolder holder;
	private LibVLC libvlc;
	private org.videolan.libvlc.MediaPlayer mMediaPlayer = null;
	private int mVideoWidth;
	private int mVideoHeight;
	private org.videolan.libvlc.MediaPlayer.EventListener mPlayerListener = new PlayerEventListener();
	private SurfaceView mSurface;

	private class PlayerEventListener implements org.videolan.libvlc.MediaPlayer.EventListener {
		@Override
		public void onEvent(org.videolan.libvlc.MediaPlayer.Event event) {
			Log.d(TAG,"event = " + event.type);
			switch(event.type) {
				case org.videolan.libvlc.MediaPlayer.Event.Opening:
					break;
				case org.videolan.libvlc.MediaPlayer.Event.EndReached:
					playerListener.onEnded();
//					mediaPlayerStateListener.mediaPlayerDidStop(DemoMediaPlayerApi.this, MediaPlayerApi.Cause.REMOTE);
					break;
				case org.videolan.libvlc.MediaPlayer.Event.TimeChanged:
					break;
				case org.videolan.libvlc.MediaPlayer.Event.SeekableChanged:
					break;
				case org.videolan.libvlc.MediaPlayer.Event.PositionChanged:
					playerListener.onTimeUpdate((int) (mMediaPlayer.getTime()/1000));
					break;
				case org.videolan.libvlc.MediaPlayer.Event.Playing:
					playerListener.onPlaying();
					playerListener.onDurationChange((int) (mMediaPlayer.getLength()/1000));
//					setDuration((int) (mMediaPlayer.getLength()/1000));
//					mState = MediaPlayerApi.State.PLAYING;
//					mediaPlayerStateListener.mediaPlayerDidStart(DemoMediaPlayerApi.this);
//					mediaPlayerStateListener.mediaPlayerDurationIsReady(DemoMediaPlayerApi.this,mMediaPlayer.getLength()/1000);
					break;
				case org.videolan.libvlc.MediaPlayer.Event.Paused:
					playerListener.onPaused();
//				}
				case org.videolan.libvlc.MediaPlayer.Event.Stopped:
				default:
					break;
			}
		}
	}

	private void initVLCPlayer() {
		if (null == libvlc) {
			ArrayList<String> options = new ArrayList<String>();

			//options.add("--subsdec-encoding <encoding>");
			options.add("--aout=opensles");
			options.add("--audio-time-stretch"); // time stretching
			options.add("-vvv"); // verbosity

			libvlc = new LibVLC(options);

			libvlc.setOnHardwareAccelerationError(this);
		}

		if(mMediaPlayer == null) {
			mMediaPlayer = new org.videolan.libvlc.MediaPlayer(libvlc);
			// Create media player
			mMediaPlayer.setEventListener(mPlayerListener);
		}
	}

	private void createMediaPlayerifNeeded() {
		initVLCPlayer();

		if (mSurface != null) {
			if (!mMediaPlayer.getVLCVout().areViewsAttached()) {
				holder = mSurface.getHolder();
				holder.setKeepScreenOn(true);
				holder.addCallback(new SurfaceHolder.Callback2() {
					@Override
					public void surfaceRedrawNeeded(SurfaceHolder holder) {
						final IVLCVout vout = mMediaPlayer.getVLCVout();
						if(!vout.areViewsAttached()) {
							vout.setVideoView(mSurface);
							vout.addCallback(MediaPlayerHelper.this);
							vout.attachViews();
						}
					}

					@Override
					public void surfaceCreated(SurfaceHolder holder) {
					}

					@Override
					public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
					}

					@Override
					public void surfaceDestroyed(SurfaceHolder holder) {
					}
				});
				// Set up video output
				final IVLCVout vout = mMediaPlayer.getVLCVout();
				vout.setVideoView(mSurface);
				//vout.setSubtitlesView(mSurfaceSubtitles);
				vout.addCallback(this);
				vout.attachViews();
			}
		}
	}

	@Override
	public void onNewLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
		if (width * height == 0)
			return;

		// store video size
		mVideoWidth = width;
		mVideoHeight = height;
		setSize(mVideoWidth, mVideoHeight);
	}

	@Override
	public void onSurfacesCreated(IVLCVout vout) {

	}

	@Override
	public void onSurfacesDestroyed(IVLCVout vout) {
		vout.removeCallback(this);
		vout.detachViews();
	}

	@Override
	public void eventHardwareAccelerationError() {
		Log.e(TAG, "Error with hardware acceleration");
		releasePlayer();
		Toast.makeText(mCtx, "Error with hardware acceleration", Toast.LENGTH_LONG).show();

	}

	private void setSize(int width, int height) {
		mVideoWidth = width;
		mVideoHeight = height;
		if (mVideoWidth * mVideoHeight <= 1)
			return;

		if(holder == null || mSurface == null)
			return;

		// get screen size
		DisplayMetrics metrics = mCtx.getResources().getDisplayMetrics();
		int w = metrics.widthPixels;
		int h = metrics.heightPixels;

		boolean isPortrait = mCtx.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		if (w > h && isPortrait || w < h && !isPortrait) {
			int i = w;
			w = h;
			h = i;
		}

		float videoAR = (float) mVideoWidth / (float) mVideoHeight;
		float screenAR = (float) w / (float) h;

		if (screenAR < videoAR)
			h = (int) (w / videoAR);
		else
			w = (int) (h * videoAR);

		// force surface buffer size
		holder.setFixedSize(mVideoWidth, mVideoHeight);

		// set display size
		ViewGroup.LayoutParams lp = mSurface.getLayoutParams();
		lp.width = w;
		lp.height = h;
		mSurface.setLayoutParams(lp);
		mSurface.invalidate();
	}

	private void releasePlayer() {
		if (libvlc == null)
			return;

		final IVLCVout vout = mMediaPlayer.getVLCVout();
		vout.removeCallback(this);
		vout.detachViews();
		holder = null;
		libvlc.release();
		libvlc = null;

		mVideoWidth = 0;
		mVideoHeight = 0;
		mMediaPlayer.stop();
		mMediaPlayer.release();
		mMediaPlayer = null;
	}
}
