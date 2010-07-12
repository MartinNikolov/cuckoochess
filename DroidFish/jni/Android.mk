LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := jni
LOCAL_SRC_FILES := jni.cpp

LOCAL_CFLAGS    := -I$(LOCAL_PATH)/stlport/stlport \
	 -mandroid \
	-DTARGET_OS=android -D__ANDROID__ \
	-isystem $(SYSROOT)/usr/include

LOCAL_STATIC_LIBRARIES := stockfish stlport

include $(BUILD_SHARED_LIBRARY)

include jni/stlport/src/Android.mk
include jni/stockfish/Android.mk
