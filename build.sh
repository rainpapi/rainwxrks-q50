#!/bin/bash
set -e
ANDROID_SDK=${ANDROID_SDK:-$HOME/Android/Sdk}
BUILD_TOOLS=$(ls -d $ANDROID_SDK/build-tools/* | sort -V | tail -n1)
PLATFORM=$(ls $ANDROID_SDK/platforms/android-*/android.jar | sort -V | tail -n1)
rm -rf /tmp/base
git clone https://github.com/bugjosh/appgarage-dash /tmp/base
mkdir -p keys tools
cp /tmp/base/keys/obu_cert.pem keys/
cp /tmp/base/tools/epk_tool.py tools/
keytool -genkey -v -keystore keys/keystore.ks -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=RainWxrks" -noprompt || true
VC=$(date +%s)
sed -i "s/versionCode=\"[0-9]*\"/versionCode=\"$VC\"/" AndroidManifest.xml
rm -rf build && mkdir build
$BUILD_TOOLS/aapt package -f -m -J src -M AndroidManifest.xml -S res -I $PLATFORM
javac -bootclasspath $PLATFORM -d build -source 1.6 -target 1.6 src/com/rainwxrks/q50/*.java src/com/rainwxrks/q50/R.java
$BUILD_TOOLS/d8 --output build/ build/com/rainwxrks/q50/*.class || $BUILD_TOOLS/dx --dex --output=build/classes.dex build/
$BUILD_TOOLS/aapt package -f -M AndroidManifest.xml -S res -I $PLATFORM -F build/rainwxrks.apk.unaligned
(cd build && $BUILD_TOOLS/aapt add rainwxrks.apk.unaligned classes.dex)
jarsigner -keystore keys/keystore.ks -storepass android -keypass android build/rainwxrks.apk.unaligned androiddebugkey
$BUILD_TOOLS/zipalign -f 4 build/rainwxrks.apk.unaligned build/rainwxrks.apk
python3 tools/epk_tool.py build build/rainwxrks.apk build/rainwxrks.epk --cert keys/obu_cert.pem
