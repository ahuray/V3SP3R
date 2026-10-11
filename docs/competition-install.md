# V3SP3R competition install

Exact install for the organizer's phone and Flipper before the lab in `docs/competition-runbook.md`. Paths and UI strings below were taken from this checkout of `feature/competition-pack`. Anything not verified in that tree is marked `TODO(verify)`.

## 1. What you have

Same bench as the runbook:

- Flipper Zero with Unleashed firmware. Target SDK API major is 88. MicroSD inserted. Bluetooth enabled (Settings → Bluetooth → ON, as in the README).
- Android phone that will run Vesper (`com.vesper.flipper`, launcher name **Vesper**). Android 8.0+ (API 26). Bluetooth on. Internet on for the catalog download.
- ESP32-S2 Wi-Fi devboard. Required only for demo D5. Flash ESP32 Marauder before the event (section 6).
- Sacrificial Windows demo laptop.
- Own RFID/NFC badges, including one EM4100 and one MIFARE Classic issued for the lab, plus one T5577 writable card.
- Own fixed-code remote on a legal ISM frequency (runbook section 2). Country/region: TODO(country/region).
- Own TV or projector and its remote.
- One BLE beacon role played by the Flipper (`findmy`) or a spare tracker the company owns.
- Computer with this repo, JDK 17 or newer, and the Android SDK, if you build the APK here. `adb` on that computer if you install over USB.

Fill in:

- Phone: TODO(model and Android version)
- Flipper firmware banner: TODO(firmware string shown on the Device screen)
- Authorization record: TODO(authorization: ticket or signed scope, date, approver)

## 2. Get the Vesper build

Two options: install the prebuilt APK that ships on this branch, or build from source.

### (a) Prebuilt APK on this branch

`vesper-competition-debug.apk` sits at the repo root (gitignore exception `!/vesper-competition-debug.apk`). On the `unsafe` branch it is the `assembleDebug` output of `unsafe`: Red Team and Blue Team chips plus the Stage Remote screen (section 10) with the competition payloads bundled. SHA-256 of the file committed on this branch:

```text
d25c18f0ab3da9970c56b03ca8352ce846e1dc3b347d0e1b379f2d472e2fecc7
```

Check it before installing: `sha256sum vesper-competition-debug.apk`. If you rebuild locally (option b), expect a different hash — debug builds are not reproducible.

Do **not** use the older `vesper-unleashed-debug.apk` for this event. It predates the competition packs and has no Red Team or Blue Team chips.

### (b) Build from source

From the repo root, on JDK 17+ with the Android SDK (README command-line build):

