#include <jni.h>
#include <android/log.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

//#define EZAESDEBUG

#ifdef EZAESDEBUG
	#define  LOG_TAG    "ezaes"
	#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
	#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
	#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
	#define  LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__)
#else
	#define  LOGI(...)
	#define  LOGD(...)
	#define  LOGE(...)
	#define  LOGV(...)
#endif

#define AES_MAXNR 14

//This should be a hidden type, but EVP requires that the size be known
struct aes_key_s {
#ifdef AES_LONG
unsigned long rd_key[4 * (AES_MAXNR + 1)];
#else
    unsigned int rd_key[4 * (AES_MAXNR + 1)];
#endif
    int rounds;
};
typedef struct aes_key_s /*AES_KEY*/aes_key_t;

typedef struct APP_AIRTUNES_S APP_AIRTUNES_T;
struct APP_AIRTUNES_S
{
	struct {
		uint8_t publicKey[ 32 ];
		uint8_t secretKey[ 32 ];
		uint8_t sharedKey[ 32 ];
		uint8_t controllerPublicKey[ 32 ];
		uint8_t controllerSignature[ 32 ];

		uint8_t edPubKey[ 32 ];
		uint8_t edSecret[ 32 ];

		uint8_t enKey[ 4 ];

		uint8_t aesKey[ 16 ];
		uint8_t aesIV[ 16 ];

		int b_mirror;
	} pair_setup;

};


void* GetSession() {
	static APP_AIRTUNES_T *app = NULL;
	if (app == NULL) {
		app = (APP_AIRTUNES_T *)malloc( sizeof(APP_AIRTUNES_T) );
		memset(app, 0, sizeof(APP_AIRTUNES_T));
	}
	return app;
}

JNIEXPORT jint JNICALL
Java_com_actionsmicro_airplay_crypto_EzAes_init(JNIEnv* env, jclass clazz, jbyteArray key, jbyteArray iv) {
	LOGD(">>>>> Java_com_actionsmicro_airplay_crypto_EzAes_init");
	int i = 0;
	jbyte* keyPtr = (*env)->GetByteArrayElements(env, key, NULL);
	jbyte* ivPtr = (*env)->GetByteArrayElements(env, iv, NULL);
	jint result = ezAesDecrytInit(keyPtr, ivPtr);
//	(*env)->ReleaseByteArrayElements(env, key, keyPtr, 0);
//	(*env)->ReleaseByteArrayElements(env, iv, ivPtr, 0);

	for(i=0;i<16;i++)
	{
		LOGD("keyPtr [%d] = %d",i,keyPtr[i]);
	}
	for(i=0;i<16;i++)
	{
		LOGD("ivPtr [%d] = %d",i,ivPtr[i]);
	}
	LOGD("<<<< Java_com_actionsmicro_airplay_crypto_EzAes_init");

	return result;
}

JNIEXPORT jint JNICALL
Java_com_actionsmicro_airplay_crypto_EzAes_decrypt(JNIEnv* env, jclass clazz, jbyteArray input, jint length, jbyteArray output) {

	jbyte* inputPtr = (*env)->GetByteArrayElements(env, input, NULL);
	jbyte* outputPtr = (*env)->GetByteArrayElements(env, output, NULL);
	jint result = ezAesDecryptBlock(inputPtr, length, outputPtr);
	(*env)->ReleaseByteArrayElements(env, input, inputPtr, 0);
	(*env)->ReleaseByteArrayElements(env, output, outputPtr, 0);

	return result;
}

