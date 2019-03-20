package com.actionsmicro.media.webplaylist.youtubeplaylist;

import android.content.Context;
import android.os.Handler;

import com.actionsmicro.androidaiurjsproxy.helper.WebVideoSourceHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class PlayListMediaNest extends PlayListMedia {
    public PlayListMediaNest(Context context, PlayListInfoItem item, PlayListMediaDelegate playListMediaDelegate) {
        super(context, item, playListMediaDelegate);
    }

    public PlayListMediaNest(Context context, JSONObject playListJson, PlayListMediaDelegate playListMediaDelegate, String parentTitle) {
        super(context, playListJson, playListMediaDelegate, parentTitle, TYPE.TYPE_NEST);
    }

    @Override
    public void play(int index) {
        if (hasList()) {
            PlayListMedia item = mList.get(mCurrent);
            if (item.hasList()) {
                item.play(index);
            } else {
                if (index >= 0 && index < mList.size()) {
                    mCurrent = index;
                    PlayListMedia playListMedia = mList.get(mCurrent);
                    playListMedia.play();
                }
            }
        } else {
            if (index != 0)
                return;
            playImp();
        }
    }

    @Override
    public void playListWithinPlayList(JSONObject playListJson) {
        if (mList == null) {
            mList = new ArrayList<PlayListMedia>();
        }
        String index = mPlayListInfoItem.getIndex();
        PlayListMedia newList = PlayListMediaFactory.createPlayListMedia(mContext, playListJson, mPlayListMediaDelegate, index, TYPE.TYPE_NEST);
        mList.add(newList);
        PlayListMedia playListMedia = mList.get(mCurrent);
        playListMedia.play();
    }

    @Override
    protected void playImp() {

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
                        mPlayListMediaDelegate.onMediaError(mPlayListInfoItem,errorcode,errorDescription);
                    }
                });

                webVideoSourceHelper.start(mPlayListInfoItem.getPage(), getTitleString(mPlayListInfoItem.getTitle()),
                        mPlayListInfoItem.getImage(), mPlayListInfoItem.getSourceType(), mPlayListInfoItem.getSrc(), mPlayListInfoItem.getUrl());

            }
        });
    }

    @Override
    public void next() {
        if (mList != null && !mList.isEmpty()) {
            PlayListMedia playListMedia = mList.get(mCurrent);
            if (playListMedia.hasNext()) {
                playListMedia.next();
            } else {
                mCurrent++;
                if (mCurrent == mList.size()) {
                    mCurrent = mList.size() - 1;
                    return;
                }
                playListMedia = mList.get(mCurrent);
                playListMedia.play();
            }
        } else {
            play(mCurrent + 1);
        }
    }

    @Override
    public void previous() {
        if (mList != null && !mList.isEmpty()) {
            PlayListMedia playListMedia = mList.get(mCurrent);
            if (playListMedia.hasPrevious()) {
                playListMedia.previous();
            } else {
                mCurrent--;
                if (mCurrent < 0) {
                    mCurrent = 0;
                    return;
                }
                playListMedia = mList.get(mCurrent);
                playListMedia.play();
            }
        } else {
            play(mCurrent - 1);
        }
    }
}
