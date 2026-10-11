# V3SP3R competition runbook

Lab runbook only. The product manual is [guide.md](guide.md).

This runbook is for the organizer of an internal company security competition. Each station shows one offense (red) and one defense (blue) on a Flipper Zero running Unleashed firmware, driven where needed by the Vesper Android app (package `com.vesper.flipper`, launcher name **Vesper**). Every transmit, badge read, USB plug-in, and Wi-Fi action stays on equipment the company owns, inside the lab, under the written scope in section 2.

## 2. Authorization and safety

Read this aloud before the first station. A team that breaks it is disqualified (section 5).

- **Scope.** Internal lab only. Devices, badges, remotes, displays, and the demo laptop are company-owned or personally owned by the participant and explicitly listed on the authorization record. No production systems, no office door readers outside the lab bench, no coworker machines, no real credentials, no coworker badge data, no personal phones as targets.
- **Authorization record.** TODO(authorization: ticket or signed scope, date, approver). Keep it at the check-in table. If a station is not on that record, skip it.
- **Radio.** Transmit only on ISM bands that are legal in your region. Country/region: TODO(country/region). Unleashed can open frequencies outside that band. Stay on the regional allocation for every capture and replay. Never capture or replay a third-party remote, gate, vehicle, or a transmitter you do not own.
- **Fixed-code prop.** The Sub-GHz station uses one fixed-code remote the company owns, bought for this lab. If 433 MHz is not legal in TODO(country/region), substitute a fixed-code remote on a band that is, and record the frequency here: TODO(legal ISM frequency).
- **USB.** BadUSB and HID demos run only on the sacrificial Windows demo laptop. Lock or remove any other laptop from the station. The payload files open Calculator, Notepad, or a browser rickroll. They are not a place to type passwords.
- **Wi-Fi.** Scan and rogue-AP work use the ESP32-S2 devboard on a lab SSID the company controls. No deauthentication outside a shielded lab enclosure, and none against production access points. The clone SSID exists only for the minutes of demo D5, then the devboard is powered off.
- **Badges and documents.** Read and clone only badges issued for this lab, including the T5577 card. `emrtd` (e-passport reader) is installed with the Blue pack and is not a station. Do not put a passport or national ID on the Flipper.
- **Stop condition.** If a transmission, door, or host behaves in a way the station card does not describe, stop and call the organizer.

## 3. Hardware and setup checklist

Check these the day before. Install steps are in `docs/competition-install.md`.

| Item | Required for | Ready |
| --- | --- | --- |
| Flipper Zero, Unleashed firmware, SDK API **major 88**, microSD inserted, Bluetooth on | All stations | [ ] |
| Android phone, Vesper installed from this branch, paired over BLE | Pack install, file check | [ ] |
| ESP32-S2 Wi-Fi devboard, ESP32 Marauder flashed beforehand (ESP Flasher on the Flipper, or a PC flasher) | D5 only | [ ] |
| Sacrificial Windows demo laptop, no production data, USB device-control policy available for the blue half | D1 | [ ] |
| Own EM4100 (125 kHz) badge, one T5577 writable card, lab door reader that this badge opens | D3 | [ ] |
| Own MIFARE Classic badge that uses a weak or default key, own reader | D4 | [ ] |
| Own fixed-code remote on the legal ISM frequency from section 2. Optional: one own rolling-code fob, used only to show a failed replay | D2 | [ ] |
| Own TV or projector and its remote | D6 | [ ] |
| Own router or devboard with a labeled UART header; own 25-series SPI flash on a bench clip. Optional: PulseView on a laptop for Logic Analyzer (catalog text: Openbench Logic Sniffer mode with PulseView) | D7 | [ ] |
| Meeting room to "hide" the Flipper. Tracker sweep is done on the Android phone (section D8). A spare tracker the company owns may sit next to it | D8 | [ ] |

Stock Flipper applications used below (BadUSB, Sub-GHz, 125 kHz RFID, NFC, Infrared) ship with the firmware. They are not FapHub installs. Extra applications come from the packs in the appendix.

Fill in the bench card:

- Demo laptop: TODO(hostname / asset tag)
- Lab door reader: TODO(reader id)
- EM4100 badge: TODO(badge id)
- MIFARE Classic badge: TODO(badge id)
- Guest SSID to clone, lab AP only: TODO(lab guest SSID)
- UART board: TODO(board name). Pinout and baud: TODO(verify) against that board's schematic before the audience arrives
- TV / projector: TODO(make and model)

## 4. The demos

