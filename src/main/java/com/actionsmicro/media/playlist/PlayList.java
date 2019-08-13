package com.actionsmicro.media.playlist;


import com.actionsmicro.media.videoobj.VideoObj;

import java.util.List;

// POJO
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


}
