"""Guard update-mode exit status across both modules and the final report.

Run the real shell entry point in an isolated tree with a stub task producer;
no Gradle, solver, golden regeneration or repository files are touched.
"""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

SCRIPTS = Path(__file__).resolve().parents[1]


def check(compiler, locality, expected):
    with tempfile.TemporaryDirectory() as directory:
        root = Path(directory)
        scripts = root / "agent-scripts"
        scripts.mkdir()
        for name in ("test.sh", "lib.sh", "junit_counts.py", "junit_first_failure.py"):
            shutil.copyfile(SCRIPTS / name, scripts / name)
        shutil.copytree(SCRIPTS / "tests/fixtures", root / "fixtures")
        (scripts / "check-testdata.sh").write_text("#!/bin/sh\nexit 0\n")
        (scripts / "check-testdata.sh").chmod(0o755)
        # No changed files: exercise the reporting footer without invoking git
        # against the caller's checkout.
        (root / "git").write_text("#!/bin/sh\nexit 0\n")
        (root / "git").chmod(0o755)
        (root / "gradlew").write_text('''#!/usr/bin/env python3
import os
from pathlib import Path
import shutil
import sys
import time
locality = ":locality:" in sys.argv[1]
module = "locality" if locality else "compiler"
with open("calls", "a") as calls:
    calls.write(module + "\\n")
kind = os.environ[module.upper()]
directory = Path("formver.compiler-plugin")
if locality:
    directory /= "locality"
directory /= "build/test-results/test"
directory.mkdir(parents=True)
if kind != "missing":
    result = directory / "result.xml"
    fixture = {"assertion": "assertion-aggregate.xml", "backend": "mixed-aggregate.xml",
               "unreadable": "malformed.xml"}[kind]
    shutil.copyfile(Path("fixtures") / fixture, result)
    future = time.time() + 2
    os.utime(result, (future, future))
print("stub " + module)
sys.exit(1)
''')
        (root / "gradlew").chmod(0o755)
        env = dict(os.environ, COMPILER=compiler, LOCALITY=locality,
                   PATH=str(root) + os.pathsep + os.environ["PATH"],
                   SNAKT_TEST_DUMP_DIR=str(root / "dumps"))
        result = subprocess.run(["bash", str(scripts / "test.sh"), "--update-goldens"],
                                cwd=root, env=env, capture_output=True, text=True)
        assert result.returncode == expected, (compiler, locality, result)
        assert (root / "calls").read_text() == "compiler\nlocality\n", result
        assert "=== golden changes ===" in result.stdout, result
        assert "Regeneration records whatever the run produced" in result.stdout, result
        assert not result.stderr, result


for case in (("assertion", "assertion", 0), ("backend", "assertion", 1),
             ("assertion", "backend", 1), ("assertion", "missing", 1),
             ("assertion", "unreadable", 1)):
    check(*case)
print("update status: 5 module/report scenarios passed")
