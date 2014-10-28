package com.actionsmicro.analytics.unittest.mock;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jmdns.ServiceInfo;

public class MockServiceInfo extends ServiceInfo {
	private Map<String, String> propertyStrings = new HashMap<String, String>();
	public MockServiceInfo(Map<String, String> propertyStrings) {
		this.propertyStrings = propertyStrings;
	}
	@Override
	public boolean hasData() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTypeWithSubtype() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getQualifiedName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Deprecated
	public String getHostAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getHostAddresses() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Deprecated
	public InetAddress getAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Deprecated
	public InetAddress getInetAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Deprecated
	public Inet4Address getInet4Address() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Deprecated
	public Inet6Address getInet6Address() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InetAddress[] getInetAddresses() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Inet4Address[] getInet4Addresses() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Inet6Address[] getInet6Addresses() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getPort() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getPriority() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getWeight() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte[] getTextBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Deprecated
	public String getTextString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Deprecated
	public String getURL() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getURLs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Deprecated
	public String getURL(String protocol) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getURLs(String protocol) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] getPropertyBytes(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPropertyString(String name) {
		return propertyStrings.get(name);
	}

	@Override
	public Enumeration<String> getPropertyNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNiceTextString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setText(byte[] text) throws IllegalStateException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setText(Map<String, ?> props) throws IllegalStateException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isPersistent() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getDomain() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProtocol() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getApplication() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSubtype() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Fields, String> getQualifiedNameMap() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
