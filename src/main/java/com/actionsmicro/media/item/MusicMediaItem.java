package com.actionsmicro.media.item;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by laicc on 2015/9/8.
 */
public class MusicMediaItem extends MediaItem {
    public String mIndex;
    public long mMediaId;
    public String mMediaName;
    public String mArtistName;
    public String mAlbumName;
    public long mAlbumId;
    public int mDuration;
    public String mData;

    public MusicMediaItem(String index, final long musicId, final String musicName, final String artistName, final String albumName, final long albumId, final int duration, final String data) {
        mIndex = index;
        mMediaId = musicId;
        mMediaName = musicName;
        mArtistName = artistName;
        mAlbumName = albumName;
        mAlbumId = albumId;
        mDuration = duration;
        mData = data;
        init();
    }

    protected MusicMediaItem(Parcel in) {
        mIndex = in.readString();
        mMediaId = in.readLong();
        mMediaName = in.readString();
        mArtistName = in.readString();
        mAlbumName = in.readString();
        mAlbumId = in.readLong();
        mDuration = in.readInt();
        mData = in.readString();
        init();
    }
    private void init() {
        setIndex(mIndex);
        setExtraDescription(mAlbumName);
        if (mMediaName != null)
            setTitle(mMediaName);
        setSource(mData);
        Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
        Uri uri = ContentUris.withAppendedId(sArtworkUri, mAlbumId);
        setImageUri(uri.toString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mIndex);
        dest.writeLong(mMediaId);
        dest.writeString(mMediaName);
        dest.writeString(mArtistName);
        dest.writeString(mAlbumName);
        dest.writeLong(mAlbumId);
        dest.writeInt(mDuration);
        dest.writeString(mData);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<MediaItem> CREATOR = new Parcelable.Creator<MediaItem>() {
        @Override
        public MediaItem createFromParcel(Parcel in) {
            return new MusicMediaItem(in);
        }

        @Override
        public MediaItem[] newArray(int size) {
            return new MediaItem[size];
        }
    };


    public String getIndex() {
        return mIndex;
    }

    public long getMediaId() {
        return mMediaId;
    }

    public String getMediaName() {
        return mMediaName;
    }

    public String getArtistName() {
        return mArtistName;
    }

    public String getAlbumName() {
        return mAlbumName;
    }

    public long getAlbumId() {
        return mAlbumId;
    }

    public int getDuration() {
        return mDuration;
    }

    public String getData() {
        return mData;
    }
}
