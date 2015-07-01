package com.actionsmicro.androidkit.ezcast.imp.ezdisplay;

import java.io.IOException;
import java.io.InputStream;

import com.actionsmicro.androidkit.ezcast.AudioApi;
import com.actionsmicro.androidkit.ezcast.AudioApiBuilder;

public class PigeonAudioApi extends PigeonApi implements AudioApi {

    public PigeonAudioApi(AudioApiBuilder audioApiBuilder) {
        super(audioApiBuilder);
    }

    /**
     * Send audio data to the device.
     * @param inputStream InputStream which wraps the PCM Audio data.
     * @throws Exception
     * @since 2.3
     */
    @Override
    public void sendAudioEncodedData(InputStream inputStream) throws IllegalArgumentException, IOException {
        if (pigeonClient != null) {
            pigeonClient.sendAudioStreamToServer(inputStream);
        }
    }

}
