package com.actionsmicro.pigeon.mediastreaming;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.actionsmicro.androidaiurjsproxy.helper.WebVideoSourceHelper;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.falcon.Falcon;
import com.actionsmicro.media.item.MusicMediaItem;
import com.actionsmicro.media.item.VideoMediaItem;
import com.actionsmicro.media.playlist.PlayList;
import com.actionsmicro.media.videoobj.Caption;
import com.actionsmicro.media.videoobj.VideoObj;
import com.actionsmicro.pigeon.MediaStreaming;
import com.actionsmicro.pigeon.MediaStreamingFileDataSource;
import com.actionsmicro.utils.CipherUtil;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.TetheringUtil;
import com.actionsmicro.utils.Utils;
import com.actionsmicro.web.SimpleContentUriHttpFileServer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Message;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Notification;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.Dispatcher;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.NotificationHandler;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

import java.io.File;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.actionsmicro.utils.CipherUtil.ALGORITHM_AES_CBC;

public class MediaStreaming2 implements IMediaStreaming2, ClientHandler {

    private static final String TAG = "MediaStreaming2";
    private static final int COMMAND_JSONRPC = 6;
    private static final int COMMAND_JSONRPC_ENCRYPT = 7;
    public static final int DEFAULT_WAIT_TIMEOUT = 5000;
    private Dispatcher dispatcher = new Dispatcher();
    private Context mContext;
    private int mDuration;
    private int mTime;
    private PlayList mCurrentPlayList;
    private WebVideoSourceHelper mMediaSoucehelper;
    private MediaPlayerApi.MediaPlayerStateListener mMediaStateListener;
    private MediaPlayerApi mMediaApi;
    private MediaPlayerApi.State mCurrentState = MediaPlayerApi.State.IDLE;
    private Gson gson = new Gson();
    private SimpleContentUriHttpFileServer simpleHttpFileServer;
    private SimpleContentUriHttpFileServer subtitleHttpFileServer;
    private Object stopLock = new Object();
    private Object resumeLock = new Object();
    private Object pauseLock = new Object();
    protected ResponseHandler mResponseHandler;
    protected final HashMap<Long, String> mResponseMap = new HashMap<>();
    private boolean isStopped = false;

    public interface ResponseHandler {
        void process(JSONRPC2Response resp, HashMap<Long, String> waitResponseMap);
    }

    public interface RPCAPI {
        // JRPC_REQUEST_METHOD
        // dongle => app
        String RPC_METHOD_GETMEDIASOURCE = "media.getMediaSource";

        // app => dongle
        String RPC_METHOD_GETCURRENTMEDIA = "media.getCurrentMedia";
        String RPC_METHOD_GETCURRENTPLAYLIST = "media.getCurrentPlaylist";
        String RPC_METHOD_GETCURRENTPLAYERSTATE = "media.getCurrentPlayerState";


        String RPC_METHOD_PLAYLIST = "media.playPlaylist";
        // old protocol
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
    private Thread receivingThread;

    public MediaStreaming2(Falcon.ProjectorInfo projectorInfo) {
        isStopped = false;
        mProjectorInfo = projectorInfo;
        receivingThread = new Thread() {
            @Override
            public void run() {
                mProjectorInfo.addMessageListener(mMessageListener = new Falcon.ProjectorInfo.MessageListener() {
                    @Override
                    public void onReceiveMessage(Falcon.ProjectorInfo projector, String message) {
                        if (message.startsWith("JSONRPC")) {
                            String jsonstring = parseMessageString(message);

                            if (3 == mJsonRPCVer && !getRealKey().isEmpty()) {
                                jsonstring = CipherUtil.DecryptAES(jsonstring, getRealKey(), ALGORITHM_AES_CBC);
                            }
                            Log.d(TAG, "decrypt string " + jsonstring);
                            handleReceiveJSON(jsonstring);
                        }
                    }

                    @Override
                    public void onException(Falcon.ProjectorInfo projector, Exception e) {
                        Log.e(TAG, "onException", e);
                    }

                    @Override
                    public void onDisconnect(Falcon.ProjectorInfo projector) {
                        Log.d(TAG, "onDisconnect");
                        onClientStop();
                    }
                });
                registerRPC();
            }
        };
        receivingThread.start();
    }

