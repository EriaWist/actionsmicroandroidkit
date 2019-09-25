package com.actionsmicro.media.item;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by laicc on 2015/9/17.
 */
public class WebSingleMediaItem extends MediaItem {
    public String title;
    public String sourceUrl;
    public String webUrl;
    public String image;
    public String sourceType;

    public WebSingleMediaItem(String title, String sourceUrl, String webUrl, String image, String sourceType) {
        this.title = title;
        this.sourceUrl = sourceUrl;
        this.webUrl = webUrl;
        this.image = image;
        this.sourceType = sourceType;
        init();
    }

    protected WebSingleMediaItem(Parcel in) {
        title = in.readString();
        sourceUrl = in.readString();
        webUrl = in.readString();
        image = in.readString();
        sourceType = in.readString();
        init();
    }
    private void init() {
        setSource(sourceUrl);
        setTitle(title);
        setImageUri(image);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(sourceUrl);
        dest.writeString(webUrl);
        dest.writeString(image);
        dest.writeString(sourceType);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<WebSingleMediaItem> CREATOR = new Parcelable.Creator<WebSingleMediaItem>() {
        @Override
        public WebSingleMediaItem createFromParcel(Parcel in) {
            return new WebSingleMediaItem(in);
        }

        @Override
        public WebSingleMediaItem[] newArray(int size) {
            return new WebSingleMediaItem[size];
        }
    };

    @Override
    public String getSourceType() {
        return sourceType;
    }
}
