package com.actionsmicro.androidrx;

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;

public class Bonjour {
	private static JmDNS sJmDns = null;
	public static JmDNS getInstance(InetAddress addr) throws IOException {
		if (sJmDns == null) {
			sJmDns = JmDNS.create(addr, "AndroidRx-Bonjour");
		}
		return sJmDns;
	}
}
