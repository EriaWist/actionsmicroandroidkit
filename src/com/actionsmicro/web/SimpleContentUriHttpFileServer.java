package com.actionsmicro.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.OpenableColumns;

import com.actionsmicro.utils.Log;

import fi.iki.elonen.NanoHTTPD;

public class SimpleContentUriHttpFileServer extends NanoHTTPD {

	private static final String TAG = "SimpleContentUriHttpFileServer";
	private long contentLength = -1;
	private Context context;
	private Uri contentUri;
	public SimpleContentUriHttpFileServer(Context context, Uri contentUri, int portNumber) {
		super(portNumber);
		this.context = context;
		this.contentUri = contentUri;
	}

	public void stop() {
		super.stop();
	}
	public String getIPAddress(boolean useIPv4) { //TODO  DRY
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();

		String ipString = String.format(
				"%d.%d.%d.%d",
				(ip & 0xff),
				(ip >> 8 & 0xff),
				(ip >> 16 & 0xff),
				(ip >> 24 & 0xff));

		return ipString;
    }
	public String getServerUrl() {
		try {
			return new URL("http", getIPAddress(true), getListeningPort(), "").toString();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	public Response serve(IHTTPSession session) {
		Map<String, String> header = session.getHeaders();
		Log.d(TAG, "serve: "+session.getMethod()+" range:"+header.get("range"));
        return serveFile(Collections.unmodifiableMap(header));
	}
	private long getContentLength() {
		if (contentLength == -1) {
			if (contentUri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
				String[] proj = { OpenableColumns.SIZE };
				Cursor cursor = context.getContentResolver().query(contentUri, null, null, null, null);
				if (cursor != null && cursor.moveToFirst()) {
					int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
					if (!cursor.isNull(sizeIndex)) {
						contentLength = Long.valueOf(cursor.getString(sizeIndex));
					} else {
						contentLength = cursor.getLong(sizeIndex);
					}
				}
			} else if (contentUri.getScheme().equalsIgnoreCase("file")) {
				contentLength = new File(contentUri.getPath()).length();				
			}
		}
		return contentLength;
	}
	private Response serveFile(Map<String, String> header) {
        Response res;
        try {
            // Calculate etag
//            String etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified() + "" + file.length()).hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Change return code and add Content-Range header when skipping is requested
            long fileLen = getContentLength();
            String mime = getMimeType();
            if (range != null && startFrom >= 0) {
                if (startFrom >= fileLen) {
                    res = createResponse(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
//                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    final long dataLen = newLen;
//                    FileInputStream fis = new FileInputStream(file) {
//                        @Override
//                        public int available() throws IOException {
//                            return (int) dataLen;
//                        }
//                    };
                    InputStream in = getInputStream(startFrom, dataLen);
                    
                    
                    res = createResponse(Response.Status.PARTIAL_CONTENT, mime, in);
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
//                    res.addHeader("ETag", etag);
                }
            } else {
//                if (etag.equals(header.get("if-none-match")))
//                    res = createResponse(Response.Status.NOT_MODIFIED, mime, "");
//                else {
            	InputStream in = getInputStream(0, fileLen);
                    res = createResponse(Response.Status.OK, mime, in);
                    res.addHeader("Content-Length", "" + fileLen);
//                    res.addHeader("ETag", etag);
//                }
            }
        } catch (IOException ioe) {
            res = createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
        }

        return res;
    }
	private InputStream getInputStream(long startFrom, final long dataLen) throws IOException {
		InputStream in = null;
		if (contentUri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
			in = context.getContentResolver().openInputStream(contentUri);
		}  else if (contentUri.getScheme().equalsIgnoreCase("file"))  {
			in = new FileInputStream(new File(contentUri.getPath())) {
                @Override
                public int available() throws IOException {
                    return (int) dataLen;
                }
            };
		}
		if (in != null) {
			in.skip(startFrom);
		}
		return in;
	}

	public String getMimeType() {
		if (contentUri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {			
			return context.getContentResolver().getType(contentUri);
		} else if (contentUri.getScheme().equalsIgnoreCase("file"))  {
			return getMimeTypeForFile(contentUri.toString());
		}
		return null;
	}

	private Response createResponse(Response.Status status, String mimeType, String message) {
        Response res = new Response(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }
	private Response createResponse(Response.Status status, String mimeType, InputStream message) {
        Response res = new Response(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }
	public static final String MIME_DEFAULT_BINARY = "application/octet-stream";
    
	/**
     * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
     */
    private static final Map<String, String> MIME_TYPES = new HashMap<String, String>() {{
        put("css", "text/css");
        put("htm", "text/html");
        put("html", "text/html");
        put("xml", "text/xml");
        put("java", "text/x-java-source, text/java");
        put("md", "text/plain");
        put("txt", "text/plain");
        put("asc", "text/plain");
        put("gif", "image/gif");
        put("jpg", "image/jpeg");
        put("jpeg", "image/jpeg");
        put("png", "image/png");
        put("mp3", "audio/mpeg");
        put("m3u", "audio/mpeg-url");
        put("mp4", "video/mp4");
        put("ogv", "video/ogg");
        put("flv", "video/x-flv");
        put("mov", "video/quicktime");
        put("swf", "application/x-shockwave-flash");
        put("js", "application/javascript");
        put("pdf", "application/pdf");
        put("doc", "application/msword");
        put("ogg", "application/x-ogg");
        put("zip", "application/octet-stream");
        put("exe", "application/octet-stream");
        put("class", "application/octet-stream");
        put("avi", "video/x-msvideo");
    }};
	public static String getMimeTypeForFile(String uri) {
        int dot = uri.lastIndexOf('.');
        String mime = null;
        if (dot >= 0) {
            mime = MIME_TYPES.get(uri.substring(dot + 1).toLowerCase());
        }
        return mime == null ? MIME_DEFAULT_BINARY : mime;
    }
}
