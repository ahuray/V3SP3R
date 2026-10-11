```
 ██╗   ██╗ ██████╗ ███████╗██████╗ ███████╗██████╗
 ██║   ██║╚════██╗██╔════╝██╔══██╗██╔════╝██╔══██╗
 ██║   ██║ █████╔╝███████╗██████╔╝█████╗  ██████╔╝
 ╚██╗ ██╔╝ ╚═══██╗╚════██║██╔═══╝ ██╔══╝  ██╔══██╗
  ╚████╔╝ ██████╔╝███████║██║     ███████╗██║  ██║
   ╚═══╝  ╚═════╝ ╚══════╝╚═╝     ╚══════╝╚═╝  ╚═╝
```

# V3SP3R — The AI Brain for Your Flipper Zero

> **Talk to your Flipper Zero like it's your partner-in-hacking.** Vesper turns your pocket hacking tool into an AI-powered command center — controlled entirely through natural language from your Android device or smart glasses.

Chat is the main way in. The other screens are there when you want them. The map is [docs/guide.md](docs/guide.md).

**Start here.** [Guide](docs/guide.md) is the map of every screen. [AGENTS.md](AGENTS.md) is the same map for a coding agent, plus how to build and what not to break. [Architecture](docs/architecture.md) is how the layers fit.

