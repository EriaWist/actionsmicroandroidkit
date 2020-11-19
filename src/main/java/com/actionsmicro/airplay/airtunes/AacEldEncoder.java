package com.actionsmicro.airplay.airtunes;

public class AacEldEncoder {
    static {
        System.loadLibrary("fairplay-jni");
    }

    private long nativeEncoder;

    private native static long init(int bitrate, int samplerate);
    private native static int encode(long nativeEncoder, byte[] data, int offset, int length, byte[] out);
    private native static int release(long nativeEncoder);
    public AacEldEncoder(int bitrate, int samplerate) throws Exception {
        nativeEncoder = AacEldEncoder.init(bitrate, samplerate);
        if (nativeEncoder == 0) {
            throw new Exception("AacEldEncoder initialization failed");
        }
    }

    public int encode(byte[] data, int offset, int length, byte[] out) {
        if (nativeEncoder != 0) {
            return AacEldEncoder.encode(nativeEncoder, data, offset, length, out);
        } else {
            throw new IllegalStateException("nativeEncoder does not exist");
        }
    }

    public int release() {
        if (nativeEncoder != 0) {
            long tmpNativeEncoder = nativeEncoder;
            nativeEncoder = 0;
            return AacEldEncoder.release(tmpNativeEncoder);
        } else {
            throw new IllegalStateException("nativeEncoder does not exist");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (nativeEncoder != 0) {
            release();
        }
        super.finalize();
    }
}
