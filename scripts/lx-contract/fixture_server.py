"""Lifetime of an original LX server plus a separate test-only proxy/protocol peer."""
from contextlib import contextmanager
import os
from pathlib import Path
import subprocess
import tempfile
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[2]


def wait_for(url, process):
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    for _ in range(100):
        if process.poll() is not None:
            raise RuntimeError('Fixture process exited before readiness')
        try:
            with opener.open(url, timeout=1) as response:
                if response.status == 200:
                    return
        except OSError:
            time.sleep(0.1)
    raise RuntimeError('Fixture startup timeout')


@contextmanager
def deployment():
    reports = ROOT / 'data/build/device-evidence'
    reports.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='lux-lx-fixture-') as directory:
        fixture_env = dict(os.environ, PORT='19527', BIND_IP='127.0.0.1', DATA_PATH=directory,
                           LX_USER_fixture_a='public-fixture-a', LX_USER_fixture_b='public-fixture-b')
        processes = []
        with (reports / 'lx-server.log').open('w') as server_log, (reports / 'integration-host.log').open('w') as host_log:
            try:
                server = subprocess.Popen(['node', 'index.js'], cwd=ROOT / '.contract-upstream/lx-deployment',
                                          env=fixture_env, stdout=server_log, stderr=subprocess.STDOUT)
                processes.append(server)
                wait_for('http://127.0.0.1:19527/hello', server)
                host = subprocess.Popen(['node', 'scripts/lx-contract/integration-host.mjs'], cwd=ROOT,
                                        stdout=host_log, stderr=subprocess.STDOUT)
                processes.append(host)
                wait_for('http://127.0.0.1:19529/stats', host)
                yield
            finally:
                for process in reversed(processes):
                    process.terminate()
                    try:
                        process.wait(timeout=10)
                    except subprocess.TimeoutExpired:
                        process.kill()
                        process.wait(timeout=5)


if __name__ == '__main__':
    try:
        with deployment():
            print('Original LX fixture running on 19527; proxy 19528; control 19529; TLS 19530', flush=True)
            while True:
                time.sleep(1)
    except KeyboardInterrupt:
        pass
