package com.actionsmicro.media.videoobj;


import java.util.List;

// POJO
public class VideoObj {
    private int code;
    private String title;
    private String image;
    private String src;
    private String page;
    private List<VideoCodec> video;
    private List<AudioCodec> audio;
    private List<String> cutlist;
    private String header;

    public List<String> getCutlist() {
        return cutlist;
    }

    public String getHeader() {
        return header;
    }

    public String getImage() {
        return image;
    }

    public List<VideoObj> getPlaylist() {
        return playlist;
    }

    private List<VideoObj> playlist;


    public List<AudioCodec> getAudio() {
        return audio;
    }

    public int getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getSrc() {
        return src;
    }

    public List<VideoCodec> getVideo() {
        return video;
    }

    public String getPage() {
        return page;
    }
}
