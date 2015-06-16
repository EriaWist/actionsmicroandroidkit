package com.actionsmicro.bonjour;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;

import javax.jmdns.NetworkTopologyDiscovery;
import javax.jmdns.NetworkTopologyDiscovery.Factory.ClassDelegate;
import javax.jmdns.impl.NetworkTopologyDiscoveryImpl;

public class NetworkTopologyDiscoveryHelper {
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
	// just a dummy class to force execution of static block above.
	public static void init() {
		
	}
}
