package com.actionsmicro.pigeon;

import android.os.Build;

import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.PigeonDeviceInfo;
import com.actionsmicro.falcon.Falcon;
import com.actionsmicro.falcon.Falcon.ProjectorInfo;
import com.actionsmicro.utils.Log;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

public class ClientV2P extends ClientV2 implements IMediaStreaming2 {
    private ProjectorInfo.MessageListener mMessageListener;
    // TODO change to jrpc mediastreaming when capability set
    private IMediaStreaming2 mMediaStreaming = new IMediaStreaming2() {
        @Override
        public void playPlayList(String playlist) {
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
        mDeviceInfo = new PigeonDeviceInfo(projectorInfo);
        mProjectorInfo.addMessageListener(mMessageListener = new ProjectorInfo.MessageListener() {
            @Override
            public void onReceiveMessage(ProjectorInfo projector, String message) {
                Log.d("dddd", projector.getName() + " msg = " + message);
            }

            @Override
            public void onException(ProjectorInfo projector, Exception e) {

            }

            @Override
            public void onDisconnect(ProjectorInfo projector) {

            }
        });


        if (isMediaStreamingV2()) {
            mMediaStreaming = new IMediaStreaming2() {
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
        }

    }

    private boolean isMediaStreamingV2() {
        return true;
//        return mDeviceInfo.isMediaStreamingV2();
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

    @Override
    protected void handleException(Exception e) {
        Log.e("dddd", "handleException", e);
        super.handleException(e);
        mProjectorInfo.removeMessageListener(mMessageListener);
    }

    @Override
    public void playPlayList(String playlist) {
        Gson gson = new Gson();
        try {
            JSONObject jsonPlayList = new JSONObject(playlist);
            Log.d("dddd", jsonPlayList.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mMediaStreaming.playPlayList(playlist);
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

}
