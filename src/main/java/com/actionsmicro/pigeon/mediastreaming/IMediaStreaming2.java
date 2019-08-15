package com.actionsmicro.pigeon.mediastreaming;

import com.actionsmicro.pigeon.MediaStreaming;

public interface IMediaStreaming2 extends MediaStreaming {
    void playPlayList(String playlist);

    void next();

    void previous();

    String getCurrentMedia();

    String getCurrentPlaylist();


}
