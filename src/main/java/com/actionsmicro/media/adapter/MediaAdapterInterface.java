package com.actionsmicro.media.adapter;

import android.os.Parcelable;

import com.actionsmicro.media.item.MediaItem;

import java.util.ArrayList;

/**
 * Created by laicc on 2015/8/24.
 */
public interface MediaAdapterInterface {
    int size();
    Parcelable getItem(int index);
    ArrayList<MediaItem> getList();
}
