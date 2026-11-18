"""Check the actual APK's ABI set, ELF PT_LOAD alignment and ZIP page alignment."""
from pathlib import Path
import os
import struct
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
APK = ROOT / 'app/build/outputs/apk/debug/app-debug.apk'
EXPECTED = {'arm64-v8a', 'armeabi-v7a', 'x86_64'}


def check_elf(data):
    if data[:4] != b'\x7fELF' or data[5] != 1:
        raise RuntimeError('Expected little-endian ELF')
    if data[4] == 2:
        offset = struct.unpack_from('<Q', data, 32)[0]
        size, count = struct.unpack_from('<HH', data, 54)
        layout = '<IIQQQQQQ'
    elif data[4] == 1:
        offset = struct.unpack_from('<I', data, 28)[0]
        size, count = struct.unpack_from('<HH', data, 42)
        layout = '<IIIIIIII'
    else:
        raise RuntimeError('Unknown ELF class')
    loads = 0
    for index in range(count):
        header = struct.unpack_from(layout, data, offset + size * index)
        if header[0] != 1:
            continue
        loads += 1
        alignment = header[7]
        file_offset, virtual = (header[2], header[3]) if data[4] == 2 else (header[1], header[2])
        if alignment < 16384 or alignment & (alignment - 1) or file_offset % alignment != virtual % alignment:
            raise RuntimeError('PT_LOAD is incompatible with 16 KB pages')
    if not loads:
        raise RuntimeError('No ELF load segments')


def main():
    with zipfile.ZipFile(APK) as apk:
        libs = [entry for entry in apk.infolist() if entry.filename.startswith('lib/') and entry.filename.endswith('.so')]
        if {entry.filename.split('/')[1] for entry in libs} != EXPECTED:
            raise RuntimeError('APK ABI set differs from the architecture contract')
        for entry in libs:
            if entry.compress_type != zipfile.ZIP_STORED:
                raise RuntimeError('Native library must be stored uncompressed')
            check_elf(apk.read(entry))
            print('16 KB ELF:', entry.filename)
        if any('mobile-user-api-preload' in name for name in apk.namelist()):
            raise RuntimeError('Test-only mobile preload leaked into production APK')
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not sdk:
        sdk = next(line.removeprefix('sdk.dir=') for line in (ROOT / 'local.properties').read_text().splitlines() if line.startswith('sdk.dir='))
    subprocess.run([str(Path(sdk) / 'build-tools/36.0.0/zipalign'), '-c', '-P', '16', '4', str(APK)], check=True)
    print('APK ZIP alignment and required ABI set verified; runtime device tests remain separate')


if __name__ == '__main__':
    main()
