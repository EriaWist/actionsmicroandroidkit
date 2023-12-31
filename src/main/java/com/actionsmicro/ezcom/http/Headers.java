/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.actionsmicro.ezcom.http;

import java.util.ArrayList;

import org.apache.http.HeaderElement;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.message.BasicHeaderValueParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.CharArrayBuffer;

import android.util.Log;

/**
 * Manages received headers
 *
 * {@hide}
 */
public final class Headers {
    private static final String LOGTAG = "Http";

    // header parsing constant
    /**
     * indicate HTTP 1.0 connection close after the response
     */
    public final static int CONN_CLOSE = 1;
    /**
     * indicate HTTP 1.1 connection keep alive
     */
    public final static int CONN_KEEP_ALIVE = 2;

    // initial values.
    public final static int NO_CONN_TYPE = 0;
    public final static long NO_TRANSFER_ENCODING = 0;
    public final static long NO_CONTENT_LENGTH = -1;

    // header strings
    public final static String TRANSFER_ENCODING = "transfer-encoding";
    public final static String CONTENT_LEN = "content-length";
    public final static String CONTENT_TYPE = "content-type";
    public final static String CONTENT_ENCODING = "content-encoding";
    public final static String CONN_DIRECTIVE = "connection";

    public final static String LOCATION = "location";
    public final static String PROXY_CONNECTION = "proxy-connection";

    public final static String WWW_AUTHENTICATE = "www-authenticate";
    public final static String PROXY_AUTHENTICATE = "proxy-authenticate";
    public final static String CONTENT_DISPOSITION = "content-disposition";
    public final static String ACCEPT_RANGES = "accept-ranges";
    public final static String EXPIRES = "expires";
    public final static String CACHE_CONTROL = "cache-control";
    public final static String LAST_MODIFIED = "last-modified";
    public final static String ETAG = "etag";
    public final static String SET_COOKIE = "set-cookie";
    public final static String PRAGMA = "pragma";
    public final static String REFRESH = "refresh";
    public final static String X_PERMITTED_CROSS_DOMAIN_POLICIES = "x-permitted-cross-domain-policies";

    // following hash are generated by String.hashCode()
    private final static int HASH_TRANSFER_ENCODING = 1274458357;
    private final static int HASH_CONTENT_LEN = -1132779846;
    private final static int HASH_CONTENT_TYPE = 785670158;
    private final static int HASH_CONTENT_ENCODING = 2095084583;
    private final static int HASH_CONN_DIRECTIVE = -775651618;
    private final static int HASH_LOCATION = 1901043637;
    private final static int HASH_PROXY_CONNECTION = 285929373;
    private final static int HASH_WWW_AUTHENTICATE = -243037365;
    private final static int HASH_PROXY_AUTHENTICATE = -301767724;
    private final static int HASH_CONTENT_DISPOSITION = -1267267485;
    private final static int HASH_ACCEPT_RANGES = 1397189435;
    private final static int HASH_EXPIRES = -1309235404;
    private final static int HASH_CACHE_CONTROL = -208775662;
    private final static int HASH_LAST_MODIFIED = 150043680;
    private final static int HASH_ETAG = 3123477;
    private final static int HASH_SET_COOKIE = 1237214767;
    private final static int HASH_PRAGMA = -980228804;
    private final static int HASH_REFRESH = 1085444827;
    private final static int HASH_X_PERMITTED_CROSS_DOMAIN_POLICIES = -1345594014;

