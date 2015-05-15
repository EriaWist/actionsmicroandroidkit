package com.koushikdutta.async.http;

import android.net.Uri;

public class AsyncHttpPost extends AsyncHttpRequest {
    public static final String METHOD = "POST";
    
    public AsyncHttpPost(String uri) {
        this(Uri.parse(uri));
    }

    public AsyncHttpPost(Uri uri) {
        super(uri, METHOD);
    }

	public AsyncHttpPost(Uri uri, Headers headers) {
        super(uri, METHOD, headers);
	}
}