    private void registerRPC() {
        dispatcher.register(new RequestHandler() {
            private long elapsetime;

            @Override
            public String[] handledRequests() {
                return new String[]{
                        RPCAPI.RPC_METHOD_GETMEDIASOURCE
                };
            }

            @Override
            public JSONRPC2Response process(JSONRPC2Request req, MessageContext msg) {
                switch (req.getMethod()) {
                    case RPCAPI.RPC_METHOD_GETMEDIASOURCE:
                        elapsetime = System.currentTimeMillis();
                        Log.d(TAG, " RPC_METHOD_GETMEDIASOURCE");
                        Map<String, Object> params = req.getNamedParams();
                        if (mContext == null) {
                            throw new IllegalStateException("context should not be null");
                        }
                        getMediaSouce(mapToJSON(req.getNamedParams()), req.getID());
                        return null;
                    default:
                        Log.d(TAG, "unhandled method " + req.getMethod());
                        break;
                }
                return null;
            }

            private void getMediaSouce(String video, Object id) {
                mMediaSoucehelper.getMediaSourceViaFw(video, new WebVideoSourceHelper.Listener() {
                    @Override
                    public void onVideoFound(String s, String s1, String s2, String s3, String s4, String s5) {
                        Log.d(TAG, " onVideoFound");
                    }

                    @Override
                    public void onPlaylistFound(String s) {
                        Log.d(TAG, " onPlaylistFound");
                    }

                    @Override
                    public void onMediaError(String s, String s1) {
                        Log.d(TAG, " onMediaError");
                    }

                    @Override
                    public void onMediaFound(String s) {
                        Log.d(TAG, "onMediaFound" + s);
                        Log.d(TAG, "elaspse time " + (System.currentTimeMillis() - elapsetime));
                        if(isStopped){
                            return;
                        }
                        final HashMap<String, Object> resParams = new HashMap<>();
                        resParams.put("video", jsonToMap(s));
                        JSONRPC2Response rpcResponse = new JSONRPC2Response(resParams, id);
                        sendJSONRPC(rpcResponse.toString());
                    }
                });

            }
        });

        dispatcher.register(new NotificationHandler() {
            private MediaPlayerApi.State lastState = mCurrentState;
            @Override
            public String[] handledNotifications() {
                return new String[]{
                        RPCAPI.RPC_NOTIFICATION_ONPLAYLISTUPDATE,
                        RPCAPI.RPC_NOTIFICATION_ONPLAY,
                        RPCAPI.RPC_NOTIFICATION_ONPLAYERSTATECHANGE,
                        RPCAPI.RPC_NOTIFICATION_ONTIMEUPDATE,
                        RPCAPI.RPC_NOTIFICATION_ONERROR,
                };
            }

            @Override
            public void process(JSONRPC2Notification notification, MessageContext messageContext) {
                Map<String, Object> params = notification.getNamedParams();
                switch (notification.getMethod()) {
                    case RPCAPI.RPC_NOTIFICATION_ONPLAYLISTUPDATE:
                        Log.d(TAG, "RPC_NOTIFICATION_ONPLAYLISTUPDATE");
                        if (!isLocalMedia()) {
                            Object playlistObj = params.get("playlist");
                            if (null != playlistObj) {
                                PlayList playlist = gson.fromJson(playlistObj.toString(), PlayList.class);
                                mCurrentPlayList.setPlaylist(playlist.getPlaylist());
                                mCurrentPlayList.setRawJson(playlistObj.toString());
                                mCurrentPlayList.setStart_index(playlist.getStart_index());
                                if (mMediaApi != null && mMediaStateListener != null) {
                                    mCurrentPlayList.getMediaPlayListListener().onPlayListChanged(mCurrentPlayList);
                                }
                            }
                        }
                        return;

                    case RPCAPI.RPC_NOTIFICATION_ONPLAY:
                        Log.d(TAG, "RPC_NOTIFICATION_ONPLAY");
                        mDuration = ((Long) params.get("duration")).intValue();
                        VideoObj videoObj = gson.fromJson(params.get("video").toString(), VideoObj.class);
                        int currentIndex = ((Long) params.get("currentIndex")).intValue();
                        String originPage = mCurrentPlayList.getPlaylist().get(currentIndex).getPage();
                        videoObj.setPage(originPage);
                        if (mMediaApi != null && mMediaStateListener != null) {
                            if (videoObj.isAudio()) {
                                mCurrentPlayList.getMediaPlayListListener().onMediaChanged(new MusicMediaItem(videoObj.getIndex(), videoObj.getMediaId(), videoObj.getMediaName(), videoObj.getArtistName(),
                                        videoObj.getAlbumName(), videoObj.getAlbumId(), videoObj.getDuration(), videoObj.getData()), currentIndex);
                            } else {
                                mCurrentPlayList.getMediaPlayListListener().onMediaChanged(new VideoMediaItem(videoObj.getSrc(), videoObj.getPage(), videoObj.getTitle(), String.valueOf(currentIndex), videoObj.getImage(),
                                        "", "stream"), currentIndex);
                            }
                            mMediaStateListener.mediaPlayerDurationIsReady(mMediaApi, mDuration);
                        }

                        return;

                    case RPCAPI.RPC_NOTIFICATION_ONPLAYERSTATECHANGE:
                        Log.d(TAG, "RPC_NOTIFICATION_ONPLAYERSTATECHANGE");

                        String state = params.get("state").toString();

                        Log.d(TAG, "state " + state);
                        switch (state) {
                            case "Idle":
                                mCurrentState = MediaPlayerApi.State.IDLE;
                                synchronized (stopLock) {
                                    Log.d(TAG, "stoplock notifyAll");
                                    stopLock.notifyAll();
                                }
                                if (mMediaApi != null && mMediaStateListener != null) {
                                    mCurrentPlayList.getMediaPlayListListener().onListEnded();
                                }

                                break;
                            case "Ended":
                                mCurrentState = MediaPlayerApi.State.ENDED;

                                break;
                            case "Playing":
                                lastState = mCurrentState;
                                mCurrentState = MediaPlayerApi.State.PLAYING;
                                if (mMediaApi != null && mMediaStateListener != null && (lastState == MediaPlayerApi.State.PROCESSING || lastState == MediaPlayerApi.State.IDLE
                                        || lastState == MediaPlayerApi.State.ENDED || lastState == MediaPlayerApi.State.PAUSED)) {
                                    mMediaStateListener.mediaPlayerDidStart(mMediaApi);
                                }
                                break;
                            case "Paused":
                                mCurrentState = MediaPlayerApi.State.PAUSED;
                                break;
                            case "Buffering":
                                mCurrentState = MediaPlayerApi.State.BUFFERING;
                                break;
                            case "Processing":
                                mCurrentState = MediaPlayerApi.State.PROCESSING;
                                break;
                            default:
                                break;
                        }
                        return;

                    case RPCAPI.RPC_NOTIFICATION_ONTIMEUPDATE: {
                        Log.d(TAG, "RPC_NOTIFICATION_ONTIMEUPDATE");
                        mTime = ((Long) params.get("position")).intValue();
                        if (mMediaApi != null && mMediaStateListener != null) {
                            mMediaStateListener.mediaPlayerTimeDidChange(mMediaApi, mTime);
                        }
                        return;
                    }

                    case RPCAPI.RPC_NOTIFICATION_ONERROR:
                        Log.d(TAG, "RPC_NOTIFICATION_ONERROR");
                        String video = params.get("video").toString();
                        String error = params.get("error").toString();

                        Log.d(TAG, "error " + error);

                        int errCode = MediaPlayerApi.AV_RESULT_ERROR_GENERIC;
                        switch (error) {
                            case "AV_RESULT_ERROR_GENERIC":
                                errCode = MediaPlayerApi.AV_RESULT_ERROR_GENERIC;
                                break;
                            case "AV_RESULT_ERROR_START_INIT_FAILED":
                                errCode = MediaPlayerApi.AV_RESULT_ERROR_START_INIT_FAILED;
                                break;
                            case "AV_RESULT_ERROR_START_OCCUPIED_OTHER_USER":
                                errCode = MediaPlayerApi.AV_RESULT_ERROR_START_OCCUPIED_OTHER_USER;
                                break;
                            case "AV_RESULT_ERROR_START_OCCUPIED_ALREADY_STREAMING":
                                errCode = MediaPlayerApi.AV_RESULT_ERROR_START_OCCUPIED_ALREADY_STREAMING;
                                break;
                            case "AV_RESULT_ERROR_STOP_FILE_FORMAT_UNSUPPORTED":
                                errCode = MediaPlayerApi.AV_RESULT_ERROR_STOP_FILE_FORMAT_UNSUPPORTED;
                                break;
                            case "AV_RESULT_ERROR_STOP_ABORTED":
                                errCode = MediaPlayerApi.AV_RESULT_ERROR_STOP_ABORTED;
                                break;
                            case "AV_RESULT_ERROR_URL_DIVERT_LINK_ERROR":
                                errCode = MediaPlayerApi.AV_RESULT_ERROR_URL_DIVERT_LINK_ERROR;
                                break;
                        }
                        if (mMediaApi != null && mMediaStateListener != null) {
                            mMediaStateListener.mediaPlayerDidFailed(mMediaApi, errCode, video);
                        }
                        return;

                    default:
                        Log.d(TAG, "unhandled notification " + notification.getMethod());
                        break;
                }

            }
        });

        mResponseHandler = new ResponseHandler() {
            @Override
            public void process(JSONRPC2Response resp, HashMap<Long, String> waitResponseMap) {
                Object id = resp.getID();
                // workaround when receive JSONRPC2Response without id
                if(id == null){
                    return;
                }
                String method = mResponseMap.remove(Long.valueOf(id.toString()));
                if (method == null) {
                    return;
                }

                Log.d(TAG, "Response method " + method);
                switch (method) {
                    case RPCAPI.RPC_METHOD_STOP:
                        synchronized (stopLock) {
                            stopLock.notifyAll();
                        }
                        break;
                    case RPCAPI.RPC_METHOD_PAUSE:
                        synchronized (pauseLock) {
                            pauseLock.notifyAll();
                        }
                        break;
                    case RPCAPI.RPC_METHOD_PLAY:
                        synchronized (resumeLock) {
                            resumeLock.notifyAll();
                        }
                }
            }
        };
    }

