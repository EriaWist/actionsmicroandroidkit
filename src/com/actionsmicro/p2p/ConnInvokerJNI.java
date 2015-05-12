package com.actionsmicro.p2p;
import java.lang.String;

public class ConnInvokerJNI {
  
	static {
  		System.loadLibrary("conn");
	}

	public native int init( String strPackageName );
	
	/** start conn client */
	public native int startConnClient(
		int nTcpListenedPort,   //in
		String strHostUUID,                //in
		int nHostPort,          //in
		String strConnServiceDomainName,                 //in  
		int nConnServicePort,         //in
		int nTimeoutMS);                   //in 
		//CConnInstanceParam &result);     //out 
	
	/** stop specific conn client */
	public native int stopConnClient(
		int nTcpListenedPort,
		String strHostUUID,
		int nHostPort);

	public native int isConnClientStop(
		int nTcpListenedPort,
		String strHostUUID,
		int nHostPort);
	
	/** stop all conn clients*/
	public native void StopAllConnClient();

	/** start conn host */	
	public native int startConnHost(
		String strHostUUID,                //in
		String strConnServiceDomainName,   //in  
		int nConnServicePort,              //in
		int nTimeoutMS);                   //in 
		//CConnInstanceParam &result);     //out 
	
	/** stop specific conn host */
	public native int stopConnHost(String strHostUUID);
	
	public native int isConnHostStop(String strHostUUID);
	
	/** stop all conn clients*/
	public native void stopAllConnHost();

	


}

