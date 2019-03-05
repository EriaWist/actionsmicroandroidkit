package com.actionsmicro.media.control;

import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.media.adapter.MediaAdapterInterface;
import com.actionsmicro.media.item.MediaItem;

import java.util.ArrayList;

/**
 * Created by laicc on 2015/8/21.
 */
public interface MediaControlInterface {
    void play();
    void play(int position);
    void play(String index);
    void stop();
    boolean pause();
    boolean resume();
    void setRepeat(boolean repeat);
    void seek(int position);
    void next();
    void previous();
    void decreaseVolume();
    void increaseVolume();
    long getDuration();
    boolean isVideo();
    MediaPlayerApi.State getMediaPlayerState();

    void setAdapter(MediaAdapterInterface mediaAdapter);
    MediaAdapterInterface getAdapter();
    ArrayList<MediaItem> getList();
    void setMediaPlayerStateListener(MediaPlayerApi.MediaPlayerStateListener mediaPlayerStateListener);
    void setMediaPlayListListener(MediaPlayListListener mediaPlayListListener);

    int getCurrentPlaying();

    void release();
}
