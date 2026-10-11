# V3SP3R Security Competition Pack — LIVE

**LAB USE ONLY.** This directory is the live-fire layer of the competition
pack. It exists only on the `unsafe` branch, stacked on top of the full-dummy
version (`competition-pack/`, branch `safe-test`). Everything here runs real
attacks against lab-owned equipment for the demo day.

## Gate before anything runs

Complete the authorization gate in `docs/competition-live-runbook.md` first:
written authorization for every target device, an isolated lab network, a
sacrificial laptop, and the restore checklist staged. No gate, no demo.

## Contents

| Path | What it is |
| --- | --- |
| `payloads/live_wifi_key_reveal_windows.txt` | BadUSB: `netsh ... key=clear` prints the saved lab WiFi password on the laptop screen. Display only; `LAB_SSID_HERE` placeholder must be edited first. |
| `payloads/live_wifi_key_reveal_elevated_windows.txt` | Elevated fallback of the same reveal for Windows builds that demand admin rights for `key=clear` (UAC consent via keyboard; needs a local-admin demo account). Rehearse both, keep the one that works. |
| `payloads/live_credential_prompt_windows.txt` | BadUSB: fake `LabGuest WiFi` sign-in prompt; shows only the typed username. The password is discarded by construction. |
| `payloads/live_speech_windows.txt` | BadUSB: the laptop speaks its own lesson via local Windows text-to-speech (System.Speech). No network, no files. Theatrical beat. |
| `payloads/live_hackertyper_windows.txt` | BadUSB: opens hackertyper.net fullscreen. Comic beat, zero impact. |
| `evilportal/index.html` | Captive-portal phish demo page for the Marauder evil portal on the lab AP. Test credentials only; captures land on the lab SD card and are deleted on stage. |
| `manifest.json` | Live manifest (categories `red-badusb-live`, `red-wifi-live`). |
| `validate_live_pack.py` | Mechanical safety validator for this pack. |

## Hard rules (enforced by `validate_live_pack.py`)

- No exfiltration: nothing typed, captured, or revealed may leave the lab
  device (no network calls, redirects, file writes, or messaging).
- No downloads, no encoded commands, no persistence, no registry/scheduled
  task/service changes, no destructive commands, no hidden windows.
- The netsh payloads (plain and elevated fallback) fire only after
  `LAB_SSID_HERE` is replaced with the lab SSID, and they are display-only.
- The credential prompt discards the password by construction; only the
  username is echoed.
- URLs in payloads are allowlisted; today the only allowed URL is
  `https://hackertyper.net`.
- Deauth runs at most 30 seconds against the lab AP on the isolated lab
  network; stop conditions are in the live runbook.
- Live payloads must differ byte-for-byte from the dummy payloads in
  `competition-pack/badusb/` (the validator checks this so the two packs can
  never silently converge).

## Verify

Run from this directory:

```
python3 validate_live_pack.py
```

Expect `PACK LIVE OK`. The dummy pack in `../competition-pack/` keeps its own
validator (`validate_pack.py`); both must pass on this branch.
