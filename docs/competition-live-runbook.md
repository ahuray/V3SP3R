# V3SP3R competition live-fire runbook

Lab runbook only. The product manual is [guide.md](guide.md).

This is the demo-day, live-fire companion to `docs/competition-runbook.md`. Read that safe runbook first, including its authorization rules, bench card, and the 60-second scripts for stations D1 through D8. Rehearse on the `safe-test` branch. Perform on this branch (`unsafe`).

## 1. Authorization gate

Read this aloud before the first live station. Safe-runbook section 2 still applies (lab scope, legal ISM band, sacrificial laptop, lab badges only, disqualification in safe-runbook section 5). This gate is the additional bar for anything in section 3 below.

> **The written authorization record from safe-runbook section 2 must explicitly list every live station below before that station runs.**
>
> - Lab and company-owned devices only. No production systems. No neighbor networks. No coworker machines. No personal phones as targets.
> - No real credentials anywhere. Test accounts only. The briefed person at the keyboard is an organizer, using the test account named on the bench card.
> - No exfiltration, no persistence, no downloads, and no destruction. If a payload would do any of those, it does not belong on stage.
> - **Stop condition.** Anything unexpected: stop, power off the devboard, call the organizer.

If a station, or an optional beat inside a station (D2 bruteforce, D8 BLE spam), is missing from that record, skip it. Record: TODO(authorization: ticket or signed scope, date, approver), the same record as the safe runbook.

## 2. Branches and files

| Branch | Tree | Use |
| --- | --- | --- |
| `safe-test` | `competition-pack/` | Full-dummy rehearsal. Calculator, Notepad, rickroll, and placeholder `.sub` / `.ir` files. |
| `unsafe` | `competition-pack-live/` | This demo-day version. Live payloads stay in this tree. |

Never mix the trees. Do not copy live payloads into `competition-pack/`. Do not copy dummy placeholders into `competition-pack-live/`. On the Flipper SD the filenames differ, so both sets may be present; on stage, run the filename the station script names.

Validators, each run from inside its own directory:

```bash
cd competition-pack
python3 validate_pack.py
```

A clean safe run prints `PACK OK`.

```bash
cd competition-pack-live
python3 validate_live_pack.py
```

A clean live run prints `PACK LIVE OK`. Any other output is a failed check. Do not perform a live station on a failed check.

Install of the live payloads is the same SD-card copy as the safe assets (install guide section 5, SD reader or qFlipper) — or push them from the phone with Stage Remote (install guide section 10), which also fires beats over BLE during the show. Computer-side notes and the validator stay on the computer.

| Repo file | Flipper path | Role |
| --- | --- | --- |
| `competition-pack-live/payloads/live_wifi_key_reveal_windows.txt` | `/ext/badusb/live_wifi_key_reveal_windows.txt` | D1. Opens `cmd` and runs the `netsh` line below. Edit `LAB_SSID_HERE` to the lab SSID before the demo. No exfiltration. Nothing written to disk. |
| `competition-pack-live/payloads/live_wifi_key_reveal_elevated_windows.txt` | `/ext/badusb/live_wifi_key_reveal_elevated_windows.txt` | D1 fallback. Elevated `cmd` via UAC consent for Windows builds that demand admin rights for `key=clear`. Needs a local-admin demo account — if it works, say so, that is a finding. Rehearse both variants; keep the working one on the SD. |
| `competition-pack-live/payloads/live_credential_prompt_windows.txt` | `/ext/badusb/live_credential_prompt_windows.txt` | D1. Native Windows credential dialog via PowerShell `Get-Credential`. Message text: `LabGuest WiFi Sign-in`. The message box echoes only the typed username. The password is never shown or stored. |
| `competition-pack-live/payloads/live_speech_windows.txt` | `/ext/badusb/live_speech_windows.txt` | D1 optional theater beat. The laptop speaks its own lesson via local Windows text-to-speech. No network, no files. |
| `competition-pack-live/payloads/live_hackertyper_windows.txt` | `/ext/badusb/live_hackertyper_windows.txt` | D1 optional laugh beat. Opens hackertyper.net. |
| `competition-pack-live/evilportal/index.html` | `/ext/apps_data/evil_portal/index.html` (TODO(verify) against the flashed Marauder build) | D5 captive portal. Generic "LabGuest WiFi" branding. Test credentials only. |
| `competition-pack-live/evilportal/README.md` | Not copied as a payload | Where the verified portal path is written down. |
| `competition-pack-live/subghz/README.md` | Not copied as a payload | Live capture notes for the lab's own remote. Points at pre-generated bruteforce files. |
| `competition-pack-live/validate_live_pack.py` | Not copied to the Flipper | Live pack check. Prints `PACK LIVE OK`. |