Each station is one minute of red, then the blue countermeasure on the same bench. Speak the quoted lines. Do the action on the same beat.

### D1. BadUSB HID injection

**(a) Goal.** Show that a USB device can type into a Windows session, then show a host policy that refuses a new keyboard.

**(b) Setup.** Sacrificial laptop signed in to a local demo account. Flipper USB cable is data-capable. Payloads on the Flipper SD (install guide): `/ext/badusb/demo_calc_windows.txt`, `/ext/badusb/demo_notepad_hello_windows.txt`, `/ext/badusb/demo_rickroll_windows.txt`. Second beat of the station: the same laptop with a USB device-control policy that blocks new HID devices. Optional same-point apps from the packs: `usb_hid_autofire`, `bettermouse` (Red), `mouse_jiggler` (Edge).

**(c) Expected result.** Red: Calculator or Notepad opens and receives the scripted keystrokes. Rickroll is the laugh beat only. Blue: the Flipper enumerates and the OS blocks it as a keyboard; the text never lands.

**(d) 60-second script.**

- **0:00–0:10.** Say: "This Flipper is about to act as a USB keyboard. The laptop is a sacrificial demo machine, and the script only opens Calculator." Action: seat the laptop so the screen faces the room. Confirm no other computer is in reach of the cable.
- **0:10–0:20.** Say: "Stock BadUSB app, file demo_calc_windows.txt, on our SD card." Action: on the Flipper, open the firmware BadUSB app and run `/ext/badusb/demo_calc_windows.txt`. TODO(verify) the on-screen run control label in this Unleashed build; the file path is the contract.
- **0:20–0:35.** Say: "The host trusted a device that claimed to be a keyboard." Action: wait until Calculator is in front. If this file is slow, say so and point at the `DELAY` lines as the reason (troubleshooting table). Do not switch to an unreviewed script.
- **0:35–0:45.** Say: "Same class of trick, different punchline: demo_rickroll_windows.txt. We are not running it unless we still have the screen." Action: show the filename. Run it only if Calculator already finished and the laptop is still the sacrificial one.
- **0:45–1:00.** Say: "Same cable, same Flipper. This image blocks new HID. The keyboard never types." Action: switch to the policy image (or apply the lab policy), plug in again, show the block. Optional: name `usb_hid_autofire` as the same idea with mouse clicks.

**(e) Blue countermeasure.** USB device control on that Windows laptop (Microsoft Defender Device Control, or the Intune policy pushed to this demo image). The audience sees the connect attempt and the block, with Calculator staying closed.

**(f) Mitigations.**

- USB port policy / device control (Defender Device Control, Intune, USBGuard on Linux).
- Lock the screen on idle so a plugged-in keyboard has no session to type into.
- User awareness: a "keyboard" that plugs itself in is a hostile device.
- Hardware USB data blockers on ports that only need to charge.

### D2. Sub-GHz fixed-code capture and replay

**(a) Goal.** Capture the company's own fixed-code remote and replay it, then show why a rolling-code fob does not fall the same way.

**(b) Setup.** Own fixed-code remote on the frequency from section 2. Stock Sub-GHz app for the capture. Analysis apps: `sub_analyzer`, `subhound` (Red), `protoview` (Radio). Optional trimmer: `subghz_raw_edit`. Blue listeners: `spectrum_analyzer` (Radio), `radio_scanner` (Blue). If `competition-pack/subghz/demo_remote_TODO.sub` is present, treat it as a placeholder. Replace it with a capture of this remote before anyone transmits it. A rolling-code fob the company owns is optional and is only used to fail a replay.

**(c) Expected result.** Red: the receiver that belongs to this remote actuates on replay. The analyzer names the modulation or shows the raw timing. Rolling code: the replay does not actuate. That failure is the lesson. Blue: the spectrum or scanner shows the energy while the remote or the Flipper transmits.

**(d) 60-second script.**

- **0:00–0:10.** Say: "Fixed-code means the remote sends the same bits every press. This remote is ours, on our band." Action: show the remote and the matching receiver. State the frequency on the bench card.
- **0:10–0:25.** Say: "Stock Sub-GHz, read, one press, save." Action: start a read in the stock Sub-GHz app, press the own remote once, save the `.sub` under `/ext/subghz/`.
- **0:25–0:40.** Say: "Replay should move our receiver. Then Sub Analyzer or Subhound tells us what we caught. ProtoView draws the pulses." Action: replay the saved file at the own receiver. Open `sub_analyzer` or `subhound` on that file. If the capture is BinRAW and unnamed, use Subhound. Flip to `protoview` only if the first tool already made the point.
- **0:40–0:50.** Say: "A rolling-code fob changes the bits every press. Replaying yesterday's capture does nothing. That silence is the defense." Action: if the rolling-code fob is on the bench, capture once, press the real fob again (so the receiver advances), replay the old capture, and show no actuation. If no rolling-code fob is present, say that sentence and point at the mitigations. Do not hunt a stranger's fob.
- **0:50–1:00.** Say: "Blue side is a receiver, not a jammer. Spectrum Analyzer or Radio Scanner should show this transmission while we press." Action: start `spectrum_analyzer` or `radio_scanner`, press the own remote once, let the room see the hit.

