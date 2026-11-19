#!/usr/bin/env bash
set -euo pipefail
umask 077
tools="$ANDROID_HOME/build-tools/36.0.0"
keys=$(mktemp -d)
trap 'rm -rf "$keys"' EXIT
printf '%s' "${ANDROID_KEYSTORE_BASE64:?}" | base64 --decode > "$keys/release.p12"
mkdir -p dist
apk="dist/lux-music-android-$VERSION.apk"
"$tools/apksigner" sign --ks "$keys/release.p12" --ks-key-alias "${ANDROID_KEY_ALIAS:?}" \
  --ks-pass env:ANDROID_KEYSTORE_PASSWORD --key-pass env:ANDROID_KEY_PASSWORD \
  --out "$apk" app/build/outputs/apk/release/app-release-unsigned.apk

cert=$("$tools/apksigner" verify --verbose --print-certs "$apk" |
  sed -n 's/^Signer #1 certificate SHA-256 digest: //p')
expected=$(printf '%s' "${ANDROID_SIGNING_CERT_SHA256:?}" | tr -d ':[:space:]' | tr 'A-F' 'a-f')
[[ "$expected" =~ ^[a-f0-9]{64}$ && "$cert" == "$expected" ]]
badging=$("$tools/aapt2" dump badging "$apk")
[[ "$badging" == *"package: name='cn.lux.music.android' versionCode='$CODE' versionName='$VERSION'"* ]]
[[ "$badging" != *application-debuggable* ]]
PYTHONPATH=scripts python3 - "$apk" <<'PY'
import sys
from verify_native import verify
verify(sys.argv[1], {"armeabi-v7a", "arm64-v8a"})
PY
(cd dist && sha256sum "lux-music-android-$VERSION.apk" > SHA256SUMS.txt)
