#!/usr/bin/env bash
# Build rainwxrks-q50: Java -> dex -> signed APK (minSdk 10, pure-Java, no native libs),
# then wrap it into a loadable .epk using the public OBU cert in keys/ (for the App Garage loader).
# Rebranded for Trujjy Rich / @rainpapi
# FIXED: works on Windows Git-Bash AND Linux GitHub Actions
set -euo pipefail
cd "$(dirname "$0")"

# Optional first argument is a build variant label (for example: debug).
# The project uses its own lightweight toolchain, so the label is informational.
BUILD_VARIANT="${1:-release}"
echo "Build variant: $BUILD_VARIANT"

# --- Project branding (change these if you rename again) ---
APP_NAME="rainwxrks-q50"
APK_BASE="rainwxrks-q50"
KEY_ALIAS="rainwxrks"
KEY_DNAME="CN=RainWxRKS-Q50, O=Trujjy Rich"

# --- Toolchain detection (Windows local + Linux CI) ---

# JAVA_HOME: use env if valid, else try Windows default, else infer from PATH
if [ -n "${JAVA_HOME:-}" ] && [ -d "$JAVA_HOME" ]; then
  : # keep it
elif [ -d "/c/Program Files/Eclipse Adoptium/jdk-25.0.3.9-hotspot" ]; then
  export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.3.9-hotspot"
else
  if command -v javac >/dev/null 2>&1; then
    JAVAC_PATH=$(command -v javac)
    # handle symlinks
    if command -v readlink >/dev/null 2>&1; then
      JAVAC_PATH=$(readlink -f "$JAVAC_PATH" 2>/dev/null || echo "$JAVAC_PATH")
    fi
    export JAVA_HOME=$(dirname "$(dirname "$JAVAC_PATH")")
  fi
fi

# ANDROID SDK: try multiple env vars and common locations
CANDIDATE_SDKS=(
  "${ANDROID_SDK:-}"
  "${ANDROID_HOME:-}"
  "${ANDROID_SDK_ROOT:-}"
  "/usr/local/lib/android/sdk"
  "$HOME/Android/Sdk"
  "/opt/android-sdk"
  "/c/Users/raid2/scoop/apps/android-clt/current"
)
SDK=""
for c in "${CANDIDATE_SDKS[@]}"; do
  if [ -n "$c" ] && [ -d "$c" ]; then
    SDK="$c"
    break
  fi
done

if [ -z "$SDK" ]; then
  echo "ERROR: Android SDK not found. Set ANDROID_SDK or ANDROID_HOME"
  echo "Checked: ${CANDIDATE_SDKS[*]}"
  exit 1
fi
echo "Using SDK: $SDK"

# Build-tools: prefer 34.0.0, else latest available
if [ -d "$SDK/build-tools/34.0.0" ]; then
  BT="$SDK/build-tools/34.0.0"
else
  LATEST_BT=$(ls "$SDK/build-tools" 2>/dev/null | sort -V | tail -n1)
  if [ -z "$LATEST_BT" ]; then
    echo "ERROR: no build-tools found in $SDK/build-tools"
    exit 1
  fi
  BT="$SDK/build-tools/$LATEST_BT"
fi
echo "Using build-tools: $BT"

# android.jar: prefer android-34, else newest
ANDJAR="$SDK/platforms/android-34/android.jar"
if [ ! -f "$ANDJAR" ]; then
  ANDJAR=$(find "$SDK/platforms" -name "android.jar" 2>/dev/null | sort -V | tail -n1)
fi
if [ ! -f "$ANDJAR" ]; then
  echo "ERROR: android.jar not found in $SDK/platforms"
  ls -l "$SDK/platforms" || true
  exit 1
fi

# tool suffixes: Windows/Git-Bash uses .exe / .bat; Linux/macOS uses none
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) X=.exe; B=.bat;; *) X=""; B="";; esac
AAPT="$BT/aapt$X"; ZIPALIGN="$BT/zipalign$X"; APKSIGNER="$BT/apksigner$B"; D8="$BT/d8$B"
JAVAC="$JAVA_HOME/bin/javac$X"; KEYTOOL="$JAVA_HOME/bin/keytool$X"; JAR="$JAVA_HOME/bin/jar$X"

VC=$(date +%s)

rm -rf build && mkdir -p build/classes build/dex

echo "== [1/6] javac (release 8) [${APP_NAME}] =="
# Compile the canonical Q50 package directory.
if [ ! -d "src/com/rainwxrks/q50" ]; then
  echo "ERROR: src/com/rainwxrks/q50 not found"
  echo "--- repo layout ---"
  find src -type f 2>/dev/null | head -n 100 || ls -R
  exit 1
fi

JAVA_FILES=$(find src/com/rainwxrks/q50 -name "*.java" -type f)
if [ -z "$JAVA_FILES" ]; then
  echo "ERROR: no .java files found in src/com/rainwxrks/q50"
  find src -type f | head -n 100
  exit 1
fi

echo "  found $(echo "$JAVA_FILES" | wc -l) files"
# shellcheck disable=SC2086
"$JAVAC" --release 8 -g -d build/classes -classpath "$ANDJAR" $JAVA_FILES
echo "  compiled: $(find build/classes -name '*.class' | wc -l) classes"

echo "== [2/6] d8 -> classes.dex (min-api 10) =="
# shellcheck disable=SC2046
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
PY="python"
if ! command -v python >/dev/null 2>&1; then
  if command -v python3 >/dev/null 2>&1; then
    PY="python3"
  fi
fi

if [ -f tools/epk_tool.py ] && [ -f keys/obu_cert.pem ]; then
  if ! $PY tools/epk_tool.py build build/${APK_BASE}.apk build/${APK_BASE}.epk --cert keys/obu_cert.pem; then
    echo "  .epk wrap failed â need Python 3 + 'pip install cryptography'. The APK is still ready."
  fi
else
  echo "  skipped: keys/obu_cert.pem or tools/epk_tool.py missing â APK is ready, but no .epk was produced."
fi

echo ""
echo "== VERIFY [${APP_NAME}] =="
"$AAPT" dump badging build/${APK_BASE}.apk 2>/dev/null | grep -iE "package:|sdkVersion|native-code|launchable" || true
"$JAR" -tf build/${APK_BASE}.apk | grep -viE "META-INF/" | head
echo "DONE -> build/${APK_BASE}.apk"; ls -l build/${APK_BASE}.apk build/${APK_BASE}.epk 2>/dev/null || true