The D1 key-reveal file runs this command after `LAB_SSID_HERE` is edited:

```text
netsh wlan show profile name="LAB_SSID_HERE" key=clear
```

Bruteforce files, if the authorization record names that beat, come from the public repository `tobiabocchi/flipperzero-bruteforce`. They are not vendored in this repo. `competition-pack-live/subghz/README.md` is the reference. Own gate only.

### Where live Wi-Fi and BLE come from

The official Flipper catalog contains no deauth FAP, no evil-portal FAP, and no BLE-spam FAP. Do not look for those names in Vesper.

Live Wi-Fi fire comes from the Marauder firmware on the ESP32-S2, through the already-installed `esp32_wifi_marauder` app: deauth, beacon spam, and evil portal. BLE beacon spam, if it is demoed, is a firmware-side plugin on Unleashed. TODO(verify) that plugin's presence and menu on the organizer's build before promising the beat.

Use the bench card in safe-runbook section 3. The same install guide (`docs/competition-install.md`) covers the phone, the packs, and the devboard flash.

## 3. The live stations

Each station keeps the safe runbook's shape: red, then the blue countermeasure, on the same bench. Speak the quoted lines. Do the action on the same beat. Scripts are 60–90 seconds unless a station states a longer clock. Optional beats are skipped when the authorization record does not name them.

### D1. BadUSB HID injection

**(a) What changes.** The safe beat runs `demo_calc_windows.txt` and `demo_notepad_hello_windows.txt` (rickroll optional). The live beat runs `live_wifi_key_reveal_windows.txt`: `cmd` opens on the sacrificial Windows laptop and `netsh` prints the lab Wi-Fi key in cleartext. The point to say out loud: anything that can type can read stored keys. Then `live_credential_prompt_windows.txt` pops the native credential dialog. A briefed organizer types the test account. The follow-up box echoes only the username. Optional laugh beat: `live_hackertyper_windows.txt`. The USB device-control blue beat is unchanged and still follows from the safe runbook.

**(b) Preconditions.**

- The authorization record lists D1 live (key reveal and credential prompt). List the hackertyper and speech beats on their own lines if they will run.
- `LAB_SSID_HERE` in `live_wifi_key_reveal_windows.txt` has been edited to the lab SSID. The demo account is signed in on the sacrificial laptop, and that SSID's profile is saved on it.
- Rehearsal on the actual demo laptop picked the key-reveal variant: run the `netsh` line once by hand. If Windows answers that `key=clear` needs elevation (seen on some Windows 11 builds), the elevated variant (`live_wifi_key_reveal_elevated_windows.txt`, UAC consent, local admin) is the one that goes on the SD.
- The person who will answer the credential dialog has been briefed and will type only the test account.
- The laptop is the sacrificial asset on the bench card. Other laptops are locked or out of reach of the cable.
- The USB device-control blue beat from the safe runbook still follows (policy image or lab policy ready).
- `python3 validate_live_pack.py` has printed `PACK LIVE OK`.

**(c) 90-second script.**

