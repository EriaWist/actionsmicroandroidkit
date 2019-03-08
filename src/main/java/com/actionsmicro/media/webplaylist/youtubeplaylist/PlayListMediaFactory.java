package com.actionsmicro.media.webplaylist.youtubeplaylist;

import android.content.Context;

import org.json.JSONObject;

public class PlayListMediaFactory {

    public static final PlayListMedia createPlayListMedia(Context context, PlayListInfoItem item, PlayListMedia.PlayListMediaDelegate playListMediaDelegate) {
        return createPlayListMedia(context, item, playListMediaDelegate, PlayListMedia.TYPE.TYPE_NEST);
    }


    public static final PlayListMedia createPlayListMedia(Context context, PlayListInfoItem item, PlayListMedia.PlayListMediaDelegate playListMediaDelegate, PlayListMedia.TYPE type) {
        switch (type) {
            case TYPE_NEST:
                return new PlayListMediaNest(context, item, playListMediaDelegate);
            case TYPE_FLAT:
                return new PlayListMediaFlat(context, item, playListMediaDelegate);
        }
        return new PlayListMediaNest(context, item, playListMediaDelegate);
    }

    public static final PlayListMedia createPlayListMedia(Context context, JSONObject playListJson, PlayListMedia.PlayListMediaDelegate playListMediaDelegate, String parentTitle) {
        return createPlayListMedia(context, playListJson, playListMediaDelegate, parentTitle, PlayListMedia.TYPE.TYPE_NEST);
    }

    public static final PlayListMedia createPlayListMedia(Context context, JSONObject playListJson, PlayListMedia.PlayListMediaDelegate playListMediaDelegate, String parentTitle, PlayListMedia.TYPE type) {
        switch (type) {
            case TYPE_NEST:
                return new PlayListMediaNest(context, playListJson, playListMediaDelegate, parentTitle);
            case TYPE_FLAT:
                return new PlayListMediaFlat(context, playListJson, playListMediaDelegate, parentTitle);
        }
        return new PlayListMediaNest(context, playListJson, playListMediaDelegate, parentTitle);
    }
}
