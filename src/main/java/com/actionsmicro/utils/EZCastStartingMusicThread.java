package com.actionsmicro.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

public class EZCastStartingMusicThread {
	
	private Thread thread;
	Handler uiThreadHanlder;
	public EZCastStartingMusicThread(final Context context, final int rawID) {
		uiThreadHanlder = new Handler(Looper.getMainLooper());
		
		thread = new Thread(new Runnable() {

			@Override
			public void run() {
				final MediaPlayer mediaPlayer = MediaPlayer.create(context, rawID);
				mediaPlayer.start();
			}

		});
	}
	public void play(int delay) {
		uiThreadHanlder.postDelayed(thread, delay);
	}

}