**(e) Blue countermeasure.** `spectrum_analyzer` (Radio pack) or `radio_scanner` (Blue pack). The audience sees a live trace or scanner hit at the remote's frequency when the authorized transmitter keys up, and a quiet trace when it does not.

**(f) Mitigations.**

- Rolling codes (KeeLoq and the same family) so a captured frame is spent.
- Jamming and anomaly detection on the receiver: energy without a valid rolling code is an alarm, not a command.
- Receiver pairing hygiene: drop unused fixed-code IDs, record which transmitters are enrolled.
- Alarm on an unexpected transmitter ID.

### D3. RFID 125 kHz badge clone

**(a) Goal.** Read the lab's own EM4100 badge, write it to a T5577 card, and open the lab reader with the clone. Then show an audit of that badge population.

**(b) Setup.** Own EM4100 badge, blank T5577, lab reader that this badge opens. Stock 125 kHz RFID app for the read. `t5577_writer` (T5577 Raw Writer) for the write. `rfid_maker` can build a file from a facility code and card number if the stock read already displayed them. Blue: `access_audit`.

**(c) Expected result.** Red: the lab reader accepts the T5577 the same way it accepts the original badge. Blue: Access Audit reports the technology and the weakness (static ID, no crypto) on a badge the room can see.

**(d) 60-second script.**

- **0:00–0:10.** Say: "125 kHz EM4100 is a number in the clear. This badge and this reader are the lab pair on the authorization card." Action: show the original badge and the blank T5577 side by side.
- **0:10–0:25.** Say: "Stock RFID app, read." Action: read the badge. Leave the ID on screen long enough to read aloud the last few digits only; do not photograph a full production badge.
- **0:25–0:40.** Say: "T5577 Raw Writer writes that ID onto a card we can see is a clone." Action: open `t5577_writer` and write the blocks from the read onto the T5577. TODO(verify) the writer’s on-screen field names in the installed build; stay inside the flow that copies this read, and do not type a badge ID from outside the lab set.
- **0:40–0:50.** Say: "The reader cannot tell the clone from the original." Action: present the T5577 to the lab reader. Then present the original so the room sees both succeed.
- **0:50–1:00.** Say: "Access Audit on the same badge: this population is still 125 kHz. That is the finding." Action: open `access_audit` and run it on the original badge.

**(e) Blue countermeasure.** `access_audit` (Blue pack, installed at `/ext/apps/Tools/access_audit.fap`). The audience sees the app identify the badge type and the lack of cryptographic access control.

**(f) Mitigations.**

- Migrate 125 kHz to MIFARE DESFire EV3 or HID Seos with diversified keys.
- Badge rotation: retire lost cards in the reader, not only in a spreadsheet.
- Tamper alarms on the reader and the door contact.
- MFA at doors for sensitive rooms, so a cloned card is not sufficient.

### D4. NFC MIFARE Classic weak keys

**(a) Goal.** Read the lab's own MIFARE Classic badge with the stock NFC app and its default key dictionary, and show what a magic card check and an iClass Legacy read add. Then audit the same fleet.

