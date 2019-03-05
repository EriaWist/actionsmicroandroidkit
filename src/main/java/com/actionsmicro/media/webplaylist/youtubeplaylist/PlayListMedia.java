package com.actionsmicro.media.webplaylist.youtubeplaylist;


import android.content.Context;
import android.os.Handler;

import com.actionsmicro.androidaiurjsproxy.helper.WebVideoSourceHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class PlayListMedia{

	public interface PlayListMediaDelegate {
        void videoSourcesFound(String src, String page, String title, String thumbnail, String sid, String sourceType);
		void playListFound(String jsonResponse);
		void onMediaError(PlayListInfoItem playListInfoItem,String errorCode, String errorDescription);
    }
	private PlayListMediaDelegate mPlayListMediaDelegate;

	private ArrayList<PlayListMedia> mList;
	private PlayListInfoItem mPlayListInfoItem;

	public PlayListInfoItem getPlayListInfoItem() {
		return mPlayListInfoItem;
	}

	private int mCurrent = 0;

	private Context mContext;

	public PlayListMedia(Context context, PlayListInfoItem item, PlayListMediaDelegate playListMediaDelegate) {
		this.mContext = context;
		this.mPlayListInfoItem = item;
		this.mPlayListMediaDelegate = playListMediaDelegate;
	}
	public PlayListMedia(Context context, JSONObject playListJson, PlayListMediaDelegate playListMediaDelegate, String parentTitle) {
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
				PlayListMedia playListMedia = new PlayListMedia(mContext, playListInfoItem, playListMediaDelegate);
				mList.add(playListMedia);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void play() {
		play(mCurrent);
	}

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

	public void playListWithinPlayList(JSONObject playListJson) {
		if (mList == null) {
			mList = new ArrayList<PlayListMedia>();
		}
		String index = mPlayListInfoItem.getIndex();
		PlayListMedia newList = new PlayListMedia(mContext, playListJson, mPlayListMediaDelegate, index);
		mList.add(newList);
		PlayListMedia playListMedia = mList.get(mCurrent);
		playListMedia.play();
	}
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
	private void playImp() {

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

				webVideoSourceHelper.start(mPlayListInfoItem.getUrl(), getTitleString(mPlayListInfoItem.getTitle()),
						mPlayListInfoItem.getImage(), mPlayListInfoItem.getSourceType());

			}
		});
	}
	private String getTitleString(String title) {
		return title + " " + mPlayListInfoItem.getIndex();
	}

}