    // keep any headers that require direct access in a presized
    // string array
    private final static int IDX_TRANSFER_ENCODING = 0;
    private final static int IDX_CONTENT_LEN = 1;
    private final static int IDX_CONTENT_TYPE = 2;
    private final static int IDX_CONTENT_ENCODING = 3;
    private final static int IDX_CONN_DIRECTIVE = 4;
    private final static int IDX_LOCATION = 5;
    private final static int IDX_PROXY_CONNECTION = 6;
    private final static int IDX_WWW_AUTHENTICATE = 7;
    private final static int IDX_PROXY_AUTHENTICATE = 8;
    private final static int IDX_CONTENT_DISPOSITION = 9;
    private final static int IDX_ACCEPT_RANGES = 10;
    private final static int IDX_EXPIRES = 11;
    private final static int IDX_CACHE_CONTROL = 12;
    private final static int IDX_LAST_MODIFIED = 13;
    private final static int IDX_ETAG = 14;
    private final static int IDX_SET_COOKIE = 15;
    private final static int IDX_PRAGMA = 16;
    private final static int IDX_REFRESH = 17;
    private final static int IDX_X_PERMITTED_CROSS_DOMAIN_POLICIES = 18;

    private final static int HEADER_COUNT = 19;

    /* parsed values */
    private long transferEncoding;
    private long contentLength; // Content length of the incoming data
    private int connectionType;
    private ArrayList<String> cookies = new ArrayList<String>(2);

    private String[] mHeaders = new String[HEADER_COUNT];
    private final static String[] sHeaderNames = {
        TRANSFER_ENCODING,
        CONTENT_LEN,
        CONTENT_TYPE,
        CONTENT_ENCODING,
        CONN_DIRECTIVE,
        LOCATION,
        PROXY_CONNECTION,
        WWW_AUTHENTICATE,
        PROXY_AUTHENTICATE,
        CONTENT_DISPOSITION,
        ACCEPT_RANGES,
        EXPIRES,
        CACHE_CONTROL,
        LAST_MODIFIED,
        ETAG,
        SET_COOKIE,
        PRAGMA,
        REFRESH,
        X_PERMITTED_CROSS_DOMAIN_POLICIES
    };

    // Catch-all for headers not explicitly handled
    private ArrayList<String> mExtraHeaderNames = new ArrayList<String>(4);
    private ArrayList<String> mExtraHeaderValues = new ArrayList<String>(4);

    public Headers() {
        transferEncoding = NO_TRANSFER_ENCODING;
        contentLength = NO_CONTENT_LENGTH;
        connectionType = NO_CONN_TYPE;
    }

