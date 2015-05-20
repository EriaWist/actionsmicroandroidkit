package com.actionsmicro.airplay.http;

import com.koushikdutta.async.http.body.StringBody;

public class PlistBody extends StringBody {
	public PlistBody() {
	}
	public PlistBody(String xmlPropertyList) {
		super(xmlPropertyList);
	}

	@Override
    public String getContentType() {
        return "application/x-apple-binary-plist";
    }
}
