package com.actionsmicro.androidrx.app;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.MediaController;

import com.actionsmicro.utils.Log;

public class MediaPlayerHelper {

	private static final String TAG = "MediaPlayerHelper";
	private TextureView textureView;
	private MediaPlayer mediaPlayer;
	private ViewGroup container;
	private PlayerListener playerListener;
	private boolean stopped;
	private MediaController mediaController;
	protected int bufferPercentage;
	protected int duration = -1;
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
	public MediaPlayerHelper(final Context context, final ViewGroup container, PlayerListener playerListener) {
		this.container = container;
		this.playerListener = playerListener;
		initTextureView(context, container, initMediaPlayer());
		initMediaController(context, container);
	}

	private void initMediaController(final Context context,
			final ViewGroup container) {
		container.post(new Runnable() {
			private int getNavBarHeight() {
				Resources resources = context.getResources();
				int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
				if (resourceId > 0) {
				    return resources.getDimensionPixelSize(resourceId);
				}
				return 96;
			}
			@Override
			public void run() {
				
				mediaController = new MediaController(context);
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				params.bottomMargin = getNavBarHeight();
				mediaController.setLayoutParams(params);
				mediaController.setAnchorView(textureView);
				mediaController.setMediaPlayer(new MediaController.MediaPlayerControl() {
					
					@Override
					public void start() {
						Log.v(TAG, "MediaPlayerControl.start");
					}
					
					@Override
					public void seekTo(int pos) {
						Log.v(TAG, "MediaPlayerControl.seekTo:"+pos);
						
					}
					
					@Override
					public void pause() {
						Log.v(TAG, "MediaPlayerControl.pause:");
						
					}
					
					@Override
					public boolean isPlaying() {
						if (mediaPlayer != null) {
							return mediaPlayer.isPlaying();
						}
						return false;
					}
					
					@Override
					public int getDuration() {
						Log.v(TAG, "MediaPlayerControl.getDuration:");
						if (mediaPlayer != null) {
							Log.v(TAG, "MediaPlayerControl.getDuration:"+mediaPlayer.getDuration());
							return mediaPlayer.getDuration();
						}
						return 0;
					}
					
					@Override
					public int getCurrentPosition() {
						Log.v(TAG, "MediaPlayerControl.getCurrentPosition:");
						if (mediaPlayer != null) {
							Log.v(TAG, "MediaPlayerControl.getCurrentPosition:"+mediaPlayer.getCurrentPosition());
							return mediaPlayer.getCurrentPosition();
						}
						return 0;
					}
					
					@Override
					public int getBufferPercentage() {
						return bufferPercentage;
					}
					
					@Override
					public int getAudioSessionId() {
						// TODO Auto-generated method stub
						return 0;
					}
					
					@Override
					public boolean canSeekForward() {
						return true;
					}
					
					@Override
					public boolean canSeekBackward() {
						return true;
					}
					
					@Override
					public boolean canPause() {
						return false;
					}
				});
			}
			
		});
	}

