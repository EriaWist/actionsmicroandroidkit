package com.actionsmicro.androidkit.ezcast;

/**
 * EZCast authorization API.
 * <p>
 * To create authorization API object, please refer to {@link AuthorizationApiBuilder}
 * <p>
 * You should ask the device for permission before you start to display contents to the device via {@link DisplayApi}.
 * <p>
 * Call {@link AuthorizationApi#requestToDisplay} to ask for permission to display
 * and implement {@link AuthorizationListener} to handle authorization result.
 * @see AuthorizationApiBuilder
 * @author James Chen
 * @version {SDK_VERSION_STRING}
 * @since 2.0
 */
public interface AuthorizationApi extends Api {
	/**
	 * Request the device to split display automatically. 
	 * @see #requestToDisplay(int, int)
	 * @since 2.0
	 */
	public final static int SPLIT_COUNT_AUTO = 0;
	/**
	 * Request the device to position current stream automatically. 
	 * @see #requestToDisplay(int, int)
	 * @since 2.0
	 */
	public final static int POSITION_AUTO = 0;
	/**
	 * Authorization callback handler. 
	 * @author jamchen
	 * @version {SDK_VERSION_STRING}
	 * @since 2.0
	 */
	public interface AuthorizationListener {
		/**
		 * Called when authorization is granted.
		 * @param authorizer The authorization API object.
		 * @param splitCount The split count of the device.
		 * @param position The position current stream is placed.
		 * @see #requestToDisplay(int, int)
		 * @since 2.0
		 */
		void authorizationIsGranted(AuthorizationApi authorizer, int splitCount, int position);
		/**
		 * The reason why authorization request was denied.
		 * @author jamchen
		 * @see #requestToDisplay(int, int)
		 * @since 2.0
		 */
		public enum DeniedReason {
			/**
			 * Undefined value.
			 * @since 2.0
			 */
			UNDEFINED,
			/**
			 * The device is fully occupied.
			 * @since 2.0
			 */
			FULLY_OCCUPIED,
			/**
			 * The authorization request is denied by host automatically.
			 * @since 2.0
			 */
			DENIED_BY_HOST_AUTOMATICALLY,
			/**
			 * The authorization request is denied by host manually.
			 * @since 2.0
			 */
			DENIED_BY_HOST_MANUALLY
		}
		/**
		 * Called when authorization is denied.
		 * @param authorizer The authorization API object.
		 * @param reason The reason why the authorization request was denied.
		 * @since 2.0
		 */
		void authorizationIsDenied(AuthorizationApi authorizer, DeniedReason reason);
	}
	/**
	 * Ask the device for permission to display stream.
	 * You should call this before you start to display contents to the device via {@link DisplayApi}.
	 * @param splitCount Request the device to split on demand. You can pass {@link #SPLIT_COUNT_AUTO} to let the device to choose best split count for current connection status.
	 * @param position Request the device to display current stream as specified position. You can pass {@link #POSITION_AUTO} to let the device to choose best position for current stream.
	 * @since 2.0
	 */
	void requestToDisplay(int splitCount, int position);
	/**
	 * Cancel previous authorization request which is not handled yet.
	 * @since 2.0
	 */
	void cancelPendingRequest();
}