```bash
git checkout feature/competition-pack
./gradlew assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install over USB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK to the phone and open it. Android will ask to allow installation from that source.

A debug-signed APK cannot upgrade an install that was signed with a different key. If Android refuses the update, uninstall the existing debug build first, then install again. Uninstall deletes the app's local data (chat history and the on-phone settings store).

```bash
adb uninstall com.vesper.flipper
adb install app/build/outputs/apk/debug/app-debug.apk
```

The README's first-launch list also says to paste an OpenRouter key under Settings. FapHub install does not use that key. Skip it unless someone will use the Chat tab.

On first launch the activity requests, for Android 12+: Bluetooth scan, Bluetooth connect, fine location, and (Android 13+) notifications. Grant them. Location is required for the BLE scan (README troubleshooting). The error string if they are missing is `Bluetooth permissions not granted`.

## 3. Pair the Flipper

1. Enable Bluetooth on the phone and on the Flipper. Unleashed: Settings → Bluetooth → ON (README). Disconnect qFlipper and any other phone. The README says another connection is a common reason the scan misses the Flipper, and to stay within about 1 meter.
2. Open Vesper. Bottom navigation titles are **Chat**, **Alchemy**, **FapHub**, **Device**, **Settings**. Open **Device**. The top bar title is `Device`.
3. When disconnected, the status line reads `Disconnected` and the detail reads `No device connected`. Tap the full-width button `Scan for Flipper`. While the scan runs the button reads `Stop Scanning` and the status title reads `Scanning...`. The detail line stays `No device connected` until a row is tapped.
4. When the list is non-empty, the heading is `Discovered Devices`, with the line `Showing Flipper devices only, including custom device names.` Tap the Flipper's row. The row shows the BLE name and either `Flipper signature detected` or `Unverified Flipper`. Status becomes `Connecting...` with the device name under it, then `Connected` with that name under it.
5. Confirm the card titled `Device Information`. It shows **Battery** and **Firmware**. It does not show the SDK API. The installer still reads `firmware_api` major and minor over RPC (or from CLI `device_info`) and pins the catalog build to that API, with a same-major fallback. For a banner that contains `unleashed` or `unlshd` and no parsed API, the fallback major is 88.
6. Optional: tap `Run Link Diagnostics` on the automation card. While connected, the folder icon in the top bar (content description `File Browser`) opens the file browser. That browser is not an uploader (section 5).

`Connect via USB` is a second button under `Scan for Flipper`. This competition uses BLE. Use USB only if BLE cannot be made to stay up.

If the link drops later: tap the disconnect icon (content description `Disconnect`), then `Scan for Flipper` again. Or, while connected, open **Advanced Operations** (`Runbooks, macros & diagnostics`), then **Recover & Scan**, then **Run**. That runbook disconnects and starts a scan. Status text while it runs: `Recovering...`, then `Recovery scan started.`

## 4. Install the packs

The phone needs a path to `https://catalog.flipperzero.one` for the whole install. Each app is resolved by catalog alias, downloaded as a FAP (ELF) for the Flipper's SDK API, and written to `/ext/apps/<installFolder>/<alias>.fap`. The client tries the device-reported API first. If that build is missing, it tries other published catalog APIs with the same major (closest minor at or below the device, then newer). That is the Unleashed-safe path: an 88.x device can take a catalog 88.2 FAP when the loader only checks the major.

1. Stay on the **Device** tab until the status is `Connected`.
2. Open the **FapHub** tab. The screen title is `FapHub`. Leave the inner tab on `Apps` (the other tab is `Resources`, which only offers `Browse Repository` in the system browser and does not copy files to the Flipper).
3. In the chip row, tap `Red Team (17)`. Tapping the selected chip again clears it, so leave it highlighted.
4. Tap `Install pack`. The line under the chips changes from `Red Team pack` to `Installing 1/17: <app name>` and walks the pack. Wait until that line returns to `Red Team pack`.
5. Tap `Blue Team (6)`, then `Install pack`, and wait for `Installing 1/6` through `6/6`.
6. Tap `Edge (7)`, then `Install pack`. This pack is `nfc_magic`, `picopass`, `metroflip`, `weebo`, `mouse_jiggler`, `findmy`, and `cyborg_detector`. The stations use `nfc_magic`, `picopass`, `weebo`, and `findmy`. `mouse_jiggler` is the optional D1 HID extra.
7. Tap `Radio (6)`, then `Install pack`. This pack is `protoview`, `spectrum_analyzer`, `weather_station`, `pocsag_pager`, `ir_scope`, and `longwave_clock`. The stations use `protoview`, `spectrum_analyzer`, and `ir_scope`.
8. Optional: tap `Daily (17)`, then `Install pack`, if the station will use Quac or Sub-GHz Remote (`quac`, `subghz_remote_ofw`).

Do not use `Install all` for this event. That button is what you see when the `All bare` chip is selected (no pack filter). It installs `bareApps` only: every catalog entry whose `needsModule` flag is false. It skips `esp32_wifi_marauder`, `esp_flasher`, `flipper_http`, and `flip_wifi`. Those four install when their pack is selected and `Install pack` runs. They copy onto the SD without the devboard. They run only with the ESP32-S2 attached. FlipWiFi also needs FlipperHTTP, which is in the Red pack.

The card UI does not grey these out, but it reads the flag: cards for the four module apps show `Devboard`, and the detail sheet shows `Needs WiFi devboard` in red. Every other card shows `Bare` / `Bare Flipper`. The screen header stays `Bare Flipper pack • 60 apps • <n> on device` during the install. The 60 count is the non-module apps, including Red and Blue apps that do not need a board.

Category chips under the pack row filter the list on screen. `Install pack` still installs the whole selected pack.

A failure snackbar reads:

