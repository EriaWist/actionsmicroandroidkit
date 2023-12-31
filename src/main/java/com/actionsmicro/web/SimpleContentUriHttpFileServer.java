package com.actionsmicro.web;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import com.actionsmicro.utils.Device;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.Reachability;
import com.actionsmicro.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class SimpleContentUriHttpFileServer extends NanoHTTPD {

	private static final String TAG = "SimpleContentUriHttpFileServer";
	private String mHostName;
	private boolean mEnableChunk = false;
	private long contentLength = -1;
	private Context context;
	private Uri contentUri;
	public SimpleContentUriHttpFileServer(Context context, Uri contentUri, int portNumber) {
		super(Reachability.isWifiApEnabled(context) ? Device.getWifiApIpAddress() : null, portNumber);
		this.context = context;
		this.contentUri = contentUri;
	}

	public SimpleContentUriHttpFileServer(Context context, Uri contentUri,String hostName ,int portNumber) {
		super(Reachability.isWifiApEnabled(context) ? Device.getWifiApIpAddress() : null, portNumber);
		this.mHostName = hostName;
		this.context = context;
		this.contentUri = contentUri;
	}

	public SimpleContentUriHttpFileServer(Context context, Uri contentUri, int portNumber,boolean enableChunk) {
		super(Reachability.isWifiApEnabled(context) ? Device.getWifiApIpAddress() : null, portNumber);
		this.context = context;
		this.contentUri = contentUri;
		mEnableChunk = enableChunk;
	}

	@Override
    public void stop() {
		super.stop();
	}
	public String getServerUrl() {
		try {
			String hostName = getHostName();
			return new URL("http", hostName, getListeningPort(), "").toString();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private String getHostName() {
		if (mHostName != null) {
			return mHostName;
		}
		String hostName = Device.getHostIpAddress(context, true);
		return hostName;
	}

	@Override
    public Response serve(IHTTPSession session) {
		Map<String, String> header = session.getHeaders();
		Log.d(TAG, "serve: "+session.getMethod()+" range:"+header.get("range"));
		if(session.getUri().equals("/LocalVideo")){
			String file = session.getParms().get("filename");
			Uri mediaUri = null;
			try {
				mediaUri = Uri.parse(file);
				if (mediaUri.getScheme() == null) {
					mediaUri = mediaUri.buildUpon().scheme("file").build();
				}
			} catch (Exception e) {
				e.printStackTrace();
				mediaUri = Uri.fromFile(new File(file));
			}

			if (!contentUri.toString().equals(mediaUri.toString())) {
				contentUri = mediaUri;
				contentLength = -1;
			}
		}
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
					    contentLength = Double.valueOf(cursor.getString(sizeIndex)).longValue();
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


					res = createResponse(Response.Status.PARTIAL_CONTENT, mime, in, fileLen);
                    res.addHeader("Content-Length", "" + getContentLengthForByteRangeResponse(fileLen, dataLen));
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
//                    res.addHeader("ETag", etag);
                }
            } else {
				InputStream in = getInputStream(0, fileLen);
				res = createResponse(Response.Status.OK, mime, in, fileLen);
				res.addHeader("Content-Length", "" + fileLen);
            }
        } catch (IOException ioe) {
            res = createResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
        }

        return res;
    }

	protected long getContentLengthForByteRangeResponse(final long fileLen, final long dataLen) {
		return dataLen;
	}
	private InputStream getInputStream(long startFrom, final long dataLen) throws IOException {
		InputStream in = null;
		if (contentUri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
			in = context.getContentResolver().openInputStream(contentUri);
		}  else if (contentUri.getScheme().equalsIgnoreCase("file"))  {
			in = new FileInputStream(new File(contentUri.getPath()));
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
        Response res = newFixedLengthResponse(status,mimeType,message);;
		if (mEnableChunk) {
			res.setChunkedTransfer(true);
		}
		res.addHeader("Accept-Ranges", "bytes");
        return res;
    }
	private Response createResponse(Response.Status status, String mimeType, InputStream message, long contentLength) {
		Response res = mEnableChunk ? newChunkedResponse(status, mimeType, message) : newFixedLengthResponse(status, mimeType, message, contentLength);
		res.addHeader("Accept-Ranges", "bytes");
        return res;
    }
	public static final String MIME_DEFAULT_BINARY = "application/octet-stream";
    
	public static String getMimeTypeForFile(String uri) {
        String ext = Utils.getFileExtension(uri);
        String mime = MIME_DEFAULT_BINARY;
        if (ext != null) {
        	mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.getDefault()));

        	if (mime == null) {
        		mime = MIME_DEFAULT_BINARY;
        	}
			if (mime.equalsIgnoreCase("application/ogg")) {
				mime = "audio/ogg";
			}
			if (ext.equalsIgnoreCase("rm") || ext.equalsIgnoreCase("rmvb")) {
				mime = "video/rm";
			}
			// workaround for dongle for compatibility issue
			if (mime.equalsIgnoreCase("video/x-ms-asf") || mime.equalsIgnoreCase("video/mp2ts")) {
				mime = MIME_DEFAULT_BINARY;
			}

		}
        return mime;
    }
}
