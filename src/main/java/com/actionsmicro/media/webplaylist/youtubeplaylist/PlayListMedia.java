package com.actionsmicro.media.webplaylist.youtubeplaylist;


import android.content.Context;
import android.os.Handler;

import com.actionsmicro.androidaiurjsproxy.helper.WebVideoSourceHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public abstract class PlayListMedia{

	public enum TYPE {
		TYPE_NEST, TYPE_FLAT
	}

	public interface PlayListMediaDelegate {
        void videoSourcesFound(String src, String page, String title, String thumbnail, String sid, String sourceType);
		void playListFound(String jsonResponse);
		void onMediaError(PlayListInfoItem playListInfoItem,String errorCode, String errorDescription);
    }
	protected PlayListMediaDelegate mPlayListMediaDelegate;

	protected ArrayList<PlayListMedia> mList;
	protected PlayListInfoItem mPlayListInfoItem;

	public PlayListInfoItem getPlayListInfoItem() {
		return mPlayListInfoItem;
	}

	protected int mCurrent = 0;

	protected Context mContext;

	public PlayListMedia(Context context, PlayListInfoItem item, PlayListMediaDelegate playListMediaDelegate) {
		this.mContext = context;
		this.mPlayListInfoItem = item;
		this.mPlayListMediaDelegate = playListMediaDelegate;
	}
	public PlayListMedia(Context context, JSONObject playListJson, PlayListMediaDelegate playListMediaDelegate, String parentTitle, TYPE type) {
		mContext = context;
		mList = new ArrayList<PlayListMedia>();
		this.mPlayListMediaDelegate = playListMediaDelegate;
		String index = "";
		try {
			JSONArray jsonArray = playListJson.getJSONArray("playlist");
			for (int i = 0;i<jsonArray.length();i++) {
				JSONObject playItem = jsonArray.getJSONObject(i);
				String image = playItem.optString("image","");
				String title = playItem.optString("title","No Title");
				if(title.equals("null")){
					title = "No Title";
				}
				String sourceType = playItem.optString("source_type","");
				String page = playItem.optString("page","");
				String url =  sourceType.equals("stream")?playItem.optString("src",""):playItem.optString("page","");
				if (parentTitle != null && !parentTitle.isEmpty()) {
					index = "(" + (i + 1) + "-" + jsonArray.length() + ")" + "/" + parentTitle;
				} else {
					index = "(" + (i + 1) + "-" + jsonArray.length() + ")";
				}
				PlayListInfoItem playListInfoItem = new PlayListInfoItem(index, page, url, title, image, sourceType);
				PlayListMedia playListMedia = PlayListMediaFactory.createPlayListMedia(mContext, playListInfoItem, playListMediaDelegate, type);
				mList.add(playListMedia);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void play() {
		play(mCurrent);
	}

	public abstract void play(int index);

	public void play(String index) {
		int peek = PlayListUtils.peekCurrent(index);
		if (peek != -1) {
			int size = PlayListUtils.peekSize(index);
			if (size != mList.size()) {
				PlayListMedia playListMedia = mList.get(0);
				playListMedia.play(index);
			} else {
				PlayListMedia playListMedia = mList.get(peek);
				mCurrent = peek;
				String dex = PlayListUtils.removeTopIndex(index);
				if (!dex.isEmpty()) {
					playListMedia.play(dex);
				} else {
					playListMedia.play();
				}
			}
		} else {
			play();
		}


	}

	public abstract void next();

	public abstract void previous();

	public int getCurrentPlaying() {
		if (hasList()) {
			PlayListMedia item = mList.get(mCurrent);
			if (item.hasList()) {
				return item.getCurrentPlaying();
			} else {
				return mCurrent;
			}
		} else {
			return mCurrent;
		}
	}
	public ArrayList<PlayListMedia> getDeepList() {
		if (hasList()) {
			PlayListMedia item = mList.get(mCurrent);
			if (item.hasList()) {
				return item.getDeepList();
			} else {
				return mList;
			}
		} else {
			return null;
		}
	}
	public ArrayList<PlayListMedia> getCurrentFullList() {
		ArrayList<PlayListMedia> list = new ArrayList<PlayListMedia>();
		if (hasList()) {
			for (PlayListMedia item:mList) {
				if (item.hasList()) {
					ArrayList<PlayListMedia> tempList = item.getCurrentFullList();
					for (PlayListMedia tempListItem:tempList) {
						list.add(tempListItem);
					}
				} else {
					list.add(item);
				}
			}
			return list;
		} else {
			return null;
		}
	}

	public abstract void playListWithinPlayList(JSONObject playListJson);

	public boolean hasNext() {
		if (hasList()) {
			PlayListMedia playListMedia = mList.get(mCurrent);
			if (playListMedia.hasList()) {
				return playListMedia.hasNext();
			} else if (mCurrent < mList.size() - 1){
				return true;
			}
		}
		return false;
	}
	public boolean hasPrevious() {
		if (hasList()) {
			PlayListMedia playListMedia = mList.get(mCurrent);
			if (playListMedia.hasPrevious()) {
				return playListMedia.hasPrevious();
			} else if (mCurrent > 0) {
				return true;
			}
		}
		return false;
	}
	public boolean hasList() {
		if (mList == null || mList.size() == 0) {
			return false;
		}
		return true;
	}
	public String getIndexString() {
		if (hasList()) {
			PlayListMedia item = mList.get(mCurrent);
			if (item.hasList()) {
				return item.getIndexString();
			} else {
				return item.mPlayListInfoItem.getIndex();
			}
		} else {
			return mPlayListInfoItem.getIndex();
		}
	}
	protected abstract void playImp();

	protected String getTitleString(String title) {
		return title + " " + mPlayListInfoItem.getIndex();
	}

}