```text
Could not install <app names>. Connect the Flipper and update its firmware, then retry.
```

A single-app failure reads `Failed to install <name>: <message>`. If the catalog has no build for the reported API, the message includes `No <alias> build is available for your firmware's SDK (API <pinned>).` Update Unleashed, confirm Bluetooth is still `Connected`, tap the pack chip, and tap `Install pack` again. Refresh (top-right icon) re-reads `/ext/apps` one folder deep and marks aliases it finds as `Installed`.

Module apps and where they land:

| Alias | Pack | Path |
| --- | --- | --- |
| `esp32_wifi_marauder` | Red | `/ext/apps/GPIO/esp32_wifi_marauder.fap` |
| `esp_flasher` | Red | `/ext/apps/GPIO/esp_flasher.fap` |
| `flipper_http` | Red | `/ext/apps/GPIO/flipper_http.fap` |
| `flip_wifi` | Blue | `/ext/apps/GPIO/flip_wifi.fap` |

The full alias list is the appendix of the runbook. Two paths that are easy to look for in the wrong folder:

- `t5577_writer` and `rfid_maker` install under `/ext/apps/RFID/`.
- `access_audit` installs under `/ext/apps/Tools/access_audit.fap`.

## 5. Push the asset files

Intended sources, once `competition-pack/` exists:

| From the repo | To the Flipper SD |
| --- | --- |
| `competition-pack/badusb/demo_calc_windows.txt` | `/ext/badusb/demo_calc_windows.txt` |
| `competition-pack/badusb/demo_notepad_hello_windows.txt` | `/ext/badusb/demo_notepad_hello_windows.txt` |
| `competition-pack/badusb/demo_rickroll_windows.txt` | `/ext/badusb/demo_rickroll_windows.txt` |
| `competition-pack/subghz/demo_remote_TODO.sub` | `/ext/subghz/demo_remote_TODO.sub` |
| `competition-pack/infrared/demo_tv_power_TODO.ir` | `/ext/infrared/demo_tv_power_TODO.ir` |

`competition-pack/nfc/README.md`, `manifest.json`, `compatibility-evidence.json`, and `validate_pack.py` stay on the computer. Do not invent NFC binaries from the NFC README.

`demo_remote_TODO.sub` and `demo_tv_power_TODO.ir` are placeholders. Replace them with a capture of the own remote and a learn of the own display before any transmit. Do not transmit a file whose name still contains `TODO`.

### (a) Vesper file browser

Not an upload path. Drop this option.

The browser opens from the Device top bar, folder icon, only while status is `Connected`. The screen title is `File Browser`. It starts at `/ext`, lists directories and files, and previews a tapped file with a read. There is no upload, import, or write control on that screen. `FlipperFileSystem.writeFileBytes` exists and is what FapHub uses to store a downloaded `.fap`. The file browser never calls it.

`WebFileServer` (default port 8888, `POST /api/upload`) can write to the Flipper SD, and nothing in the app constructs or starts that server. It is not a way to move the pack.

The FapHub `Resources` tab lists public repos and a Flipper directory label, then opens the repo URL in the browser. It does not download those files onto the SD.

### (b) SD card in a reader

1. Power the Flipper off. Remove the microSD.
2. Mount it on a computer.
3. Copy the three `competition-pack/badusb/*.txt` files into the card's `badusb` directory (Flipper path `/ext/badusb/`).
4. Copy `demo_remote_TODO.sub` into `subghz` (`/ext/subghz/`).
5. Copy `demo_tv_power_TODO.ir` into `infrared` (`/ext/infrared/`).
6. Eject the card, put it back, power the Flipper on.
7. Replace the `.sub` and `.ir` placeholders with real captures of the lab's own remote and display before the audience arrives.

### (c) qFlipper on a computer

The README uses qFlipper to update firmware. Use that same computer session as the file-copy fallback: connect the Flipper over USB and copy the files in the table onto the SD paths in column 2. TODO(verify) the qFlipper file-pane control names against the version installed on the organizer's computer. This repo does not ship a qFlipper UI.

## 6. Prepare the Wi-Fi devboard (D5 only)

Skip this section if the event drops D5.

