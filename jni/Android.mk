LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

FAIRPLAY_MODULE := $(LOCAL_PATH)/fairplay_jni/Android.mk
FDK_AAC_MODULE := $(LOCAL_PATH)/fdk-aac-master/Android.mk

LOCAL_SRC_FILES := fairplay_jni/fairplay_jni.c \
		fairplay_jni/ez_aes.c \
		fairplay_jni/ez_aes_jni.c \
		aaceld_jni/aac_interface.c \
		aaceld_jni/aaceld_jni.c \

LOCAL_C_INCLUDES := \
		$(LOCAL_PATH)/fdk-aac-master/libAACdec/include \
		$(LOCAL_PATH)/fdk-aac-master/libSYS/include \


LOCAL_STATIC_LIBRARIES := libFraunhoferAAC libFairplay

LOCAL_LDLIBS    := -llog

LOCAL_MODULE := fairplay-jni
LOCAL_MODULE_FILENAME := libfairplay-jni

include $(BUILD_SHARED_LIBRARY)

include $(FDK_AAC_MODULE)
include $(FAIRPLAY_MODULE)