package com.actionsmicro.p2p;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import android.util.Log;

//2015-03-09 EricHwang for youtube shared url
public class QueryUUIDPort {

	//log tag
	private static final String TAG="QueryUUIDPort";

	//host uuid port url
	private static final String mHostUUIDPortURL="http://54.148.80.10:8080//Conn/HostUUIDPortServlet";
	
	public static final String mCONN_SERVICE_DOMAIN="54.148.80.10";
	
	public static final int mSERVICE_PORT=8080;
	
	private static P2PWebApi singleton = null;
	
	private boolean bFinish=false;
	
	private String portStr;
	
	public QueryUUIDPort()
	{
		portStr = "";
	}
	
	private String getQueryHostUUIDPort(
		  	String hostuuid,
		    String type)
    {
		//type="jsonrpc" -> table "JsonrpcUUIDPort"
		//type="content" -> table "ContentUUIDPort"
		//type="mjpg"   -> table "MjpgUUIDPort"
		String url = mHostUUIDPortURL;
		if(!url.endsWith("?"))
		   url += "?";
		//append query string	      	
	    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
	    nameValuePairs.add(new BasicNameValuePair("hostuuid", hostuuid));
	    nameValuePairs.add(new BasicNameValuePair("type", type));
	    nameValuePairs.add(new BasicNameValuePair("query", "true"));
	    String paramString = URLEncodedUtils.format(nameValuePairs, "utf-8");
        url += paramString;
        Log.d(TAG,"getQueryHostUUIDPort="+url);
        return url;
    }
	
	//return port
	private String QueryHostUUIDPortAsync(
			String hostuuid,
		    String type)
	{
	   	HttpClient client = new DefaultHttpClient(); 
	   	try
	   	{
	   		String url=getQueryHostUUIDPort(hostuuid, type);
	    	HttpGet get = new HttpGet(url);
	    	HttpResponse response = client.execute(get);
	      	/*String status=*/
	    	StatusLine  sl=response.getStatusLine();
	 	   	int statuscode=sl.getStatusCode();
	 	    android.util.Log.d(TAG,url+" rsp="+String.valueOf(statuscode));
	 	   	if (statuscode==200)
	 	   	{
	 	   		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
	 	   		String line = "";
	 	   		StringBuilder rsp= new StringBuilder();
	 	   		while ((line = rd.readLine()) != null) {
	 	   			rsp.append(line);
	 	   	    }
	 	   		Log.d(TAG,"url="+url+" rsp="+rsp.toString());
	 	   		int nPort=Integer.parseInt(rsp.toString());
	 	   		Log.d(TAG,"url="+url+" port="+ String.valueOf(nPort));
	 	   		return String.valueOf(nPort);
	 	   		
	 	   	}	
	    } catch (IllegalArgumentException  e) {
	 	   	Log.e(TAG,"QueryHostUUIDPort IllegalArgumentException occurs, "+ e.getMessage());
	 	} catch (ClientProtocolException e) {
	 	  	Log.e(TAG,"QueryHostUUIDPort ClientProtocolException occurs, "+ e.getMessage());
	 	} catch (IOException e) {
	 	   	Log.e(TAG,"QueryHostUUIDPort IOException occurs, "+ e.getMessage());
	 	} catch (Exception e) {
	 	   	Log.e(TAG,"QueryHostUUIDPort Exception occurs, "+ e.getMessage()+ Log.getStackTraceString(e));
	 	}
	  	return "";
	}
	
	public String QueryHostUUIDPort(
			String hostuuid,
		    String type)
	{
		final String hostuuidTmp=hostuuid;
		final String typeTmp=type;
		bFinish=false;
		portStr="";
		Thread t=new Thread() {
			public void run() {
				portStr = QueryHostUUIDPortAsync(hostuuidTmp, typeTmp);
				bFinish=true;
			}
		};
		t.start();
		while (!bFinish) {
			try	{
				Thread.sleep(1);
			}
			catch (Exception e) 
			{
				Log.e(TAG,"QueryHostUUIDPort Thread.sleep(1) exception "+e.getMessage());
			}
			if (bFinish) break;
		}
		return portStr;
	}
}


