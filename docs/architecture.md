# Vesper Architecture

## Overview

Vesper is an Android application that enables AI-driven control of a Flipper Zero device. The architecture follows clean architecture principles with a clear separation between presentation, domain, and data layers.

## Core Principles

1. **AI issues commands, Android enforces reality** - The model never touches BLE or raw device primitives. It only issues structured commands through the `execute_command` interface.

2. **One command interface** - All operations go through a single, well-defined tool interface. No ad-hoc execution paths.

3. **Diffs before writes** - Any modification to an existing file shows a diff before execution.

4. **Confirm only when necessary** - Reads and safe writes happen silently. Destructive operations require confirmation.

5. **Everything is logged** - All agent actions are auditable and replayable.

## Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌────────┐ │
│  │  Chat   │ │  Files  │ │ Device  │ │  Audit  │ │Settings│ │
│  │ Screen  │ │ Browser │ │ Screen  │ │  Screen │ │ Screen │ │
│  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └───┬────┘ │
│       │           │           │           │          │       │
│  ┌────┴───────────┴───────────┴───────────┴──────────┴────┐ │
│  │                      ViewModels                         │ │
│  └────────────────────────────┬────────────────────────────┘ │
└───────────────────────────────┼──────────────────────────────┘
                                │
┌───────────────────────────────┼──────────────────────────────┐
│                      Domain Layer                            │
│  ┌────────────────────────────┴────────────────────────────┐ │
│  │                    VesperAgent                           │ │
│  │  (Orchestrates conversation and command execution)       │ │
│  └────────────────────────────┬────────────────────────────┘ │
│                               │                               │
│  ┌──────────────┐  ┌──────────┴─────────┐  ┌──────────────┐  │
│  │   OpenRouter │  │  CommandExecutor   │  │   Audit      │  │
│  │    Client    │  │                    │  │   Service    │  │
│  └──────────────┘  └──────────┬─────────┘  └──────────────┘  │
│                               │                               │
│  ┌──────────────┐  ┌──────────┴─────────┐  ┌──────────────┐  │
│  │    Risk      │  │    Permission      │  │    Diff      │  │
│  │   Assessor   │  │     Service        │  │   Service    │  │
│  └──────────────┘  └────────────────────┘  └──────────────┘  │
└───────────────────────────────┬──────────────────────────────┘
                                │
┌───────────────────────────────┼──────────────────────────────┐
│                       Data Layer                             │
│  ┌────────────────────────────┴────────────────────────────┐ │
│  │                  FlipperFileSystem                       │ │
│  └────────────────────────────┬────────────────────────────┘ │
│                               │                               │
│  ┌──────────────┐  ┌──────────┴─────────┐  ┌──────────────┐  │
│  │   Flipper    │  │    FlipperBLE      │  │    Room      │  │
│  │   Protocol   │  │     Service        │  │   Database   │  │
│  └──────────────┘  └────────────────────┘  └──────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

## Component Details

### Presentation Layer

- **ChatScreen**: Main interaction point with the AI agent
- **FileBrowserScreen**: Direct file system navigation
- **DeviceScreen**: BLE connection management and device info
- **AuditScreen**: View and filter action history
- **SettingsScreen**: Configure API keys, permissions, and preferences

### Domain Layer

- **VesperAgent**: Orchestrates the conversation flow, manages state
- **OpenRouterClient**: Handles API calls to OpenRouter with tool calling
- **CommandExecutor**: Processes commands with risk assessment and approval
- **RiskAssessor**: Classifies operations by risk level
- **PermissionService**: Manages path-based, time-limited permissions
- **DiffService**: Computes file diffs for write operations
- **AuditService**: Logs all actions for accountability

### Data Layer

- **FlipperFileSystem**: High-level file operations
- **FlipperProtocol**: Serial protocol implementation
- **FlipperBleService**: BLE connection and GATT operations
- **VesperDatabase**: Room database for audit logs and chat history
- **SettingsStore**: DataStore for preferences

## Risk Classification Flow

```
Command Received
       │
       ▼
┌──────────────────┐
│  Risk Assessment │
└────────┬─────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌──────┐  ┌───────┐
│ LOW  │  │BLOCKED│──────────► Reject with reason
└──┬───┘  └───────┘
   │
   ▼
Execute immediately
   │
   └────────────────┐
                    │
    ┌───────────────┴───────────────┐
    │                               │
    ▼                               ▼
┌───────┐                       ┌──────┐
│MEDIUM │                       │ HIGH │
└───┬───┘                       └──┬───┘
    │                              │
    ▼                              ▼
Show diff                   Hold-to-confirm
    │                              │
    ▼                              ▼
User clicks               User holds button
"Apply"                      1.5 seconds
    │                              │
    └──────────────┬───────────────┘
                   │
                   ▼
              Execute command
                   │
                   ▼
              Log to audit
```

## BLE Communication

The app communicates with Flipper Zero over BLE using the device's serial profile:

1. **Discovery**: Scan for devices advertising the Flipper service UUID
2. **Connection**: Connect to selected device with GATT
3. **Service Discovery**: Find serial TX/RX characteristics
4. **Notifications**: Enable notifications on RX characteristic
5. **Protocol**: Frame-based communication with length prefix

## Security Considerations

- API keys stored in encrypted DataStore
- Permissions are scoped and time-limited
- Protected paths require explicit unlock
- All operations are logged for audit
- Hold-to-confirm prevents accidental destructive actions
- No raw BLE access from AI model

## Dependencies

- **Jetpack Compose**: Modern declarative UI
- **Hilt**: Dependency injection
- **Room**: Local database
- **DataStore**: Preferences storage
- **OkHttp**: Network requests
- **Kotlinx Serialization**: JSON parsing
- **java-diff-utils**: Diff computation
