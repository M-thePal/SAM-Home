#!/bin/sh
# Builds out/samhome.apk with the Android SDK's build-tools (no Gradle).
#   sh build.sh
set -e
cd "$(dirname "$0")"

SDK=${ANDROID_HOME:-$HOME/Library/Android/sdk}
BT=$(ls -d "$SDK"/build-tools/* | sort -V | head -1)
JAR=$(ls "$SDK"/platforms/*/android.jar | sort -V | head -1)

rm -rf out; mkdir -p out/classes out/dex out/gen
"$BT/aapt2" compile --dir res -o out/res.zip
"$BT/aapt2" link -o out/base.apk -I "$JAR" --manifest AndroidManifest.xml --java out/gen -A assets \
  --min-sdk-version 21 --target-sdk-version 23 out/res.zip
javac -nowarn -Xlint:-options -encoding UTF-8 -source 8 -target 8 \
  -bootclasspath "$JAR:$BT/core-lambda-stubs.jar" -d out/classes $(find src out/gen -name '*.java')
"$BT/d8" --min-api 21 --release --lib "$JAR" --output out/dex $(find out/classes -name '*.class')
cp out/base.apk out/unsigned.apk; (cd out/dex && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 out/unsigned.apk out/aligned.apk

# Keep key.jks: updates must be signed with the same key.
[ -f key.jks ] || keytool -genkeypair -keystore key.jks -storepass samhome123 -keypass samhome123 \
  -alias samhome -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=samhome" >/dev/null 2>&1
"$BT/apksigner" sign --ks key.jks --ks-pass pass:samhome123 --out out/samhome.apk out/aligned.apk
echo "Built out/samhome.apk"
