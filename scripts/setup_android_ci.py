"""Bootstrap the pinned Linux CI SDK without assuming sdkmanager is on PATH."""
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import urllib.request
import zipfile

CLI_URL = 'https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip'
CLI_SHA256 = '7ec965280a073311c339e571cd5de778b9975026cfcbe79f2b1cdcb1e15317ee'
runtime = Path(os.environ.get('RUNNER_TEMP', tempfile.gettempdir()))
sdk = Path(os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT') or runtime / 'lux-android-sdk')
sdk.mkdir(parents=True, exist_ok=True)
cli = sdk / 'cmdline-tools/19.0'
manager = cli / 'bin/sdkmanager'
if not manager.exists():
    with tempfile.TemporaryDirectory(prefix='lux-sdk-cli-', dir=runtime) as directory:
        temporary = Path(directory)
        archive = temporary / 'commandline.zip'
        digest = hashlib.sha256()
        with urllib.request.urlopen(CLI_URL, timeout=120) as response, archive.open('wb') as output:
            while chunk := response.read(1024 * 1024):
                digest.update(chunk)
                output.write(chunk)
        if digest.hexdigest() != CLI_SHA256:
            raise RuntimeError('Official Android command-line tools checksum mismatch')
        with zipfile.ZipFile(archive) as package:
            package.extractall(temporary / 'extracted')
        cli.parent.mkdir(parents=True, exist_ok=True)
        shutil.copytree(temporary / 'extracted/cmdline-tools', cli, dirs_exist_ok=True)
        for executable in (cli / 'bin').iterdir():
            executable.chmod(executable.stat().st_mode | 0o111)
subprocess.run(
    [str(manager), '--sdk_root=' + str(sdk), 'platforms;android-36', 'build-tools;36.0.0'],
    input='y\n' * 50, text=True, check=True,
)
if not (sdk / 'platforms/android-36/android.jar').is_file() or not (sdk / 'build-tools/36.0.0/aapt2').is_file():
    raise RuntimeError('Android SDK installation incomplete; check license or download errors')
if os.environ.get('GITHUB_ENV'):
    with Path(os.environ['GITHUB_ENV']).open('a') as output:
        output.write('ANDROID_HOME=' + str(sdk) + '\nANDROID_SDK_ROOT=' + str(sdk) + '\n')
if os.environ.get('GITHUB_PATH'):
    with Path(os.environ['GITHUB_PATH']).open('a') as output:
        output.write(str(cli / 'bin') + '\n' + str(sdk / 'platform-tools') + '\n')
print('Verified Android API 36 and build-tools 36.0.0; command-line tools 19.0')
