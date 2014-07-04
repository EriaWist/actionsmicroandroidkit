package com.actionsmicro.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;

public class EZCastStartingMusicThread {
	
	private Thread thread;
	public EZCastStartingMusicThread(final Context context, final int rawID) {
		thread = new Thread(new Runnable() {

			@Override
			public void run() {
				final MediaPlayer mediaPlayer = MediaPlayer.create(context, rawID);
				mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
					
					@Override
					public void onCompletion(MediaPlayer arg0) {
						mediaPlayer.stop();
					}
				});
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.stop();
				}
				mediaPlayer.start();
			}

		});
	}
	public void play() {
		thread.start();
	}

}
