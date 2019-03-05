package com.actionsmicro.media.item;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by laicc on 2015/9/8.
 */
public class VideoMediaItem extends MediaItem{

    public String src;
    public String page;
    public String title;
    public String thumbnail;
    public String sid;
    public String index;
    public String sourceType;

    public VideoMediaItem(String src, String page, String title, String index, String thumbnail, String sid, String sourceType) {
        this.src = src;
        this.page = page;
        this.title = title;
        this.index = index;
        this.thumbnail = thumbnail;
        this.sid = sid;
        this.sourceType = sourceType;
        init();
    }

    protected VideoMediaItem(Parcel in) {
        src = in.readString();
        page = in.readString();
        title = in.readString();
        index = in.readString();
        thumbnail = in.readString();
        sid = in.readString();
        sourceType = in.readString();
        init();
    }

    private void init() {
        setIndex(index);
        if (title != null)
            setTitle(title);
        setSource(src);
        setImageUri(thumbnail);
    }

    public String getPage() {
        return page;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(src);
        dest.writeString(page);
        dest.writeString(title);
        dest.writeString(index);
        dest.writeString(thumbnail);
        dest.writeString(sid);
        dest.writeString(sourceType);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<VideoMediaItem> CREATOR = new Parcelable.Creator<VideoMediaItem>() {
        @Override
        public VideoMediaItem createFromParcel(Parcel in) {
            return new VideoMediaItem(in);
        }

        @Override
        public VideoMediaItem[] newArray(int size) {
            return new VideoMediaItem[size];
        }
    };

    @Override
    public String getSourceType() {
        return sourceType;
    }
}
