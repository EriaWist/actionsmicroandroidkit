package com.actionsmicro.media.webplaylist.youtubeplaylist;

import android.content.Context;
import android.os.Handler;

import com.actionsmicro.androidaiurjsproxy.helper.WebVideoSourceHelper;
import com.actionsmicro.media.playlist.PlayList;
import com.actionsmicro.media.videoobj.VideoObj;
import com.actionsmicro.utils.Log;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlayListMediaFlat extends PlayListMedia {

    public PlayListMediaFlat(Context context, PlayList playList, PlayListMediaDelegate playListMediaDelegate) {
        super(context, playList, playListMediaDelegate);
    }

    @Override
    public void play(int index) {
        if (hasList()) {
            if (index >= 0 && index < getListSize()) {
                mCurrent = index;
                playImp();
            }
        } else {
            if (index != 0)
                return;
            playImp();
        }
    }

    @Override
    public void playListWithinPlayList(JSONObject playListJson) {
        PlayList playList = new Gson().fromJson(playListJson.toString(), PlayList.class);
        List<VideoObj> newVideoList = playList.getPlaylist();
        List<VideoObj> currentVideoList = mPlaylist.getPlaylist();
        currentVideoList.remove(mCurrent);
        currentVideoList.addAll(mCurrent, newVideoList);
        playImp();
    }

    @Override
    protected void playImp() {
        VideoObj video = mPlaylist.getPlaylist().get(mCurrent);
        Handler h = new Handler(mContext.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {

                WebVideoSourceHelper webVideoSourceHelper = WebVideoSourceHelper.getInstance(mContext.getApplicationContext());
                webVideoSourceHelper.setListener(new WebVideoSourceHelper.Listener() {
                    @Override
                    public void onVideoFound(String src, String page, String title, String thumbnail, String sid, String soucesType) {
                        mPlayListMediaDelegate.videoSourcesFound(src, page, title, thumbnail, sid, soucesType);
                    }

                    @Override
                    public void onPlaylistFound(String jsonResponse) {
                        JSONObject jsonObj = null;
                        try {
                            jsonObj = new JSONObject(jsonResponse);
                            playListWithinPlayList(jsonObj);
                            mPlayListMediaDelegate.playListFound(jsonResponse);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onMediaError(String errorcode, String errorDescription) {
                        mPlayListMediaDelegate.onMediaError(PlayListMediaFlat.this, errorcode, errorDescription);
                    }

                    @Override
                    public void onMediaFound(String videoObj) {
                        try {
                            JSONObject jsonObj = null;
                            jsonObj = new JSONObject(videoObj);
                            JSONArray playList = jsonObj.optJSONArray("playlist");
                            if (playList != null && playList.length() > 1) {
                                // expand videoobj to array
                                playListWithinPlayList(jsonObj);
                                mPlayListMediaDelegate.playListFound(videoObj);
                            } else {
                                mPlayListMediaDelegate.onMediaFound(videoObj);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

                webVideoSourceHelper.start(video.getPage(), video.getTitle(),
                        video.getImage(), video.getType() == null ? "html" : video.getType(), video.getSrc(), video.getPage());

            }
        });
    }

    @Override
    public void next() {
        if (hasNext()) {
            mCurrent++;
            playImp();
        }
    }

    @Override
    public void previous() {
        if (hasPrevious()) {
            mCurrent--;
            playImp();
        }
    }


}
