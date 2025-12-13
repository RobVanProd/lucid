# CMakeLists.txt for LUCID native inference with llama.cpp
#
# This file configures the native build for llama.cpp integration.
# llama.cpp is fetched and built as part of the Android build process.

cmake_minimum_required(VERSION 3.22.1)

project("lucid-ai")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Enable position independent code
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Optimization flags for mobile
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math -DNDEBUG")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -ffast-math -DNDEBUG")

# Include FetchContent for llama.cpp
include(FetchContent)

# Fetch llama.cpp from GitHub
FetchContent_Declare(
    llama_cpp
    GIT_REPOSITORY https://github.com/ggerganov/llama.cpp.git
    GIT_TAG b4257
    GIT_SHALLOW TRUE
    GIT_PROGRESS TRUE
)

# Configure llama.cpp build options for mobile
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_CUDA OFF CACHE BOOL "" FORCE)
set(LLAMA_METAL OFF CACHE BOOL "" FORCE)
set(LLAMA_VULKAN OFF CACHE BOOL "" FORCE)
set(LLAMA_SYCL OFF CACHE BOOL "" FORCE)
set(LLAMA_HIPBLAS OFF CACHE BOOL "" FORCE)
set(LLAMA_CLBLAST OFF CACHE BOOL "" FORCE)
set(LLAMA_BLAS OFF CACHE BOOL "" FORCE)
set(LLAMA_NATIVE OFF CACHE BOOL "" FORCE)
set(LLAMA_AVX OFF CACHE BOOL "" FORCE)
set(LLAMA_AVX2 OFF CACHE BOOL "" FORCE)
set(LLAMA_AVX512 OFF CACHE BOOL "" FORCE)
set(LLAMA_FMA OFF CACHE BOOL "" FORCE)
set(LLAMA_F16C OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS ON CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_ACCELERATE OFF CACHE BOOL "" FORCE)

# Populate llama.cpp
FetchContent_MakeAvailable(llama_cpp)

# Find required libraries
find_library(log-lib log)
find_library(android-lib android)

# Add JNI bridge library
add_library(llama-jni SHARED
    llama-jni.cpp
)

# Include llama.cpp headers
target_include_directories(llama-jni PRIVATE
    ${llama_cpp_SOURCE_DIR}/include
    ${llama_cpp_SOURCE_DIR}/ggml/include
    ${llama_cpp_SOURCE_DIR}/common
)

# Link libraries
target_link_libraries(llama-jni
    llama
    ggml
    common
    ${log-lib}
    ${android-lib}
)

# Set library output name
set_target_properties(llama-jni PROPERTIES
    OUTPUT_NAME "llama-jni"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}"
)
