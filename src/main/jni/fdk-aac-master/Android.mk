LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

aacdec_sources := $(wildcard $(LOCAL_PATH)/libAACdec/src/*.cpp)
aacdec_sources := $(aacdec_sources:$(LOCAL_PATH)/libAACdec/src/%=%)

aacenc_sources := $(wildcard $(LOCAL_PATH)/libAACenc/src/*.cpp)
aacenc_sources := $(aacenc_sources:$(LOCAL_PATH)/libAACenc/src/%=%)

pcmutils_sources := $(wildcard $(LOCAL_PATH)/libPCMutils/src/*.cpp)
pcmutils_sources := $(pcmutils_sources:$(LOCAL_PATH)/libPCMutils/src/%=%)

fdk_sources := $(wildcard $(LOCAL_PATH)/libFDK/src/*.cpp)
fdk_sources := $(fdk_sources:$(LOCAL_PATH)/libFDK/src/%=%)

sys_sources := $(wildcard $(LOCAL_PATH)/libSYS/src/*.cpp)
sys_sources := $(sys_sources:$(LOCAL_PATH)/libSYS/src/%=%)

mpegtpdec_sources := $(wildcard $(LOCAL_PATH)/libMpegTPDec/src/*.cpp)
mpegtpdec_sources := $(mpegtpdec_sources:$(LOCAL_PATH)/libMpegTPDec/src/%=%)

mpegtpenc_sources := $(wildcard $(LOCAL_PATH)/libMpegTPEnc/src/*.cpp)
mpegtpenc_sources := $(mpegtpenc_sources:$(LOCAL_PATH)/libMpegTPEnc/src/%=%)

sbrdec_sources := $(wildcard $(LOCAL_PATH)/libSBRdec/src/*.cpp)
sbrdec_sources := $(sbrdec_sources:$(LOCAL_PATH)/libSBRdec/src/%=%)

sbrenc_sources := $(wildcard $(LOCAL_PATH)/libSBRenc/src/*.cpp)
sbrenc_sources := $(sbrenc_sources:$(LOCAL_PATH)/libSBRenc/src/%=%)

arithcoding_sources := $(wildcard $(LOCAL_PATH)/libArithCoding/src/*.cpp)
arithcoding_sources := $(arithcoding_sources:$(LOCAL_PATH)/libArithCoding/src/%=%)

drcdec_sources := $(wildcard $(LOCAL_PATH)/libDRCdec/src/*.cpp)
drcdec_sources := $(drcdec_sources:$(LOCAL_PATH)/libDRCdec/src/%=%)

sacdec_sources := $(wildcard $(LOCAL_PATH)/libSACdec/src/*.cpp)
sacdec_sources := $(sacdec_sources:$(LOCAL_PATH)/libSACdec/src/%=%)

sacenc_sources := $(wildcard $(LOCAL_PATH)/libSACenc/src/*.cpp)
sacenc_sources := $(sacenc_sources:$(LOCAL_PATH)/libSACenc/src/%=%)

LOCAL_SRC_FILES := \
        $(aacdec_sources:%=libAACdec/src/%) \
        $(aacenc_sources:%=libAACenc/src/%) \
        $(pcmutils_sources:%=libPCMutils/src/%) \
        $(fdk_sources:%=libFDK/src/%) \
        $(sys_sources:%=libSYS/src/%) \
        $(mpegtpdec_sources:%=libMpegTPDec/src/%) \
        $(mpegtpenc_sources:%=libMpegTPEnc/src/%) \
        $(sbrdec_sources:%=libSBRdec/src/%) \
        $(sbrenc_sources:%=libSBRenc/src/%) \
        $(arithcoding_sources:%=libArithCoding/src/%) \
        $(drcdec_sources:%=libDRCdec/src/%) \
        $(sacdec_sources:%=libSACdec/src/%) \
        $(sacenc_sources:%=libSACenc/src/%) 

LOCAL_CFLAGS := -DANDROID
LOCAL_CFLAGS += -Wno-sequence-point -Wno-extra -Werror -Wno-unused-parameter -Wno-#warnings -Wuninitialized -Wno-self-assign -Wno-implicit-fallthrough

LOCAL_C_INCLUDES := \
        $(LOCAL_PATH)/libAACdec/include \
        $(LOCAL_PATH)/libAACenc/include \
        $(LOCAL_PATH)/libPCMutils/include \
        $(LOCAL_PATH)/libFDK/include \
        $(LOCAL_PATH)/libSYS/include \
        $(LOCAL_PATH)/libMpegTPDec/include \
        $(LOCAL_PATH)/libMpegTPEnc/include \
        $(LOCAL_PATH)/libSBRdec/include \
        $(LOCAL_PATH)/libSBRenc/include \
        $(LOCAL_PATH)/libArithCoding/include \
        $(LOCAL_PATH)/libDRCdec/include \
        $(LOCAL_PATH)/libSACdec/include \
        $(LOCAL_PATH)/libSACenc/include

LOCAL_MODULE:= libFraunhoferAAC

include $(BUILD_STATIC_LIBRARY)
