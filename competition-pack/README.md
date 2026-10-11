# V3SP3R Security Competition Pack

Demo assets for the lab. The product manual is [../docs/guide.md](../docs/guide.md).

Internal lab pack for the company red-team / blue-team Flipper Zero exercise. Catalog apps are installed by V3SP3R from [catalog.flipperzero.one](https://catalog.flipperzero.one). The files in this tree are the demo assets pushed to the Flipper SD card (`/ext/badusb`, `/ext/subghz`, `/ext/infrared`). NFC notes live in `nfc/`; badge dumps do not.

Firmware target: Unleashed, SDK API major 88. The loader compares API major only, so a catalog build with the same major as the device-reported SDK API is the one to install.

## Layout

```
competition-pack/
├── README.md
├── manifest.json
├── compatibility-evidence.json
├── LICENSES.md
├── validate_pack.py
├── badusb/
│   ├── README.md
│   ├── demo_calc_windows.txt
│   ├── demo_notepad_hello_windows.txt
│   └── demo_rickroll_windows.txt
├── subghz/
│   ├── README.md
│   └── demo_remote_TODO.sub
├── infrared/
│   ├── README.md
│   └── demo_tv_power_TODO.ir
└── nfc/
    └── README.md
```

## Manifest

`manifest.json` is the machine-readable index (`vesper-security-competition`, version 1.0.0). Each category is `red-badusb`, `red-subghz`, `red-nfc-rfid`, `red-wifi`, `red-ir`, `red-gpio`, or `blue`.

- `catalog-app` entries are official catalog aliases. `pack` is the catalog pack (`daily`, `radio`, `edge`, `play`, `red`, `blue`). `needsModule: true` means the ESP32-S2 Wi-Fi devboard.
- `builtin` entries are stock firmware apps already on the Flipper.
- `file` entries are assets in this tree. `path` is relative to this directory. `target` is the Flipper SD path.

`compatibility-evidence.json` records the per-app build proof captured on 2026-10-11: for every alias, the catalog version, the build URL, and the size and SHA-256 of the verified API 88.2 FAP (ELF-checked, the same check the app runs in `FlipperCatalogJson.isFapElf`).

## Validate

From this directory:

```
python3 validate_pack.py
```

The last line is `PACK OK` when the manifest, asset paths, BadUSB safety lint, Sub-GHz placeholder key, and infrared placeholder all pass.

## Safety

BadUSB files are demo scripts only (Calculator, a Notepad reminder, a browser rickroll). The Sub-GHz file is a fake Princeton key (`DE AD BE EF`) and will not open a receiver. The infrared file is the public `POWER_DEMO` pattern. There are no badge dumps and no real credentials in this tree.

## Still to fill in on the lab card

- `subghz/README.md`: write your country/region on the fill-in line, then capture a remote you own (Flipper → Sub-GHz → Read → Save). Keep the capture on the SD card.
- Infrared: learn your own TV or projector remote (Infrared app → Learn) and save it on the SD card.
- NFC: add company-specific keys as a user dictionary on the lab SD card, not in git.
