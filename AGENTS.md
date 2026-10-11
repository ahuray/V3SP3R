# Vesper agent guide

Vesper is an Android app that drives a Flipper Zero. The model proposes commands. The app decides whether they run.

Read [docs/guide.md](docs/guide.md) for what each screen does. Read [docs/architecture.md](docs/architecture.md) for the layers. This file is only the rules for changing the repo.

## Do not drop a feature

The screens in `MainActivity` are the product. Chat, Alchemy, FapHub, Device, and Settings are the tabs. Files, Audit, Spectral Oracle, Signal Arsenal, Payload Lab, Stage Remote, and Ops open from those tabs. Keep all of them working.

These modules are in the tree and are not a tab. Leave them unless the task is specifically about them:

- `ui/screen/DeviceTrackerScreen.kt`
- `ui/viewmodel/ChimeraLabViewModel.kt`
- `web/WebFileServer.kt`

## Build and test

JDK 17. Android SDK. From the repo root:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Install with `adb install -r` that APK. Package name: `com.vesper.flipper`.

Do not commit `local.properties`, keystores, or API keys. Keys live in encrypted preferences on the device.

## Where code lives

```
app/src/main/java/com/vesper/flipper/
  ui/screen/        Compose screens
  ui/viewmodel/     Screen state
  ui/theme/         Type scale and colors
  ai/               OpenRouter client, prompts, agent loop
  ble/              Flipper BLE, USB, screen mirror, protocol
  domain/executor/  Risk checks and command execution
  domain/service/   Audit, diff, permissions
  glasses/          Mentra bridge client
  voice/            Speech recognition and TTS
  widget/           Home-screen signal widget
```

`docs/execute_command_schema.json` is the command contract the model uses. `docs/guide.md` is the human manual. `docs/competition-*.md` and `competition-pack*/` are the lab demo, not the product manual.

Accessory ids live in `domain/model/FlipperAccessory.kt`. The mounted set is `mounted_accessories` in `SettingsStore`. It starts empty. Passport codec is `domain/model/FlipperPassport.kt`. The running model prompt is `ai/VesperPrompts.kt`, not `docs/vesper_system.txt`.

## Safety invariants

- New device actions go through `CommandExecutor` and `RiskAssessor`.
- Low risk can run. Medium risk shows a diff. High risk asks. Blocked paths stay blocked until the user unlocks them.
- Every executed action is written to the audit log.
- Do not add a path that sends BLE or CLI commands from the model without that gate.

## UI

Type sizes live in `ui/theme/Theme.kt`. Do not set a text style below `labelSmall` (12sp). The Device tab's live Flipper picture is a 128×64 LCD and must stay full width at that aspect. The Live switch stays in a fixed row under that picture. Toggling it must not change the picture's height. Remote buttons stay above the passport. Passport edits go through `FlipperPassport` and `/int/.dolphin.state`. Do not invent a hardware-name writer. Accessories start unmounted. Do not put a board on the Device screen by default. `MainActivity` owns edge-to-edge insets; child screens should not add a second copy of the same padding.
