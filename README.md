# Portable Brain - Android Client (Public Build)

Portable Brain is an AI-powered agent that can understand and interact with any Android app. It reads the screen through Android's AccessibilityService, tracks context in the background, and executes multi-step commands (tap, type, scroll, navigate) driven by a backend LLM - all from a single natural-language request.

This repository contains the **open-source public build** of the Android client. It includes the full UI layer, networking stack, authentication, accessibility integration, and data models. Some proprietary components are replaced with documented stubs.

Please keep an eye out for a beta release on Google Play Store!

## Architecture

```
User request ("open Settings and enable Wi-Fi")
  → Android Client (this repo)
    → AccessibilityService reads current screen state
    → Posts UI context to backend
  → Backend (private)
    → LLM plans next action based on screen state
    → Returns command: { action: "click", target: "Wi-Fi" }
  → Android Client executes action, captures new state, repeats
```

### What's included

| Component | Path | Description |
|---|---|---|
| Accessibility Service | `accessibility/` | Reads the Android UI tree, produces indexed snapshots, executes actions (tap, type, scroll, navigate) |
| Data Models | `data/Models.kt` | Full API contract — request/response types, enums, action definitions |
| Network Layer | `network/` | Retrofit client, API endpoint definitions, Firebase token injection |
| Authentication | `auth/` | Firebase email/password sign-in |
| UI | `ui/` | Jetpack Compose screens (login, main dashboard) and theme |
| App Scaffolding | `MainActivity.kt`, `PortableBrainApp.kt` | Entry points, navigation, notification channel setup |

### What's omitted

Two components are replaced with **documented stubs** that preserve the public API surface and keep the project compilable:

| Component | Path | What it does |
|---|---|---|
| Tracking Service | `service/TrackingForegroundService.kt` | The full implementation contains proprietary state-change detection and context delivery logic that determines what UI data is sent to the backend and when. The stub starts/stops the foreground service but does not poll. |
| Execution Loop | `MainViewModel.kt` | The full implementation contains the multi-step execution orchestrator — session management, command dispatch, error recovery, and the client-backend feedback loop. The stub exposes the same ViewModel interface but does not execute commands. |

Both stubs include documentation describing the expected behavior and how to build your own implementation using the public models and API surface.

## Getting Started

### Prerequisites
- Android Studio Ladybug or later
- Android SDK 34+
- A Firebase project with Email/Password authentication enabled

### Setup
1. Clone this repository
2. Create a Firebase project and add an Android app with package name `com.portablebrain.client`
3. Download `google-services.json` and place it at `app/google-services.json`
4. Enable Email/Password under Firebase Console → Authentication → Sign-in method
5. Build and run

### Connecting to a backend
Configure the backend URL in `network/ApiConfig.kt`. The API endpoints are defined in `network/ApiService.kt`. See `data/Models.kt` for the full request/response schema.