1. Install the Red pack first so `esp_flasher` is at `/ext/apps/GPIO/esp_flasher.fap`. The catalog description is "Flash ESP32 firmware (e.g. Marauder) straight from the Flipper."
2. Put a Marauder build that matches this ESP32-S2 on the Flipper SD where that ESP Flasher build expects firmware files. TODO(verify) the directory and the on-screen menu for the installed ESP Flasher version. This repo does not contain a Marauder `.bin`.
3. On the Flipper, open Apps and run ESP Flasher from the GPIO apps, or flash the same binary with a PC flasher the organizer already uses.
4. Power the Flipper off, seat the ESP32-S2 on the GPIO header, power on.
5. `esp32_wifi_marauder` and `flip_wifi` stay quiet until this board is attached. `flip_wifi` also needs `flipper_http` installed (Red pack, same GPIO folder).

## 7. Verify

1. On the Flipper, open Apps. The new files sit in the category folders that match the install path (`Sub-GHz`, `RFID`, `GPIO`, `Infrared`, `USB`, `Tools`, `NFC`, `Bluetooth`). Spot-check:
   - `/ext/apps/Sub-GHz/sub_analyzer.fap`
   - `/ext/apps/RFID/t5577_writer.fap`
   - `/ext/apps/Tools/access_audit.fap`
   - `/ext/apps/GPIO/esp32_wifi_marauder.fap` (present even before the devboard is attached)
   - `/ext/apps/Bluetooth/findmy.fap`
   - `/ext/apps/Infrared/ir_scope.fap`
2. In Vesper, on FapHub, tap the refresh icon. Cards that match a `.fap` name under `/ext/apps/<folder>/` show `Installed`. The scan is one directory level under `/ext/apps`, which is where these packs write.
3. `/ext/badusb/` contains `demo_calc_windows.txt`, `demo_notepad_hello_windows.txt`, and `demo_rickroll_windows.txt`. The file browser can open `/ext/badusb` and preview those files. It cannot have been the tool that put them there.
4. From a computer, once the asset directory exists:

```bash
python competition-pack/validate_pack.py
```

The script ships in `competition-pack/` on this branch and prints `PACK OK` on a clean run. Treat a missing script or any `FAIL` line as a failed check, not a pass.

5. The day before, dry-run D1 through D8 from the runbook on the lab hardware. Confirm the Android unknown-tracker menu on this phone (TODO(verify) Settings path for this OEM and Android version) and the UART pinout (TODO(verify) against the bench schematic).

## 8. Troubleshooting

| Symptom | What to do |
| --- | --- |
| `adb install -r` refuses the APK, signatures do not match | `adb uninstall com.vesper.flipper`, then install again. Local app data is removed. |
| Scan stays empty | Flipper Settings → Bluetooth is ON. Phone Bluetooth is on. Location permission is granted. qFlipper and other phones are disconnected. Move within about 1 meter (README). Status should leave `Disconnected` / `No device connected` after `Scan for Flipper`. |
| BLE drops mid-install | Device → disconnect, or **Advanced Operations** → **Recover & Scan** → **Run**. Reconnect until status is `Connected`. Tap the pack chip and `Install pack` again. Already written `.fap` files stay on the SD. |
| Snackbar: no build for the firmware SDK API | Read the API in the snackbar. Device Information shows **Firmware** only. Update Unleashed so the reported API major matches a published catalog build (major 88 is the Unleashed hint in the client). Retry `Install pack`. |
| `Could not install ... Connect the Flipper and update its firmware, then retry.` | Confirm `Connected`, confirm the phone can open `https://catalog.flipperzero.one`, then run that pack again. |
| Wi-Fi app is on the SD and does nothing | Expected without the ESP32-S2. The card says `Devboard`. `Install all` never copies these four; `Install pack` on Red or Blue does. |
| BadUSB file missing | The Vesper file browser cannot upload it. Use the SD reader or qFlipper (section 5). |
| BadUSB runs and the laptop ignores it | Data-capable cable, script launched from the stock BadUSB app, sacrificial account unlocked. Try `demo_calc_windows.txt`. If keystrokes arrive late, the file's `DELAY` lines are the first knob. |
| Sub-GHz replay is silent | On a rolling-code receiver that is the teaching result (runbook D2). On the fixed-code prop, replay the capture of that prop, on the frequency written on the bench card. |
| `validate_pack.py` not found | Run it from inside `competition-pack/` (`python3 validate_pack.py`). It ships on this branch; a missing file means the checkout is incomplete — re-clone or check out `feature/competition-pack`. |

