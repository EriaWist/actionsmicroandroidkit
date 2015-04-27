package com.actionsmicro.androidkit.ezcast;

/**
 * ApiBuilder to build AudioApi.
 * 
 * @author Fred Chen
 * @version {SDK_VERSION_STRING}
 * @since 2.x
 */
public class AudioApiBuilder extends ApiBuilder<AudioApi> {
    /**
     * Create AudioApiBuilder.
     * 
     * @param sdk
     *            The EzCaskSdk instance.
     * @param device
     *            The device this API will be bound to.
     * @since 2.x
     */
    public AudioApiBuilder(EzCastSdk sdk, DeviceInfo device) {
        super(sdk, device);
    }

    /**
     * Create the AudioApi object.
     * 
     * @since 2.x
     */
    @Override
    public AudioApi build() {
        return device.createAudioApi(this);
    }

}