JNIEXPORT void JNICALL
			   Java_com_actionsmicro_airplay_crypto_EzAes_pairVerify(JNIEnv *env, jclass clazz,
																	  jbyteArray edPubKey, jbyteArray edSecKey,
																	  jbyteArray ctlPubKey, jbyteArray ctlSigature,
																	  jbyteArray out) {
	LOGD(">>>>> pairVerify");
	int keyLen = 32, outLen = 96, i = 0, n = 0;
	unsigned char edPubKeyPtr[keyLen];
	unsigned char edSecKeyPtr[keyLen];
	unsigned char ctlPubKeyPtr[keyLen];
	unsigned char ctlSigaturePtr[keyLen];

	(*env)->GetByteArrayRegion(env, edPubKey, 0, keyLen, edPubKeyPtr);
	(*env)->GetByteArrayRegion(env, edSecKey, 0, keyLen, edSecKeyPtr);
	(*env)->GetByteArrayRegion(env, ctlPubKey, 0, keyLen, ctlPubKeyPtr);
	(*env)->GetByteArrayRegion(env, ctlSigature, 0, keyLen, ctlSigaturePtr);

	const unsigned char curveBasePoint[] = { 0x09, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
	unsigned char secretKey[ 32 ];
	unsigned char temp1[ 64 ];
	unsigned char temp2[ 64 ];
	unsigned char temp3[ 64 ];
	unsigned char temp4[ 64 ];
	unsigned char temp5[ 64 ];
    unsigned char output96[ 96 ];


	APP_AIRTUNES_T *app = (APP_AIRTUNES_T *)GetSession();

	for (i = 0, n = sizeof(secretKey/*random_number*/); i < n; i++)
	{
		secretKey/*random_number*/[ i ] = rand();
	}

	memcpy(app->pair_setup.secretKey, secretKey/*random_number*/, 32);
	memcpy(app->pair_setup.controllerPublicKey, ctlPubKeyPtr, 32);
	memcpy(app->pair_setup.controllerSignature, ctlSigaturePtr, 32);

	for(i=0;i<32;i++)
	{
		LOGD("edPubKeyPtr [%d] = %d",i,edPubKeyPtr[i]);
	}
	for(i=0;i<32;i++)
	{
		LOGD("edSecKeyPtr [%d] = %d",i,edSecKeyPtr[i]);
	}
	for(i=0;i<32;i++)
	{
		LOGD("ctlPubKeyPtr [%d] = %d",i,ctlPubKeyPtr[i]);
	}
	for(i=0;i<32;i++)
	{
		LOGD("ctlSigaturePtr [%d] = %d",i,ctlSigaturePtr[i]);
	}

	for(i=0;i<32;i++)
	{
		LOGD("app->pair_setup.controllerSignature [%d] = %d",i,app->pair_setup.controllerSignature[i]);
	}

	sub_00000000x6(app->pair_setup.publicKey, app->pair_setup.secretKey, curveBasePoint/*k*/);
	sub_00000000x6(app->pair_setup.sharedKey, app->pair_setup.secretKey, app->pair_setup.controllerPublicKey);

	LOGD("<<<<< sub_00000000x6");

	memcpy(app->pair_setup.edSecret, edSecKeyPtr, 32);
	memcpy(app->pair_setup.edPubKey, edPubKeyPtr, 32);

	for(i=0;i<32;i++)
	{
		LOGD("publicKey [%d] = %d",i,app->pair_setup.publicKey[i]);
	}
	for(i=0;i<32;i++)
	{
		LOGD("sharedKey [%d] = %d",i,app->pair_setup.sharedKey[i]);
	}

	memset(temp1, 0, sizeof(temp1));
	memset(temp2, 0, sizeof(temp2));
	memset(temp3, 0, sizeof(temp3));
	memset(temp4, 0, sizeof(temp4));
	memset(temp5, 0, sizeof(temp5));

	memcpy(temp1, app->pair_setup.publicKey, 32);
	memcpy(temp1 + 32, app->pair_setup.controllerPublicKey, 32);
	sub_00000000x8(temp2, temp1, 64, app->pair_setup.edPubKey, app->pair_setup.edSecret);
	LOGD("<<<<< sub_00000000x8");
	AES_PairVerify1(app->pair_setup.sharedKey, (unsigned char *)"Pair-Verify-AES-Key", 0x13u, (unsigned char *)"Pair-Verify-AES-IV", 0x12u, temp3, temp4);
	LOGD("<<<<< AES_PairVerify1");
	if (*((unsigned int *)(app->pair_setup.enKey)))
	{
		free((void *)*((unsigned int *)(app->pair_setup.enKey)));
		*((unsigned int *)(app->pair_setup.enKey)) = 0;
	}

	AES_PairVerify2(app->pair_setup.enKey, temp3, temp4);
	LOGD("<<<<< AES_PairVerify2");

	for(i=0;i<64;i++)
	{
		LOGD("temp5 [%d] = %d",i,temp5[i]);
	}
	AES_PairVerify3(temp2, temp5,
		64, (aes_key_t *)(*(unsigned int *)((unsigned char *)app->pair_setup.enKey)),
		(unsigned char *)((*(unsigned int *)((unsigned char *)app->pair_setup.enKey)) + 244), 1);
	LOGD("<<<<< AES_PairVerify3");
	memcpy(output96, (void *)app->pair_setup.publicKey, 32);
	memcpy(output96 + 32, (void *)temp5, 64);

    (*env)->SetByteArrayRegion(env, out, 0, 96, output96);
    for(i=0;i<96;i++)
    {
        LOGD("output96 [%d] = %d",i,output96[i]);
    }

	LOGD("<<<<< pairVerify");

}


JNIEXPORT jint JNICALL
			  Java_com_actionsmicro_airplay_crypto_EzAes_setup(JNIEnv *env, jclass clazz, jdouble ver,
																 jbyteArray eiv, jbyteArray ekey,
																 jlong conID, jbyteArray jtemp8,
															     jbyteArray jtemp9,jbyteArray jtemp10) {
	LOGD(">>>>> setup");
	APP_AIRTUNES_T *app = (APP_AIRTUNES_T *)GetSession();
	int i = 0;
	int len = (*env)->GetArrayLength(env, eiv);
	int ekeyLen = (*env)->GetArrayLength(env, ekey);
	LOGD("eiv len %d", len);
	LOGD("ekeyLen len %d",ekeyLen);
	unsigned char eivPtr[len];
	unsigned char ekeyPtr[ekeyLen];
	unsigned char temp8[64];
	unsigned char temp9[64];
	unsigned char temp10[64];

	memset(temp8, 0, sizeof(temp8));
	memset(temp9, 0, sizeof(temp9));
	memset(temp10, 0, sizeof(temp10));

	(*env)->GetByteArrayRegion(env, eiv, 0, len, eivPtr);
	(*env)->GetByteArrayRegion(env, ekey, 0, ekeyLen, ekeyPtr);

	LOGD("eivPtr %s",eivPtr);
	LOGD("ekeyPtr %s",ekeyPtr);
	LOGD("temp8 %s", temp8);
	LOGD("temp10 %s", temp10);
	LOGD("temp9 %s", temp9);


	unsigned char *dok = NULL;
	unsigned char *doi = NULL;
	//AirPlayStreamKey
	//AirPlayStreamIV

	jbyte dokInitValue[] = {28, -25, 34, -52, 114, -127, 22, 48,
						   -113, -67, 113, 60, -10, -14, 83, 21,
						   -30, 1, 38, -25, 115, -44, 110, 68,
						   -107, -103, -66, 57, -59, 47, -82};
	jbyte doiInitValue[] = {28, -25, 34, -52, 114, -127, 22, 48,
						   -113, -67, 113, 60, -10, -16, 96, 108,
						   -30, 1, 38, -25, 115, -44, 110, 68,
						   -107, -103, -66, 57, -59, 47, -82};

	for(i=0;i<16;i++)
	{
		LOGD("mirrorKey before [%d] = %d",i, temp9[i]);
	}
	for(i=0;i<16;i++)
	{
		LOGD("mirrorIV beofre [%d] = %d",i, temp10[i]);
	}
	memcpy(app->pair_setup.aesIV, eivPtr, len);
	memcpy(app->pair_setup.aesKey, ekeyPtr, len);

	dok = (unsigned char *)AES_AirplayStreamKeyInit(
			dokInitValue[0], dokInitValue[1], dokInitValue[2], dokInitValue[3], dokInitValue[4], dokInitValue[5], dokInitValue[6], dokInitValue[7], dokInitValue[8], dokInitValue[9],
			dokInitValue[10], dokInitValue[11], dokInitValue[12], dokInitValue[13], dokInitValue[14], dokInitValue[15], dokInitValue[16], dokInitValue[17], dokInitValue[18], dokInitValue[19],
			dokInitValue[20], dokInitValue[21], dokInitValue[22], dokInitValue[23], dokInitValue[24], dokInitValue[25], dokInitValue[26], dokInitValue[27], dokInitValue[28], dokInitValue[29],
			dokInitValue[30]);

	doi = (unsigned char *)AES_AirplayStreamKeyInit(
			doiInitValue[0], doiInitValue[1], doiInitValue[2], doiInitValue[3], doiInitValue[4], doiInitValue[5], doiInitValue[6], doiInitValue[7], doiInitValue[8], doiInitValue[9],
			doiInitValue[10], doiInitValue[11], doiInitValue[12], doiInitValue[13], doiInitValue[14], doiInitValue[15], doiInitValue[16], doiInitValue[17], doiInitValue[18], doiInitValue[19],
			doiInitValue[20], doiInitValue[21], doiInitValue[22], doiInitValue[23], doiInitValue[24], doiInitValue[25], doiInitValue[26], doiInitValue[27], doiInitValue[28], doiInitValue[29],
			dokInitValue[30]);


	unsigned char temp6[ 128 ];
	unsigned char temp7[ 128 ];
	memset(temp6, 0, sizeof(temp6));
	memset(temp7, 0, sizeof(temp7));

	snprintf((char *)temp6, sizeof(temp6), "%s%llu", dok, conID);
	snprintf((char *)temp7, sizeof(temp7), "%s%llu", doi, conID);

	LOGD("temp6 %s",temp6);
	LOGD("temp7 %s",temp7);

	if(ver >= 230.0) {
		LOGD("ios9");
		AES_FpSetup(app->pair_setup.sharedKey, app->pair_setup.aesKey, 16, NULL, NULL, temp8, NULL);
		AES_AirplayStreamKeyDecrypt(/*sharedKey,*/ temp6, temp7, temp8, temp9, temp10);
	} else{
		LOGD("not ios9");
		memcpy(temp8, app->pair_setup.aesKey, 16);
		AES_AirplayStreamKeyDecrypt(/*sharedKey,*/ temp6, temp7, app->pair_setup.aesKey, temp9,
												   temp10);
	}

	LOGD("temp8 %s", temp8);
	LOGD("temp10 %s", temp10);
	LOGD("temp9 %s", temp9);

	for(i=0;i<16;i++)
	{
		LOGD("mirrorKey [%d] = %d",i, temp9[i]);
	}
	for(i=0;i<16;i++)
	{
		LOGD("mirrorIV [%d] = %d",i, temp10[i]);
	}

	(*env)->SetByteArrayRegion(env, jtemp8, 0, 64, temp8);
	(*env)->SetByteArrayRegion(env, jtemp9, 0, 64, temp9);
	(*env)->SetByteArrayRegion(env, jtemp10, 0, 64, temp10);


	LOGD("<<<<< setup");
	return 1;
}

JNIEXPORT jbyteArray JNICALL
			   Java_com_actionsmicro_airplay_crypto_EzAes_airplayStreamKeyInit(JNIEnv *env, jclass clazz,
																  jbyte a1, jbyte a2, jbyte a3, jbyte a4, jbyte a5, jbyte a6, jbyte a7, jbyte a8, jbyte a9, jbyte a10,
																  jbyte a11, jbyte a12, jbyte a13, jbyte a14, jbyte a15, jbyte a16, jbyte a17, jbyte a18, jbyte a19, jbyte a20,
																  jbyte a21, jbyte a22, jbyte a23, jbyte a24, jbyte a25, jbyte a26, jbyte a27, jbyte a28, jbyte a29, jbyte a30,
																  jbyte a31) {

	jbyteArray bArray = (*env)->NewByteArray(env, 31);
	(*env)->SetByteArrayRegion(env, bArray, 0, 31,
							   (jbyte *) AES_AirplayStreamKeyInit(a1, a2, a3, a4, a5, a6, a7, a8,
																  a9, a10,
																  a11, a12, a13, a14, a15, a16, a17,
																  a18, a19, a20,
																  a21, a22, a23, a24, a25, a26, a27,
																  a28, a29, a30,
																  a31));
	return bArray;
}


JNIEXPORT jint JNICALL
			   Java_com_actionsmicro_airplay_crypto_EzAes_airplayStreamKeyDecrypt(JNIEnv *env, jclass clazz,
																  jbyteArray a1, jbyteArray a2,
																  jbyteArray a3, jbyteArray a4,
																  jbyteArray a5, jbyteArray a6) {
	jbyte *a1Ptr = (*env)->GetByteArrayElements(env, a1, NULL);
	jbyte *a2Ptr = (*env)->GetByteArrayElements(env, a2, NULL);
	jbyte *a3Ptr = (*env)->GetByteArrayElements(env, a3, NULL);
	jbyte *a4Ptr = (*env)->GetByteArrayElements(env, a4, NULL);
	jbyte *a5Ptr = (*env)->GetByteArrayElements(env, a5, NULL);
	jbyte *a6Ptr = (*env)->GetByteArrayElements(env, a6, NULL);
	jint result = AES_AirplayStreamKeyDecrypt(a1Ptr,a2Ptr,a3Ptr,a4Ptr,a5Ptr,a6Ptr);
	(*env)->ReleaseByteArrayElements(env, a1, a1Ptr, 0);
	(*env)->ReleaseByteArrayElements(env, a2, a2Ptr, 0);
	(*env)->ReleaseByteArrayElements(env, a3, a3Ptr, 0);
	(*env)->ReleaseByteArrayElements(env, a4, a4Ptr, 0);
	(*env)->ReleaseByteArrayElements(env, a5, a5Ptr, 0);
	(*env)->ReleaseByteArrayElements(env, a6, a6Ptr, 0);
	return result;
}

JNIEXPORT jint JNICALL
			   Java_com_actionsmicro_airplay_crypto_EzAes_sub00000000x6(JNIEnv* env, jclass clazz, jbyteArray mypublic, jbyteArray secret,jbyteArray basepoint) {
	LOGD(">>>>> sub00000000x6");
	int i = 0;
	jsize length = 32;

	unsigned char pkPtr[length];
	unsigned char skPtr[length];
	unsigned char basepointPtr[length];
	(*env)->GetByteArrayRegion(env, mypublic, 0, length, pkPtr);
	(*env)->GetByteArrayRegion(env, secret, 0, length, skPtr);
	(*env)->GetByteArrayRegion(env, basepoint, 0, length, basepointPtr);
	jint result  = sub_00000000x6(pkPtr, skPtr, basepointPtr);

	(*env)->SetByteArrayRegion(env, mypublic, 0, length, pkPtr);
	(*env)->SetByteArrayRegion(env, secret, 0, length, skPtr);
	LOGD("<<<<< sub00000000x6");
	return result;
}

JNIEXPORT jint JNICALL
			   Java_com_actionsmicro_airplay_crypto_EzAes_sub00000000x7(JNIEnv* env, jclass clazz, jbyteArray pk, jbyteArray sk,jbyteArray seed) {
	LOGD(">>>>> sub00000000x7");
	int i = 0;
	jsize length = 32;
	unsigned char pkPtr[length];
	unsigned char skPtr[length];
	unsigned char seedPtr[length];
	(*env)->GetByteArrayRegion(env, pk, 0, length, pkPtr);
	(*env)->GetByteArrayRegion(env, sk, 0, length, skPtr);
	(*env)->GetByteArrayRegion(env, seed, 0, length, seedPtr);

	LOGD("before pkPtr %s",pkPtr);
	LOGD("before skPtr %s",skPtr);
	LOGD("seedPtr %s",seedPtr);
	jint result  = sub_00000000x7(pkPtr, skPtr, seedPtr);
	(*env)->SetByteArrayRegion(env, pk, 0, length, pkPtr);
	(*env)->SetByteArrayRegion(env, sk, 0, length, skPtr);
	LOGD("<<<<< sub00000000x7");
	return result;
}

JNIEXPORT jint JNICALL
			   Java_com_actionsmicro_airplay_crypto_EzAes_sub00000000x8(JNIEnv *env, jclass clazz,
																				  jbyteArray a1, jbyteArray a2,
																				  jint len, jbyteArray a3, jbyteArray a4) {
	LOGD(">>>>> sub00000000x8");
	int keyLeng = 32;
	unsigned char a1Ptr[len];
	unsigned char a2Ptr[len];
	unsigned char a3Ptr[keyLeng];
	unsigned char a4Ptr[keyLeng];
	(*env)->GetByteArrayRegion(env, a1, 0, len, a1Ptr);
	(*env)->GetByteArrayRegion(env, a2, 0, len, a2Ptr);
	(*env)->GetByteArrayRegion(env, a3, 0, keyLeng, a3Ptr);
	(*env)->GetByteArrayRegion(env, a4, 0, keyLeng, a4Ptr);

	jint result = sub_00000000x8(a1Ptr,a2Ptr,len,a3Ptr,a4Ptr);


	(*env)->SetByteArrayRegion(env, a1, 0, len, a1Ptr);
	(*env)->SetByteArrayRegion(env, a2, 0, len, a2Ptr);
	LOGD("<<<<< sub00000000x8");
	return result;
}