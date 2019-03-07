package com.actionsmicro.media.webplaylist.youtubeplaylist;

import android.content.Context;

import org.json.JSONObject;

public class PlayListMediaFactory {
    public enum TYPE {
        TYPE_NEST, TYPE_FLAT
    }

    public static final PlayListMedia createPlayListMedia(Context context, PlayListInfoItem item, PlayListMedia.PlayListMediaDelegate playListMediaDelegate) {
        return createPlayListMedia(context, item, playListMediaDelegate, TYPE.TYPE_NEST);
    }


    public static final PlayListMedia createPlayListMedia(Context context, PlayListInfoItem item, PlayListMedia.PlayListMediaDelegate playListMediaDelegate, TYPE type) {
        switch (type) {
            case TYPE_NEST:
                return new PlayListMediaNest(context, item, playListMediaDelegate);
            case TYPE_FLAT:
                // TODO flat
                break;
        }
        return new PlayListMediaNest(context, item, playListMediaDelegate);
    }

    public static final PlayListMedia createPlayListMedia(Context context, JSONObject playListJson, PlayListMedia.PlayListMediaDelegate playListMediaDelegate, String parentTitle) {
        return createPlayListMedia(context, playListJson, playListMediaDelegate, parentTitle, TYPE.TYPE_NEST);
    }

    public static final PlayListMedia createPlayListMedia(Context context, JSONObject playListJson, PlayListMedia.PlayListMediaDelegate playListMediaDelegate, String parentTitle, TYPE type) {
        switch (type) {
            case TYPE_NEST:
                return new PlayListMediaNest(context, playListJson, playListMediaDelegate, parentTitle);
            case TYPE_FLAT:
                // TODO flat
                break;
        }
        return new PlayListMediaNest(context, playListJson, playListMediaDelegate, parentTitle);
    }
}
