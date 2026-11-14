"""Run isolated QuickJS tests on a real Android emulator; fail on boot/page-size drift."""
from pathlib import Path
import os
import subprocess
import sys
import time

api = sys.argv[1]
image = sys.argv[2]
page_size = sys.argv[3]
revision = sys.argv[4]
sdk = Path(os.environ['ANDROID_HOME'])
cli = sdk / 'cmdline-tools/19.0/bin'
package = f'system-images;android-{api};{image};x86_64'
avd_home = Path(os.environ.get('RUNNER_TEMP', '/tmp')) / 'lux-source-avd'
avd_home.mkdir(parents=True, exist_ok=True)
device_env = dict(os.environ, ANDROID_AVD_HOME=str(avd_home), ANDROID_USER_HOME=str(avd_home.parent / 'lux-android-user'))
subprocess.run([str(cli / 'sdkmanager'), '--sdk_root=' + str(sdk), 'emulator', package], input='y\n' * 50, text=True, check=True, env=device_env)
for properties, expected in [
    (sdk / 'emulator/source.properties', '37.2.13'),
    (sdk / f'system-images/android-{api}/{image}/x86_64/source.properties', revision),
]:
    actual = dict(line.split('=', 1) for line in properties.read_text().splitlines() if '=' in line)
    if actual['Pkg.Revision'].strip() != expected:
        raise RuntimeError('SDK emulator/image revision changed; review and pin the new package')
subprocess.run([str(cli / 'avdmanager'), 'create', 'avd', '--force', '--name', 'lux-source', '--package', package, '--device', 'pixel_5', '--path', str(avd_home / 'lux-source.avd')], input='no\n', text=True, check=True, env=device_env)
if not (avd_home / 'lux-source.ini').is_file():
    raise RuntimeError('AVD creation did not produce the expected instance')
adb = str(sdk / 'platform-tools/adb')
serial = 'emulator-5554'


def device(*args, **kwargs):
    return subprocess.run([adb, '-s', serial, *args], timeout=20, **kwargs)


reports = Path('source-runtime/build/device-evidence')
reports.mkdir(parents=True, exist_ok=True)
with (reports / 'emulator.log').open('w') as log:
    emulator = subprocess.Popen([
        str(sdk / 'emulator/emulator'), '-avd', 'lux-source', '-port', '5554',
        '-no-window', '-no-audio', '-no-boot-anim', '-no-snapshot', '-no-metrics',
        '-gpu', 'swiftshader_indirect', '-accel', 'on', '-memory', '3072',
    ], stdout=log, stderr=subprocess.STDOUT, env=device_env)
    try:
        subprocess.run([adb, 'start-server'], check=True)
        deadline = time.monotonic() + 420
        while time.monotonic() < deadline:
            if emulator.poll() is not None:
                raise RuntimeError('Emulator exited before boot')
            try:
                boot = device('shell', 'getprop', 'sys.boot_completed', capture_output=True, text=True)
            except subprocess.TimeoutExpired:
                continue
            if boot.returncode == 0 and boot.stdout.strip() == '1':
                break
            time.sleep(2)
        else:
            raise RuntimeError('Android boot timeout')
        actual = device('shell', 'getconf', 'PAGE_SIZE', check=True, capture_output=True, text=True).stdout.strip()
        if actual != page_size:
            raise RuntimeError(f'Expected {page_size} byte pages, got {actual}')
        (reports / 'device.txt').write_text(f'API={api}\nimage={package}\nrevision={revision}\nPAGE_SIZE={actual}\n')
        device('shell', 'input', 'keyevent', '82', check=True)
        subprocess.run([
            './gradlew', ':source-runtime:connectedDebugAndroidTest', '--console=plain',
            '-Pandroid.testInstrumentationRunnerArguments.expectedPageSize=' + page_size,
        ], check=True, timeout=900)
    except Exception:
        log.flush()
        print((reports / 'emulator.log').read_text()[-8000:])
        raise
    finally:
        try:
            with (reports / 'logcat.txt').open('w') as output:
                if emulator.poll() is None:
                    device('logcat', '-d', '-t', '4000', stdout=output, stderr=subprocess.DEVNULL)
        except subprocess.TimeoutExpired:
            print('Diagnostic logcat timed out')
        finally:
            if emulator.poll() is None:
                try:
                    device('emu', 'kill', stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                except subprocess.TimeoutExpired:
                    print('Emulator console shutdown timed out')
                finally:
                    emulator.terminate()
                    try:
                        emulator.wait(timeout=20)
                    except subprocess.TimeoutExpired:
                        emulator.kill()
