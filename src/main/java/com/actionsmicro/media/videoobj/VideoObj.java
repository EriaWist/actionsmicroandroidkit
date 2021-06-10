package com.actionsmicro.media.videoobj;


import org.json.JSONObject;

import java.util.List;

// POJO
public class VideoObj {
    private int code;
    private String ercode;
    private String remark;
    private String title;
    private String image;
    private String src;
    private String page;
    private List<VideoCodec> video;
    private List<AudioCodec> audio;
    private List<String> cutlist;
    private JSONObject header;
    private String type;
    // for MusicMediaItem
    private String index;
    private long mediaId;
    private String mediaName;
    private String artistName;
    private String albumName;
    private long albumId;
    private int duration;
    private String data;
    // for Local video sub title
    private List<Caption> captions;

    public List<String> getCutlist() {
        return cutlist;
    }

    public JSONObject getHeader() {
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

    public String getErcode() {
        return ercode;
    }

    public String getRemark() {
        return remark;
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

    public void setPage(String page) {
        this.page = page;
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

    public List<Caption> getCaptions() {
        return captions;
    }

    public void setCaptions(List<Caption> captions) {
        this.captions = captions;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
