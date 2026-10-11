# SAFETY MODE: FULL DUMMY

This branch (`safe-test`) is the **fully safe** version of the V3SP3R Security
Competition Pack. Everything here is inert by construction:

- **BadUSB payloads** open Calculator, Notepad with a friendly message, or a
  browser rickroll. Nothing destructive, nothing that reads or changes data.
- **Sub-GHz / IR captures** are clearly fake placeholders (`DE AD BE EF` key,
  `POWER_DEMO` pattern) that cannot actuate anything. Replace them with
  own-device captures on the lab SD card only.
- **NFC** content is documentation only. No badge dumps, no keys, no
  credentials anywhere in this tree.
- **Catalog apps** install from the official Flipper catalog; the four
  Wi-Fi-devboard apps are flagged `needsModule` and need an ESP32-S2.

Use this branch for rehearsal, screenshots, CI, and the safety review. It is
the version to show approvers.

## Demo-day live content

The live-fire versions of the demos (real payload behavior on the sacrificial
laptop and lab AP) live **only** on the `unsafe` branch under
`competition-pack-live/`, fenced behind the written-authorization rules in
`docs/competition-live-runbook.md`. Nothing from that branch belongs here; if a
file without a dummy marker appears on this branch, treat it as a packaging
error and remove it before use.

The mechanical gate is `validate_pack.py` (53 checks): it fails on any
forbidden payload pattern, so a live payload cannot pass as a dummy one.
