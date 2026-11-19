"""Allocate a monotonic Android release slot from immutable Git tags."""
import os
import re
import subprocess
from pathlib import Path


def git(*args):
    return subprocess.check_output(["git", *args], text=True).strip()


def plan(base, branch, sha):
    pattern = r"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)"
    match = re.fullmatch(pattern, base)
    if not match or branch not in {"main", "dev"}:
        raise ValueError("Invalid version or release branch")
    version = tuple(map(int, match.groups()))
    major, minor, patch = version
    if major > 209 or minor > 99 or patch > 99:
        raise ValueError("Version exceeds Android versionCode range")
    slots = {}
    for tag in git("tag", "--list", "v*").splitlines():
        parsed = re.fullmatch("v" + pattern + r"(?:-dev\.([1-9]\d*))?", tag)
        if not parsed:
            continue
        tag_version = tuple(map(int, parsed.groups()[:3]))
        if tag_version > version:
            raise ValueError("A newer version has already been reserved")
        if tag_version == version:
            slot = int(parsed[4]) if parsed[4] else 999
            if parsed[4] and slot >= 999:
                raise ValueError("Invalid development slot")
            slots[slot] = tag
    if branch == "main":
        slot = 999
        if slot in slots and git("rev-parse", slots[slot] + "^{}") != sha:
            return None
    else:
        if 999 in slots:
            raise ValueError("Increment stableVersion before the next dev release")
        last = max(slots, default=0)
        slot = last if last and git("rev-parse", slots[last] + "^{}") == sha else last + 1
        if slot >= 999:
            raise ValueError("Development slots exhausted")
    name = base if slot == 999 else f"{base}-dev.{slot}"
    return {"tag": "v" + name, "slot": slot, "version": name,
            "code": ((major * 100 + minor) * 100 + patch) * 1000 + slot}


if __name__ == "__main__":
    base = re.search(r"^stableVersion=(\S+)$", Path("version.properties").read_text(), re.M)[1]
    result = plan(base, os.environ["GITHUB_REF_NAME"], os.environ["GITHUB_SHA"])
    with open(os.environ["GITHUB_OUTPUT"], "a") as output:
        for key, value in (result or {}).items():
            print(f"{key}={value}", file=output)
