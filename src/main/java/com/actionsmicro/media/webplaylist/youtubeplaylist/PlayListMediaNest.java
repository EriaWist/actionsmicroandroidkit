package com.actionsmicro.media.webplaylist.youtubeplaylist;

import android.content.Context;

import org.json.JSONObject;

import java.util.ArrayList;

public class PlayListMediaNest extends PlayListMedia {
    public PlayListMediaNest(Context context, PlayListInfoItem item, PlayListMediaDelegate playListMediaDelegate) {
        super(context, item, playListMediaDelegate);
    }

    public PlayListMediaNest(Context context, JSONObject playListJson, PlayListMediaDelegate playListMediaDelegate, String parentTitle) {
        super(context, playListJson, playListMediaDelegate, parentTitle);
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
        PlayListMedia newList = PlayListMediaFactory.createPlayListMedia(mContext, playListJson, mPlayListMediaDelegate, index);
        mList.add(newList);
        PlayListMedia playListMedia = mList.get(mCurrent);
        playListMedia.play();
    }
}