    public void parseHeader(CharArrayBuffer buffer) {
        int pos = CharArrayBuffers.setLowercaseIndexOf(buffer, ':');
        if (pos == -1) {
            return;
        }
        String name = buffer.substringTrimmed(0, pos);
        if (name.length() == 0) {
            return;
        }
        pos++;

        String val = buffer.substringTrimmed(pos, buffer.length());
        if (HttpLog.LOGV) {
            HttpLog.v("hdr " + buffer.length() + " " + buffer);
        }

        switch (name.hashCode()) {
        case HASH_TRANSFER_ENCODING:
            if (name.equals(TRANSFER_ENCODING)) {
                mHeaders[IDX_TRANSFER_ENCODING] = val;
                HeaderElement[] encodings = BasicHeaderValueParser.DEFAULT
                        .parseElements(buffer, new ParserCursor(pos,
                                buffer.length()));
                // The chunked encoding must be the last one applied RFC2616,
                // 14.41
                int len = encodings.length;
                if (HTTP.IDENTITY_CODING.equalsIgnoreCase(val)) {
                    transferEncoding = ContentLengthStrategy.IDENTITY;
                } else if ((len > 0)
                        && (HTTP.CHUNK_CODING
                                .equalsIgnoreCase(encodings[len - 1].getName()))) {
                    transferEncoding = ContentLengthStrategy.CHUNKED;
                } else {
                    transferEncoding = ContentLengthStrategy.IDENTITY;
                }
            }
            break;
        case HASH_CONTENT_LEN:
            if (name.equals(CONTENT_LEN)) {
                mHeaders[IDX_CONTENT_LEN] = val;
                try {
                    contentLength = Long.parseLong(val);
                } catch (NumberFormatException e) {
                    if (false) {
                        Log.v(LOGTAG, "Headers.headers(): error parsing"
                                + " content length: " + buffer.toString());
                    }
                }
            }
            break;
        case HASH_CONTENT_TYPE:
            if (name.equals(CONTENT_TYPE)) {
                mHeaders[IDX_CONTENT_TYPE] = val;
            }
            break;
        case HASH_CONTENT_ENCODING:
            if (name.equals(CONTENT_ENCODING)) {
                mHeaders[IDX_CONTENT_ENCODING] = val;
            }
            break;
        case HASH_CONN_DIRECTIVE:
            if (name.equals(CONN_DIRECTIVE)) {
                mHeaders[IDX_CONN_DIRECTIVE] = val;
                setConnectionType(buffer, pos);
            }
            break;
        case HASH_LOCATION:
            if (name.equals(LOCATION)) {
                mHeaders[IDX_LOCATION] = val;
            }
            break;
        case HASH_PROXY_CONNECTION:
            if (name.equals(PROXY_CONNECTION)) {
                mHeaders[IDX_PROXY_CONNECTION] = val;
                setConnectionType(buffer, pos);
            }
            break;
        case HASH_WWW_AUTHENTICATE:
            if (name.equals(WWW_AUTHENTICATE)) {
                mHeaders[IDX_WWW_AUTHENTICATE] = val;
            }
            break;
        case HASH_PROXY_AUTHENTICATE:
            if (name.equals(PROXY_AUTHENTICATE)) {
                mHeaders[IDX_PROXY_AUTHENTICATE] = val;
            }
            break;
        case HASH_CONTENT_DISPOSITION:
            if (name.equals(CONTENT_DISPOSITION)) {
                mHeaders[IDX_CONTENT_DISPOSITION] = val;
            }
            break;
        case HASH_ACCEPT_RANGES:
            if (name.equals(ACCEPT_RANGES)) {
                mHeaders[IDX_ACCEPT_RANGES] = val;
            }
            break;
        case HASH_EXPIRES:
            if (name.equals(EXPIRES)) {
                mHeaders[IDX_EXPIRES] = val;
            }
            break;
        case HASH_CACHE_CONTROL:
            if (name.equals(CACHE_CONTROL)) {
                // In case where we receive more than one header, create a ',' separated list.
                // This should be ok, according to RFC 2616 chapter 4.2
                if (mHeaders[IDX_CACHE_CONTROL] != null &&
                    mHeaders[IDX_CACHE_CONTROL].length() > 0) {
                    mHeaders[IDX_CACHE_CONTROL] += (',' + val);
                } else {
                    mHeaders[IDX_CACHE_CONTROL] = val;
                }
            }
            break;
        case HASH_LAST_MODIFIED:
            if (name.equals(LAST_MODIFIED)) {
                mHeaders[IDX_LAST_MODIFIED] = val;
            }
            break;
        case HASH_ETAG:
            if (name.equals(ETAG)) {
                mHeaders[IDX_ETAG] = val;
            }
            break;
        case HASH_SET_COOKIE:
            if (name.equals(SET_COOKIE)) {
                mHeaders[IDX_SET_COOKIE] = val;
                cookies.add(val);
            }
            break;
        case HASH_PRAGMA:
            if (name.equals(PRAGMA)) {
                mHeaders[IDX_PRAGMA] = val;
            }
            break;
        case HASH_REFRESH:
            if (name.equals(REFRESH)) {
                mHeaders[IDX_REFRESH] = val;
            }
            break;
        case HASH_X_PERMITTED_CROSS_DOMAIN_POLICIES:
            if (name.equals(X_PERMITTED_CROSS_DOMAIN_POLICIES)) {
                mHeaders[IDX_X_PERMITTED_CROSS_DOMAIN_POLICIES] = val;
            }
            break;
        default:
            mExtraHeaderNames.add(name);
            mExtraHeaderValues.add(val);
        }
    }

    public long getTransferEncoding() {
        return transferEncoding;
    }

    public long getContentLength() {
        return contentLength;
    }

    public int getConnectionType() {
        return connectionType;
    }

    public String getContentType() {
        return mHeaders[IDX_CONTENT_TYPE];
    }

    public String getContentEncoding() {
        return mHeaders[IDX_CONTENT_ENCODING];
    }