- **0:00–0:10.** Say: "Rehearsal opened Calculator. This file is a keyboard on our sacrificial laptop, and it is about to ask Windows for the lab Wi-Fi key this demo account already saved." Action: seat the screen toward the room. Confirm the filename and that no other computer is in reach of the cable.
- **0:10–0:35.** Say: "Stock BadUSB, live_wifi_key_reveal_windows.txt. It opens a command prompt and runs netsh for our lab SSID. Anything that can type can read stored keys. Nothing is written to disk." Action: run `/ext/badusb/live_wifi_key_reveal_windows.txt`. TODO(verify) the on-screen run control, same as the safe runbook. Leave the window up so the room can see the key in cleartext. Do not photograph the screen.
- **0:35–1:00.** Say: "Second file. Native Windows credential dialog. The message is exactly: LabGuest WiFi Sign-in. Our briefed operator types the test account." Action: run `/ext/badusb/live_credential_prompt_windows.txt`. The briefed organizer types the test username and test password.
- **1:00–1:15.** Say: "The password never left the dialog. A real phish would have taken it. This box shows the username only." Action: point at the message box. Confirm the password is not on screen.
- **1:15–1:25.** Say: "Optional beats only if the first two files have already finished: live_speech_windows.txt makes the laptop say its own lesson out loud, live_hackertyper_windows.txt opens hackertyper.net." Action: run the named optional beats only in that case, and only on the sacrificial laptop. Otherwise skip.
- **1:25–1:30.** Say: "Same cable, same Flipper. Blue is unchanged: this image blocks new HID, and the keyboard never types." Action: follow the safe-runbook D1 blue beat (USB device control). Show the block.

**(d) Stop condition.** If the laptop is not the sacrificial one, do not run the file. If a payload writes a file, copies data off the laptop, shows the password, or does anything other than the `cmd` window, the credential dialog, the username box, or the optional browser page, stop. Close the windows. Power off the devboard if it is attached. Call the organizer.

**(e) Restore.** Close the `cmd` window. Close the credential dialog and the browser if those beats ran. After demo day, rotate the lab AP password. The key was shown to the room.

### D2. Sub-GHz fixed-code capture and replay

**(a) What changes.** The safe pack's `.sub` file is a placeholder and is not transmitted. The live beat is a capture and replay of the lab's own fixed-code remote, then the rolling-code failure beat if that fob is on the bench. An optional bruteforce segment runs only when the authorization record names it: pre-generated files described in `competition-pack-live/subghz/README.md`, from `tobiabocchi/flipperzero-bruteforce`, not vendored here. Own gate only. Legal band from safe-runbook section 2. Warn the room about airtime before that transmission starts.

**(b) Preconditions.**

- The authorization record lists D2 live capture and replay. The bruteforce segment has its own line, naming the own gate, the legal frequency, and the airtime, or it does not run.
- The remote and the receiver are the lab pair on the bench card. Frequency is the legal ISM frequency recorded in safe-runbook section 2.
- The rolling-code fob, if used, is one the company owns.
- `python3 validate_live_pack.py` has printed `PACK LIVE OK`. That check does not replace a look at the remote in your hand.

**(c) 90-second script.** Bruteforce, when authorized, is an extension after this clock. State its airtime, run only that window, then stop.

- **0:00–0:10.** Say: "Fixed-code means the same bits every press. This remote is ours, on our band. We are not using the placeholder file." Action: show the lab remote and its receiver. State the frequency on the bench card. Do not transmit `demo_remote_TODO.sub`.
- **0:10–0:30.** Say: "Stock Sub-GHz, read, one press, save." Action: read in the stock Sub-GHz app, press the own remote once, save the `.sub` under `/ext/subghz/`.
- **0:30–0:50.** Say: "Replay should move our receiver." Action: replay the saved file at the own receiver. Open `sub_analyzer` or `subhound` only if the actuation is already visible and time remains.
- **0:50–1:10.** Say: "A rolling-code fob changes the bits every press. Replaying the old capture does nothing. That silence is the defense." Action: if the own rolling-code fob is on the bench, capture once, press the real fob again so the receiver advances, replay the old capture, and show no actuation. If the fob is absent, say the sentence and stop. Do not hunt a stranger's fob.
- **1:10–1:30.** Say: "Blue is a receiver, not a jammer. Spectrum Analyzer or Radio Scanner shows this transmission while we press." Action: start `spectrum_analyzer` or `radio_scanner`, press the own remote once, let the room see the hit.
- **Extension, only if the authorization record names bruteforce.** Say: "Pre-generated bruteforce, our gate only, on the legal band, for the airtime written on the authorization record. Files are the ones named in the live Sub-GHz README. They are not in the rehearsal pack." Action: transmit only at that own gate, only for the recorded airtime, then stop. If the record does not name this beat, say: "Bruteforce is not on today's authorization record. We stop at replay."

