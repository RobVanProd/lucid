# LUCID Build Dockerfile
# ----------------------
# Build the LUCID APK in a container
#
# Usage:
#   docker build -t lucid-builder .
#   docker run --rm -v $(pwd)/output:/output lucid-builder
#
# The APK will be copied to ./output/

FROM openjdk:17-slim

# Install required packages
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# Set up Android SDK
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools

# Download and install Android command line tools
RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools && \
    cd $ANDROID_SDK_ROOT/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O tools.zip && \
    unzip -q tools.zip && \
    rm tools.zip && \
    mv cmdline-tools latest

# Accept licenses and install SDK components
RUN yes | sdkmanager --licenses && \
    sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"

# Copy project
WORKDIR /app
COPY . .

# Create local.properties
RUN echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties

# Make gradlew executable
RUN chmod +x gradlew

# Build APK
RUN ./gradlew assembleDebug --no-daemon

# Copy APK to output on run
CMD cp app/build/outputs/apk/debug/app-debug.apk /output/lucid-debug.apk && \
    echo "APK copied to /output/lucid-debug.apk"
