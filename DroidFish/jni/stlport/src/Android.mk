LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS) 

#LOCAL_ARM_MODE := arm # otherwise 16bit (thumb)
LOCAL_MODULE := stlport

LOCAL_SRC_FILES := \
	strstream.cpp messages.cpp iostream.cpp ctype.cpp dll_main.cpp complex_trig.cpp \
	locale_catalog.cpp complex_io.cpp allocators.cpp string.cpp ostream.cpp \
	locale_impl.cpp locale.cpp istream.cpp time_facets.cpp fstream.cpp \
	bitset.cpp codecvt.cpp stdio_streambuf.cpp complex.cpp ios.cpp sstream.cpp \
	num_put.cpp num_put_float.cpp cxa.c facets_byname.cpp numpunct.cpp monetary.cpp \
	num_get_float.cpp num_get.cpp c_locale.c collate.cpp 

LOCAL_CFLAGS = -I$(LOCAL_PATH)/../stlport \
	 -mandroid \
	-DTARGET_OS=android -D__ANDROID__ \
	-isystem $(SYSROOT)/usr/include

#LOCAL_CFLAGS +=  -v

include $(BUILD_STATIC_LIBRARY)