**(d) Stop condition.** If the receiver or the gate is not the lab's own, stop. If the frequency is outside the legal band on the bench card, stop. If the target list or the file is not the authorized transmitter, stop. If bruteforce is not a line on the authorization record, do not start it. Anything unexpected: stop, power off the devboard, call the organizer.

**(e) Restore.** Delete captures that are not meant for the badge library. If the demo unpaired the receiver, pair it again before leaving the station. Confirm the gate responds to its own remote.

### D3. RFID 125 kHz badge clone

**(a) What changes.** The mechanics are the safe beat. This station was already live there: read the lab EM4100, write it onto the lab T5577 with `t5577_writer`, and open the lab reader with the clone. This runbook tightens the script and adds the restore. No lab badge data leaves the room.

**(b) Preconditions.**

- The authorization record lists D3.
- The EM4100, the T5577, and the reader are the lab set on the bench card (safe-runbook section 3).
- The audience can see the T5577 is a separate card from the original badge.

**(c) 60-second script.**

- **0:00–0:10.** Say: "125 kHz EM4100 is a number in the clear. This badge and this reader are the lab pair on the authorization card." Action: show the original badge and the blank T5577 side by side.
- **0:10–0:25.** Say: "Stock RFID app, read." Action: read the badge. Read aloud the last few digits only. Do not photograph the badge.
- **0:25–0:40.** Say: "T5577 Raw Writer writes that ID onto a card we can see is a clone." Action: open `t5577_writer` and write the blocks from this read onto the T5577. TODO(verify) the writer’s on-screen field names, same as the safe runbook. Do not type a badge ID from outside the lab set.
- **0:40–0:50.** Say: "The reader cannot tell the clone from the original." Action: present the T5577 to the lab reader, then the original. Both succeed.
- **0:50–1:00.** Say: "Access Audit on the same badge: this population is still 125 kHz. That is the finding. The clone does not leave the bench." Action: open `access_audit` on the original badge. Keep the T5577 on the bench.

**(d) Stop condition.** If the badge or the reader is not the lab pair on the bench card, stop. Do not read a coworker badge. Anything unexpected: stop, power off the devboard, call the organizer.

**(e) Restore.** In front of the audience, retire the cloned T5577. Wipe it in `t5577_writer` — TODO(verify) the wipe control on this build — or mark the card `CLONE` and remove it from the lab badge set. No lab badge data leaves the room. Do not pocket the clone.

### D4. NFC MIFARE Classic weak keys

**(a) What changes.** The read is the safe beat: stock NFC app, default key dictionary, lab MIFARE Classic badge only. Optional `nfc_magic` / `picopass` rules are unchanged from the safe runbook. The live addition is the restore: no photograph, no export, and any dump file on the SD is deleted after the station.

**(b) Preconditions.**

- The authorization record lists D4.
- The badge is the lab Classic issue on the bench card, with a weak or default key, as in the safe runbook.
- `picopass` runs only if that iClass Legacy card and reader are on the authorization record.

**(c) 60-second script.**

- **0:00–0:10.** Say: "MIFARE Classic keys live on the card. If the key is a factory default, this reader walks the sectors. This badge is ours." Action: show the lab Classic badge.
- **0:10–0:30.** Say: "Stock NFC, default dictionary, read." Action: read the badge in the stock NFC app with the default key list. Show a sector that opens. Do not display a full dump. Do not photograph the screen.
- **0:30–0:45.** Say: "The reader accepted this because the keys never changed. That is a deployment bug." Action: tap the lab reader with the original badge. Run `nfc_magic` or `picopass` only under the safe-runbook rules, and only if time remains.
- **0:45–1:00.** Say: "Blue is Access Audit on this same badge. We want a verdict on weak keys, and then the dump is deleted." Action: run `access_audit` on the lab badge. If a dump file was written, it is deleted in the restore before the next station. `seader` only if this card is a type the safe runbook already allows.