This project is a fork of [elder-plinius/V3SP3R](https://github.com/elder-plinius/V3SP3R), which is licensed under [AGPL-3.0](https://github.com/elder-plinius/V3SP3R?tab=AGPL-3.0-1-ov-file). This fork stays under that same license. See [LICENSE](LICENSE).

[![License: AGPL-3.0](https://img.shields.io/badge/License-AGPL--3.0-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-purple.svg)](https://kotlinlang.org)

---

## Why Vesper?

The Flipper Zero is one of the most versatile hardware hacking tools ever made — but navigating its menus, managing files, and crafting signals by hand is tedious. **Vesper eliminates the friction.** Plug in an API key, connect over Bluetooth, and you have a voice-commanded hardware lab in your pocket. OpenRouter, Google Gemini, OpenAI, xAI, and Anthropic all work.

- **Instant expertise** — Don't memorize SubGHz protocols or IR formats. Just say what you want.
- **Real-time control** — The AI reads your Flipper's state, executes commands, and reports back in seconds.
- **Multimodal input** — Voice commands, photo analysis, and text chat. Use your phone camera or smart glasses to show the AI what you're looking at.
- **Signal alchemy** — Build, layer, and export custom RF waveforms with a visual editor.
- **Smart glasses integration** — Pair with Mentra smart glasses for hands-free, heads-up Flipper control.
- **Safety-first architecture** — Every AI action is risk-classified. Destructive operations require explicit confirmation. System paths are locked by default.

Whether you're a security researcher, a red teamer, a CTF competitor, a hardware tinkerer, or just someone who wants to understand the invisible signals around you — Vesper makes the Flipper Zero *dramatically* more accessible and more powerful.

---

## Features

### Chat — AI-Driven Flipper Control
Talk to your Flipper in plain English (voice or text):
- *"Show me my SubGHz captures"*
- *"What's my battery level?"*
- *"Create a backup of all my IR remotes"*
- *"Generate a BadUSB script that opens a reverse shell"*
- *"Change the frequency in garage.sub to 315MHz"*

### Hardware Control
Direct control over all Flipper subsystems:
- **SubGHz** — Transmit and analyze RF signals
- **Infrared** — Send and record IR commands
- **NFC / RFID / iButton** — Emulate stored credentials
- **BadUSB** — Execute HID attack scripts
- **GPIO / LED / Vibro** — Control hardware peripherals
- **App Launcher** — Start any Flipper app by name

### Multimodal Input
- **Voice input** — Speak commands using on-device speech recognition
- **Photo analysis** — Snap a picture of a remote, a device label, or anything — the AI sees what you see
- **Text-to-speech** — AI responses can be read aloud. OpenRouter, OpenAI, Gemini, and xAI use that provider's speech. Anthropic is chat only.
- **Smart glasses** — Hands-free voice + camera via Mentra glasses bridge

### Ops Center
Built for reliability-obsessed users:
- **Pipeline Health** — BLE/RPC/CLI readiness and diagnostics at a glance
- **Runbooks** — One-tap recovery and smoke-test sequences
- **Live Status** — Transport and command pipeline behavior in one view

### Alchemy Lab — Signal Synthesis
Build custom RF signals from scratch:
- Visual waveform editor with real-time preview
- Layer and fuse multiple signal patterns
- Export directly to your Flipper's SD card

### Payload Lab
AI-powered payload generation:
- BadUSB scripts, SubGHz signals, IR remotes, NFC tags
- Validated before deployment — the AI checks format and safety
- Direct push to Flipper storage

### FapHub Browser
Browse and install Flipper applications:
- Search the Flipper app catalog
- One-tap install to your device

### Resource Browser
Find and download community resources:
- Search GitHub for Flipper-compatible files
- Browse repositories and download directly to your Flipper

### Device
Two tabs, Flipper and Accessories.

On Flipper, while connected: the live LCD, a pause switch that keeps the last frame in place, remote buttons, the tool kit, and the passport (mood, portrait, idle animation). The passport name is the hardware name.

On Accessories: official boards are the Wi-Fi Developer Board, the Video Game Module, and the Prototyping Boards. Unofficial boards, including 5Ghost, stay hidden until that switch is on. Nothing is mounted until you mark it, and a marked board is described only while the Flipper is connected.

### Stage Remote
Safe and live demo beats. Live beats ask before they run. Push the files, then run a beat from the phone.

### Risk & Permissions Engine
Every AI action is classified before execution:
- **Low risk** — Read-only ops execute automatically
- **Medium risk** — File writes show a diff for review
- **High risk** — Destructive ops require double-tap confirmation
- **Blocked** — System/firmware paths require explicit unlock

Configure **auto-approve** per risk tier in Settings to move faster when you trust the workflow.

### Audit Log
Every action the AI takes is logged:
- Full history of commands, results, and approvals
- Filterable by action type and session

---

## Recommended AI Models

Vesper works with any chat model on [OpenRouter](https://openrouter.ai). Settings also accepts a direct key for Google Gemini, OpenAI, xAI, and Anthropic. The ids below are OpenRouter ids. A direct provider uses that provider's own model ids. For the best experience:

| Model | Why Use It | Speed | Cost |
|-------|-----------|-------|------|
| **`anthropic/claude-sonnet-5.5`** | Best balance of speed, intelligence, and cost. Strong default for tool use. | Fast | $$ |
| **`anthropic/claude-haiku-5.5`** | Fast and inexpensive for simple reads and quick commands. | Fastest | $ |
| **`openai/gpt-6.1-sol`** | Strong general-purpose alternative for agentic control. | Fast | $$ |
| **`google/gemini-3.8-flash`** | Fast multimodal model with solid tool use. | Fast | $ |
| **`x-ai/grok-4.7`** | Current Grok flagship for coding and longer agent tasks. | Fast | $$ |
| **`moonshotai/kimi-k3`** | Open-weight multimodal model for long coding and agent tasks. | Medium | $$$ |
| **`z-ai/glm-5.3`** | Current GLM flagship for long-horizon agent work and software tasks. | Medium | $$ |

**Our recommendation:** Start with **Claude Sonnet 5.5** or **GPT-6.1 Sol** for daily use. Use **Claude Haiku 5.5** for quick commands. **Kimi K3** and **GLM 5.3** are strong open-weight choices for longer agent tasks.

---

## Quick Start

### Requirements

| Item | Notes |
|------|-------|
| **Flipper Zero** | Official firmware or [Unleashed](https://github.com/DarkFlippers/unleashed-firmware). [shop.flipperzero.one](https://shop.flipperzero.one) |
| **Android device** | Android 8.0+ (API 26), Bluetooth required |
| **JDK 17** | The debug build uses JDK 17 |
| **Android SDK** | Platform 35. Gradle reads `sdk.dir` from `local.properties` |
| **API key** | [OpenRouter](https://openrouter.ai), or a direct key for Google Gemini, OpenAI, xAI, or Anthropic |

### 1. Prep Your Flipper
1. Charge it up (USB-C)
2. Install official firmware via [qFlipper](https://flipperzero.one/update), or install Unleashed. Vesper works with both.
3. Enable Bluetooth: Settings > Bluetooth > ON

### 2. Get an API Key

In Settings, choose **LLM provider**, then paste that provider's key. These providers work:

| Provider | Key |
|----------|-----|
| **OpenRouter** | [openrouter.ai](https://openrouter.ai) → Keys → Create Key. Starts with `sk-or-`. Pay-per-use; a few dollars of credit is enough to start. This matches the model table above. |
| **Google Gemini** | Key from [Google AI Studio](https://aistudio.google.com/apikey). Starts with `AIza`. |
| **OpenAI** | [platform.openai.com](https://platform.openai.com/api-keys). Starts with `sk-`. |
| **xAI** | Key from the xAI console. Starts with `xai-`. |
| **Anthropic** | [console.anthropic.com](https://console.anthropic.com/settings/keys). Starts with `sk-ant-`. Chat only; this provider has no speech. |

### 3. Build & Install

The debug APK is built with the Gradle wrapper from a terminal. Open the repo in [Cursor](https://cursor.com) or [VS Code](https://code.visualstudio.com) and use the integrated terminal. JDK 17 and the Android SDK do the compile.

```bash
git clone <repository-url>
cd V3SP3R
```

Create `local.properties` in the repo root. Gradle reads the SDK location from `sdk.dir`. Leave this file uncommitted.

```
sdk.dir=/path/to/Android/sdk
```

The debug build needs Android SDK platform 35.

```bash
./gradlew :app:assembleDebug
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`. Turn on USB debugging on the phone, then:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The package name is `com.vesper.flipper`. You can also copy the APK to the phone and install it there.

### 4. First Launch
1. **Grant permissions** — Bluetooth, Location (required for BLE scanning), Notifications
2. **Add your API key** — Settings > LLM provider > paste that provider's key > pick a model
3. **Connect** — Device tab > Scan > tap your Flipper
4. **Go** — Chat tab > start talking to your Flipper

---

## Smart Glasses Setup (Optional)

Vesper supports hands-free operation via Mentra smart glasses.

1. Deploy the bridge server from `mentra-bridge/`:
   ```bash
   cd mentra-bridge
   npm install && npm run build && npm start
   ```
2. In Vesper Settings, enable **Smart Glasses** and enter the bridge URL
3. Voice transcriptions from your glasses flow directly to Vesper
4. The AI can request photos through your glasses camera

---

## Architecture

Layers, risk flow, and where code lives: [docs/architecture.md](docs/architecture.md).

## Project Structure

```
V3SP3R/
├── app/src/main/java/com/vesper/flipper/
│   ├── ai/                     # AI integration
│   │   ├── OpenRouterClient.kt # LLM API, tool calling, JSON repair
│   │   ├── VesperAgent.kt      # Conversation orchestrator
│   │   ├── VesperPrompts.kt    # System prompts
│   │   ├── PayloadEngine.kt    # Payload generation
│   │   └── FlipperToolExecutor.kt
│   ├── ble/                    # Bluetooth
│   │   ├── FlipperBleService.kt
│   │   ├── FlipperProtocol.kt
│   │   ├── FlipperFileSystem.kt
│   │   └── MarauderBridge.kt
│   ├── glasses/                # Smart glasses
│   │   ├── GlassesIntegration.kt
│   │   └── GlassesBridgeClient.kt
│   ├── voice/                  # Voice I/O
│   │   ├── SpeechRecognitionHelper.kt
│   │   └── ElevenLabsTtsService.kt
│   ├── domain/
│   │   ├── executor/           # Command execution & risk
│   │   ├── model/              # Data models
│   │   ├── service/            # Audit, diff, permissions
│   │   └── protocol/           # SubGHz, Pwnagotchi
│   ├── data/                   # Persistence & settings
│   ├── security/               # Input validation, sanitization
│   ├── ui/                     # Jetpack Compose screens
│   │   ├── screen/
│   │   ├── viewmodel/
│   │   ├── components/
│   │   └── theme/
│   └── widget/                 # Home screen widget
├── mentra-bridge/              # Smart glasses bridge server (Node.js)
├── docs/                       # Architecture docs, schemas
└── gradle/                     # Build configuration
```

---

## Supported Actions

| Action | Description | Risk Level |
|--------|-------------|------------|
| `list_directory` | List files and folders | Low |
| `read_file` | Read file contents | Low |
| `write_file` | Create or modify files | Medium |
| `create_directory` | Create folders | Low |
| `delete` | Delete files or folders | High |
| `move` / `rename` / `copy` | File operations | Medium-High |
| `get_device_info` | Battery, firmware, hardware info | Low |
| `get_storage_info` | SD card and internal storage stats | Low |
| `execute_cli` | Run Flipper CLI commands | Medium |
| `push_artifact` | Push generated files to Flipper | Medium |
| `forge_payload` | AI-generate SubGHz/IR/BadUSB/NFC payloads | Medium |
| `subghz_transmit` | Transmit SubGHz signal | High |
| `ir_transmit` | Send infrared command | Medium |
| `nfc_emulate` / `rfid_emulate` / `ibutton_emulate` | Emulate credentials | Medium |
| `badusb_execute` | Run HID attack script | High |
| `launch_app` | Start a Flipper app | Medium |
| `led_control` / `vibro_control` | Hardware peripherals | Low |
| `search_faphub` / `install_faphub_app` | Browse & install apps | Medium |
| `browse_repo` / `download_resource` / `github_search` | Find community resources | Low |
| `request_photo` | Capture photo via smart glasses | Low |

---

## Troubleshooting

<details>
<summary><strong>Flipper not found when scanning</strong></summary>

1. On Flipper: Settings > Bluetooth > make sure it's ON
2. Toggle Bluetooth off/on on your phone
3. Make sure Flipper isn't connected to another device (e.g. qFlipper)
4. Move within 3 feet / 1 meter
5. Check that Location permission is granted (required for BLE scanning on Android)
</details>

<details>
<summary><strong>Build failed</strong></summary>

1. `java -version` should be JDK 17
2. `local.properties` in the repo root should set `sdk.dir` to an Android SDK that includes platform 35
3. From the repo root: `./gradlew :app:assembleDebug`
4. If the Gradle cache is stale: `./gradlew --stop`, then `./gradlew :app:clean :app:assembleDebug`
</details>

<details>
<summary><strong>AI not responding</strong></summary>

1. In Settings, the LLM provider should match the key you pasted
2. For OpenRouter, check the credit balance at [openrouter.ai](https://openrouter.ai). For Gemini, OpenAI, xAI, or Anthropic, check that account's billing
3. Check internet connection
4. Try a different model — some may be temporarily unavailable
</details>

<details>
<summary><strong>"Could not parse tool arguments" errors</strong></summary>

This means the AI model returned malformed JSON. Vesper includes automatic JSON repair, but some models are more reliable than others. Try:
1. Tap **Retry** on the error message
2. Switch to a recommended model (Claude Sonnet 5.5, GPT-6.1 Sol)
3. Simplify your request
</details>

<details>
<summary><strong>Permission denied errors</strong></summary>

- Some Flipper paths are protected by default (system files, firmware areas)
- Go to Settings > Permissions to unlock specific paths
- Enable auto-approve per risk tier to move faster
- Blocked paths always require manual unlock
</details>

---

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting a PR.

Areas that need love:
- iOS version (SwiftUI)
- Signal format parsers (new protocols)
- Additional payload templates
- UI/UX improvements
- Translations / i18n
- Test coverage

---

## Security

Found a vulnerability? Please report it responsibly. See [SECURITY.md](SECURITY.md) for details.

---

## Safety & Legal

- Vesper is a tool for **education and legitimate security research**
- Only use on devices you own or have explicit authorization to test
- All AI actions are logged and auditable
- The AI refuses clearly malicious requests
- Destructive operations require explicit user confirmation
- You are responsible for complying with all applicable laws in your jurisdiction

---

## License

GNU Affero General Public License v3.0. The full text is in [LICENSE](LICENSE).

This repository is a fork of [elder-plinius/V3SP3R](https://github.com/elder-plinius/V3SP3R). The upstream project is AGPL-3.0, and this fork is released under AGPL-3.0 as well. Changes in this fork are the work on top of that upstream tree. If you run a modified copy as a network service, the AGPL requires you to offer the corresponding source to its users.

---

**V3SP3R** — AI-powered hardware hacking, in your pocket. Your Flipper Zero just got a brain upgrade.
