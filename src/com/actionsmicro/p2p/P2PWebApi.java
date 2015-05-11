package com.actionsmicro.p2p;

import com.actionsmicro.p2p.QueryUUIDPort;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
//import org.json.JSONException;
//import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

//2015-03-09 EricHwang for youtube shared url
public class P2PWebApi {

	//log tag
	private static final String TAG="P2PWebApi";

	//message to shared url
	public static final int MSG_ADD_SHARED_URL = 1;
	//message to show UI status dailog
	public static final int MSG_SHOW_STATUS_DIALOG = 2;
	//account device url
	private static final String mAccountDeviceURL="http://54.148.80.10:8080//Conn/AccountUUIDServlet";
	//host uuid port url
	private static final String mHostUUIDPortURL="http://54.148.80.10:8080//Conn/HostUUIDPortServlet";
	
	public static final String mCONN_SERVICE_DOMAIN="54.148.80.10";
	
	public static final int mSERVICE_PORT=8080;
	
	public static final String EZCASTSCREEN_HOSTUUID_KEY = "com.actionsmicro.ezcastscreen.hostuuid";
	
	public static final String EZCAST_ACCOUNT_KEY = "com.actionsmicro.ezcast.account";
	
	private static Context  mContext = null;
	
	private static P2PWebApi singleton = null;
	
	private Vector<String> mDeviceuuids= new Vector<String>();
	
	private P2PDeviceListener mlistener=null;
	
	public static P2PWebApi getInstance() {
		if (singleton == null) {
			singleton = new P2PWebApi();
		}
		return singleton;
	}
	
	public P2PWebApi()
	{
		
	}
	
	public static HostDevice parseP2PURI(String url)
	{
		Uri uri=Uri.parse(url);
		HostDevice device = new HostDevice();
		try
		{
			device.hostuuid=uri.getQueryParameter("hostuuid");
			device.hostport=uri.getPort();
		}
		catch (Exception e)
		{
			String strException=Log.getStackTraceString(e);
			String strLog="parseP2PURI exception="+ strException;
			Log.e(TAG, strLog);
		}
		android.util.Log.d(TAG,"parseP2PURI uri="+uri+" hostuuid="+device.hostuuid+" hostport="+String.valueOf(device.hostport));
		return device;
	}
	
	public static InetAddress getLocalAddr()
	{
		try
		{
			InetAddress addr = InetAddress.getByName("127.0.0.1");
			return addr;
		}
		catch (Exception e)
		{
			android.util.Log.e(TAG,"getLocalAddr exception"+e.getMessage());
		}
		return null;
	}
	
	public static void saveEzcastAccountToSharePreferences(String account, Context context) {
    	mContext = context;
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor = sharedPreferences.edit();
		editor.putString(EZCAST_ACCOUNT_KEY, account);
		editor.commit();
	}
	
	public static String getEzcastAccountFromSharePreferences() {
		String ret="";
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        ret = prefs.getString(EZCAST_ACCOUNT_KEY, ""); 
        return ret;
	}
	
    public static void saveEzScreenHostuuidToSharePreferences(String deviceuuid, Context context) {
    	mContext = context;
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor = sharedPreferences.edit();
		editor.putString(EZCASTSCREEN_HOSTUUID_KEY, deviceuuid);
		editor.commit();
	}
	
	public static String getEzScreenHostuuidFromSharePreferences(Context context) {
		String ret="";
		if (mContext == null)
			mContext = context;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        ret = prefs.getString(EZCASTSCREEN_HOSTUUID_KEY, ""); 
        return ret;
	}
	
	public static String getEzScreenCiientMjpguuidFromSharePreferences(Context context) {
		String ret="";
		if (mContext == null)
			mContext = context;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        ret = prefs.getString(EZCASTSCREEN_HOSTUUID_KEY, ""); 
        ret=ret+"_client_mjpg";
        return ret;
	}
	
	public static String getEzScreenCiientJsonrpcCallbackuuidFromSharePreferences(Context context) {
		String ret="";
		if (mContext == null)
			mContext = context;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        ret = prefs.getString(EZCASTSCREEN_HOSTUUID_KEY, ""); 
        ret=ret+"_client_jsonrpccallback";
        return ret;
	}
	
	public static String getEzScreenCiientContentuuidFromSharePreferences(Context context) {
		String ret="";
		if (mContext == null)
			mContext = context;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        ret = prefs.getString(EZCASTSCREEN_HOSTUUID_KEY, ""); 
        ret=ret+"_client_content";
        return ret;
	}
	
