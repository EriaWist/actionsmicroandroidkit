package com.actionsmicro.media.playlist;


import com.actionsmicro.media.control.MediaPlayListListener;
import com.actionsmicro.media.videoobj.VideoObj;

import java.util.List;

public class PlayList {
    private String id;
    private List<VideoObj> playlist;
    private int start_index;
    private String error;

    public String getId() {
        return id;
    }

    public List<VideoObj> getPlaylist() {
        return playlist;
    }

    public int getStart_index() {
        return start_index;
    }

    public String getError() {
        return error;
    }


    private transient MediaPlayListListener mediaPlayListListener;

    public void setMediaPlayListListener(MediaPlayListListener mediaPlayListListener) {
        this.mediaPlayListListener = mediaPlayListListener;
    }

    public MediaPlayListListener getMediaPlayListListener() {
        return mediaPlayListListener;
    }

    public void setStart_index(int start_index) {
        this.start_index = start_index;
    }

    public void setPlaylist(List<VideoObj> playlist) {
        this.playlist = playlist;
    }
}
