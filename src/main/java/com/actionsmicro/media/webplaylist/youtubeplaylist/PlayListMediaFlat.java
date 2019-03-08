package com.actionsmicro.media.webplaylist.youtubeplaylist;

import android.content.Context;

import org.json.JSONObject;

import java.util.ArrayList;

public class PlayListMediaFlat extends PlayListMedia {
    public PlayListMediaFlat(Context context, PlayListInfoItem item, PlayListMediaDelegate playListMediaDelegate) {
        super(context, item, playListMediaDelegate);
    }

    public PlayListMediaFlat(Context context, JSONObject playListJson, PlayListMediaDelegate playListMediaDelegate, String parentTitle) {
        super(context, playListJson, playListMediaDelegate, parentTitle, TYPE.TYPE_FLAT);
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
        PlayListMedia newList = PlayListMediaFactory.createPlayListMedia(mContext, playListJson, mPlayListMediaDelegate, index,TYPE.TYPE_FLAT);
        mList.add(newList);
        PlayListMedia playListMedia = mList.get(mCurrent);
        playListMedia.play();
    }
}
