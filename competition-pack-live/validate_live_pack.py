#!/usr/bin/env python3
"""Validate the V3SP3R Security Competition Pack - LIVE.

This validator is the mechanical half of the live-pack safety story. It
enforces the hard rules from README.md: no exfiltration, no downloads, no
persistence, no destructive or stealthy commands, filename-scoped netsh and
powershell usage, an URL allowlist, the LAB_SSID_HERE placeholder, and a
byte-level guard that keeps the live payloads distinct from the dummy pack.

Run from the pack directory:

    python3 validate_live_pack.py

Paths resolve relative to this script. Standard library only.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path, PurePosixPath

ROOT = Path(__file__).resolve().parent
SAFE_BADUSB = ROOT.parent / "competition-pack" / "badusb"

MIN_ENTRIES = 3
MAX_ENTRIES = 7

REQUIRED_TOP = (
    "id",
    "name",
    "version",
    "generated",
    "firmware",
    "authorization",
    "categories",
)
ALLOWED_CATEGORY_IDS = {"red-badusb-live", "red-wifi-live"}
ALLOWED_PACKS = {"daily", "radio", "edge", "play", "red", "blue"}
CATALOG_SOURCE = "https://catalog.flipperzero.one"
FIRMWARE_NOTE = (
    "Apps resolve from catalog.flipperzero.one pinned to the device-reported "
    "SDK API; Unleashed accepts same-major builds (loader compares API major only)."
)
RUNBOOK_REF = "docs/competition-live-runbook.md"
ALIAS_RE = re.compile(r"[a-z0-9_]+")
URL_RE = re.compile(r"https?://[^\s\"')<>]+")
FORM_ACTION_RE = re.compile(r'action="([^"]*)"')

WIFI_PAYLOAD = "live_wifi_key_reveal_windows.txt"
CRED_PAYLOAD = "live_credential_prompt_windows.txt"
LAB_SSID_PLACEHOLDER = 'name="LAB_SSID_HERE"'
NETSH_LINE = 'STRING netsh wlan show profile name="LAB_SSID_HERE" key=clear'
DISCARD_NOTE = "(password was discarded, never stored or sent)"

ALLOWED_URLS = {"https://hackertyper.net"}

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

# Live payloads permit netsh (scoped to the WiFi payload) and powershell
# (scoped to the credential payload). Everything below is banned everywhere:
# exfiltration, downloads, encoded commands, persistence, tampering,
# destruction, stealth windows, and credential-dumping primitives.
FORBIDDEN = (
    "powershell -e",
    " -enc",
    "-encodedcommand",
    "frombase64",
    "downloadstring",
    "downloadfile",
    "downloaddata",
    "invoke-webrequest",
    "invoke-restmethod",
    "invoke-expression",
    "invoke-mimikatz",
    "iex(",
    " iex ",
    "start-bitstransfer",
    "bitsadmin",
    "certutil",
    "wget ",
    "curl ",
    "net user",
    "net localgroup",
    "net.exe",
    "reg add",
    "reg delete",
    "regedit",
    "schtasks",
    "new-service",
    "set-service",
    "set-itemproperty",
    "new-itemproperty",
    "new-item ",
    "add-mppreference",
    "set-mppreference",
    "remove-item",
    "rm -rf",
    "del /",
    "rd /s",
    "erase ",
    "format ",
    "shutdown",
    "restart-computer",
    "stop-computer",
    "takeown",
    "icacls",
    "attrib",
    "out-file",
    "set-content",
    "add-content",
    "-windowstyle hidden",
    "-w hidden",
    "getnetworkcredential",
    "sekurlsa",
    "mimikatz",
    "meterpreter",
    "reverse shell",
    "ncat",
    "nc -e",
    "nc.exe",
    "netcat",
    "mailto:",
    "ftp://",
    "tftp",
    "-ep bypass",
    "-executionpolicy bypass",
    "set-executionpolicy",
    "amsi",
    "start-process",
    "rundll32",
    "mshta",
    "wmic",
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


def read_payload(path: Path) -> tuple[str | None, str]:
    display = path.relative_to(ROOT).as_posix()
    try:
        return path.read_text(encoding="utf-8"), ""
    except UnicodeError:
        return None, f"{display}: not valid UTF-8"
    except OSError as exc:
        return None, f"{display}: {exc}"


def command_lines(text: str) -> list[str]:
    """Non-REM, non-blank Ducky lines (the lines that actually execute)."""
    lines: list[str] = []
    for raw in text.splitlines():
        body = raw.strip()
        if body and not body.startswith("REM"):
            lines.append(body)
    return lines


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


def check_identity(data: dict) -> tuple[bool, str]:
    problems: list[str] = []
    expected = {
        "id": "vesper-security-competition-live",
        "name": "V3SP3R Security Competition Pack - LIVE",
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
    authorization = data.get("authorization")
    if not nonempty(authorization):
        problems.append("authorization must be a non-empty string")
    elif RUNBOOK_REF not in authorization:
        problems.append(f"authorization must reference {RUNBOOK_REF}")
    if problems:
        return False, "manifest identity: " + "; ".join(problems)
    return True, "manifest identity fields match the live pack"


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
        results.insert(0, (True, "category ids are exactly the live allowed set"))
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


def manifest_file_paths(data: dict) -> set[str]:
    paths: set[str] = set()
    for category in data.get("categories", []):
        if not isinstance(category, dict):
            continue
        for entry in category.get("entries", []):
            if isinstance(entry, dict) and entry.get("type") == "file":
                path = entry.get("path")
                if isinstance(path, str):
                    paths.add(path)
    return paths


def disk_asset_paths() -> set[str]:
    paths: set[str] = set()
    for subdir, pattern in (("payloads", "*.txt"), ("evilportal", "*.html")):
        directory = ROOT / subdir
        if directory.is_dir():
            for path in directory.rglob(pattern):
                if path.is_file():
                    paths.add(path.relative_to(ROOT).as_posix())
    return paths


def check_manifest_disk_sync(data: dict) -> tuple[bool, str]:
    listed = manifest_file_paths(data)
    on_disk = disk_asset_paths()
    missing_on_disk = sorted(listed - on_disk)
    missing_in_manifest = sorted(on_disk - listed)
    problems: list[str] = []
    if missing_on_disk:
        problems.append("listed but absent: " + ", ".join(missing_on_disk))
    if missing_in_manifest:
        problems.append("on disk but unlisted: " + ", ".join(missing_in_manifest))
    if problems:
        return False, "manifest/disk sync: " + "; ".join(problems)
    return True, "manifest lists exactly the payload and portal files on disk"


def check_payloads() -> list[tuple[bool, str]]:
    directory = ROOT / "payloads"
    if not directory.is_dir():
        return [(False, "payload safety lint: payloads/ is missing")]
    files = sorted(path for path in directory.rglob("*.txt") if path.is_file())
    if not files:
        return [(False, "payload safety lint: no .txt payloads found")]

    results: list[tuple[bool, str]] = []
    texts: dict[str, str] = {}
    for path in files:
        display = path.relative_to(ROOT).as_posix()
        text, error = read_payload(path)
        if text is None:
            results.append((False, error))
            continue
        texts[path.name] = text
        problems = lint_ducky(text)
        if "LAB USE ONLY" not in text:
            problems.append("missing 'LAB USE ONLY' banner")
        if problems:
            results.append((False, display + ": " + "; ".join(problems)))
        else:
            results.append(
                (True, f"{display}: Ducky whitelist, forbidden scan, LAB banner")
            )

    for name, text in texts.items():
        display = f"payloads/{name}"
        urls = set(URL_RE.findall(text))
        bad = urls - ALLOWED_URLS
        if bad:
            results.append(
                (False, f"{display}: URLs outside allowlist: " + ", ".join(sorted(bad)))
            )
        else:
            shown = ", ".join(sorted(urls)) if urls else "none"
            results.append((True, f"{display}: URL allowlist ({shown})"))

    for name, text in texts.items():
        display = f"payloads/{name}"
        lines = command_lines(text)
        if name == WIFI_PAYLOAD:
            netsh_lines = [line for line in lines if "netsh" in line.lower()]
            if netsh_lines != [NETSH_LINE]:
                results.append(
                    (
                        False,
                        f"{display}: the only netsh command line must be exactly "
                        f"{NETSH_LINE!r}",
                    )
                )
            elif LAB_SSID_PLACEHOLDER not in text:
                results.append(
                    (False, f"{display}: LAB_SSID_HERE placeholder was replaced")
                )
            else:
                results.append(
                    (True, f"{display}: netsh scoped to placeholder lab SSID")
                )
        elif "netsh" in text.lower():
            results.append((False, f"{display}: netsh is only allowed in {WIFI_PAYLOAD}"))

        if name == CRED_PAYLOAD:
            ps_lines = [line for line in lines if "powershell" in line.lower()]
            if len(ps_lines) != 1 or not ps_lines[0].startswith(
                "STRING powershell -NoProfile -Command "
            ):
                results.append(
                    (False, f"{display}: powershell must be a single visible command")
                )
            elif "get-credential" not in ps_lines[0].lower():
                results.append((False, f"{display}: missing Get-Credential prompt"))
            elif "getnetworkcredential" in ps_lines[0].lower():
                results.append(
                    (False, f"{display}: plaintext password extraction is banned")
                )
            elif DISCARD_NOTE not in ps_lines[0]:
                results.append(
                    (False, f"{display}: missing on-screen password-discard note")
                )
            else:
                results.append(
                    (True, f"{display}: credential prompt discards the password")
                )
        elif "powershell" in text.lower():
            results.append(
                (False, f"{display}: powershell is only allowed in {CRED_PAYLOAD}")
            )
    return results


def check_cross_pack_guard() -> tuple[bool, str]:
    live_dir = ROOT / "payloads"
    if not live_dir.is_dir():
        return False, "cross-pack guard: payloads/ is missing"
    if not SAFE_BADUSB.is_dir():
        return False, f"cross-pack guard: {SAFE_BADUSB} is missing"
    live_files = sorted(live_dir.rglob("*.txt"))
    dummy_bytes = set()
    for path in SAFE_BADUSB.rglob("*.txt"):
        if path.is_file():
            try:
                dummy_bytes.add(path.read_bytes())
            except OSError:
                continue
    collisions = [
        path.relative_to(ROOT).as_posix()
        for path in live_files
        if path.read_bytes() in dummy_bytes
    ]
    if collisions:
        return False, "cross-pack guard: identical to dummy pack: " + ", ".join(collisions)
    return True, "cross-pack guard: live payloads differ from every dummy payload"


def check_evilportal() -> list[tuple[bool, str]]:
    path = ROOT / "evilportal" / "index.html"
    display = "evilportal/index.html"
    results: list[tuple[bool, str]] = []
    if not path.is_file():
        return [(False, f"{display} is missing")]
    try:
        text = path.read_text(encoding="utf-8")
    except (OSError, UnicodeError) as exc:
        return [(False, f"{display}: {exc}")]

    problems: list[str] = []
    if "LAB USE ONLY" not in text:
        problems.append("missing 'LAB USE ONLY' marker")
    if "http://" in text or "https://" in text:
        problems.append("contains external URLs (all resources must be inline)")
    if "<script" in text.lower():
        problems.append("contains <script> (not allowed)")
    if "<iframe" in text.lower():
        problems.append("contains <iframe> (not allowed)")
    if 'type="password"' not in text:
        problems.append("missing a password input")
    actions = FORM_ACTION_RE.findall(text)
    if not actions:
        problems.append("missing a form action")
    elif any(not action.startswith("/") for action in actions):
        problems.append("form action must be a relative path on the lab AP")
    if "Internal security demo" not in text:
        problems.append("missing the visible 'Internal security demo' footer")
    if problems:
        results.append((False, display + ": " + "; ".join(problems)))
    else:
        results.append(
            (True, f"{display}: inline-only, relative form, demo markers present")
        )

    readme = ROOT / "evilportal" / "README.md"
    if not readme.is_file():
        results.append((False, "evilportal/README.md is missing"))
    else:
        try:
            readme_text = readme.read_text(encoding="utf-8")
        except (OSError, UnicodeError) as exc:
            results.append((False, f"evilportal/README.md: {exc}"))
        else:
            if "LAB USE ONLY" in readme_text:
                results.append((True, "evilportal/README.md carries the lab banner"))
            else:
                results.append(
                    (False, "evilportal/README.md: missing 'LAB USE ONLY' banner")
                )
    return results


def check_readme() -> tuple[bool, str]:
    path = ROOT / "README.md"
    if not path.is_file():
        return False, "README.md is missing"
    try:
        text = path.read_text(encoding="utf-8")
    except (OSError, UnicodeError) as exc:
        return False, f"README.md: {exc}"
    if "LAB USE ONLY" not in text:
        return False, "README.md: missing 'LAB USE ONLY' banner"
    if RUNBOOK_REF not in text:
        return False, f"README.md: must reference {RUNBOOK_REF}"
    return True, "README.md carries the lab banner and runbook gate"


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
            (
                True,
                "required top-level keys present (id, name, version, generated, "
                "firmware, authorization, categories)",
            )
        )

    results.append(check_identity(data))

    categories = data.get("categories")
    if not isinstance(categories, list):
        results.append((False, "categories must be a list"))
    else:
        results.extend(check_categories(categories))

    results.append(check_manifest_disk_sync(data))
    results.extend(check_payloads())
    results.append(check_cross_pack_guard())
    results.extend(check_evilportal())
    results.append(check_readme())
    return results


def main() -> int:
    results = collect()
    failed = False
    for ok, reason in results:
        print(("PASS: " if ok else "FAIL: ") + reason)
        failed = failed or not ok
    if not failed and results:
        print("PACK LIVE OK")
        return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
