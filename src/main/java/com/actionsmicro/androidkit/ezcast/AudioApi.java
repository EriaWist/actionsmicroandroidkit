package com.actionsmicro.androidkit.ezcast;

import java.io.IOException;
import java.io.InputStream;

public interface AudioApi extends Api {

    /**
     * Play PCM data on the device.
     * @param input InputStream which wraps the PCM data.
     * @throws Exception
     * @since 2.3
     */
    public void sendAudioEncodedData(InputStream inputStream)
    		throws IllegalArgumentException, IOException;

}
