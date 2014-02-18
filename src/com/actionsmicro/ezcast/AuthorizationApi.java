package com.actionsmicro.ezcast;

public interface AuthorizationApi extends Api {
	public final static int SPLIT_COUNT_AUTO = 0;
	public final static int POSITION_AUTO = 0;
	public interface AuthorizationListener {
		void authorizationIsGranted(AuthorizationApi authorizer, int splitCount, int position);
		public enum DeniedReason {
			UNDEFINED, FULLY_OCCUPIED, DENIED_BY_HOST_AUTOMATICALLY, DENIED_BY_HOST_MANUALLY
		}
		void authorizationIsDenied(AuthorizationApi authorizer, DeniedReason reason);
	}
	void requestToDisplay(int splitCount, int position);
	void cancelPendingRequest();
}
