"""Exercise the real Gradle boundary task without changing tracked project files."""
import os
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CASES = (
    ('project(":feature").dependencies.add("implementation", project(":data"))',
     "Forbidden dependencies from :feature"),
    ('project(":data").dependencies.add("implementation", project(":playback"))',
     "Forbidden dependencies from :data"),
    ('project(":core:api").dependencies.add("implementation", "androidx.annotation:annotation:1.9.1")',
     "Core API/model cannot depend on Android or infrastructure libraries"),
)

for mutation, expected in CASES:
    with tempfile.TemporaryDirectory(prefix="lux-architecture-") as directory:
        fixture = Path(directory) / "violation.init.gradle"
        fixture.write_text(
            "gradle.projectsEvaluated { if (gradle.rootProject.name == 'LuxMusicAndroid') { "
            "def root = gradle.rootProject; "
            + mutation.replace("project(", "root.project(") + " } }\n"
        )
        command = [str(ROOT / "gradlew"), "architectureCheck", "--console=plain", "-I", str(fixture)]
        proxy_init = os.environ.get("LUX_GRADLE_INIT")
        if proxy_init:
            command += ["-I", proxy_init]
        result = subprocess.run(command, cwd=ROOT, text=True, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, timeout=180)
        if result.returncode == 0 or expected not in result.stdout:
            raise RuntimeError("Architecture negative case failed:\n" + result.stdout)
        print("Rejected:", expected)
