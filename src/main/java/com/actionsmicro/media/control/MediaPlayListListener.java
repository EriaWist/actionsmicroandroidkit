package com.actionsmicro.media.control;

import com.actionsmicro.media.item.MediaItem;

/**
 * Created by laicc on 2015/6/29.
 */
public interface MediaPlayListListener {
    void onMediaChanged(MediaItem mediaItem, int position);
    void onListChanged();
    void onListEnded();
}
