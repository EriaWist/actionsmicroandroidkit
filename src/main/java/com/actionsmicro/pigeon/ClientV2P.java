package com.actionsmicro.pigeon;

import android.os.Build;

import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.falcon.Falcon;
import com.actionsmicro.falcon.Falcon.ProjectorInfo;

public class ClientV2P extends ClientV2 {
    // TODO change to jrpc mediastreaming when capability set
    private MediaStreaming mMediaStreaming = new MediaStreaming() {
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

    public ClientV2P(ProjectorInfo projectorInfo) {
        super(projectorInfo.getAddress().getHostAddress(), Falcon.EZ_WIFI_DISPLAY_PORT_NUMBER, Build.MODEL);
        mProjectorInfo = projectorInfo;

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
}
