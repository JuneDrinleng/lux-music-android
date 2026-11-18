"""Verify the official Gradle 8.13 wrapper JAR and distribution checksum offline."""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAR_SHA256 = "81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f"
ZIP_SHA256 = "20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78"
wrapper = ROOT / "gradle/wrapper"
actual = hashlib.sha256((wrapper / "gradle-wrapper.jar").read_bytes()).hexdigest()
if actual != JAR_SHA256:
    raise RuntimeError("Gradle wrapper JAR differs from the official 8.13 checksum")
properties = (wrapper / "gradle-wrapper.properties").read_text()
if "distributionSha256Sum=" + ZIP_SHA256 not in properties:
    raise RuntimeError("Gradle distribution checksum is missing or changed")
if "https\\://services.gradle.org/distributions/gradle-8.13-bin.zip" not in properties:
    raise RuntimeError("Unexpected Gradle distribution URL")
print("Verified official Gradle 8.13 wrapper JAR and distribution checksum")
