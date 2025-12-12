#!/bin/bash
#
# LUCID APK Build Script
# ----------------------
# Builds the LUCID Android APK
#
# Prerequisites:
# - JDK 17 or higher
# - Android SDK with:
#   - Build tools 34.0.0
#   - Platform SDK 34
#   - Android SDK Command-line Tools
#
# Usage:
#   ./build-apk.sh [debug|release]
#

set -e

BUILD_TYPE="${1:-debug}"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "============================================"
echo "  LUCID - The Cognitive Firewall"
echo "  Building $BUILD_TYPE APK..."
echo "============================================"
echo

# Check for ANDROID_HOME
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    # Try common locations
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    elif [ -d "/usr/local/android-sdk" ]; then
        export ANDROID_HOME="/usr/local/android-sdk"
    elif [ -d "/opt/android-sdk" ]; then
        export ANDROID_HOME="/opt/android-sdk"
    else
        echo "ERROR: Android SDK not found."
        echo "Please set ANDROID_HOME or ANDROID_SDK_ROOT environment variable."
        echo
        echo "To install Android SDK:"
        echo "  1. Download from: https://developer.android.com/studio#command-tools"
        echo "  2. Extract and run: sdkmanager 'platforms;android-34' 'build-tools;34.0.0'"
        exit 1
    fi
fi

export ANDROID_SDK_ROOT="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
echo "Using Android SDK: $ANDROID_SDK_ROOT"

# Create local.properties
echo "sdk.dir=$ANDROID_SDK_ROOT" > "$PROJECT_DIR/local.properties"

# Check Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java not found. Please install JDK 17 or higher."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
echo "Java version: $JAVA_VERSION"

# Build
cd "$PROJECT_DIR"

if [ "$BUILD_TYPE" = "release" ]; then
    echo "Building release APK..."
    ./gradlew assembleRelease --no-daemon
    APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
else
    echo "Building debug APK..."
    ./gradlew assembleDebug --no-daemon
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
fi

if [ -f "$APK_PATH" ]; then
    echo
    echo "============================================"
    echo "  BUILD SUCCESSFUL!"
    echo "============================================"
    echo "APK location: $APK_PATH"
    echo
    echo "To install on a connected device:"
    echo "  adb install $APK_PATH"
    echo
else
    echo "ERROR: Build failed. APK not found."
    exit 1
fi
