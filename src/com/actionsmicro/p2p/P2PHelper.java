package com.actionsmicro.p2p;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.actionsmicro.p2p.ConnInvokerJNI;

public class P2PHelper{

	public Context mContext;
	
    public static ConnInvokerJNI invoker = null;
    
    private static final String TAG_HOST="P2PHelperHost";
    private static final String TAG_CLIENT="P2PHelperClient";
    
    public P2PHelper(Context context) {
    	mContext=context;
		invoker = new ConnInvokerJNI();
		 //set Package name
        //call init API first for jni native c code to create the folder /data/data/[Package Name]/p2p
        //to store temp and configure setting files
		String strPackageName = mContext.getPackageName(); 
	    invoker.init(strPackageName);
	}
	
    public void starConntHost(
    		String strHostUUID, 
    		String strConnServiceDomainName,
    		int nConnServicePort,
    		int nTimeoutMS)
    {
    	int nRet = invoker.startConnHost(strHostUUID, strConnServiceDomainName, nConnServicePort, nTimeoutMS);
		String res;
		if (nRet == 0) {
			res = "startConnHost success";
		} else {
			res = "startConnHost failed nRet = " + Integer.toString(nRet);
		}
		android.util.Log.e(TAG_HOST,res);
    }
    
    //stop btn click event     
    public void stopConntHost(String strHostUUID) 
    {
     	int nRet = invoker.stopConnHost(strHostUUID);
     	String res;
     	if (nRet == 0) {
     		res = "stopConnHost success hostUUID="+strHostUUID;
     	} else {
     		res = "stopConnHost failed hostUUID="+strHostUUID+" nRet = " + Integer.toString(nRet);
     	}
     	android.util.Log.e(TAG_HOST,res);
     }
     

     public void stopAllConnHost()
     {
    	 invoker.stopAllConnHost();
     	 String res="stopAllConnHost";
     	 android.util.Log.e(TAG_HOST,res);
     }
     
     public int startConnClient(
           	int nTcpListenedPort,
         	String strHostUUID,
         	int nHostPort,
         	String strConnServiceDomainName,
         	int nConnServicePort,
         	int nTimeoutMS)
     {
    	 //return the client listened port
    	 int nRet = invoker.startConnClient(nTcpListenedPort, 
    			 					   strHostUUID, 
         							   nHostPort, 
         							   strConnServiceDomainName, 
         							   nConnServicePort,
         							   nTimeoutMS);
         String res;
         if (nRet == 0) {
        	 res = "startConnClient success";
         } else {
         	 res = "startConnClient nRet = " + Integer.toString(nRet);
         }
         android.util.Log.e(TAG_CLIENT,res);
         
         return nRet;
     }
               
     //stop btn click event     
     public void stopConnClient(
          	int nTcpListenedPort,
     		String strHostUUID,
     		int nHostPort)
     {
    	 int nRet = invoker.stopConnClient(nTcpListenedPort, strHostUUID, nHostPort);
    	 String res;
    	 if (nRet == 0) {
    		 res = "stopConnClient success";
    	 } else {
    		 res = "stopConnClient nRet = " + Integer.toString(nRet);
    	 }
    	 android.util.Log.e(TAG_CLIENT,res);
     }
     
     public void stopAllConnClient()
     {
    	 invoker.StopAllConnClient(); 
    	 String res="stopAllConnClient";
    	 android.util.Log.e(TAG_HOST,res);
     }
     

}
