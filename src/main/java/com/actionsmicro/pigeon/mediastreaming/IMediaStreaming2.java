package com.actionsmicro.pigeon.mediastreaming;

import android.content.Context;

import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.media.playlist.PlayList;
import com.actionsmicro.pigeon.MediaStreaming;

public interface IMediaStreaming2 extends MediaStreaming {
    void playPlayList(Context context, PlayList playlist) throws Exception;

    void next();

    void previous();

    String getCurrentMedia();

    String getCurrentPlaylist();

    void setMediaStreamingStateListener(MediaPlayerApi api, MediaPlayerApi.MediaPlayerStateListener mediaPlayerStateListener);
}