	private void initTextureView(final Context context,
			final ViewGroup container, final MediaPlayer mediaPlayer) {
		synchronized (container) {
			container.post(new Runnable() {

				@Override
				public void run() {
					textureView = new TextureView(context);
					textureView.setSurfaceTextureListener(new SurfaceTextureListener() {

						@Override
						public void onSurfaceTextureAvailable(SurfaceTexture surface,
								int width, int height) {
							Log.v(TAG, "onSurfaceTextureAvailable:"+surface);
							
							mediaPlayer.setSurface(new Surface(surface));
						}

						@Override
						public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
							// TODO Auto-generated method stub
							return true;
						}

						@Override
						public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
								int width, int height) {
							// TODO Auto-generated method stub

						}

						@Override
						public void onSurfaceTextureUpdated(SurfaceTexture surface) {
						}

					});
					synchronized (container) {
						container.addView(textureView);
						container.notifyAll();
					}
				}

			});
			try {
				container.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private MediaPlayer initMediaPlayer() {
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {

			@Override
			public void onBufferingUpdate(MediaPlayer mp, int percent) {
				Log.v(TAG, "onBufferingUpdate:"+percent+"%");
				bufferPercentage = percent;
			}
			
		});
		mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				Log.v(TAG, "onCompletion:");
				stop();
				if (playerListener != null) {
					playerListener.onEnded();
				}
			}
			
		});
		mediaPlayer.setOnErrorListener(new OnErrorListener() {

			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				Log.v(TAG, "onError: what:"+what+", extra:"+extra);
				if (what != -38) { // MediaPlayer keeps sending -38 for unknown reason after error occurs. Let's filter it.
					if (playerListener != null) {
						playerListener.onError(convertErrorCode(what, extra));
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
		mediaPlayer.setOnInfoListener(new OnInfoListener() {

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
			
		});
		mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

			@Override
			public void onPrepared(MediaPlayer mp) {
				Log.v(TAG, "onPrepared:");
				if (playerListener != null) {
					playerListener.onLoadedMetadata();
				}
				scheduleInfoPoller();
			}
			
		});
		mediaPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener() {

			@Override
			public void onSeekComplete(MediaPlayer mp) {
				Log.v(TAG, "onSeekComplete:");		
				if (playerListener != null) {
					playerListener.onSeeked();
				}
			}
			
		});
		return mediaPlayer;		
	}

	private void setDuration(int duration) {
		if (this.duration != duration) {
			this.duration = duration;
			if (playerListener != null) {
				playerListener.onDurationChange(duration);
			}
		}
	}

	private void scheduleInfoPoller() {
		if (textureView != null) {
			textureView.postDelayed(new Runnable() {

				@Override
				public void run() {
					if (!stopped) {
						if (mediaPlayer != null) {
							if (playerListener != null) {
								playerListener.onTimeUpdate(mediaPlayer.getCurrentPosition()/1000);
							}
							setDuration(mediaPlayer.getDuration()/1000);
							scheduleInfoPoller();
						}
					}
				}
				
			}, 1000);
		}
	}

	public void load(String url) {
		Log.v(TAG, "load:"+url);
		if (mediaPlayer != null) {
			try {
				mediaPlayer.reset();
				mediaPlayer.setDataSource(url);
				if (playerListener != null) {
					playerListener.onLoadStart();
				}
				mediaPlayer.prepare();			
				showControl();

			} catch (Throwable e) {
				if (playerListener != null) {
					playerListener.onError(4);
				}
				e.printStackTrace();
			}
		}
	}

	public void play(int startpos) {
		Log.v(TAG, "play:"+startpos);
		if (mediaPlayer != null) {
			try {
				if (startpos != 0) {
					mediaPlayer.seekTo(startpos);
				}
				mediaPlayer.start();
				showControl();
				if (playerListener != null) {
					playerListener.onPlay();
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	private void showControl() {
		if (textureView != null) {
			textureView.post(new Runnable() {

				@Override
				public void run() {
					try {
						if (mediaController != null) {
							mediaController.show();
						}
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}

			});
		}
	}

	public void stop() {
		Log.v(TAG, "stop:");
		if (mediaPlayer != null) {
			try {
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.stop();
				}
				final MediaPlayer mediaPlayerToBeReleased = mediaPlayer;
				final MediaController mc = mediaController;
				textureView.post(new Runnable() {

					@Override
					public void run() {
						try {
							mediaPlayerToBeReleased.release();
							if (mc != null) {
								mc.hide();
							}
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
					
				});
				mediaPlayer = null;
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		if (textureView != null) {
			final View viewToBeRemoved = textureView;
			textureView.post(new Runnable() {

				@Override
				public void run() {
					container.removeView(viewToBeRemoved);					
				}
				
			});
			textureView = null;
		}
		stopped = true;
		duration = -1;
	}

	public void pause() {
		Log.v(TAG, "pause:");
		if (mediaPlayer != null) {
			try {
				mediaPlayer.pause();
				showControl();
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
		if (mediaPlayer != null) {
			try {
				mediaPlayer.start();
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

	public void seek(int msec) {
		Log.v(TAG, "seek:"+msec);
		if (mediaPlayer != null) {
			try {
				mediaPlayer.seekTo(msec);
				showControl();

			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

}