**(d) Stop condition.** If the badge is not the lab issue, stop. If a dump is about to be saved to a phone, a shared drive, or a photograph, stop. Anything unexpected: stop, power off the devboard, call the organizer.

**(e) Restore.** Do not photograph or export the dump. Delete any dump file from the Flipper SD after the station, before the next team. Confirm the file browser no longer shows it.

### D5. Wi-Fi live fire

**(a) What changes.** This is the largest delta from the safe beat. The safe station scans the lab and brings up a short-lived clone of the guest SSID. It does not deauthenticate, and it does not run a portal. Rehearsal on `safe-test` stays that way. Demo day, when the authorization record lists this station, runs five beats in order: (i) Marauder scan of the lab, as in the safe script; (ii) deauth, 30 seconds, lab access point only, with one lab client associated so the room sees it drop and rejoin; (iii) beacon spam, lab-named SSIDs only, one minute; (iv) evil portal, the "LabGuest WiFi" page from the live pack, test credential only, then the capture file deleted on stage; (v) blue, a `flip_wifi` sweep that shows the rogue SSID gone, then the devboard powered off. There is no catalog FAP for deauth or evil portal. The radio is Marauder on the ESP32-S2, driven by `esp32_wifi_marauder`. `flip_wifi` needs that same board, so the sweep runs after Marauder transmit stops and before the board is unplugged.

**(b) Preconditions.**

- The authorization record lists D5 live and names deauth, beacon spam, and the evil portal as separate lines.
- The lab AP SSID is the one on the bench card. One lab client is associated to it and is the machine the room will watch drop and rejoin.
- The presenter has a sacrificial phone or laptop for the portal, and a test credential. No real password.
- `esp32_wifi_marauder` and `flip_wifi` are installed (install guide). The ESP32-S2 is on the GPIO header with Marauder already flashed.
- `competition-pack-live/evilportal/index.html` is on the Marauder portal path. TODO(verify) that path against the flashed Marauder version, and write it into `competition-pack-live/evilportal/README.md`.
- TODO(verify) the on-screen labels in this Marauder build for scan, single-target deauth, beacon spam, evil portal, and the capture-file path.
- `python3 validate_live_pack.py` has printed `PACK LIVE OK`.

**(c) Station clock.** The other stations are 60–90 seconds. This one is longer because the transmit windows are fixed: 30 seconds of deauth, then one minute of beacon spam. Those windows are caps. Do not extend them. Do not start a transmit beat until the target list has been checked.

- **0:00–0:15. Scan.** Say: "The Flipper has no Wi-Fi radio. This ESP32-S2 does. Marauder is already on it. We scan our lab first. Every later transmit is the lab access point, or a lab-named SSID, on the bench card." Action: show the devboard. Open `esp32_wifi_marauder` and run the scan. Confirm the lab AP is on the list.
- **0:15–0:45. Deauth.** Say: "Thirty seconds, our lab access point only, one lab client. The room should see it drop and rejoin. We do not touch any other network." Action: select only the lab AP. Confirm the target list shows no other SSID and no other client. Start deauth. Stop at 30 seconds even if the drop was early. Watch the lab client leave and come back.
- **0:45–1:45. Beacon spam.** Say: "Beacon spam, one minute, names we own: LabGuest and DEFINITELY_NOT_A_HONEYPOT. Harmless labels. No neighbor names." Action: start beacon spam with those lab-named SSIDs only. Stop at one minute.
- **1:45–2:35. Evil portal.** Say: "LabGuest WiFi portal, on our devboard. I am connecting a sacrificial device and typing the test account. Then we delete what it captured." Action: start the evil portal with `index.html` ("LabGuest WiFi"). Connect the sacrificial phone or laptop. Type the test credential. Show the captured line in the Marauder log. Delete that capture file from the SD in front of the room. TODO(verify) the capture-file path on this flashed build.
- **2:35–2:55. Blue.** Say: "Blue scan. Transmit is stopped. The rogue name should be gone. Then the devboard goes off." Action: stop beacon spam and the portal. Open `flip_wifi` (FlipperHTTP already installed, as in the safe runbook) and scan. Show that the beacon and portal SSIDs are gone. Power off the ESP32.

