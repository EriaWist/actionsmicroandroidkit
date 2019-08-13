package com.actionsmicro.pigeon;

public interface IMediaStreaming2 extends MediaStreaming {
    void playPlayList(String playlist);

    void next();

    void previous();

    String getCurrentMedia();

    String getCurrentPlaylist();


}
