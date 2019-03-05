package com.actionsmicro.media.adapter;

import android.os.Parcelable;

import com.actionsmicro.media.item.MediaItem;

import java.util.ArrayList;

/**
 * Created by laicc on 2015/8/24.
 */
public class MusicMediaAdapter implements MediaAdapterInterface {

    private ArrayList<MediaItem> mList;

    public MusicMediaAdapter(ArrayList<MediaItem> list) {
        mList = list;
    }

    @Override
    public int size() {
        return mList.size();
    }

    @Override
    public Parcelable getItem(int index) {
        return mList.get(index);
    }

    @Override
    public ArrayList<MediaItem> getList() {
        return mList;
    }
}
