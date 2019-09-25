package com.actionsmicro.media.webplaylist.youtubeplaylist;


import android.content.Context;
import android.os.Handler;

import com.actionsmicro.androidaiurjsproxy.helper.WebVideoSourceHelper;
import com.actionsmicro.media.playlist.PlayList;
import com.actionsmicro.media.videoobj.VideoObj;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class PlayListMedia {

    public enum TYPE {
        TYPE_NEST, TYPE_FLAT
    }

    public interface PlayListMediaDelegate {
        void videoSourcesFound(String src, String page, String title, String thumbnail, String sid, String sourceType);

        void playListFound(String jsonResponse);

        void onMediaError(PlayListMedia playListMedia, String errorCode, String errorDescription);

        void onMediaFound(String videoObj);
    }

    protected PlayListMediaDelegate mPlayListMediaDelegate;

    protected PlayList mPlaylist;
    protected int mCurrent = 0;

    protected Context mContext;

    public PlayListMedia(Context context, PlayList playList, PlayListMediaDelegate playListMediaDelegate) {
        mContext = context;
        mPlaylist = playList;
        mPlayListMediaDelegate = playListMediaDelegate;
    }

    public void play() {
        play(mCurrent);
    }

    public abstract void play(int index);

    public abstract void next();

    public abstract void previous();

    public int getCurrentPlaying() {
        return mCurrent;
    }

    public PlayList getCurrentFullList() {
        return mPlaylist;
    }

    public abstract void playListWithinPlayList(JSONObject playListJson);

    public boolean hasNext() {
        return mCurrent < (getListSize() - 1);
    }

    public boolean hasPrevious() {
        return mCurrent > 0;
    }

    public boolean hasList() {
        List<VideoObj> videoObjs = mPlaylist.getPlaylist();
        if (videoObjs == null || videoObjs.size() == 0) {
            return false;
        }
        return true;
    }

    public String getIndexString() {
        return String.valueOf(mPlaylist.getStart_index());
    }

    protected abstract void playImp();

    protected int getListSize() {
        if (mPlaylist.getPlaylist() != null) {
            return mPlaylist.getPlaylist().size();
        }
        return 0;
    }
}
