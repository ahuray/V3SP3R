# Vesper guide

Vesper is an Android app for a Flipper Zero. You talk to it in Chat, or you open a tool and drive the Flipper yourself. The phone is the control surface. The Flipper is the radio, the badge, the infrared blaster, and the USB keyboard.

Use a Flipper and a network you are allowed to test. High-risk actions ask first. The audit log keeps what ran.

## Set up

1. On the Flipper: Settings, Bluetooth, on. Update firmware with qFlipper if you can.
2. Build a debug APK (`./gradlew :app:assembleDebug`) and install it, or open the project in Android Studio and run it. Android 8 or newer. Bluetooth is required.
3. In the app: Settings, paste an API key, pick a model.
4. Device tab, Scan for Flipper, tap your device. USB is the other path.
5. Chat tab. Ask for something the Flipper can actually do.

Location permission is required for Bluetooth scanning on Android. Notifications keep the connection service visible.

## Tabs

| Tab | What it is |
| --- | --- |
| Chat | Text, voice, and photos. The model reads Flipper state and proposes commands. You approve the ones that need it. |
| Alchemy | Describe a payload. The Forge writes it. The Vault keeps what you made. |
| FapHub | Flipper app catalog, plus community repos you can search and copy onto the device. |
| Device | Two pages. Flipper connects, shows the LCD, and drives the buttons. Accessories is the board list. |
| Settings | API key, model, permissions, dark mode, voice, glasses, audit retention. |

Five tabs is the whole bottom bar. Everything else opens from Chat or Device.

## Tools

Open these from the Device tab. The kit is split into Defend and Attack. Audit is also linked from Chat.

| Tool | What it is |
| --- | --- |
| Files | Browse, read, and manage files on the Flipper. |
| Audit | Commands, approvals, errors, and replays for this session. |
| Signals | Radio, infrared, and other captures stored on the Flipper. |
| Oracle | A closer read of one captured signal. |
| Payloads | Generate BadUSB, Sub-GHz, infrared, NFC, and portal files, then save them to the Flipper. |
| Stage | Demo files grouped as safe or live. Push them, then run a beat. Live beats ask first. |
| Ops | Link health, recovery runbooks, and button macros. |

Device, while connected, also shows the live Flipper screen and Up, Down, Left, Right, OK, and Back. Hold a button to hold the key. The Live screen switch sits under the picture and does not move when you toggle it. Pausing keeps the last frame in the same place. Disconnect is on the button card.

The remote buttons sit above the passport so the pad is the first control under the picture. The Passport card sets the dolphin's mood and portrait, and which SD-card animation plays most often. Mood and portrait restart the Flipper. The name on the passport is the hardware name. Official firmware does not offer a safe rename from the phone. Custom animations belong in `/ext/dolphin`.

The Accessories page lists boards you mark as mounted. Official boards are the Wi-Fi Developer Board, the Video Game Module, and the Prototyping Boards. Unofficial boards, including 5Ghost, stay hidden until that switch is on. A marked board is described only while the Flipper is connected. The app does not detect a board electrically. Nothing is mounted until you say so.

The home-screen widget launches saved signals. Add it from the Android widget picker and assign up to four captures. The Flipper has to be connected for the list to fill.

## What the model is allowed to do

Chat does not touch Bluetooth itself. It calls one command interface. The app grades each call:

| Grade | What happens |
| --- | --- |
| Low | Runs. Reads, device info, lights, vibration. |
| Medium | Shows a diff or asks you to look before a write, a CLI command, a launch, or an emulate. |
| High | Asks before delete, Sub-GHz transmit, or BadUSB. |
| Blocked | System and firmware paths. Unlock them in Settings if you mean to. |

Settings can auto-approve a grade when you want fewer prompts. Blocked paths still need an unlock. The audit log records the action either way.

The command list and risk grades are also in the README table "Supported Actions". The machine-readable contract is `docs/execute_command_schema.json`.

## Chat input

- Type, or hold the mic for on-device speech.
- Attach a photo from the gallery or the camera. The model sees the image.
- If smart glasses are connected, the mic button mutes the glasses instead.

Glasses are optional. Run `mentra-bridge/`, then in Settings turn Smart Glasses on and set the bridge URL. Voice from the glasses arrives as chat. The model can ask the glasses for a photo.

## Settings that matter

- **API Configuration.** Provider, key, model, how many tool rounds one reply may take.
- **Connection.** Auto-connect to the last Flipper.
- **Permissions.** Project path, how long a grant lasts, and auto-approve for medium and high risk. Blocked paths still need an unlock.
- **User Interface.** Dark or light, and haptic feedback.
- **Voice.** Spoken replies, and whether they start on their own.
- **Smart Glasses.** The bridge fields stay on screen. They are inactive until glasses are enabled.
- **Data.** How long audit rows are kept.

## When something fails

- Flipper missing from the scan: Bluetooth on, no other phone or qFlipper connected, Location allowed, stay close.
- No model reply: key, credit, and network. Try another model.
- A tool call fails to parse: retry, or switch model. The app repairs some bad JSON and still rejects the rest.
- Permission denied: Settings, Permissions. Protected paths stay locked until you open them.
- Stage or Ops looks idle: connect on the Device tab first. Those tools use the same link.

## Demo pack

`docs/competition-install.md`, `docs/competition-runbook.md`, `docs/competition-live-runbook.md`, and `docs/competition-presenter-notes.md` are the security-team demonstration. They do not add hidden app features, and they do not relax the risk grades above.