	private String getInsertAccountDeviceURL(
			String account,
			String deviceuuid)
	{
		String url = mAccountDeviceURL;
		if(!url.endsWith("?"))
		   url += "?";
		//append query string	      	
	    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
	    nameValuePairs.add(new BasicNameValuePair("account", account));
	    nameValuePairs.add(new BasicNameValuePair("deviceuuid", deviceuuid));
	    nameValuePairs.add(new BasicNameValuePair("type", "insert"));
	    String paramString = URLEncodedUtils.format(nameValuePairs, "utf-8");
        url += paramString;
        Log.d(TAG,"getInsertAccountDeviceURL="+url);
        return url;
	}
	
	private String getDeleteAccountDeviceURL(
			String account,
			String deviceuuid)
	{
		String url = mAccountDeviceURL;
		if(!url.endsWith("?"))
		   url += "?";
		//append query string	      	
	    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
	    nameValuePairs.add(new BasicNameValuePair("account", account));
	    nameValuePairs.add(new BasicNameValuePair("deviceuuid", deviceuuid));
	    nameValuePairs.add(new BasicNameValuePair("type", "delete"));
	    String paramString = URLEncodedUtils.format(nameValuePairs, "utf-8");
        url += paramString;
        Log.d(TAG,"getDeleteAccountDeviceURL="+url);
        return url;
	}
	
	private String getQueryAccountDeviceURL(
			String account)
	{
		String url = mAccountDeviceURL;
		if(!url.endsWith("?"))
		   url += "?";
		//append query string	      	
	    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
	    nameValuePairs.add(new BasicNameValuePair("account", account));
	    nameValuePairs.add(new BasicNameValuePair("type", "query"));
	    String paramString = URLEncodedUtils.format(nameValuePairs, "utf-8");
        url += paramString;
        Log.d(TAG,"getQueryAccountDeviceURL="+url);
        return url;
	}
	
	private String getUpdateHostUUIDPort(
		  	String hostuuid,
			String port,
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
	    nameValuePairs.add(new BasicNameValuePair("port", port));
	    nameValuePairs.add(new BasicNameValuePair("type", type));
	    String paramString = URLEncodedUtils.format(nameValuePairs, "utf-8");
        url += paramString;
        Log.d(TAG,"getUpdateHostUUIDPort="+url);
        return url;
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
	
	public boolean InsertAccountDeviceURL(
				String account,
				String deviceuuid)
	{
		
	   	HttpClient client = new DefaultHttpClient(); 
	   	try
	   	{
	   		String url=getInsertAccountDeviceURL(account,deviceuuid);
	    	HttpGet get = new HttpGet(url);
	    	HttpResponse response = client.execute(get);
	      	/*String status=*/
	    	StatusLine  sl=response.getStatusLine();
	 	   	int statuscode=sl.getStatusCode();
	 	    android.util.Log.d(TAG,url+" rsp="+String.valueOf(statuscode));
	 	   	if (statuscode==200)
	 	   		return true;
	 	   	
	    } catch (IllegalArgumentException  e) {
	 	   	Log.e(TAG,"InsertAccountDeviceURL IllegalArgumentException occurs, "+ e.getMessage());
	 	} catch (ClientProtocolException e) {
	 	  	Log.e(TAG,"InsertAccountDeviceURL ClientProtocolException occurs, "+ e.getMessage());
	 	} catch (IOException e) {
	 	   	Log.e(TAG,"InsertAccountDeviceURL IOException occurs, "+ e.getMessage());
	 	} catch (Exception e) {
	 	   	Log.e(TAG,"InsertAccountDeviceURL Exception occurs, "+ e.getMessage()+ Log.getStackTraceString(e));
	 	}
		return false;
	}	
	
	
	public void DeleteAccountDevice(
			String account,
			String deviceuuid)
	{
		final String accountTmp=account;
		final String deviceuuidTmp=deviceuuid;
		Thread thread =new Thread() {
			public void run() {
				 DeleteAccountDeviceAsync(accountTmp,deviceuuidTmp);
			}
		};
		thread.start();
	   	    		
	}
	
	public boolean DeleteAccountDeviceAsync(
			String account,
			String deviceuuid)
	{
	   	HttpClient client = new DefaultHttpClient(); 
	   	try
	   	{
	   		String url=getDeleteAccountDeviceURL(account,deviceuuid);
	    	HttpGet get = new HttpGet(url);
	    	HttpResponse response = client.execute(get);
	      	/*String status=*/
	    	StatusLine  sl=response.getStatusLine();
	 	   	int statuscode=sl.getStatusCode();
	 	    android.util.Log.d(TAG,url+" rsp="+String.valueOf(statuscode));
	 	   	if (statuscode==200)
	 	   		return true;

	    } catch (IllegalArgumentException  e) {
	 	   	Log.e(TAG,"DeleteAccountDeviceAsync IllegalArgumentException occurs, "+ e.getMessage());
	 	} catch (ClientProtocolException e) {
	 	  	Log.e(TAG,"DeleteAccountDeviceAsync ClientProtocolException occurs, "+ e.getMessage());
	 	} catch (IOException e) {
	 	   	Log.e(TAG,"DeleteAccountDeviceAsync IOException occurs, "+ e.getMessage());
	 	} catch (Exception e) {
	 	   	Log.e(TAG,"DeleteAccountDeviceAsync Exception occurs, "+ e.getMessage()+ Log.getStackTraceString(e));
	 	}
	  	return false;
	    		
	}
	
	//return uuid string vector
	public boolean QueryAccountDeviceURL(
			String account,
			Vector<String> vDeviceuuid)
	{
	   	HttpClient client = new DefaultHttpClient(); 
	   	vDeviceuuid.clear();
	   	try
	   	{
	   		String url=getQueryAccountDeviceURL(account);
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
	 	   	    String vectorStr = rsp.toString();
	 	   	    String delimiter = ","; 
	 	   	    StringTokenizer tokens = new StringTokenizer(vectorStr, delimiter); 
	 	   	    while(tokens.hasMoreTokens()) { 
	 	   	    	vDeviceuuid.add(tokens.nextToken()); 
	 	   	    } 
	 	   	    return true;
	 	   	}    
	    } catch (IllegalArgumentException  e) {
	 	   	Log.e(TAG,"QueryAccountDeviceURL IllegalArgumentException occurs, "+ e.getMessage());
	 	} catch (ClientProtocolException e) {
	 	  	Log.e(TAG,"QueryAccountDeviceURL ClientProtocolException occurs, "+ e.getMessage());
	 	} catch (IOException e) {
	 	   	Log.e(TAG,"QueryAccountDeviceURL IOException occurs, "+ e.getMessage());
	 	} catch (Exception e) {
	 	   	Log.e(TAG,"QueryAccountDeviceURL Exception occurs, "+ e.getMessage()+ Log.getStackTraceString(e));
	 	}
	  	return false;
	    		
	}
	
