package com.actionsmicro.androidkit.ezcast.imp.ezdisplay;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.actionsmicro.androidkit.ezcast.AudioApi;
import com.actionsmicro.androidkit.ezcast.AudioApiBuilder;

public class PigeonAudioApi extends PigeonApi implements AudioApi {

    public PigeonAudioApi(AudioApiBuilder audioApiBuilder) {
        super(audioApiBuilder);
    }

    /**
     * Send pcm audio data to the device.
     * @param inputStream InputStream which wraps the PCM Audio data.
     * @throws Exception
     * @since 2.3
     */
    @Override
    public void sendAudioEncodedData(InputStream inputStream,int offset, int length) throws IllegalArgumentException, IOException {
        if (pigeonClient != null) {
            pigeonClient.sendAudioStreamToServer(inputStream, offset, length);
        }
    }

    /**
     * Send aac audio data to the device.
     * @param dataBuffer ByteBuffer which wraps the AAC Audio data
     * @param size Data size
     * @throws IllegalArgumentException
     * @throws IOException
     * @since 2.14
     */
    @Override
    public void sendAACAudioEncodedData(ByteBuffer dataBuffer, int size) throws IllegalArgumentException, IOException {
        if (pigeonClient != null) {
            pigeonClient.sendAACAudioToServer(dataBuffer, size);
        }
    }
}
