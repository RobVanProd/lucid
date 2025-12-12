@echo off
set JAVA_HOME=C:\Users\WDAGUtilityAccount\android-tools\jdk-17.0.9+8
set ANDROID_HOME=C:\Users\WDAGUtilityAccount\android-tools\android-sdk
set PATH=%JAVA_HOME%\bin;%PATH%

echo Building LUCID APK...
echo.
cd /d C:\Users\WDAGUtilityAccount\Documents\Projects\lucid
call gradlew.bat assembleDebug --no-daemon