    public String getLocation() {
        return mHeaders[IDX_LOCATION];
    }

    public String getWwwAuthenticate() {
        return mHeaders[IDX_WWW_AUTHENTICATE];
    }

    public String getProxyAuthenticate() {
        return mHeaders[IDX_PROXY_AUTHENTICATE];
    }

    public String getContentDisposition() {
        return mHeaders[IDX_CONTENT_DISPOSITION];
    }

    public String getAcceptRanges() {
        return mHeaders[IDX_ACCEPT_RANGES];
    }

    public String getExpires() {
        return mHeaders[IDX_EXPIRES];
    }

    public String getCacheControl() {
        return mHeaders[IDX_CACHE_CONTROL];
    }

    public String getLastModified() {
        return mHeaders[IDX_LAST_MODIFIED];
    }

    public String getEtag() {
        return mHeaders[IDX_ETAG];
    }

    public ArrayList<String> getSetCookie() {
        return this.cookies;
    }

    public String getPragma() {
        return mHeaders[IDX_PRAGMA];
    }

    public String getRefresh() {
        return mHeaders[IDX_REFRESH];
    }

    public String getXPermittedCrossDomainPolicies() {
        return mHeaders[IDX_X_PERMITTED_CROSS_DOMAIN_POLICIES];
    }

    public void setContentLength(long value) {
        this.contentLength = value;
    }

    public void setContentType(String value) {
        mHeaders[IDX_CONTENT_TYPE] = value;
    }

    public void setContentEncoding(String value) {
        mHeaders[IDX_CONTENT_ENCODING] = value;
    }

    public void setLocation(String value) {
        mHeaders[IDX_LOCATION] = value;
    }

    public void setWwwAuthenticate(String value) {
        mHeaders[IDX_WWW_AUTHENTICATE] = value;
    }

    public void setProxyAuthenticate(String value) {
        mHeaders[IDX_PROXY_AUTHENTICATE] = value;
    }

    public void setContentDisposition(String value) {
        mHeaders[IDX_CONTENT_DISPOSITION] = value;
    }

    public void setAcceptRanges(String value) {
        mHeaders[IDX_ACCEPT_RANGES] = value;
    }

    public void setExpires(String value) {
        mHeaders[IDX_EXPIRES] = value;
    }

    public void setCacheControl(String value) {
        mHeaders[IDX_CACHE_CONTROL] = value;
    }

    public void setLastModified(String value) {
        mHeaders[IDX_LAST_MODIFIED] = value;
    }

    public void setEtag(String value) {
        mHeaders[IDX_ETAG] = value;
    }

    public void setXPermittedCrossDomainPolicies(String value) {
        mHeaders[IDX_X_PERMITTED_CROSS_DOMAIN_POLICIES] = value;
    }

    public interface HeaderCallback {
        public void header(String name, String value);
    }

    /**
     * Reports all non-null headers to the callback
     */
    public void getHeaders(HeaderCallback hcb) {
        for (int i = 0; i < HEADER_COUNT; i++) {
            String h = mHeaders[i];
            if (h != null) {
                hcb.header(sHeaderNames[i], h);
            }
        }
        int extraLen = mExtraHeaderNames.size();
        for (int i = 0; i < extraLen; i++) {
            if (false) {
                HttpLog.v("Headers.getHeaders() extra: " + i + " " +
                          mExtraHeaderNames.get(i) + " " + mExtraHeaderValues.get(i));
            }
            hcb.header(mExtraHeaderNames.get(i),
                       mExtraHeaderValues.get(i));
        }

    }

    private void setConnectionType(CharArrayBuffer buffer, int pos) {
        if (CharArrayBuffers.containsIgnoreCaseTrimmed(
                buffer, pos, HTTP.CONN_CLOSE)) {
            connectionType = CONN_CLOSE;
        } else if (CharArrayBuffers.containsIgnoreCaseTrimmed(
                buffer, pos, HTTP.CONN_KEEP_ALIVE)) {
            connectionType = CONN_KEEP_ALIVE;
        }
    }
}
