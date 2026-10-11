#!/usr/bin/env python3
"""Validate the V3SP3R Security Competition Pack.

Run from the pack directory:

    python3 validate_pack.py

Paths resolve relative to this script. Standard library only.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path, PurePosixPath

ROOT = Path(__file__).resolve().parent

# The brief sketched 3–6 entries per category. The named contents are 7 for
# red-badusb (3 catalog + 1 builtin + 3 demo files) and 7 for blue (7 catalog
# apps). Every named entry is required, so the bound is 3–7.
MIN_ENTRIES = 3
MAX_ENTRIES = 7

REQUIRED_TOP = ("id", "name", "version", "generated", "firmware", "categories")
ALLOWED_CATEGORY_IDS = {
    "red-badusb",
    "red-subghz",
    "red-nfc-rfid",
    "red-wifi",
    "red-ir",
    "red-gpio",
    "blue",
}
ALLOWED_PACKS = {"daily", "radio", "edge", "play", "red", "blue"}
CATALOG_SOURCE = "https://catalog.flipperzero.one"
FIRMWARE_NOTE = (
    "Apps resolve from catalog.flipperzero.one pinned to the device-reported "
    "SDK API; Unleashed accepts same-major builds (loader compares API major only)."
)
ALIAS_RE = re.compile(r"[a-z0-9_]+")

DUCKY_COMMANDS = {
    "REM",
    "DELAY",
    "STRING",
    "STRINGLN",
    "GUI",
    "CTRL",
    "ALT",
    "SHIFT",
    "ENTER",
    "TAB",
    "ESCAPE",
    "ESC",
    "SPACE",
    "BACKSPACE",
    "DELETE",
    "HOME",
    "END",
    "INSERT",
    "UPARROW",
    "DOWNARROW",
    "LEFTARROW",
    "RIGHTARROW",
    "UP",
    "DOWN",
    "LEFT",
    "RIGHT",
    "DEFAULT_DELAY",
    "DEFAULTDELAY",
    "REPEAT",
    "MENU",
    "APP",
    "CAPSLOCK",
    "NUMLOCK",
    "SCROLLLOCK",
    "PRINTSCREEN",
    "PAUSE",
    "BREAK",
    "PAGEUP",
    "PAGEDOWN",
    *(f"F{number}" for number in range(1, 13)),
}

# Substrings, matched case-insensitively against the whole file.
FORBIDDEN = (
    "powershell -e",
    " -enc",
    "iex",
    "invoke-",
    "wget ",
    "curl ",
    "bitsadmin",
    "certutil",
    "reg add",
    "netsh",
    "rm -rf",
    "mkfs",
    "format ",
    "del /",
    "rd /s",
    "shutdown",
    ":(){",
    " ncat",
    "reverse shell",
    "meterpreter",
)

CATALOG_KEYS = {"type", "alias", "name", "pack", "needsModule", "role", "source"}
FILE_KEYS = {"type", "path", "target", "role"}
BUILTIN_KEYS = {"type", "name", "role"}
CATEGORY_KEYS = {"id", "title", "entries"}
FIRMWARE_KEYS = {"fork", "sdkApiMajor", "note"}


def nonempty(value: object) -> bool:
    return isinstance(value, str) and value.strip() != ""


def resolve_pack_file(rel: object) -> Path | None:
    if not isinstance(rel, str) or rel == "" or "\x00" in rel:
        return None
    if rel.startswith("/") or rel.startswith("\\") or "\\" in rel:
        return None
    try:
        candidate = (ROOT / rel).resolve()
        candidate.relative_to(ROOT)
    except (OSError, ValueError):
        return None
    return candidate


def lint_ducky(text: str) -> list[str]:
    """Return safety-lint problems for one Ducky Script 1.0 payload."""
    problems: list[str] = []
    lowered = text.lower()
    for needle in FORBIDDEN:
        if needle in lowered:
            problems.append(f"forbidden pattern {needle!r}")

    continued = False
    saw_command = False
    for lineno, raw in enumerate(text.splitlines(), 1):
        is_continuation = continued
        continued = raw.rstrip().endswith("\\")
        if is_continuation:
            continue
        body = raw.strip()
        if body.endswith("\\"):
            body = body[:-1].strip()
        if not body:
            continue
        saw_command = True
        token = body.split()[0]
        if token not in DUCKY_COMMANDS:
            problems.append(
                f"line {lineno}: {token!r} is not a Ducky Script 1.0 command"
            )
    if not saw_command:
        problems.append("no command lines")
    return problems


def lint_badusb_file(path: Path) -> tuple[bool, str]:
    display = path.relative_to(ROOT).as_posix()
    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeError:
        return False, f"{display}: not valid UTF-8"
    except OSError as exc:
        return False, f"{display}: {exc}"
    problems = lint_ducky(text)
    if problems:
        return False, display + ": " + "; ".join(problems)
    return True, f"{display}: Ducky Script whitelist and safety lint"


def check_catalog(entry: dict) -> list[str]:
    problems: list[str] = []
    if set(entry) != CATALOG_KEYS:
        problems.append(
            "keys must be exactly type, alias, name, pack, needsModule, role, source"
        )
    alias = entry.get("alias")
    if not isinstance(alias, str) or ALIAS_RE.fullmatch(alias) is None:
        problems.append("alias must match ^[a-z0-9_]+$")
    if entry.get("pack") not in ALLOWED_PACKS:
        problems.append("pack must be one of daily, radio, edge, play, red, blue")
    if type(entry.get("needsModule")) is not bool:
        problems.append("needsModule must be a bool")
    if not nonempty(entry.get("name")):
        problems.append("name must be a non-empty string")
    if not nonempty(entry.get("role")):
        problems.append("role must be a non-empty string")
    source = entry.get("source")
    if not nonempty(source):
        problems.append("source must be a non-empty string")
    elif source != CATALOG_SOURCE:
        problems.append(f"source must be {CATALOG_SOURCE}")
    return problems


def check_file(entry: dict) -> list[str]:
    problems: list[str] = []
    if set(entry) != FILE_KEYS:
        problems.append("keys must be exactly type, path, target, role")
    if not nonempty(entry.get("role")):
        problems.append("role must be a non-empty string")
    path = entry.get("path")
    resolved = resolve_pack_file(path)
    if resolved is None or not resolved.is_file():
        problems.append("path must exist inside the pack")
    target = entry.get("target")
    if not isinstance(target, str) or not target.startswith("/ext/"):
        problems.append("target must start with /ext/")
    elif isinstance(path, str) and path != "":
        name = PurePosixPath(path.replace("\\", "/")).name
        if name == "" or not target.endswith("/" + name):
            problems.append("target must end with the asset filename")
    return problems


def check_builtin(entry: dict) -> list[str]:
    problems: list[str] = []
    if set(entry) != BUILTIN_KEYS:
        problems.append("keys must be exactly type, name, role")
    if not nonempty(entry.get("name")):
        problems.append("name must be a non-empty string")
    if not nonempty(entry.get("role")):
        problems.append("role must be a non-empty string")
    return problems


def entry_label(category_id: str, entry: object, index: int) -> str:
    if isinstance(entry, dict):
        kind = entry.get("type")
        if kind == "catalog-app" and isinstance(entry.get("alias"), str):
            return f"{category_id}/{entry['alias']} catalog-app"
        if kind == "file" and isinstance(entry.get("path"), str):
            return f"{category_id}/{entry['path']} file"
        if kind == "builtin" and isinstance(entry.get("name"), str):
            return f"{category_id}/{entry['name']} builtin"
        if isinstance(kind, str) and kind != "":
            return f"{category_id} entry {index} ({kind})"
    return f"{category_id} entry {index}"


def check_subghz() -> tuple[bool, str]:
    path = ROOT / "subghz" / "demo_remote_TODO.sub"
    display = "subghz/demo_remote_TODO.sub"
    if not path.is_file():
        return False, f"{display} is missing"
    try:
        text = path.read_text(encoding="utf-8")
    except (OSError, UnicodeError) as exc:
        return False, f"{display}: {exc}"
    problems: list[str] = []
    if "Filetype: Flipper SubGhz Key File" not in text:
        problems.append("missing 'Filetype: Flipper SubGhz Key File'")
    key_lines = [
        line.strip() for line in text.splitlines() if line.strip().startswith("Key:")
    ]
    if not key_lines:
        problems.append("missing a Key: line")
    elif any("DE AD BE EF" not in line for line in key_lines):
        problems.append("Key: line is not the DE AD BE EF placeholder")
    if problems:
        return False, display + ": " + "; ".join(problems)
    return True, f"{display}: placeholder key DE AD BE EF"


def check_ir() -> tuple[bool, str]:
    path = ROOT / "infrared" / "demo_tv_power_TODO.ir"
    display = "infrared/demo_tv_power_TODO.ir"
    if not path.is_file():
        return False, f"{display} is missing"
    try:
        text = path.read_text(encoding="utf-8")
    except (OSError, UnicodeError) as exc:
        return False, f"{display}: {exc}"
    problems: list[str] = []
    if "Filetype: IR signals file" not in text:
        problems.append("missing 'Filetype: IR signals file'")
    if "POWER_DEMO" not in text:
        problems.append("missing POWER_DEMO")
    if problems:
        return False, display + ": " + "; ".join(problems)
    return True, f"{display}: POWER_DEMO placeholder"


def check_identity(data: dict) -> tuple[bool, str]:
    problems: list[str] = []
    expected = {
        "id": "vesper-security-competition",
        "name": "V3SP3R Security Competition Pack",
        "version": "1.0.0",
        "generated": "2026-10-11",
    }
    for key, value in expected.items():
        if data.get(key) != value:
            problems.append(f"{key} must be {value!r}")
    firmware = data.get("firmware")
    if not isinstance(firmware, dict):
        problems.append("firmware must be an object")
    else:
        if set(firmware) != FIRMWARE_KEYS:
            problems.append("firmware keys must be exactly fork, sdkApiMajor, note")
        else:
            if firmware.get("fork") != "unleashed":
                problems.append("firmware.fork must be 'unleashed'")
            major = firmware.get("sdkApiMajor")
            if type(major) is not int or major != 88:
                problems.append("firmware.sdkApiMajor must be the integer 88")
            if firmware.get("note") != FIRMWARE_NOTE:
                problems.append("firmware.note text does not match")
    if problems:
        return False, "manifest identity: " + "; ".join(problems)
    return True, "manifest identity fields match the pack"


def check_categories(categories: list) -> list[tuple[bool, str]]:
    results: list[tuple[bool, str]] = []
    ids: list[object] = []
    parsed: list[tuple[str, dict]] = []
    shape_ok = True
    for index, category in enumerate(categories):
        if not isinstance(category, dict) or set(category) != CATEGORY_KEYS:
            shape_ok = False
            results.append(
                (
                    False,
                    f"category {index}: keys must be exactly id, title, entries",
                )
            )
            continue
        cid = category.get("id")
        if not isinstance(cid, str):
            shape_ok = False
            results.append((False, f"category {index}: id must be a string"))
            continue
        ids.append(cid)
        if not nonempty(category.get("title")):
            shape_ok = False
            results.append((False, f"category {cid}: title must be a non-empty string"))
        parsed.append((cid, category))
    id_ok = set(ids) == ALLOWED_CATEGORY_IDS and len(ids) == len(ALLOWED_CATEGORY_IDS)
    if not shape_ok:
        results.insert(
            0,
            (
                False,
                "category ids: structure problems prevent a full set check",
            ),
        )
    elif id_ok:
        results.insert(0, (True, "category ids are exactly the allowed set"))
    else:
        results.insert(
            0,
            (
                False,
                "category ids must be exactly "
                + ", ".join(sorted(ALLOWED_CATEGORY_IDS)),
            ),
        )

    for cid, category in parsed:
        entries = category.get("entries")
        if not isinstance(entries, list):
            results.append((False, f"category {cid}: entries must be a list"))
            continue
        count = len(entries)
        count_ok = MIN_ENTRIES <= count <= MAX_ENTRIES
        results.append(
            (
                count_ok,
                f"category {cid} has {count} entries (allowed {MIN_ENTRIES}-{MAX_ENTRIES})",
            )
        )
        for index, entry in enumerate(entries, 1):
            label = entry_label(cid, entry, index)
            if not isinstance(entry, dict):
                results.append((False, f"{label}: must be an object"))
                continue
            kind = entry.get("type")
            if kind == "catalog-app":
                problems = check_catalog(entry)
            elif kind == "file":
                problems = check_file(entry)
            elif kind == "builtin":
                problems = check_builtin(entry)
            else:
                problems = ["type must be catalog-app, builtin, or file"]
            if problems:
                results.append((False, label + ": " + "; ".join(problems)))
            else:
                results.append((True, label))
    return results


def check_badusb() -> list[tuple[bool, str]]:
    directory = ROOT / "badusb"
    if not directory.is_dir():
        return [(False, "badusb safety lint: badusb/ is missing")]
    files = sorted(path for path in directory.rglob("*.txt") if path.is_file())
    if not files:
        return [(False, "badusb safety lint: no .txt payloads found")]
    return [lint_badusb_file(path) for path in files]


def load_manifest() -> tuple[dict | None, str]:
    path = ROOT / "manifest.json"
    try:
        raw = path.read_text(encoding="utf-8")
    except OSError as exc:
        return None, f"manifest.json cannot be read ({exc})"
    try:
        data = json.loads(raw)
    except json.JSONDecodeError as exc:
        return None, (
            f"manifest.json is not valid JSON ({exc.msg} at line {exc.lineno})"
        )
    if not isinstance(data, dict):
        return None, "manifest.json must be a JSON object"
    return data, ""


def collect() -> list[tuple[bool, str]]:
    results: list[tuple[bool, str]] = []
    data, error = load_manifest()
    if data is None:
        results.append((False, f"manifest.json parses: {error}"))
        return results
    results.append((True, "manifest.json parses"))

    missing = [key for key in REQUIRED_TOP if key not in data]
    extra = sorted(set(data) - set(REQUIRED_TOP))
    if missing or extra:
        detail = []
        if missing:
            detail.append("missing " + ", ".join(missing))
        if extra:
            detail.append("unexpected " + ", ".join(extra))
        results.append((False, "required top-level keys: " + "; ".join(detail)))
    else:
        results.append(
            (True, "required top-level keys present (id, name, version, generated, firmware, categories)")
        )

    results.append(check_identity(data))

    categories = data.get("categories")
    if not isinstance(categories, list):
        results.append((False, "categories must be a list"))
    else:
        results.extend(check_categories(categories))

    results.extend(check_badusb())
    results.append(check_subghz())
    results.append(check_ir())
    return results


def main() -> int:
    results = collect()
    failed = False
    for ok, reason in results:
        print(("PASS: " if ok else "FAIL: ") + reason)
        failed = failed or not ok
    if not failed and results:
        print("PACK OK")
        return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
