package com.actionsmicro.utils;

import java.io.InputStream;

import org.apache.http.entity.mime.content.InputStreamBody;

public class InputStreamKnownSizeBody extends InputStreamBody {
    private int lenght;

    public InputStreamKnownSizeBody(
            final InputStream in, final int lenght, final String filename) {
        super(in, filename);
        this.lenght = lenght;
    }

    @Override
    public long getContentLength() {
        return this.lenght;
    }
}