package com.actionsmicro.pigeon.mediastreaming;

import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.falcon.Falcon;
import com.actionsmicro.pigeon.MediaStreaming;
import com.actionsmicro.utils.Log;

public class MediaStreaming2 implements IMediaStreaming2 {

    public interface RPCAPI {
        // JRPC_REQUEST_METHOD
        // dongle => app
        String RPC_METHOD_GETMEDIASOURCE = "media.getMediaSource";

        // app => dongle
        String RPC_METHOD_GETCURRENTMEDIA = "media.getCurrentMedia";
        String RPC_METHOD_GETCURRENTPLAYLIST = "media.getCurrentPlaylist";
        String RPC_METHOD_GETCURRENTPLAYERSTATE = "media.getCurrentPlayerState";

        // old protocol
        String RPC_METHOD_PLAYLIST = "media.Playlist";
        String RPC_METHOD_PLAY = "media.play";
        String RPC_METHOD_PAUSE = "media.pause";
        String RPC_METHOD_STOP = "media.stop";
        String RPC_METHOD_SEEKTO = "media.seekTo";
        String RPC_METHOD_VOLUMEDOWN = "media.volumeDown";
        String RPC_METHOD_VOLUMEUP = "media.volumeUp";
        // from cloud control
        String RPC_METHOD_PREVIOUS = "media.previous";
        String RPC_METHOD_NEXT = "media.next";
        String RPC_METHOD_PLAYAT = "media.playAt";
        String RPC_METHOD_SEEKRELATIVELY = "media.seekRelatively";
        String RPC_METHOD_GETVOLUME = "media.getVolume";
        String RPC_METHOD_SETVOULME = "media.setVolume";
        String RPC_METHOD_MUTE = "media.mute";
        String RPC_METHOD_UNMUTE = "media.unmute";


        // JRPC_NOTIFICATION_METHOD
        // dongle => app
        String RPC_NOTIFICATION_ONPLAYLISTUPDATE = "media.onPlaylistUpdate";
        String RPC_NOTIFICATION_ONPLAY = "media.onPlay";
        String RPC_NOTIFICATION_ONPLAYERSTATECHANGE = "media.onPlayerStateChange";
        String RPC_NOTIFICATION_ONTIMEUPDATE = "media.onTimeUpdate";
        String RPC_NOTIFICATION_ONERROR = "media.onError";
    }

    private Falcon.ProjectorInfo mProjectorInfo;
    private Falcon.ProjectorInfo.MessageListener mMessageListener;

    public MediaStreaming2(Falcon.ProjectorInfo projectorInfo) {
        mProjectorInfo = projectorInfo;
        mProjectorInfo.addMessageListener(mMessageListener = new Falcon.ProjectorInfo.MessageListener() {
            @Override
            public void onReceiveMessage(Falcon.ProjectorInfo projector, String message) {
                Log.d("dddd", projector.getName() + " msg = " + message);
            }

            @Override
            public void onException(Falcon.ProjectorInfo projector, Exception e) {

            }

            @Override
            public void onDisconnect(Falcon.ProjectorInfo projector) {

            }
        });
    }

    @Override
    public void playPlayList(String playlist) {
        Log.d("dddd", "playPlayList");

    }

    @Override
    public void next() {
        Log.d("dddd", "next");
    }

    @Override
    public void previous() {
        Log.d("dddd", "previous");
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
    public void startMediaStreaming(MediaStreaming.DataSource dataSource) {
//        ClientV2P.super.startMediaStreaming(dataSource);
        // use play
    }

    @Override
    public int getDuration() {
        // return current duration
        return 0;
    }

    @Override
    public int getTime() {
        // TODO return current time
        return 0;
    }

    @Override
    public int seekTo(int position) {
        // TODO
//        return ClientV2P.super.seekTo(position);
        return 0;
    }

    @Override
    public int pauseMediaStreaming() {
        // TODO
//        return ClientV2P.super.pauseMediaStreaming();
        return 0;
    }

    @Override
    public int resumeMediaStreaming() {
//        return ClientV2P.super.resumeMediaStreaming();
        return 0;
    }

    @Override
    public int increaseVolume() {
//        return ClientV2P.super.increaseVolume();
        return 0;
    }

    @Override
    public int decreaseVolume() {
//        return ClientV2P.super.decreaseVolume();
        return 0;
    }

    @Override
    public void stopMediaStreaming() {
//        ClientV2P.super.stopMediaStreaming();
        // use stop instead
    }

    @Override
    public void sendStreamingContents(byte[] contents, int length) {
//        ClientV2P.super.sendStreamingContents(contents, length);
        // no use
    }

    @Override
    public void sendStreamingContentsUdp(byte[] contents, int length) {
        // ClientV2P.super.sendStreamingContentsUdp(contents, length);
        // no use
    }

    @Override
    public void sendEofPacket() {
//        ClientV2P.super.sendEofPacket();
        // no use
    }

    @Override
    public void resetPlayer() {
//        ClientV2P.super.resetPlayer();
        // no use
    }

    @Override
    public MediaPlayerApi.State getPlayerState() {
//        return ClientV2P.super.getPlayerState();
        return MediaPlayerApi.State.IDLE;
    }
}
