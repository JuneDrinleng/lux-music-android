"""Build an unchanged, pinned LX deployment copy; original research checkout stays read-only."""
import hashlib
import io
import os
from pathlib import Path
import subprocess
import tarfile

ROOT = Path(__file__).resolve().parents[2]
SHA = 'd47aca4284a7c4d9ef755df1f44fb0b0a5b2af36'
source = Path(os.environ['LUX_LX_SOURCE']).resolve()
actual = subprocess.check_output(['git', '-C', str(source), 'rev-parse', 'HEAD'], text=True).strip()
if actual != SHA:
    raise RuntimeError('LX deployment source revision mismatch')
subprocess.run(['git', '-C', str(source), 'diff', '--quiet', 'HEAD', '--', 'src', 'package-lock.json'], check=True)
target = ROOT / '.contract-upstream/lx-deployment'
target.mkdir(parents=True, exist_ok=True)
archive = subprocess.check_output(['git', '-C', str(source), 'archive', SHA])
with tarfile.open(fileobj=io.BytesIO(archive)) as files:
    files.extractall(target, filter='data')
if not (target / 'node_modules/message2call/dist/message2call.js').exists():
    subprocess.run(['npm', 'ci', '--ignore-scripts', '--no-audit', '--no-fund'], cwd=target, check=True)
subprocess.run(['npm', 'run', 'build'], cwd=target, check=True)
for path in (source / 'src').rglob('*'):
    if path.is_file() and path.read_bytes() != (target / path.relative_to(source)).read_bytes():
        raise RuntimeError('LX deployment source was modified')
dist = target / 'node_modules/message2call/dist/message2call.js'
if hashlib.sha256(dist.read_bytes()).hexdigest() != 'e60f226619e81a0c700aeb72ede193cd14ca5e0d119c7830dd97fb89381b623e':
    raise RuntimeError('LX deployment installed a different message2call artifact')
print('Built unchanged LX', SHA, 'in ignored deployment copy')