    private void handleReceiveJSON(String msg) {
        try {
            JSONRPC2Message message = JSONRPC2Message.parse(msg);
            if (message instanceof JSONRPC2Request) {
                dispatcher.process((JSONRPC2Request) message, null);
            } else if (message instanceof JSONRPC2Notification) {
                dispatcher.process((JSONRPC2Notification) message, null);
            } else if (message instanceof JSONRPC2Response) {
                JSONRPC2Response resp = (JSONRPC2Response) message;
                mResponseHandler.process((JSONRPC2Response) message, mResponseMap);
            }
        } catch (JSONRPC2ParseException e) {
            Log.e(TAG, "JSONRPC2ParseException", e);
        }
    }

    private int generateRpcId() {
        return mProjectorInfo.getRpcID().getAndIncrement();
    }

    @Override
    public void playPlayList(Context context, PlayList playlist) throws Exception {
        stopHttpFileServer();
        mCurrentPlayList = playlist;
        Log.d(TAG, "playPlayList");
        if (mMediaSoucehelper == null) {
            mMediaSoucehelper = WebVideoSourceHelper.getInstance(context);
        }
        PlayList transformedPlaylist = transformPlayListIfNeed(context, playlist);
        final HashMap<String, Object> params = new HashMap<>();
        if(playlist.getRawJson() != null){
            params.put("playlist", jsonToMap(playlist.getRawJson()));
        } else{
            params.put("playlist", jsonToMap(gson.toJson(transformedPlaylist)));
        }

        JSONRPC2Request req = new JSONRPC2Request(RPCAPI.RPC_METHOD_PLAYLIST, params, generateRpcId());
        sendJSONRPC(req.toString());
        mContext = context;
    }

