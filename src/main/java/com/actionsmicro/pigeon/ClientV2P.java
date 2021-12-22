package com.actionsmicro.pigeon;

import android.content.Context;
import android.os.Build;

import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.PigeonDeviceInfo;
import com.actionsmicro.falcon.Falcon;
import com.actionsmicro.falcon.Falcon.ProjectorInfo;
import com.actionsmicro.media.playlist.PlayList;
import com.actionsmicro.pigeon.mediastreaming.ClientHandler;
import com.actionsmicro.pigeon.mediastreaming.IMediaStreaming2;
import com.actionsmicro.pigeon.mediastreaming.MediaStreaming2;
import com.actionsmicro.utils.Log;

public class ClientV2P extends ClientV2 implements IMediaStreaming2 {
    private static final String TAG = "ClientV2P";

    private IMediaStreaming2 mMediaStreaming = new IMediaStreaming2() {
        @Override
        public void playPlayList(Context context, PlayList playlist) {
            // V2 don't have this function
        }

        @Override
        public void next() {
            // V2 don't have this function
        }

        @Override
        public void previous() {
            // V2 don't have this function
        }

        @Override
        public String getCurrentMedia() {
            return null;
        }

        @Override
        public String getCurrentPlaylist() {
            return null;
        }

        @Override
        public void setMediaStreamingStateListener(MediaPlayerApi api, MediaPlayerApi.MediaPlayerStateListener mediaPlayerStateListener) {

        }

        @Override
        public boolean playAt(int position) {
            return false;
        }

        @Override
        public void startMediaStreaming(DataSource dataSource) {
            ClientV2P.super.startMediaStreaming(dataSource);
        }

        @Override
        public int getDuration() {
            return ClientV2P.super.getDuration();
        }

        @Override
        public int getTime() {
            return ClientV2P.super.getTime();
        }

        @Override
        public int seekTo(int position) {
            return ClientV2P.super.seekTo(position);
        }

        @Override
        public int pauseMediaStreaming() {
            return ClientV2P.super.pauseMediaStreaming();
        }

        @Override
        public int resumeMediaStreaming() {
            return ClientV2P.super.resumeMediaStreaming();
        }

        @Override
        public int increaseVolume() {
            return ClientV2P.super.increaseVolume();
        }

        @Override
        public int decreaseVolume() {
            return ClientV2P.super.decreaseVolume();
        }

        @Override
        public void stopMediaStreaming() {
            ClientV2P.super.stopMediaStreaming();
        }

        @Override
        public void sendStreamingContents(byte[] contents, int length) {
            ClientV2P.super.sendStreamingContents(contents, length);
        }

        @Override
        public void sendStreamingContentsUdp(byte[] contents, int length) {
            ClientV2P.super.sendStreamingContentsUdp(contents, length);
        }

        @Override
        public void sendEofPacket() {
            ClientV2P.super.sendEofPacket();
        }

        @Override
        public void resetPlayer() {
            ClientV2P.super.resetPlayer();
        }

        @Override
        public MediaPlayerApi.State getPlayerState() {
            return ClientV2P.super.getPlayerState();
        }
    };

    private ProjectorInfo mProjectorInfo;
    private PigeonDeviceInfo mDeviceInfo;

    public ClientV2P(ProjectorInfo projectorInfo) {
        super(projectorInfo.getAddress().getHostAddress(), Falcon.EZ_WIFI_DISPLAY_PORT_NUMBER, Build.MODEL);
        mProjectorInfo = projectorInfo;
        mProjectorInfo.setCapabilityListener(new ProjectorInfo.CapabilityListener() {
            @Override
            public void onCapabilitySet() {
                bulidMediaStreaming();
            }
        });

        bulidMediaStreaming();
    }

    private void bulidMediaStreaming() {
        mDeviceInfo = new PigeonDeviceInfo(mProjectorInfo);
        if (isMediaStreamingV2()) {
            mMediaStreaming = new MediaStreaming2(mProjectorInfo);
        }
    }

    private boolean isMediaStreamingV2() {
        return mDeviceInfo.isMediaStreamingV2();
    }

    @Override
    public void startMediaStreaming(DataSource dataSource) {
        mMediaStreaming.startMediaStreaming(dataSource);
    }

    @Override
    public int getDuration() {
        return mMediaStreaming.getDuration();
    }

    @Override
    public int getTime() {
        return mMediaStreaming.getTime();
    }

    @Override
    public int seekTo(int position) {
        return mMediaStreaming.seekTo(position);
    }

    @Override
    public int pauseMediaStreaming() {
        return mMediaStreaming.pauseMediaStreaming();
    }

    @Override
    public int resumeMediaStreaming() {
        return mMediaStreaming.resumeMediaStreaming();
    }

    @Override
    public int increaseVolume() {
        return mMediaStreaming.increaseVolume();
    }

    @Override
    public int decreaseVolume() {
        return mMediaStreaming.decreaseVolume();
    }

    @Override
    public void stopMediaStreaming() {
        mMediaStreaming.stopMediaStreaming();
    }

    @Override
    protected boolean shouldSendHeartbeat() {
        if(mMediaStreaming == null) {
            Log.d(TAG, "mediaStreaming is released, should not send heartbeat anymore");
            shouldStop = true;
            return false;
        }
        return true;
    }

    @Override
    public void sendStreamingContents(final byte[] contents, int length) {
        mMediaStreaming.sendStreamingContents(contents, length);
    }

    @Override
    public void sendStreamingContentsUdp(final byte[] contents, int length) {
        mMediaStreaming.sendStreamingContentsUdp(contents, length);
    }

    @Override
    public void sendEofPacket() {
        mMediaStreaming.sendEofPacket();
    }

    @Override
    public void resetPlayer() {
        mMediaStreaming.resetPlayer();
    }

    @Override
    public MediaPlayerApi.State getPlayerState() {
        return mMediaStreaming.getPlayerState();
    }

    @Override
    protected void handleException(Exception e) {
        Log.e(TAG, "handleException", e);
        super.handleException(e);
    }

    @Override
    public void playPlayList(Context context, PlayList playlist) throws Exception {
        mMediaStreaming.playPlayList(context, playlist);
    }

    @Override
    public void next() {
        mMediaStreaming.next();
    }

    @Override
    public void previous() {
        mMediaStreaming.previous();
    }

    @Override
    public String getCurrentMedia() {
        return null;
    }

    @Override
    public String getCurrentPlaylist() {
        return null;
    }

    @Override
    public void setMediaStreamingStateListener(MediaPlayerApi api, MediaPlayerApi.MediaPlayerStateListener mediaPlayerStateListener) {
        mMediaStreaming.setMediaStreamingStateListener(api, mediaPlayerStateListener);
    }

    @Override
    public boolean playAt(int position) {
        return mMediaStreaming.playAt(position);
    }

    @Override
    protected void stop() {
        if(mMediaStreaming instanceof ClientHandler){
            ((ClientHandler)mMediaStreaming).onClientStop();
        }
        super.stop();
    }
}
