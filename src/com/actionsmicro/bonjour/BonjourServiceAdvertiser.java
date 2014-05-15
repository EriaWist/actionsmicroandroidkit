package com.actionsmicro.bonjour;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;

import javax.jmdns.JmmDNS;
import javax.jmdns.NetworkTopologyDiscovery;
import javax.jmdns.NetworkTopologyDiscovery.Factory.ClassDelegate;
import javax.jmdns.NetworkTopologyEvent;
import javax.jmdns.NetworkTopologyListener;
import javax.jmdns.ServiceInfo;
import javax.jmdns.impl.NetworkTopologyDiscoveryImpl;

import com.actionsmicro.utils.Log;

public class BonjourServiceAdvertiser {
	private static final String TAG = "BonjourServiceAdvertiser";
	private JmmDNS jmDNS;
	private ServiceInfo serviceInfo;
	static {
		NetworkTopologyDiscovery.Factory.setClassDelegate(new ClassDelegate() {

			@Override
			public NetworkTopologyDiscovery newNetworkTopologyDiscovery() {
				return new NetworkTopologyDiscoveryImpl() {
					@Override
					public boolean useInetAddress(NetworkInterface networkInterface, InetAddress interfaceAddress) {
						if (interfaceAddress instanceof Inet6Address) { // support no IPv6 address since it has issue.
							return false;
						}
						if (networkInterface.getName().startsWith("usb")) { // rule out some redundant interfaces for performance
							return false;
						}
						return super.useInetAddress(networkInterface, interfaceAddress);
					}

				};
			}
			
		});
	}
	public BonjourServiceAdvertiser(ServiceInfo serviceInfo) {
		jmDNS = JmmDNS.Factory.newJmmDNS();
		try {
			Log.d(TAG, "jmDNS interface count:"+jmDNS.getInterfaces().length);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.serviceInfo = serviceInfo;
		jmDNS.addNetworkTopologyListener(networkListener);
	}

	public void register() throws IOException {
		jmDNS.addNetworkTopologyListener(networkListener);		
		registerService();
	}
	public void unregister() {
		if (jmDNS != null) {
			if (networkListener != null) {
				jmDNS.removeNetworkTopologyListener(networkListener);
			}
			if (serviceInfo != null) {
				jmDNS.unregisterService(serviceInfo);
				Log.d(TAG, "Unregister Service:"+serviceInfo.getQualifiedName());
			}
		}
	}
	public void close() {
		if (jmDNS != null) {
			try {
				jmDNS.close();
			} catch (IOException e) {
				Log.e(TAG, "jmDNS close failed", e);
			}
			jmDNS = null;
		}
	}
	private void registerService() {
		Log.d(TAG, "Start Registered Service as " + serviceInfo.getQualifiedName());
		try {
			jmDNS.registerService(serviceInfo);
		} catch (IOException e) {
			//TODO add callback
			Log.e(TAG, "Failed to register server:"+serviceInfo, e);
		}
		Log.d(TAG, "Registered Service as " + serviceInfo.getQualifiedName());				
	}
	private NetworkTopologyListener networkListener = new NetworkTopologyListener() {

		@Override
		public void inetAddressAdded(NetworkTopologyEvent event) {
			Log.d(TAG, "inetAddressAdded :"+event.getInetAddress());
			registerService();
		}

		@Override
		public void inetAddressRemoved(NetworkTopologyEvent event) {
			Log.d(TAG, "inetAddressRemoved :"+event.getInetAddress());			
		}

	};
}
