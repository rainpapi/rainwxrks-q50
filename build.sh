#!/usr/bin/env bash
# Build rainwxrks-q50: Java -> dex -> signed APK (minSdk 10, pure-Java, no native libs),
# then wrap it into a loadable .epk using the public OBU cert in keys/ (for the App Garage loader).
# Rebranded for Trujjy Rich / @rainpapi
set -euo pipefail
cd "$(dirname "$0")"

# --- Project branding (change these if you rename again) ---
APP_NAME="rainwxrks-q50"
APK_BASE="rainwxrks-q50"
KEY_ALIAS="rainwxrks"
KEY_DNAME="CN=RainWxRKS-Q50, O=Trujjy Rich"

# Cloners: point these at your own toolchain — set JAVA_HOME / ANDROID_SDK env vars, or edit here.
export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Eclipse Adoptium/jdk-25.0.3.9-hotspot}"
SDK="${ANDROID_SDK:-/c/Users/raid2/scoop/apps/android-clt/current}"
BT="$SDK/build-tools/34.0.0"
ANDJAR="$SDK/platforms/android-34/android.jar"

# tool suffixes: Windows/Git-Bash uses .exe / .bat; Linux/macOS uses none
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) X=.exe; B=.bat;; *) X=""; B="";; esac
AAPT="$BT/aapt$X"; ZIPALIGN="$BT/zipalign$X"; APKSIGNER="$BT/apksigner$B"; D8="$BT/d8$B"
JAVAC="$JAVA_HOME/bin/javac$X"; KEYTOOL="$JAVA_HOME/bin/keytool$X"; JAR="$JAVA_HOME/bin/jar$X"

VC=$(date +%s)

rm -rf build && mkdir -p build/classes build/dex
echo "== [1/6] javac (release 8) [${APP_NAME}] =="
"$JAVAC" --release 8 -g -d build/classes -classpath "$ANDJAR" src/com/appgarage/dash/*.java
echo "  compiled: $(find build/classes -name '*.class' | wc -l) classes"

echo "== [2/6] d8 -> classes.dex (min-api 10) =="
"$D8" --min-api 10 --lib "$ANDJAR" --output build/dex $(find build/classes -name '*.class')
ls -l build/dex/classes.dex

echo "== [3/6] package APK (versionCode=$VC) =="
sed "s/android:versionCode=\"[0-9]*\"/android:versionCode=\"$VC\"/" AndroidManifest.xml > build/AndroidManifest.xml
"$AAPT" package -f -M build/AndroidManifest.xml -S res -I "$ANDJAR" -F build/${APK_BASE}.unsigned.apk
( cd build/dex && "$AAPT" add ../${APK_BASE}.unsigned.apk classes.dex >/dev/null )

echo "== [4/6] zipalign =="
"$ZIPALIGN" -f -p 4 build/${APK_BASE}.unsigned.apk build/${APK_BASE}.aligned.apk

echo "== [5/6] sign (v1 for API 10) =="
if [ ! -f keystore.ks ]; then
  "$KEYTOOL" -genkeypair -keystore keystore.ks -alias "$KEY_ALIAS" -keyalg RSA \
    -keysize 2048 -validity 10000 -storepass android -keypass android -dname "$KEY_DNAME" >/dev/null 2>&1
fi
"$APKSIGNER" sign --ks keystore.ks --ks-pass pass:android --key-pass pass:android \
  --min-sdk-version 10 --v1-signing-enabled true --v2-signing-enabled true \
  --out build/${APK_BASE}.apk build/${APK_BASE}.aligned.apk
"$APKSIGNER" verify --min-sdk-version 10 build/${APK_BASE}.apk >/dev/null 2>&1 && echo "  signature OK [$KEY_ALIAS]"

echo "== [6/6] wrap into .epk (uses the public OBU cert in keys/obu_cert.pem) =="
if [ -f tools/epk_tool.py ] && [ -f keys/obu_cert.pem ]; then
  if ! python tools/epk_tool.py build build/${APK_BASE}.apk build/${APK_BASE}.epk --cert keys/obu_cert.pem; then
    echo "  .epk wrap failed — need Python 3 + 'pip install cryptography'. The APK is still ready."
  fi
else
  echo "  skipped: keys/obu_cert.pem or tools/epk_tool.py missing — APK is ready, but no .epk was produced."
fi

echo ""
echo "== VERIFY [${APP_NAME}] =="
"$AAPT" dump badging build/${APK_BASE}.apk 2>/dev/null | grep -iE "package:|sdkVersion|native-code|launchable" || true
"$JAR" -tf build/${APK_BASE}.apk | grep -viE "META-INF/" | head
echo "DONE -> build/${APK_BASE}.apk"; ls -l build/${APK_BASE}.apk build/${APK_BASE}.epk 2>/dev/null || true
