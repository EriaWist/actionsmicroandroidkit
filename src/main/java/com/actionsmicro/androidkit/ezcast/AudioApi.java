package com.actionsmicro.androidkit.ezcast;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public interface AudioApi extends Api {

    public void sendAudioEncodedData(InputStream inputStream,int offset , int length)
    		throws IllegalArgumentException, IOException;


    public void sendAACAudioEncodedData(ByteBuffer dataBuffer, int size) throws IllegalArgumentException, IOException;
}
