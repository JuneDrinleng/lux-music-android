"""Fetch only the locked test oracle artifact; never install or execute npm scripts."""
import base64
import hashlib
import io
from pathlib import Path
import tarfile
import urllib.request

URL = 'https://registry.npmjs.org/message2call/-/message2call-0.1.3.tgz'
INTEGRITY = 'sha512-jOOPKWTE5LMfuxz8D1o186ex+kJl7FgRz033Z371kvyfZDyCn+qhtrsIvgf4kPUf4zYAWM2SU4GFWbA7F2U+Ag=='
DIST_SHA256 = 'e60f226619e81a0c700aeb72ede193cd14ca5e0d119c7830dd97fb89381b623e'
ROOT = Path(__file__).resolve().parents[2]
TARGET = ROOT / '.contract-upstream/message2call.cjs'
TARGET.parent.mkdir(exist_ok=True)
if not TARGET.exists() or hashlib.sha256(TARGET.read_bytes()).hexdigest() != DIST_SHA256:
    with urllib.request.urlopen(URL, timeout=30) as response:
        archive = response.read(1024 * 1024)
    actual = 'sha512-' + base64.b64encode(hashlib.sha512(archive).digest()).decode()
    if actual != INTEGRITY:
        raise RuntimeError('Published message2call artifact integrity mismatch')
    with tarfile.open(fileobj=io.BytesIO(archive), mode='r:gz') as tar:
        data = tar.extractfile('package/dist/message2call.js').read()
    if hashlib.sha256(data).hexdigest() != DIST_SHA256:
        raise RuntimeError('Published message2call dist hash mismatch')
    TARGET.write_bytes(data)
print('Verified locked message2call 0.1.3 test artifact')
