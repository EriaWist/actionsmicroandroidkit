package com.actionsmicro.media.item;

import android.os.Parcelable;

/**
 * Created by laicc on 2015/6/26.
 */
public abstract class MediaItem implements Parcelable {

    private String index = "";
    private String title = "No Title";
    private String imageUri = "";
    private String description = "";
    private String source = null;

    protected void setIndex (String index) {
        this.index = index;
    }
    protected void setTitle(String title) {
        this.title = title;
    }
    protected void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
    protected void setSource(String source) {
        this.source = source;
    }
    protected void setExtraDescription(String description) {
        this.description = description;
    }

    public String getIndex() {
        return index;
    }
    public String getTitle() {
        return title;
    }
    public String getImage() {
        return imageUri;
    }
    public String getSource() {
        return source;
    }
    public String getExtraDescription() {
        return description;
    }
    public String getSourceType() {
        return "";
    }
}