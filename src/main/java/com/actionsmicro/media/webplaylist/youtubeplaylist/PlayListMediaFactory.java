package com.actionsmicro.media.webplaylist.youtubeplaylist;

import android.content.Context;

import org.json.JSONObject;

public class PlayListMediaFactory {

    public static final PlayListMedia createPlayListMedia(Context context, PlayListInfoItem item, PlayListMedia.PlayListMediaDelegate playListMediaDelegate) {
        return createPlayListMedia(context, item, playListMediaDelegate, PlayListMedia.TYPE.TYPE_FLAT);
    }


    public static final PlayListMedia createPlayListMedia(Context context, PlayListInfoItem item, PlayListMedia.PlayListMediaDelegate playListMediaDelegate, PlayListMedia.TYPE type) {
        return new PlayListMediaFlat(context, item, playListMediaDelegate);
    }

    public static final PlayListMedia createPlayListMedia(Context context, JSONObject playListJson, PlayListMedia.PlayListMediaDelegate playListMediaDelegate, String parentTitle) {
        return createPlayListMedia(context, playListJson, playListMediaDelegate, parentTitle, PlayListMedia.TYPE.TYPE_FLAT);
    }

    public static final PlayListMedia createPlayListMedia(Context context, JSONObject playListJson, PlayListMedia.PlayListMediaDelegate playListMediaDelegate, String parentTitle, PlayListMedia.TYPE type) {
        return new PlayListMediaFlat(context, playListJson, playListMediaDelegate, parentTitle);
    }
}
