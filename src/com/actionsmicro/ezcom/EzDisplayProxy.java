package com.actionsmicro.ezcom;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import com.actionsmicro.ezcom.impl.AuthorizerImpl;
import com.actionsmicro.ezcom.impl.DisplayImpl;
import com.actionsmicro.ezcom.impl.ModeratorImpl;
import com.actionsmicro.ezcom.impl.RemoteControlImpl;

public class EzDisplayProxy extends BasicProxy implements RemoteControl, Authorizer, Display, Moderator {
	private RemoteControlImpl remoteControl;
	private AuthorizerImpl authorizer;
	private DisplayImpl display;
	private ModeratorImpl moderator;
	public EzDisplayProxy(String ipV4Address, int portNumber) {
		super(ipV4Address, portNumber);
		remoteControl = new RemoteControlImpl(this);
		authorizer = new AuthorizerImpl(this);
		display = new DisplayImpl(this);
		moderator = new ModeratorImpl(this);
	}

	@Override
	public void up() {
		remoteControl.up();
	}

	@Override
	public void down() {
		remoteControl.down();
	}

	@Override
	public void left() {
		remoteControl.left();
	}

	@Override
	public void right() {
		remoteControl.right();
	}

	@Override
	public void enter() {
		remoteControl.enter();
	}

	@Override
	public void escape() {
		remoteControl.escape();
	}

	@Override
	public void enterDisplayMode() {
		remoteControl.enterDisplayMode();
	}

	@Override
	public void sendKey(int code) {
		remoteControl.sendKey(code);
	}

	@Override
	public void requestToDisplay(int splitCount, int position) {
		authorizer.requestToDisplay(splitCount, position);
	}

	@Override
	public void cancelPendingRequest() {
		authorizer.cancelPendingRequest();
	}

	public AuthorizationListener getAuthorizationListener() {
		return authorizer.getAuthorizationListener();
	}

	public void setAuthorizationListener(AuthorizationListener listener) {
		authorizer.setAuthorizationListener(listener);
	}
	
	@Override
	public void startDisplaying() {
		display.startDisplaying();
	}

	@Override
	public void stopDisplaying() {
		display.stopDisplaying();
	}

	public DisplayListener getDisplayListener() {
		return display.getDisplayListener();
	}
	
	public void setDisplayListener(DisplayListener listener) {
		display.setDisplayListener(listener);
	}

	@Override
	public void replyToRequest(String userId, Reply reply) {
		moderator.replyToRequest(userId, reply);
	}

	public ModerationDelegate getModerationDelegate() {
		return moderator.getModerationDelegate();
	}

	public void setModerationDelegate(ModerationDelegate moderationDelegate) {
		moderator.setModerationDelegate(moderationDelegate);
	}
	
	private URL urlWithPath(String path) throws MalformedURLException {
		return new URL("http", getAddress(), getPortNumber(), path);
	}

	private InputStream openUrl(URL url) throws IOException {
		try {
			return url.openStream();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} finally {
			
		}
		return null;
	}
	public InputStream getProductImage() throws IOException {
		return openUrl(urlWithPath("object/product_image"));
	}
	public InputStream getStandbyPage() throws IOException {
		return openUrl(urlWithPath("object/standby_page"));
	}

}
