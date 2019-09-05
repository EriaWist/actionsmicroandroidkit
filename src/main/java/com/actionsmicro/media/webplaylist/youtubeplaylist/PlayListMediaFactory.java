package com.actionsmicro.media.webplaylist.youtubeplaylist;

import android.content.Context;

import com.actionsmicro.media.playlist.PlayList;
import com.google.gson.Gson;

import org.json.JSONObject;

public class PlayListMediaFactory {
    public static final PlayListMedia createPlayListMedia(Context context, PlayList playList, PlayListMedia.PlayListMediaDelegate playListMediaDelegate) {
        return new PlayListMediaFlat(context, playList, playListMediaDelegate);
    }
}