    private PlayList transformPlayListIfNeed(Context context, PlayList playlist) throws Exception {
        PlayList result = playlist;
        int index = result.getStart_index();
        VideoObj currentVideoObj = result.getPlaylist().get(index);
        String mediaUrl = currentVideoObj.getSrc();

        if (mediaUrl == null || mediaUrl.startsWith("http") || mediaUrl.startsWith("rtsp") || mediaUrl.startsWith("mms")) {
            return result;
        } else if (MediaStreamingFileDataSource.supportsFileExt(com.actionsmicro.utils.Utils.getFileExtension(mediaUrl).toLowerCase())
                || mediaUrl.startsWith(ContentResolver.SCHEME_CONTENT)) {
            Uri mediaUri = buildLocalUri(mediaUrl);
            List<Caption> captionList = currentVideoObj.getCaptions();

            if (captionList != null && captionList.size() > 0) {
                Caption caption = captionList.get(0);
                String subtitlePath = caption.getUrl();
                Uri subtitleUri = buildLocalUri(subtitlePath);

                if (TetheringUtil.isUsbTethered(context)) {
                    subtitleHttpFileServer = new SimpleContentUriHttpFileServer(context, subtitleUri, "192.168.42.129", 0);
                } else {
                    subtitleHttpFileServer = new SimpleContentUriHttpFileServer(context, subtitleUri, 0);
                }
                subtitleHttpFileServer.start();

                String subTitleUrlPath = subtitleHttpFileServer.getServerUrl() + "/SubTitle?filename=" + URLEncoder.encode(subtitleUri.toString(), "UTF-8");
                caption.setUrl(subTitleUrlPath);
            }

            if (TetheringUtil.isUsbTethered(context)) {
                simpleHttpFileServer = new SimpleContentUriHttpFileServer(context, mediaUri, "192.168.42.129", 0);
            } else {
                simpleHttpFileServer = new SimpleContentUriHttpFileServer(context, mediaUri, 0);
            }
            simpleHttpFileServer.start();

            for (int i = 0; i < result.getPlaylist().size(); i++) {
                VideoObj v = result.getPlaylist().get(i);

                Uri uri = buildLocalUri(v.getSrc());
                String mediaUriString = simpleHttpFileServer.getServerUrl() + "/LocalVideo?filename=" + URLEncoder.encode(uri.toString(), "UTF-8");
                v.setSrc(mediaUriString);
            }
        }
        return result;
    }