**(d) Stop condition.** If any non-lab SSID or any non-lab client is visible in the target list, stop. Do not start deauth, beacon spam, or the portal. If a window reaches its cap, stop that transmit. If the capture would be kept, emailed, or photographed, stop and delete it. Anything unexpected: stop, power off the devboard, call the organizer.

**(e) Restore.** Devboard powered off and unplugged. Portal files and captures deleted from the SD. Lab AP and the lab client confirmed reconnected. No beacon spam still airing (`flip_wifi` or the lab AP's own view shows only the real lab SSID).

### D6. Infrared replay

**(a) What changes.** Unchanged from the safe runbook. Learn one button from the lab display's own remote and replay it. The placeholder `demo_tv_power_TODO.ir` is still not a file to transmit.

**(b) Preconditions.** The authorization record lists D6. The display and remote are the lab pair on the bench card. The Flipper is aimed at that display only.

**(c) 60-second script.**

- **0:00–0:10.** Say: "Infrared is line of sight and a shared code. This remote belongs to this display." Action: point at the lab TV or projector and the receiver window.
- **0:10–0:25.** Say: "Learn one button. Stock Infrared app or Cross Remote." Action: learn power or volume from the own remote into `xremote` or the stock IR app.
- **0:25–0:40.** Say: "Replay. The display does not know who sent it." Action: replay that button. Wait for the on-screen change.
- **0:40–0:50.** Say: "IR Scope watches the real remote. We stay on this set." Action: open `ir_scope`, press the real remote once, show the trace.
- **0:50–1:00.** Say: "Blue is the absence of a check. There is no key and no pairing. Physical control of the room is the control." Action: replay once more, then put the Flipper down.

**(d) Stop condition.** If the display is not the lab unit, stop. Do not aim at another screen. Anything unexpected: stop, power off the devboard, call the organizer.

**(e) Restore.** None beyond the safe runbook. Leave the display in the state the room expects (powered as you found it if the power button was the learned key).

### D7. GPIO hardware audit

**(a) What changes.** Unchanged from the safe runbook: UART and SPI on the labeled bench board, read-only on stage. Addition: any dumped flash content stays on the bench laptop and is deleted after the station.

**(b) Preconditions.** The authorization record lists D7. The board is the labeled bench unit. Pinout, voltage, and baud are the ones already checked for the safe runbook (TODO(verify) against that board's schematic). The SPI part is the bench clip, not a production board that must boot tomorrow.

**(c) 60-second script.**

- **0:00–0:10.** Say: "Debug headers survive into hardware people forget to close. This board is ours, and the header is labeled." Action: show the marked GND/TX/RX. Do not clip a random connector.
- **0:10–0:25.** Say: "UART Terminal, the baud on the bench card." Action: open `uart_terminal`, set the verified baud, and show the boot log or the lab banner. If it is a shell, run a read-only command such as printing the banner. Do not change configuration.
- **0:25–0:40.** Say: "SPI Mem Manager, 25-series flash, test clip, our part. A dump stays on the bench laptop and is deleted when we finish." Action: seat the clip and read the chip ID. Stop when the ID and the first page are visible.
- **0:40–0:50.** Say: "Logic Analyzer plus Signal Generator is the same header. PulseView is the viewer, if it is cabled." Action: show one captured line if PulseView is set up. Otherwise name the apps and move on.
- **0:50–1:00.** Say: "Blue is the inventory. Every debug port on company hardware gets a row: present, locked, or removed." Action: show the one-page inventory from the safe runbook.

**(d) Stop condition.** If the board is unlabeled or is a production chassis, stop. Do not copy a dump to a phone or a shared drive. Anything unexpected: stop, power off the devboard, call the organizer.

**(e) Restore.** Any dumped flash content stays on the bench laptop and is deleted after the station. Confirm the file is gone before the next team. Do not print it.

### D8. BLE tracker plant and sweep

**(a) What changes.** The safe beat is unchanged and still runs: `findmy` plants the Flipper as a Find My beacon, then the Android phone sweeps the room. The official catalog has no BLE-spam FAP, and Vesper is not how this beat is installed. Live adds a 30-second pairing-popup storm only if both of these are true: the authorization record names it, and the organizer has confirmed a BLE spam / beacon-flood plugin on this Unleashed build. TODO(verify) that plugin's presence and menu. The spoken point is why Bluetooth discovery is disabled in sensitive areas. Then the same phone-side sweep as the safe runbook.

**(b) Preconditions.**

- The authorization record lists D8. The popup-storm beat has its own line, or it does not run.
- TODO(verify) the plugin menu on this build. If it is not there, say so and run the safe script only.
- Every device that will be in range is a company device listed on the authorization record, or Bluetooth is off on anything not listed. Personal phones are not targets.
- Vesper's BLE session is disconnected before the plant, as in the safe runbook. The retrieval spot is written on the bench card.

**(c) 90-second script.** If the plugin is not confirmed, use the safe-runbook D8 60-second script and skip the middle beat.

- **0:00–0:15.** Say: "FindMy Flipper turns this Flipper into a Find My beacon. We are hiding our own device, in our own room." Action: open `findmy`, start the beacon mode this build provides (TODO(verify) the on-screen start control, same as the safe runbook), and place the Flipper at the retrieval spot.
- **0:15–0:45.** Say: "Thirty seconds of pairing popups, our room, our listed devices. This is why we disable Bluetooth discovery in sensitive areas." Action: start the firmware plugin from the menu verified before the show. Stop at 30 seconds. If that menu is not confirmed, say: "This build does not have a verified BLE spam plugin. We stay with the tracker sweep."
- **0:45–1:15.** Say: "Detection is the phone. Android Settings, Safety and emergency, Unknown tracker alerts, manual scan. The bench card has the path we confirmed." Action: run the safe-runbook sweep. TODO(verify) the Settings path on this phone, same as the safe runbook. Retrieve the Flipper.
- **1:15–1:30.** Say: "Policy is: sensitive rooms get this sweep, alerts stay on, and someone walks the room. The popup beat is over." Action: confirm the plugin is stopped and the beacon is off.

**(d) Stop condition.** If the plugin menu is not the one verified before the show, do not start the storm. If an unlisted phone is discoverable in the room, do not start the storm. If the storm does not stop at 30 seconds, power the Flipper off. Anything unexpected: stop, power off the devboard, call the organizer.

**(e) Restore.** Spam stopped. Find My beacon off. Flipper retrieved from the plant spot. No Flipper left advertising in the room.

## 4. Post-demo restore checklist

Do this once, after the last live station, with a witness watching. The per-station restore still happens if the day stops early.

> - [ ] Rotate the lab AP password. D1 showed it to the room.
> - [ ] Delete all captured files from the Flipper SD in front of the witness: portal posts, badge dumps, and `.sub` captures that are not meant for the badge library.
> - [ ] Power off and bag the devboard.
> - [ ] Close all windows on the sacrificial laptop and re-image it per company policy.
> - [ ] Confirm the lab AP and the lab client are back in their normal state.
> - [ ] Confirm no beacons are still transmitting.
> - [ ] Shred any printed captures.
> - [ ] Log the stations actually run against the authorization record, including skipped optional beats.

## 5. Emergency and incident

If anything touches a non-lab network or a non-lab device:

1. Stop.
2. Power off the transmitter. That is the devboard for D5, and the Flipper when the Flipper is the transmitter (D2, D6, D8).
3. Note the time, the duration, and the target.
4. Inform the organizer and the network owner.
5. Document it in the authorization record.

Do not resume that station. The section 1 stop condition still covers every other surprise: stop, power off the devboard, call the organizer.
