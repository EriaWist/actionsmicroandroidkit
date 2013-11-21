package com.actionsmicro.ezcom;

public interface Authorizer {
	public final static int SPLIT_COUNT_AUTO = 0;
	public final static int POSITION_AUTO = 0;
	public interface AuthorizationListener {
		void authorizationIsGranted(Authorizer authorizer, int splitCount, int position);
		public enum DeniedReason {
			UNDEFINED, FULLY_OCCUPIED, DENIED_BY_HOST_AUTOMATICALLY, DENIED_BY_HOST_MANUALLY
		}
		void authorizationIsDenied(Authorizer authorizer, DeniedReason reason);
	}
	void requestToDisplay(int splitCount, int position);
	void cancelPendingRequest();
}
