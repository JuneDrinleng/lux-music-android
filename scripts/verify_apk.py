"""Check the built artifact, not just its Gradle configuration."""
import json
import os
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sdk_root = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
if not sdk_root:
    local = ROOT / "local.properties"
    if local.exists():
        sdk_root = next((line.split("=", 1)[1] for line in local.read_text().splitlines()
                         if line.startswith("sdk.dir=")), None)
if not sdk_root:
    raise RuntimeError("Set ANDROID_HOME or sdk.dir in local.properties")
SDK = Path(sdk_root)
APK = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
TOOLS = SDK / "build-tools/36.0.0"
result = subprocess.run([str(TOOLS / "aapt2"), "dump", "badging", str(APK)],
                        check=True, stdout=subprocess.PIPE, text=True).stdout
metadata = json.loads(APK.with_name("output-metadata.json").read_text())
if len(metadata["elements"]) != 1:
    raise RuntimeError("Expected one debug APK")
artifact = metadata["elements"][0]
for expected in ("name='cn.lux.music.android.debug'", "minSdkVersion:'33'", "targetSdkVersion:'36'", "application-debuggable",
                 "versionCode='" + str(artifact["versionCode"]) + "'",
                 "versionName='" + artifact["versionName"] + "'"):
    if expected not in result:
        raise RuntimeError("Unexpected APK metadata: " + expected)
subprocess.run([str(TOOLS / "apksigner"), "verify", str(APK)], check=True)
print("Verified: debug package, API 33/36 and APK signature")