**(b) Setup.** Own MIFARE Classic badge and own reader. Stock NFC app. Edge pack: `nfc_magic` (sector 0 / magic-card check), `picopass` (PicoPass / iClass on the lab's own iClass Legacy reader, only if that reader and card are on the authorization record), `weebo` (NTAG215, only if the prop is an NTAG the company owns). Blue: `access_audit`, `seader`. `seos` is the check for a Seos credential the company owns; skip it if the lab badges are Classic-only.

**(c) Expected result.** Red: the stock NFC app reads sectors whose keys are in the default dictionary and shows that those keys are not secret. `nfc_magic` reports whether the lab's spare card is a magic card. Blue: Access Audit or Seader states the credential type and whether default keys are in use.

**(d) 60-second script.**

- **0:00–0:10.** Say: "MIFARE Classic keys live on the card. If the key is a factory default, a phone-sized reader can walk the sectors. This badge is ours." Action: show the lab Classic badge.
- **0:10–0:30.** Say: "Stock NFC, default dictionary, read." Action: read the badge in the stock NFC app with the default key list. Show a sector that opens. Do not display a full dump of anything that looks like a production credential; this badge must be a lab issue.
- **0:30–0:40.** Say: "NFC Magic answers a narrower question: can this spare card rewrite sector 0? PicoPass is the same conversation for our iClass Legacy reader, if we brought that card." Action: run `nfc_magic`'s check on the spare card. Open `picopass` only when the iClass Legacy prop is on the bench.
- **0:40–0:50.** Say: "The reader accepted this because the keys never changed. That is a deployment bug, not a clever attack." Action: tap the lab reader with the original badge so the room sees the intended access still works.
- **0:50–1:00.** Say: "Blue: Access Audit, and Seader if this fleet includes iClass or DESFire. We want a list of weak keys, not a collection of other people's cards." Action: run `access_audit` on the lab badge. If the badge is iClass or DESFire EV1/EV2, run `seader` on that same card.

**(e) Blue countermeasure.** `access_audit` and, for HID iClass / iClass SE / DESFire / Seos cards the company owns, `seader`. The audience sees a per-badge verdict: algorithm, and whether a default key answered.

**(f) Mitigations.**

- Migrate to DESFire or Seos.
- Diversified keys per card. No default keys anywhere, including spare and test cards.
- Badge rotation policy with reader-side revocation.
- The same door MFA as D3 for sensitive areas.

### D5. Wi-Fi recon and rogue AP

**(a) Goal.** Scan the lab from an ESP32-S2 running Marauder, list the networks, and bring up a temporary clone of the company guest SSID on that devboard. Then find that clone with a second scan.

**(b) Setup.** ESP32-S2 on the Flipper GPIO header, Marauder already flashed (install guide). Red apps that need the board, marked `needsModule` in the catalog: `esp32_wifi_marauder`, `esp_flasher`, `flipper_http`. Blue scan app: `flip_wifi` (also `needsModule`; catalog text says it needs FlipperHTTP). Lab AP only, SSID TODO(lab guest SSID). Their cards read `Devboard` and the detail sheet reads `Needs WiFi devboard`. They install with the Red or Blue pack and run only with the devboard attached.

**(c) Expected result.** Red: a network list that includes the real lab AP, then a second SSID with the guest name coming from the devboard. Blue: the same scan shows two sources for that name. The devboard is powered off before the team leaves the station. No deauthentication. No production AP.

**(d) 60-second script.**

- **0:00–0:10.** Say: "The Flipper has no Wi-Fi radio. This ESP32-S2 does. Marauder is already on it. We are scanning our lab, and we will not transmit at the building." Action: show the devboard on the GPIO header. Confirm the lab AP is the one on the bench card.
- **0:10–0:25.** Say: "WiFi Marauder, scan, read the list." Action: open `esp32_wifi_marauder` and run the scan view of this Marauder build. TODO(verify) the menu labels for the Marauder version flashed on this board; stop at the scan results. Do not open a deauth or handshake action.
- **0:25–0:45.** Say: "Rogue AP means a second transmitter using our guest name, on hardware we own, in this room, for this minute." Action: start a lab-only clone of TODO(lab guest SSID) from the devboard. TODO(verify) the Marauder menu entry for that AP mode in the flashed build. Use a channel the lab AP owner reserved for the demo.
- **0:45–1:00.** Say: "Blue scan, same room. Two answers for one name. Then the devboard goes off." Action: open `flip_wifi` (FlipperHTTP must already be installed) and scan. Show the duplicate. Power off the ESP32 before the next team.

**(e) Blue countermeasure.** `flip_wifi` scan (Blue pack) of the same room. The audience sees the legitimate lab SSID and the clone the station just started, then sees the clone disappear when the devboard loses power.

**(f) Mitigations.**

- WIDS/WIPS monitoring on the corporate wireless network.
- 802.1X on the corporate SSID, so a cloned name is not enough to join.
- Guest isolation on the guest SSID.
- Periodic rogue-AP sweeps with the same scan the blue team just ran.
- Evil-twin awareness: HTTPS and HSTS, and a VPN when the network is not the corporate one.

### D6. Infrared replay

**(a) Goal.** Learn the lab TV or projector remote and replay power or volume, then say clearly that infrared has no authentication.

**(b) Setup.** Own TV or projector and its remote. Learn with `xremote` (Cross Remote, Red) or the stock Infrared app. Carrier experiments: `flipirfreq`. Watch the remote's bursts with `ir_scope` (Radio). If `competition-pack/infrared/demo_tv_power_TODO.ir` is on the card, it is a placeholder. Learn the real remote before transmitting. Point the Flipper at this display only.

**(c) Expected result.** Red: the display toggles power or changes volume from the Flipper. `ir_scope` draws the incoming burst while the real remote is pressed. Blue: the same replay, narrated as "anyone in the room who can see the receiver can send this," because the protocol does not authenticate the remote.

**(d) 60-second script.**

- **0:00–0:10.** Say: "Infrared is line of sight and a shared code. This remote belongs to this display." Action: point at the TV or projector and the receiver window.
- **0:10–0:25.** Say: "Learn one button. Stock Infrared app or Cross Remote." Action: learn power or volume from the own remote into `xremote` or the stock IR app.
- **0:25–0:40.** Say: "Replay. The display does not know who sent it." Action: replay that button. Wait for the on-screen change.
- **0:40–0:50.** Say: "IR Scope watches the real remote. FlipIRFreq is how we would change carrier if a stubborn set ignored the learned file. We stay on this set." Action: open `ir_scope`, press the real remote once, show the trace. Mention `flipirfreq` without retuning onto a different display.
- **0:50–1:00.** Say: "Blue is not another app. Blue is the absence of a check. There is no key, no rolling code, no pairing. Physical control of the room is the control." Action: replay once more while saying that, then put the Flipper down.

**(e) Blue countermeasure.** No cryptographic countermeasure exists to demonstrate on this link. Show `ir_scope` so the audience sees the command in the clear, then state that any emitter aimed at the receiver can send it. The "tool" is the lack of authentication, made visible by the scope.

**(f) Mitigations.**

- Physical controls on shared meeting-room gear (locked remote, staff-only cabinet, display input lock).
- Network-managed displays instead of IR for rooms that matter.
- Awareness that a "harmless" port is still a control channel.

### D7. GPIO hardware audit

**(a) Goal.** Show a debug port and a SPI flash on hardware the company owns, then show the inventory question those ports create.

**(b) Setup.** Own router or devboard with a UART header the organizer has already identified. Own 25-series SPI flash in a test clip, not a production board that must still boot tomorrow. Apps: `uart_terminal`, `spi_mem_manager`, `logic_analyzer`, `signal_generator` (all Red, no Wi-Fi board). Logic Analyzer's catalog description is Openbench Logic Sniffer mode with PulseView, so a laptop running PulseView is part of that optional beat. TODO(verify) pinout, voltage, and baud against the bench board's schematic before the audience arrives. Do not probe an unlabeled production chassis.

**(c) Expected result.** Red: UART shows a boot log or a shell that this lab image already exposes. SPI Mem Manager shows the chip identify response (and a dump only of this bench part). Blue: a written inventory row that maps "this header exists on this product" to "it is locked, coated, or absent in the production build."

**(d) 60-second script.**

- **0:00–0:10.** Say: "Debug headers survive into hardware people forget to close. This board is ours, and the header is labeled." Action: show the bench board and the marked GND/TX/RX. Do not clip a random connector.
- **0:10–0:25.** Say: "UART Terminal, the baud on the bench card." Action: open `uart_terminal`, set the verified baud, and show the boot log or the lab root shell. If it is a shell, run a read-only command such as printing the banner. Do not change the board's configuration on stage.
- **0:25–0:40.** Say: "SPI Mem Manager, 25-series flash, test clip, our part." Action: seat the clip on the bench flash and read the chip ID. A short dump is in scope only for this part. Stop when the ID and the first page are visible.
- **0:40–0:50.** Say: "Logic Analyzer plus Signal Generator is the same header used as a probe and as a playback. PulseView is the viewer the app expects." Action: if PulseView is cabled, show one captured line. Otherwise point at the two apps and move on. TODO(verify) the PulseView connection for this Flipper build if that beat is in the show.
- **0:50–1:00.** Say: "Blue is the inventory. Every debug port on company hardware gets a row: present, locked, or removed." Action: show the one-page inventory for this product (TODO(product name on the inventory sheet)) with UART and JTAG called out.

**(e) Blue countermeasure.** The inventory sheet, filled with the same tools (`uart_terminal`, `spi_mem_manager`, `logic_analyzer`) during a lab audit. The audience sees a port that speaks on the bench unit and a production row that says the port is disabled, fused, or coated.

**(f) Mitigations.**

- Disable or lock UART in production firmware.
- eFuse or JTAG lock.
- Conformal coat or epoxy on programming pins.
- Firmware signing so a dumped image is not a writable copy.
- Repeat this audit with these apps when a board rev changes.

### D8. BLE tracker plant and sweep

**(a) Goal.** Plant the Flipper as a Find My beacon, then sweep the room from an Android phone. The Flipper catalog in this app has no AirTag-class detector. The sweep leg is phone-side on purpose.

**(b) Setup.** `findmy` (FindMy Flipper, Edge pack, `/ext/apps/Bluetooth/findmy.fap`). Catalog description: "BLE Find My beacon so you can locate this Flipper." Hide the Flipper in the meeting room in a place the organizer can retrieve. Sweep phone: Android Settings. A spare tracker the company owns may be planted as well. Disconnect Vesper's BLE session before the plant so the Flipper is free to advertise.

**(c) Expected result.** Red: the Flipper is out of sight and advertising. Blue: the phone's unknown-tracker scan or a BLE scanner app reports a device in the room. The organizer retrieves the Flipper before the next talk.

**(d) 60-second script.**

- **0:00–0:10.** Say: "FindMy Flipper turns this Flipper into a Find My beacon. We are hiding our own device, in our own room." Action: open `findmy` and start the beacon mode this build provides. TODO(verify) the on-screen start control in the installed version.
- **0:10–0:25.** Say: "Plant. I am putting it under the lectern, and I am the person who picks it up." Action: place the Flipper. Keep the retrieval spot written on the bench card.
- **0:25–0:35.** Say: "Detection is not a Flipper app. We checked the official catalog list inside Vesper. There is no AirTag-class detector in that list. The sweep is the phone." Action: hold up the Android phone. Leave the Flipper where it is.
- **0:35–0:50.** Say: "Android Settings, Safety and emergency, Unknown tracker alerts, manual scan. On a skinned phone the path may differ; the bench card has the path we confirmed." Action: run that scan. TODO(verify) the exact Settings path on this phone's Android version. If the OEM has no unknown-tracker scan, use a BLE scanner app and say that this is a beacon list, not Apple's tracker alert.
- **0:50–1:00.** Say: "Policy is: sensitive rooms get this sweep, alerts stay on, and someone actually walks the room." Action: retrieve the Flipper and power the beacon off.

**(e) Blue countermeasure.** Phone-side detection. Primary path, when the phone provides it: Android Settings → Safety & emergency → Unknown tracker alerts → manual scan. Alternate: a BLE scanner app on the same phone. The audience sees a hit while walking toward the plant, then an empty sweep after retrieval.

**(f) Mitigations.**

- Tracker-sweep policy for sensitive rooms before and after external meetings.
- Unknown-tracker alerts enabled by device policy on company phones.
- A physical sweep habit, because the detector is the phone a person carries, not the Flipper that was planted.

## 5. Scoring

Suggestions, not a locked rule set. TODO(point values) if the organizer wants different weights.

- Eight stations, D1 through D8. Teams rotate on a fixed clock. One Flipper and one phone per station, reset to the bench card between teams.
- A station scores when the red result matches section 4(c) and the team names the blue control from section 4(e) without being prompted. Suggested split: 2 points for the red result, 2 points for the correct blue pairing, 1 point for one mitigation from 4(f) in the team's own words.
- Calling a rolling-code failure (D2) or a phone-side sweep (D8) incorrectly is a missed point, not a bonus. The failure and the phone sweep are the correct results.
- Safety violations in section 2 are disqualification for the team, not a point deduction. Examples: a production AP, a badge that is not on the lab list, a USB cable into a non-sacrificial laptop, a transmission outside the recorded band, leaving the rogue AP powered after the station.
- Ties break on fewer safety warnings, then on a shorter written mitigation for D7's inventory.

## 6. Troubleshooting

| Symptom | What to do |
| --- | --- |
| App will not install, or the snackbar names an SDK API | The phone downloads a `.fap` from `https://catalog.flipperzero.one` for the SDK API the Flipper reports (`firmware_api` major.minor over RPC, with a same-major fallback). Unleashed is pinned to major 88 when the banner says Unleashed and no API was parsed. The Device screen shows **Firmware**, not the API number. The API shows up in the install error: "No \<alias\> build is available for your firmware's SDK (API …)". Update Unleashed so the reported major matches a catalog build, then retry. Full steps are in the install guide. |
| BLE drops | On the Flipper: Settings → Bluetooth, on, and not already connected to qFlipper or another phone. In Vesper: Device tab, status **Connected**, disconnect control (icon content description "Disconnect"), then **Scan for Flipper**. While connected, Device → **Advanced Operations** → **Recover & Scan** → **Run** disconnects and starts scanning. README also says: toggle phone Bluetooth, stay within about 1 meter, grant Location (Android requires it for the scan). |
| BadUSB does nothing | Data-capable cable, Flipper USB mode actually in the BadUSB app, laptop unlocked on the sacrificial account. Prefer `demo_calc_windows.txt`. If the script starts and stalls, the file's `DELAY` lines are the first place to look; a longer delay is the usual fix. Do not replace the file with an unreviewed payload on stage. |
| Sub-GHz replay does nothing | On a rolling-code receiver this is the expected result and the blue talking point. On the fixed-code prop: confirm the saved file is from this remote, the frequency is the one on the bench card, and Unleashed has not been left on an extended band. |
| Wi-Fi apps look installable and then do nothing on the Flipper | Vesper does not grey them out, but their cards read `Devboard`. `esp32_wifi_marauder`, `esp_flasher`, `flipper_http`, and `flip_wifi` have `needsModule = true`. They need the ESP32-S2 on the GPIO header and, for FlipWiFi, FlipperHTTP. **Install all** (the button shown when the **All bare** chip is selected) skips these four. Select **Red Team** or **Blue Team** and tap **Install pack** so they are copied. |
| Pack button seems idle | The button text is `Install pack`. While it runs, the line under the chips becomes `Installing N/M: <app name>`. A failure snackbar reads `Could not install <names>. Connect the Flipper and update its firmware, then retry.` |
| Tracker sweep finds nothing | Confirm `findmy` is still running and Vesper has disconnected BLE. Confirm the phone path on the bench card (TODO(verify) for this Android build). Walk closer. The Flipper is not the scanner. |

## 7. Appendix

### How install paths are built

`FapHubCatalog.installPath` writes `/ext/apps/<installFolder>/<catalogAlias>.fap`. The phone creates that directory, then writes the file over BLE. Counts below match `FapHub.kt` on `feature/competition-pack` at the time this runbook was written. The chip label is the live count (`Red Team (17)`, `Blue Team (6)`, and the same pattern for the other packs).

`needsModule = true` is marked with an asterisk. Those apps still install. They run with the Wi-Fi devboard.

### Red Team (17)

| Alias | Name on the chip card | Flipper path |
| --- | --- | --- |
| `sub_analyzer` | Sub Analyzer | `/ext/apps/Sub-GHz/sub_analyzer.fap` |
| `subghz_raw_edit` | Sub-GHz RAW Edit | `/ext/apps/Sub-GHz/subghz_raw_edit.fap` |
| `subhound` | Subhound | `/ext/apps/Sub-GHz/subhound.fap` |
| `t5577_writer` | T5577 Raw Writer | `/ext/apps/RFID/t5577_writer.fap` |
| `rfid_maker` | RFID Maker | `/ext/apps/RFID/rfid_maker.fap` |
| `wiegand_reader` | Wiegand Reader | `/ext/apps/GPIO/wiegand_reader.fap` |
| `esp32_wifi_marauder` * | WiFi Marauder | `/ext/apps/GPIO/esp32_wifi_marauder.fap` |
| `esp_flasher` * | ESP Flasher | `/ext/apps/GPIO/esp_flasher.fap` |
| `flipper_http` * | FlipperHTTP | `/ext/apps/GPIO/flipper_http.fap` |
| `xremote` | Cross Remote | `/ext/apps/Infrared/xremote.fap` |
| `flipirfreq` | FlipIRFreq | `/ext/apps/Infrared/flipirfreq.fap` |
| `signal_generator` | Signal Generator | `/ext/apps/GPIO/signal_generator.fap` |
| `uart_terminal` | UART Terminal | `/ext/apps/GPIO/uart_terminal.fap` |
| `spi_mem_manager` | SPI Mem Manager | `/ext/apps/GPIO/spi_mem_manager.fap` |
| `logic_analyzer` | Logic Analyzer | `/ext/apps/GPIO/logic_analyzer.fap` |
| `usb_hid_autofire` | USB HID Autofire | `/ext/apps/USB/usb_hid_autofire.fap` |
| `bettermouse` | Better Mouse | `/ext/apps/USB/bettermouse.fap` |

`t5577_writer` and `rfid_maker` use the install folder `RFID`, so they do not land under `NFC`.

### Blue Team (6)

| Alias | Name on the chip card | Flipper path |
| --- | --- | --- |
| `access_audit` | Access Audit | `/ext/apps/Tools/access_audit.fap` |
| `seader` | Seader | `/ext/apps/NFC/seader.fap` |
| `seos` | Seos Compatible | `/ext/apps/NFC/seos.fap` |
| `radio_scanner` | Radio Scanner | `/ext/apps/Sub-GHz/radio_scanner.fap` |
| `flip_wifi` * | FlipWiFi | `/ext/apps/GPIO/flip_wifi.fap` |
| `emrtd` | eMRTD Reader | `/ext/apps/NFC/emrtd.fap` |

`access_audit` is category NFC in the model and install folder `Tools`. `emrtd` is not a station (section 2).

### Cross-pack references

Install these packs from the same FapHub screen (install guide). They are not inside Red or Blue.

| Pack | Alias | Name | Flipper path | Used in |
| --- | --- | --- | --- | --- |
| Edge | `nfc_magic` | NFC Magic | `/ext/apps/NFC/nfc_magic.fap` | D4 |
| Edge | `picopass` | PicoPass | `/ext/apps/NFC/picopass.fap` | D4 |
| Edge | `weebo` | Weebo | `/ext/apps/NFC/weebo.fap` | D4, NTAG prop only |
| Edge | `mouse_jiggler` | Mouse Jiggler | `/ext/apps/USB/mouse_jiggler.fap` | D1 optional |
| Edge | `findmy` | FindMy Flipper | `/ext/apps/Bluetooth/findmy.fap` | D8 |
| Radio | `protoview` | ProtoView | `/ext/apps/Sub-GHz/protoview.fap` | D2 |
| Radio | `spectrum_analyzer` | Spectrum Analyzer | `/ext/apps/Sub-GHz/spectrum_analyzer.fap` | D2 |
| Radio | `ir_scope` | IR Scope | `/ext/apps/Infrared/ir_scope.fap` | D6 |
| Daily | `subghz_remote_ofw` | Sub-GHz Remote | `/ext/apps/Sub-GHz/subghz_remote_ofw.fap` | Optional five-slot remote for saved `.sub` files |
| Daily | `quac` | Quac! | `/ext/apps/Tools/quac.fap` | Optional launcher for saved IR, Sub-GHz, NFC, RFID, iButton files |

Edge also contains `metroflip` and `cyborg_detector`. Radio also contains `weather_station`, `pocsag_pager`, and `longwave_clock`. They are not part of these eight stations.

### Asset files

Intended layout and Flipper destinations:

| Repo file | Flipper path | Role |
| --- | --- | --- |
| `competition-pack/badusb/demo_calc_windows.txt` | `/ext/badusb/demo_calc_windows.txt` | D1 primary |
| `competition-pack/badusb/demo_notepad_hello_windows.txt` | `/ext/badusb/demo_notepad_hello_windows.txt` | D1 alternate |
| `competition-pack/badusb/demo_rickroll_windows.txt` | `/ext/badusb/demo_rickroll_windows.txt` | D1 laugh beat |
| `competition-pack/subghz/demo_remote_TODO.sub` | `/ext/subghz/demo_remote_TODO.sub` | Placeholder. Replace with a capture of the own remote before transmit |
| `competition-pack/infrared/demo_tv_power_TODO.ir` | `/ext/infrared/demo_tv_power_TODO.ir` | Placeholder. Replace with a learn of the own display before transmit |
| `competition-pack/nfc/README.md` | Not a Flipper payload | Badge-audit workflow notes; do not invent tag data from it |
| `competition-pack/manifest.json` | Not copied to the Flipper | Pack index |
| `competition-pack/compatibility-evidence.json` | Not copied to the Flipper | Per-app build proof: alias, version, sha256 of the verified 88.2 FAP |
| `competition-pack/validate_pack.py` | Run on the computer from `competition-pack/` | Sanity check of the pack files |

Do not transmit a file whose name still contains `TODO`.

### Code corrections worth knowing on stage

- The FapHub header always reads `Bare Flipper pack • 60 apps • <n> on device`. The 60 are every catalog app except the four `needsModule` apps. Red and Blue non-module apps are inside that 60.
- Cards for the four module apps print `Devboard` and the detail sheet prints `Needs WiFi devboard` in red; every other card prints `Bare` / `Bare Flipper`.
- The chip row starts with `All bare`, then `Daily (17)`, `Radio (6)`, `Edge (7)`, `Play (11)`, `Red Team (17)`, `Blue Team (6)`.
- With `All bare` selected, the button says `Install all` and installs the 60 apps that do not need a module. It does not install the Wi-Fi apps.
