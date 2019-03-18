package com.actionsmicro.media.webplaylist.youtubeplaylist;

import android.os.Parcel;
import android.os.Parcelable;

public class PlayListInfoItem implements Parcelable {
    public PlayListInfoItem(String index, String page, String url, String title, String image, String sourceType, String src) {
        this.index = index;
        this.page = page;
        this.url = url;
        this.title = title;
        this.image = image;
        this.sourceType = sourceType;
        this.src = src;
    }

    private String index;
    private String page;
    private String url;
    private String title;
    private String sourceType;
    private String image;
    private String src;

    public String getIndex() {
        return index;
    }

    public String getPage() {
        return page;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getImage() {
        return image;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSrc() {
        return src;
    }

    protected PlayListInfoItem(Parcel in) {
        index = in.readString();
        page = in.readString();
        url = in.readString();
        title = in.readString();
        sourceType = in.readString();
        image = in.readString();
        src = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(index);
        dest.writeString(page);
        dest.writeString(url);
        dest.writeString(title);
        dest.writeString(sourceType);
        dest.writeString(image);
        dest.writeString(src);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<PlayListInfoItem> CREATOR = new Parcelable.Creator<PlayListInfoItem>() {
        @Override
        public PlayListInfoItem createFromParcel(Parcel in) {
            return new PlayListInfoItem(in);
        }

        @Override
        public PlayListInfoItem[] newArray(int size) {
            return new PlayListInfoItem[size];
        }
    };
}