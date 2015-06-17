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
                if(null != mediaPlayer) {
                    mediaPlayer.start();
                }
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        mediaPlayer.release();
                    }
                });
            }

		});
	}
	public void play(int delay) {
		uiThreadHanlder.postDelayed(thread, delay);
	}

}