    private Uri buildLocalUri(String mediaUrl) {
        Uri mediaUri = null;
        try {
            mediaUri = Uri.parse(mediaUrl);
            if (mediaUri.getScheme() == null) {
                mediaUri = mediaUri.buildUpon().scheme("file").build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            mediaUri = Uri.fromFile(new File(mediaUrl));
        }
        return mediaUri;
    }

    private Map<String, Object> jsonToMap(String playlist) {
        return gson.fromJson(
                playlist, new TypeToken<HashMap<String, Object>>() {
                }.getType()
        );
    }

    private String mapToJSON(Object object) {
        return gson.toJson(object);
    }


    @Override
    public void next() {
        Log.d(TAG, "next");
        JSONRPC2Request req = new JSONRPC2Request(RPCAPI.RPC_METHOD_NEXT, generateRpcId());
        sendJSONRPC(req.toString());
    }

    @Override
    public void previous() {
        Log.d(TAG, "previous");
        JSONRPC2Request req = new JSONRPC2Request(RPCAPI.RPC_METHOD_PREVIOUS, generateRpcId());
        sendJSONRPC(req.toString());
    }

    @Override
    public String getCurrentMedia() {
        Log.d(TAG, "getCurrentMedia");
        JSONRPC2Request req = new JSONRPC2Request(RPCAPI.RPC_METHOD_GETCURRENTMEDIA, generateRpcId());
        sendJSONRPC(req.toString());
        return null;
    }

    @Override
    public String getCurrentPlaylist() {
        Log.d(TAG, "getCurrentMedia");
        JSONRPC2Request req = new JSONRPC2Request(RPCAPI.RPC_METHOD_GETCURRENTPLAYLIST, generateRpcId());
        sendJSONRPC(req.toString());
        return null;
    }

    @Override
    public void setMediaStreamingStateListener(MediaPlayerApi api, MediaPlayerApi.MediaPlayerStateListener mediaPlayerStateListener) {
        mMediaApi = api;
        mMediaStateListener = mediaPlayerStateListener;
    }

    @Override
    public boolean playAt(int position) {
        Log.d(TAG, "playAt");
        final HashMap<String, Object> params = new HashMap<>();
        params.put("videoIndex", position);
        JSONRPC2Request req = new JSONRPC2Request(RPCAPI.RPC_METHOD_PLAYAT, params, generateRpcId());
        sendJSONRPC(req.toString());
        return true;
    }

    @Override
    public void startMediaStreaming(MediaStreaming.DataSource dataSource) {
        // TODO check is deprecated or use play instead
        Log.d(TAG, "startMediaStreaming");
        JSONRPC2Request req = new JSONRPC2Request(RPCAPI.RPC_METHOD_PLAY, generateRpcId());
        sendJSONRPC(req.toString());
    }

    @Override
    public int getDuration() {
        return mDuration;
    }

    @Override
    public int getTime() {
        return mTime;
    }

    @Override
    public int seekTo(int position) {
        Log.d(TAG, "seekto");
        final HashMap<String, Object> params = new HashMap<>();
        params.put("position", position);
        JSONRPC2Request req = new JSONRPC2Request(RPCAPI.RPC_METHOD_SEEKTO, params, generateRpcId());
        sendJSONRPC(req.toString());
        return 0;
    }

    @Override
    public int pauseMediaStreaming() {
        Log.d(TAG, "pauseMediaStreaming");
        JSONRPC2Request req = new JSONRPC2Request(RPCAPI.RPC_METHOD_PAUSE, generateRpcId());
        mResponseMap.put(Long.valueOf(req.getID().toString()), req.getMethod());
        sendJSONRPC(req.toString());
        try {
            synchronized (pauseLock) {
                pauseLock.wait(DEFAULT_WAIT_TIMEOUT);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int resumeMediaStreaming() {
        Log.d(TAG, "resumeMediaStreaming");
        JSONRPC2Request req = new JSONRPC2Request(RPCAPI.RPC_METHOD_PLAY, generateRpcId());
        mResponseMap.put(Long.valueOf(req.getID().toString()), req.getMethod());
        sendJSONRPC(req.toString());
        try {
            synchronized (resumeLock) {
                resumeLock.wait(DEFAULT_WAIT_TIMEOUT);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int increaseVolume() {
        Log.d(TAG, "increaseVolume");
        JSONRPC2Request req = new JSONRPC2Request(RPCAPI.RPC_METHOD_VOLUMEUP, generateRpcId());
        sendJSONRPC(req.toString());
        return 0;
    }

    @Override
    public int decreaseVolume() {
        Log.d(TAG, "decreaseVolume");
        JSONRPC2Request req = new JSONRPC2Request(RPCAPI.RPC_METHOD_VOLUMEDOWN, generateRpcId());
        sendJSONRPC(req.toString());
        return 0;
    }

    @Override
    public void stopMediaStreaming() {
        Log.d(TAG, "stopMediaStreaming");
        JSONRPC2Request req = new JSONRPC2Request(RPCAPI.RPC_METHOD_STOP, generateRpcId());
        mResponseMap.put(Long.valueOf(req.getID().toString()), req.getMethod());
        sendJSONRPC(req.toString());
        try {
            synchronized (stopLock) {
                stopLock.wait(DEFAULT_WAIT_TIMEOUT);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
        return mCurrentState;
    }


    private void sendJSONRPC(String json) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mProjectorInfo != null && json != null) {
                    String realKey = mProjectorInfo.getRealKey();
                    String content = json;
                    if (!realKey.isEmpty()) {
                        Log.d(TAG, "before encrypt: " + content);
                        content = CipherUtil.EncryptAES(content, realKey, ALGORITHM_AES_CBC);
                        Log.d(TAG, "after encrypt: " + content);
                        String decryptAES = CipherUtil.DecryptAES(content, realKey, ALGORITHM_AES_CBC);
                        Log.d(TAG, "after decrypt: " + decryptAES);
                    }
                    mProjectorInfo.sendJSONRPC(getEncryptCommand(), content);
                }
            }
        }).start();
    }


    private int getEncryptCommand() {
        if (!getRealKey().isEmpty()) {
            return COMMAND_JSONRPC_ENCRYPT;
        }
        return COMMAND_JSONRPC;
    }

    private int mJsonRPCVer = 2;

    private String parseMessageString(final String receiveString) {
        final String[] parameters = receiveString.split(":");
        if (parameters.length >= 3) {
            mJsonRPCVer = Integer.valueOf(parameters[1]);
            return Utils.concatStringsWithSeparator(Arrays.asList(Arrays.copyOfRange(parameters, 2, parameters.length)), ":");
        }
        return null;
    }

    private String getRealKey() {
        return mProjectorInfo == null ? "" : mProjectorInfo.getRealKey();
    }

    private void stopHttpFileServer() {
        if (simpleHttpFileServer != null) {
            simpleHttpFileServer.stop();
            simpleHttpFileServer = null;
        }

        if (subtitleHttpFileServer != null) {
            subtitleHttpFileServer.stop();
            subtitleHttpFileServer = null;
        }
    }


    private boolean isLocalMedia() {
        return simpleHttpFileServer != null;
    }

    @Override
    public void onClientStop() {
        Log.d(TAG, "onClientStop");
        cleanup();
    }

    private void cleanup() {
        mProjectorInfo.setCapabilityListener(null);
        mProjectorInfo.removeMessageListener(mMessageListener);
        isStopped = true;
        stopHttpFileServer();
    }
}
