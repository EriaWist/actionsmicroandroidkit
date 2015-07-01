package com.actionsmicro.androidkit.ezcast;

import java.io.InputStream;

import android.graphics.YuvImage;

/**
 * API to display image to the device.
 * <p>
 * To create DisplayApi you should use {@link DisplayApiBuilder}.
 * @author James Chen
 * @version {SDK_VERSION_STRING}
 * @since 2.0
 */
public interface DisplayApi extends Api {
	/**
	 * Roles in multi-session mode.
	 * @author James Chen
	 * @since 2.0
	 */
	public enum Role {
		/**
		 * Undefined role.
		 * @since 2.0
		 */
		UNDEFINED, 
		/**
		 * Host in a multi-session mode.
		 * @since 2.0
		 */
		HOST,
		/**
		 * Guest in a multi-session mode.
		 * @since 2.0
		 */
		GUEST
	}
	/**
	 * Callback to handle display related events from the device.
	 * @author James Chen
	 *
	 * @since 2.0
	 */
	public interface DisplayListener {
		/**
		 * The device requests current stream to start display.
		 * <p>
		 * App can start to send contents to the device via {@link #sendJpegEncodedScreenData} or {@link #sendYuvScreenData}
		 * @param display The Display API object.
		 * @param splitCount Current split count.
		 * @param position Current position of the stream.
		 * @since 2.0
		 */
		void remoteRequestToStartDisplaying(DisplayApi display, int splitCount, int position);
		/**
		 * The device requests current stream to stop display.
		 * <p>
		 * App should not send any contents after receive this callback.
		 * @param display The Display API object
		 * @since 2.0
		 */
		void remoteRequestToStopDisplaying(DisplayApi display);
		/**
		 * The device requests current stream to disconnect from it.
		 * @param display The Display API object.
		 * @since 2.0
		 */
		void remoteRequestToDisconnect(DisplayApi display);
		/**
		 * The device informs current stream that it's position or split count has been changed.
		 * @param display The Display API object.
		 * @param splitCount Current split count.
		 * @param position Current position of the stream.
		 * @since 2.0
		 */
		void positionDidChange(DisplayApi display, int splitCount, int position);
		/**
		 * The device informs current stream that role of the stream has been changed. 
		 * @param display The Display API object.
		 * @param newRole New role.
		 * @since 2.0
		 */
		void roleDidChange(DisplayApi display, Role newRole);
	}
	/**
	 * Before sending contents, app should call startDisplaying.
	 * @since 2.0
	 */
	void startDisplaying(); //TODO consider to eliminate this API
	/**
	 * Calls to stopDisplaying should be paired with startDisplaying.
	 * @since 2.0
	 */
	void stopDisplaying(); //TODO consider to eliminate this API
	/**
	 * A convenient method to send last contents again.
	 * @throws Exception
	 * @since 2.0
	 */
	public void resendLastImage() throws Exception;
	/**
	 * Display JPEG data on the device.
	 * @param input InputStream which wraps the JPEG data.
	 * @param length Length of the JPEG data.
	 * @throws Exception
	 * @since 2.0
	 */
	public void sendJpegEncodedScreenData(InputStream input, long length) throws Exception;
	/**
	 * Display YUV data on the device.
	 * @param yuvImage The YUV data.
	 * @param quailty The quality of the compression, from 0 to 100. 0 meaning compress for small size, 100 meaning compress for max quality. The YUV data will be compressed as JPEG to be displayed on the device.
	 * @throws Exception
	 * @since 2.0
	 */
	public void sendYuvScreenData(YuvImage yuvImage, int quailty) throws Exception;

	/**
	 * Display H264 data on the device.
	 * @param contents H264 raw data.
	 * @param width  Width of the H264 data
	 * @param height Height of the H264 data.
	 * @throws Exception
	 * @since 2.4
	 */
	public void sendH264EncodedScreenData(byte[] contents, int width, int height) throws Exception;
}
