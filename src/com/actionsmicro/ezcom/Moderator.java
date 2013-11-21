package com.actionsmicro.ezcom;

public interface Moderator {
	public enum Reply {
		ALLOW, DENY, WAIT
	}
	void replyToRequest(String userId, Reply reply);
	public interface ModerationDelegate {
		Reply userRequestToDisplay(Moderator moderator, String userId, String userName);
		void userCancelPendingRequest(Moderator moderator, String userId);
	}
}
