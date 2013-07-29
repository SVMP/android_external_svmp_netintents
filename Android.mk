LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_STATIC_JAVA_LIBRARIES := SVMPProtocol
LOCAL_STATIC_JAVA_LIBRARIES += netty

LOCAL_PACKAGE_NAME := net_intents

# Builds against the public SDK
#LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)

# This finds and builds the test apk as well, so a single make does both.
include $(call all-makefiles-under,$(LOCAL_PATH))
