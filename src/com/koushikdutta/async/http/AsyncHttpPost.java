package com.koushikdutta.async.http;

import java.net.URI;

import com.koushikdutta.async.http.libcore.RawHeaders;

public class AsyncHttpPost extends AsyncHttpRequest {
    public static final String METHOD = "POST";
    
    public AsyncHttpPost(String uri) {
        this(URI.create(uri));
    }

    public AsyncHttpPost(URI uri) {
        super(uri, METHOD);
    }
    public AsyncHttpPost(URI uri, RawHeaders headers) {
    	super(uri, METHOD, headers);
    }
}
