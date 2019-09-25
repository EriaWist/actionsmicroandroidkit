package com.actionsmicro.androidrx.app;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import com.actionsmicro.utils.Log;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import java.util.ArrayList;

public class MediaPlayerHelper implements IVLCVout.Callback{
	private static final String TAG = "MediaPlayerHelper";
	private ViewGroup container;
	private PlayerListener playerListener;
	protected int duration = -1;
	protected MediaPlayer mediaPlayer;
	private Context mCtx;
	private View.OnLayoutChangeListener mOnLayoutChangeListener;
	private IVLCVout.OnNewVideoLayoutListener mOnNewVideoLayoutListener = new IVLCVout.OnNewVideoLayoutListener(){

		@Override
		public void onNewVideoLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
			mSurface.setBackgroundColor(Color.TRANSPARENT);
			// store video size
			mVideoWidth = width;
			mVideoHeight = height;
			setSize(mVideoWidth, mVideoHeight);
		}
	};

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
		mSurface.setBackgroundColor(Color.BLACK);
		container.addView(mSurface);
		createMediaPlayerifNeeded();
	}


	private void setDuration(int duration) {
		if (this.duration != duration) {
			this.duration = duration;
			if (playerListener != null) {
				playerListener.onDurationChange(duration);
			}
		}
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
		playMediaAtPath();
		if (playerListener != null) {
			playerListener.onPlay();
		}
	}

	private void setMediaPath(String mediaUrl) {
		setMediaPath(mediaUrl, false);
	}

	private void setMediaPath(String mediaPath, boolean enableHWDecoded) {
		Media m;
		if (mediaPath.contains("http")) {
			m = new Media(libvlc, Uri.parse(mediaPath));
		} else {
			m = new Media(libvlc, mediaPath);
		}

		if (!enableHWDecoded|| mediaPath.contains("mov") || mediaPath.contains("mts")) {
			m.setHWDecoderEnabled(false, false);
		}

		mMediaPlayer.setMedia(m);
		m.release();
	}

	private void playMediaAtPath() {
		try {
			mMediaPlayer.play();
		} catch (Exception e) {
			Log.e(TAG, "Play fail", e);
		}
	}

	public void stop() {
		Log.v(TAG, "stop:");
		if (null != mOnLayoutChangeListener) {
			container.removeOnLayoutChangeListener(mOnLayoutChangeListener);
		}
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
			mSurface.setBackgroundColor(Color.BLACK);
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
					setDuration((int) (mMediaPlayer.getLength()/1000));
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
			options.add("--vout=android_display,none");
			options.add("-vvv"); // verbosity

			libvlc = new LibVLC(mCtx,options);
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
	public void onSurfacesCreated(IVLCVout vout) {

	}

	@Override
	public void onSurfacesDestroyed(IVLCVout vout) {
		vout.removeCallback(this);
		vout.detachViews();
	}

	private void setSize(int width, int height) {
		mVideoWidth = width;
		mVideoHeight = height;
		if (mVideoWidth * mVideoHeight <= 1)
			return;

		if(holder == null || mSurface == null)
			return;

		// force surface buffer size
		holder.setFixedSize(mVideoWidth, mVideoHeight);

		// get screen size
//		DisplayMetrics metrics = mCtx.getResources().getDisplayMetrics();
		updateSurfaceLayout();

		mOnLayoutChangeListener = new View.OnLayoutChangeListener() {
			@Override
			public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
									   int oldTop, int oldRight, int oldBottom) {

				updateSurfaceLayout();
			}
		};
		container.addOnLayoutChangeListener(mOnLayoutChangeListener);
	}

	private void updateSurfaceLayout() {
		int w = container.getWidth();
		int h = container.getHeight();

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

		// set display size
		LayoutParams lp = mSurface.getLayoutParams();
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
