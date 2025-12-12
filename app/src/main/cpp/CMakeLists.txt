# CMakeLists.txt for LUCID native inference
#
# This file configures the native build for llama.cpp integration.
# The actual llama.cpp library needs to be built separately and linked.
#
# Prerequisites:
# 1. Build llama.cpp for Android targets (arm64-v8a, armeabi-v7a)
# 2. Place libllama.so in app/src/main/jniLibs/{abi}/
# 3. Uncomment the relevant sections below
#
cmake_minimum_required(VERSION 3.22.1)

project("lucid-ai")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add JNI bridge library
add_library(llama-jni SHARED
    llama-jni.cpp
)

# Find required libraries
find_library(log-lib log)
find_library(android-lib android)

# Link libraries
target_link_libraries(llama-jni
    ${log-lib}
    ${android-lib}
)

# Uncomment below when llama.cpp is available:
#
# # Add pre-built llama library
# add_library(llama SHARED IMPORTED)
# set_target_properties(llama PROPERTIES
#     IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libllama.so
# )
#
# # Include llama.cpp headers
# target_include_directories(llama-jni PRIVATE
#     ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp
# )
#
# # Link llama library
# target_link_libraries(llama-jni
#     llama
#     ${log-lib}
#     ${android-lib}
# )