## Open TODOs

- TODO(country/region) and TODO(legal ISM frequency) on the bench card.
- TODO(authorization: ticket or signed scope, date, approver).
- Use `vesper-competition-debug.apk` (or a fresh `assembleDebug`) for this event, never the older `vesper-unleashed-debug.apk`.
- Fill in `Country/region` in `competition-pack/subghz/README.md` and record the legal ISM frequency on the bench card.
- TODO(verify) ESP Flasher's firmware directory and menu for the Marauder `.bin`, and the Marauder scan / lab-AP menu labels in the flashed build.
- TODO(verify) qFlipper file-pane labels on the organizer's computer.
- TODO(verify) Android Settings path for unknown-tracker scan on this phone.
- TODO(verify) UART pinout, voltage, and baud for the bench board.
- TODO(verify) on-screen run controls inside stock BadUSB, stock Sub-GHz, T5577 Raw Writer, and FindMy Flipper on this Unleashed build. File paths in this guide are the part the app guarantees.

## 9. Live content (unsafe branch only)

The `unsafe` branch adds `competition-pack-live/` for demo day. The safe pack (`competition-pack/`) and `validate_pack.py` are unchanged. `docs/competition-live-runbook.md` is required reading before any live station. Copy from a checkout of `unsafe`. `competition-pack-live/` is not on `safe-test`.

Use the SD-card reader or qFlipper method from section 5. Copy `competition-pack-live/payloads/live_*.txt` into `/ext/badusb/`. Copy `competition-pack-live/evilportal/index.html` to `/ext/apps_data/evil_portal/index.html`, the conventional Marauder evil-portal path. TODO(verify) that directory for the flashed Marauder build; record it in `competition-pack-live/evilportal/README.md`, which stays on the computer.

Edit `LAB_SSID_HERE` in `live_wifi_key_reveal_windows.txt` to the lab SSID before the demo.

From inside `competition-pack-live/`:

```bash
python3 validate_live_pack.py
```

A clean run prints `PACK LIVE OK`. Any other result is a failed check.

## 10. Stage Remote (unsafe branch only)

The `unsafe` APK adds a **Stage Remote** screen: demo beats triggered from the phone over BLE while the Flipper stays cabled to the target (BadUSB) or on the bench (D2, D5, D6, D8). Open it from the remote icon in the Device top bar.

**Physics.** The Flipper must still be USB-cabled to the sacrificial laptop for D1 (it is the keyboard). The phone only replaces walking up to the Flipper and pressing its buttons. Keep the phone within BLE range of the Flipper, and keep other BLE clients (qFlipper, the official app) disconnected — one session at a time.

Three sections:

- **Beats.** One card per demo beat. SAFE cards are the three dummy D1 scripts and run immediately. LIVE cards (all five live D1 payloads, haunted mouse, soundbar IR, Sub-GHz replay, BLE room buzz, Start WiFi station) are red and always ask for confirmation in the app first; the executor's own risk approval can still follow. Beats run through the same CommandExecutor path as the rest of the app, so they are audited.
- **Push payloads.** The APK bundles `competition-pack/badusb` and `competition-pack-live` as assets (synced by `scripts/sync_stage_assets.py`; the live validator fails if they drift). Push any file to the Flipper over BLE: scripts land in `/ext/badusb/`, the portal page in `/ext/apps_data/evil_portal/index.html` (the folder is created on first push if missing). On-device indicators show what is already there. This replaces the SD-card shuffle in section 9 for the payload files.
- **Stage apps.** The nine stage-relevant catalog apps with installed indicators and Install / Install all, using the same download path as FapHub.

Before demo day, rehearse the whole show from the phone once: every beat you will fire, on the real laptop and AP. TODO(verify) on-device: BadUSB while cabled, IR/Sub-GHz file pickers, BLE spam when the firmware plugin is missing, and the portal-folder create-on-push.