	public void search()
	{
		Thread thread = new Thread() {
		//	@Override
			public void run() {
				searchAsync();
			}
		};
		thread.start();
	}
	
	public void searchAsync()
	{
		removeDevices();
		String account = getEzcastAccountFromSharePreferences(); 
		boolean bok=QueryAccountDeviceURL(account, mDeviceuuids);
		if (bok) addDevices();
		
		
	}
	
	private void addDevices()
	{
		if (mlistener == null) return;
		for (int i=0;i<mDeviceuuids.size();i++)
		{
			String deviceuuid=(String)mDeviceuuids.get(i);
			mlistener.onDeviceAdded(deviceuuid);
		}
	}
	
	private void removeDevices()
	{
		if (mlistener == null) return;
		for (int i=0;i<mDeviceuuids.size();i++)
		{
			String deviceuuid=(String)mDeviceuuids.get(i);
			mlistener.onDeviceRemoved(deviceuuid);
		}
	}
	
	public Vector<String> getDeviceUUIDs()
	{
		return mDeviceuuids;
	}
	
	public void addListener(P2PDeviceListener listener) {
		mlistener=listener;
	}
	
	public boolean UpdateHostUUIDPort(
			String hostuuid,
			String port,
		    String type)
	{
	   	HttpClient client = new DefaultHttpClient(); 
	   	try
	   	{
	   		String url=getUpdateHostUUIDPort(hostuuid, port, type);
	    	HttpGet get = new HttpGet(url);
	    	HttpResponse response = client.execute(get);
	      	/*String status=*/
	    	StatusLine  sl=response.getStatusLine();
	 	   	int statuscode=sl.getStatusCode();
	 	    android.util.Log.d(TAG,url+" rsp="+String.valueOf(statuscode));
	 	   	if (statuscode==200)
	 	   		return true;

	    } catch (IllegalArgumentException  e) {
	 	   	Log.e(TAG,"UpdateHostUUIDPort IllegalArgumentException occurs, "+ e.getMessage());
	 	} catch (ClientProtocolException e) {
	 	  	Log.e(TAG,"UpdateHostUUIDPort ClientProtocolException occurs, "+ e.getMessage());
	 	} catch (IOException e) {
	 	   	Log.e(TAG,"UpdateHostUUIDPort IOException occurs, "+ e.getMessage());
	 	} catch (Exception e) {
	 	   	Log.e(TAG,"UpdateHostUUIDPort Exception occurs, "+ e.getMessage()+ Log.getStackTraceString(e));
	 	}
	  	return false;
	}
	
	//return port
	public String QueryHostUUIDPort(
			String hostuuid,
		    String type)
	{
		QueryUUIDPort q= new QueryUUIDPort();
		return q.QueryHostUUIDPort(hostuuid,type);
	   	
	}
}


