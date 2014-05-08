#include <jni.h>
#include <android/log.h>
#include <stdint.h>

JNIEXPORT jint JNICALL
Java_com_actionsmicro_airplay_crypto_EzAes_init(JNIEnv* env, jclass clazz, jbyteArray key, jbyteArray iv) {

	jbyte* keyPtr = (*env)->GetByteArrayElements(env, key, NULL);
	jbyte* ivPtr = (*env)->GetByteArrayElements(env, iv, NULL);
	jint result = ezAesDecrytInit(keyPtr, ivPtr);
	(*env)->ReleaseByteArrayElements(env, key, keyPtr, 0);
	(*env)->ReleaseByteArrayElements(env, iv, ivPtr, 0);
	
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