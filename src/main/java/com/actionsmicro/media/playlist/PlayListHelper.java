package com.actionsmicro.media.playlist;

import com.actionsmicro.media.item.MediaItem;
import com.actionsmicro.media.item.MusicMediaItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class PlayListHelper {
    public static String createPlayList(ArrayList<MediaItem> list, int start_index) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("start_index", start_index);
            JSONArray playlist = new JSONArray();
            for (int i = 0; i < list.size(); i++) {
                MediaItem item = list.get(i);
                JSONObject videoObj = new JSONObject();
                String src = item.getSource();
                String type = "audio";
                String title = item.getTitle();
                String image = item.getImage();


                videoObj.put("src", src);
                videoObj.put("type", type);
                videoObj.put("title", title);
                videoObj.put("image", image);
                if(item instanceof MusicMediaItem){
                    MusicMediaItem musicItem = (MusicMediaItem) item;
                    videoObj.put("index", musicItem.getIndex());
                    videoObj.put("mediaId", musicItem.getMediaId());
                    videoObj.put("mediaName", musicItem.getMediaName());
                    videoObj.put("artistName", musicItem.getArtistName());
                    videoObj.put("albumName", musicItem.getAlbumName());
                    videoObj.put("albumId", musicItem.getAlbumId());
                    videoObj.put("duration", musicItem.getDuration());
                    videoObj.put("data", musicItem.getData());
                }
                playlist.put(i, videoObj);
            }
            jsonObject.put("playlist", playlist);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject.toString();
    }
}
