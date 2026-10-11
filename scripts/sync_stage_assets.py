#!/usr/bin/env python3
"""Copy competition payloads into the phone stage assets.

The Stage Remote screen reads these paths from the APK assets. Sources stay
in the packs; this script only copies bytes and writes a sha256 manifest.

Run from the repository root:

    python3 scripts/sync_stage_assets.py

Paths resolve relative to the repository root (the parent of this script).
Standard library only.
"""

from __future__ import annotations

import hashlib
import json
import sys
from datetime import date
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
ASSETS = REPO / "app" / "src" / "main" / "assets"

# (source relative to the repo root, asset path relative to app/src/main/assets)
# Keep in lockstep with competition-pack-live/validate_live_pack.py STAGE_COPIES.
STAGE_COPIES: tuple[tuple[str, str], ...] = (
    (
        "competition-pack/badusb/demo_calc_windows.txt",
        "stage/safe/demo_calc_windows.txt",
    ),
    (
        "competition-pack/badusb/demo_notepad_hello_windows.txt",
        "stage/safe/demo_notepad_hello_windows.txt",
    ),
    (
        "competition-pack/badusb/demo_rickroll_windows.txt",
        "stage/safe/demo_rickroll_windows.txt",
    ),
    (
        "competition-pack-live/payloads/live_wifi_key_reveal_windows.txt",
        "stage/live/live_wifi_key_reveal_windows.txt",
    ),
    (
        "competition-pack-live/payloads/live_wifi_key_reveal_elevated_windows.txt",
        "stage/live/live_wifi_key_reveal_elevated_windows.txt",
    ),
    (
        "competition-pack-live/payloads/live_credential_prompt_windows.txt",
        "stage/live/live_credential_prompt_windows.txt",
    ),
    (
        "competition-pack-live/payloads/live_speech_windows.txt",
        "stage/live/live_speech_windows.txt",
    ),
    (
        "competition-pack-live/payloads/live_hackertyper_windows.txt",
        "stage/live/live_hackertyper_windows.txt",
    ),
    (
        "competition-pack-live/evilportal/index.html",
        "stage/live/evilportal/index.html",
    ),
)


def repo_file(rel: str) -> Path | None:
    if rel == "" or rel.startswith("/") or rel.startswith("\\") or "\\" in rel or "\x00" in rel:
        return None
    try:
        candidate = (REPO / rel).resolve()
        candidate.relative_to(REPO)
    except (OSError, ValueError):
        return None
    return candidate


def asset_file(rel: str) -> Path | None:
    if rel == "" or rel.startswith("/") or rel.startswith("\\") or "\\" in rel or "\x00" in rel:
        return None
    try:
        candidate = (ASSETS / rel).resolve()
        candidate.relative_to(ASSETS)
    except (OSError, ValueError):
        return None
    return candidate


def copy_one(source_rel: str, asset_rel: str) -> str | None:
    """Copy one source onto its asset path. Return an error string, or None."""
    source = repo_file(source_rel)
    dest = asset_file(asset_rel)
    if source is None:
        return f"refusing source path {source_rel}"
    if dest is None:
        return f"refusing asset path {asset_rel}"
    if not source.is_file():
        return f"missing source {source_rel}"
    try:
        data = source.read_bytes()
    except OSError as exc:
        return f"cannot read {source_rel} ({exc})"
    try:
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_bytes(data)
        written = dest.read_bytes()
    except OSError as exc:
        return f"cannot write {asset_rel} ({exc})"
    if written != data:
        return f"byte mismatch after copy {asset_rel}"
    return None


def write_manifest(copied: list[tuple[str, bytes]]) -> str | None:
    manifest_path = asset_file("stage/manifest.json")
    if manifest_path is None:
        return "refusing asset path stage/manifest.json"
    files = {
        asset_rel: hashlib.sha256(data).hexdigest() for asset_rel, data in copied
    }
    manifest = {"synced": date.today().isoformat(), "files": files}
    payload = json.dumps(manifest, indent=2) + "\n"
    try:
        manifest_path.write_text(payload, encoding="utf-8")
        loaded = json.loads(manifest_path.read_text(encoding="utf-8"))
    except OSError as exc:
        return f"cannot write stage/manifest.json ({exc})"
    except json.JSONDecodeError as exc:
        return (
            "stage/manifest.json is not valid JSON after write "
            f"({exc.msg} at line {exc.lineno})"
        )
    if not isinstance(loaded, dict) or loaded.get("files") != files:
        return "stage/manifest.json hashes do not match copied bytes"
    if loaded.get("synced") != manifest["synced"]:
        return "stage/manifest.json synced date does not match"
    return None


def main() -> int:
    problems: list[str] = []
    copied: list[tuple[str, bytes]] = []
    for source_rel, asset_rel in STAGE_COPIES:
        problem = copy_one(source_rel, asset_rel)
        if problem is not None:
            problems.append(problem)
            continue
        dest = asset_file(asset_rel)
        if dest is None:
            problems.append(f"refusing asset path {asset_rel}")
            continue
        try:
            copied.append((asset_rel, dest.read_bytes()))
        except OSError as exc:
            problems.append(f"cannot re-read {asset_rel} ({exc})")
            continue
        print(f"copied {asset_rel}")

    if problems or len(copied) != len(STAGE_COPIES):
        for problem in problems:
            print(f"FAIL: {problem}", file=sys.stderr)
        return 1

    problem = write_manifest(copied)
    if problem is not None:
        print(f"FAIL: {problem}", file=sys.stderr)
        return 1

    print("STAGE ASSETS SYNCED")
    return 0


if __name__ == "__main__":
    sys.exit(main())
