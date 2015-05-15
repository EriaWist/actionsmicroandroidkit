package com.actionsmicro.androidrx;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;

public class Bonjour {
	private static JmDNS sJmDns = null;
	private static InetAddress sAddr;
	public static JmDNS getInstance(InetAddress addr) throws IOException {
		if (sJmDns == null) {
			renewJmDNS(addr);
		} else {
			if (!sAddr.equals(addr)) {
				sJmDns.close();
				renewJmDNS(addr);
			}
		}
		return sJmDns;
	}
	private static void renewJmDNS(InetAddress addr) throws IOException {
		sJmDns = JmDNS.create(addr, "AndroidRx-Bonjour");
		sAddr = addr;
	}
}
