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
    private String type;
    // for MusicMediaItem
    public String index;
    public long mediaId;
    public String mediaName;
    public String artistName;
    public String albumName;
    public long albumId;
    public int duration;
    public String data;

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

    public void setSrc(String src) {
        this.src = src;
    }

    public List<VideoCodec> getVideo() {
        return video;
    }

    public String getPage() {
        return page;
    }

    public String getType() {
        return type;
    }

    public boolean isAudio() {
        return type != null && type.equals("audio");
    }


    public String getIndex() {
        return index;
    }

    public long getMediaId() {
        return mediaId;
    }

    public String getMediaName() {
        return mediaName;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public long getAlbumId() {
        return albumId;
    }

    public int getDuration() {
        return duration;
    }

    public String getData() {
        return data;
    }
}
