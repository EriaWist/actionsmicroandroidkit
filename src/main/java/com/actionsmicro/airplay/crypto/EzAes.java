package com.actionsmicro.airplay.crypto;

public class EzAes {
	static {
        System.loadLibrary("fairplay-jni");
    }
	public native static int init(byte[] key, byte[] iv);
	public native static int decrypt(byte[] input, int length, byte[] out);
	public native static void pairVerify(byte[] edPubKey, byte[] edSecKey, byte[] ctlPubKey,byte[] ctlSigature,byte[] out);
	public native static void airplayPairVerify(byte[] edPubKey, byte[] edSecKey, byte[] ctlPubKey,byte[] ctlSigature,byte[] out);
	public native static int setup(double ver, byte[] eiv, byte[] ekey ,long conID, byte[] temp8,byte[] temp9, byte[] temp10);
	public native static byte[] airplayStreamKeyInit(byte a1, byte a2, byte a3, byte a4, byte a5, byte a6, byte a7, byte a8, byte a9, byte a10,
													 byte a11, byte a12, byte a13, byte a14, byte a15, byte a16, byte a17, byte a18, byte a19, byte a20,
													 byte a21, byte a22, byte a23, byte a24, byte a25, byte a26, byte a27, byte a28, byte a29, byte a30,
													 byte a31);
	public native static int airplayStreamKeyDecrypt(byte[] a1, byte[] a2, byte[] a3, byte[] a4, byte[] a5, byte[] a6);
	// sub xy module
	public native static int sub00000000x6(byte[] mypublic, byte[] secret, byte[] basepoint);
	// int sub_00000000x6(u8 *mypublic, u8 *secret, u8 *basepoint);

	public native static int sub00000000x7(byte[] pk, byte[] sk, byte[] seed);
	//	int sub_00000000x7(unsigned char *pk, unsigned char *sk,
//					   const unsigned char *seed)
	public native static int sub00000000x8(byte[] a1, byte[] a2, int len,byte[] a3,byte[] a4);
//				   unsigned char *a4)
}
