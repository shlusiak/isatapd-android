LOCAL_PATH := $(call my-dir)
 
include $(CLEAR_VARS)
 
LOCAL_CFLAGS  := -DHAVE_CONFIG_H=1
LOCAL_MODULE    := isatapd
LOCAL_SRC_FILES := \
	src/isatap.c \
	src/main.c \
	src/rdisc.c \
	src/tunnel.c

# LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)
