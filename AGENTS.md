# AGENTS.md

## Project overview

This repository contains **Klockodo**, a Kotlin/Native Linux desktop/statusbar client for the Clockodo time tracking
service.

The app builds a native Linux executable, uses Gtk3/AppIndicator/libnotify through cinterop, and separates domain logic
from UI and outbound integrations.

## Repository layout

- `src/nativeMain/kotlin/main.kt`: native entrypoint and application wiring.
- `src/nativeMain/kotlin/com/qupaya/klockodo/`: core time-tracking logic.
- `src/nativeMain/kotlin/com/qupaya/ui/`: Gtk/AppIndicator/libnotify UI integration.
- `src/nativeMain/kotlin/com/qupaya/outbound/`: API, time, and request-building adapters.
- `src/nativeMain/kotlin/com/qupaya/Configuration.kt`: runtime config loading from the user config directory.
- `src/nativeMain/resources/`: icons and `Klockodo.desktop`.
- `src/nativeInterop/cinterop/klockodo.def`: native headers and linker/compiler flags.
- `src/nativeTest/kotlin/`: Kotest coverage for business logic.

## Architecture

The codebase follows a lightweight ports-and-adapters style:

- Core business logic lives in `com.qupaya.klockodo`.
- Port interfaces live in `com.qupaya.klockodo.inboundPorts` and `com.qupaya.klockodo.outboundPorts`.
- Adapter implementations live outside the core package, under `com.qupaya.outbound` and `com.qupaya.inbound`.
- Adapter subpackages are named after the port they serve, such as `forGettingData`, `forGettingTime`, and
  `forBuildingEntryRequest`.
- `main.kt` is the composition root that wires configuration, adapters, domain logic, UI, and shutdown behavior
  together.

When adding new integrations, keep this split intact: define or extend the port in the core package, then place the
concrete implementation in the matching adapter package.

## Build and test commands

Prefer existing Gradle tasks only:

- `./gradlew build`: full project build and checks.
- `./gradlew check`: standard verification task.
- `./gradlew nativeTest`: Kotlin/Native unit tests.
- `./gradlew kotest`: explicit Kotest task if you only want tests.
- `./gradlew linkDebugExecutableNative`: build a debug native executable.
- `./gradlew linkReleaseExecutableNative`: build the release native executable.

Only run `runDebugExecutableNative` or `runReleaseExecutableNative` when the environment clearly supports a Linux
desktop session with Gtk/AppIndicator available.

## Native/Linux prerequisites

This project targets `linuxX64("native")` in Gradle.

Builds depend on system headers and libraries declared in `src/nativeInterop/cinterop/klockodo.def`, including:

- Gtk3
- Glib 2.0 / GObject
- Ayatana AppIndicator3
- libnotify
- Pango
- Cairo
- Harfbuzz
- Atk
- Gdk Pixbuf 2.0

If compilation fails during cinterop or native linking, check system packages first before changing Gradle or Kotlin
code.

## Runtime assumptions

The app expects a user config file at:

- `~/.config/Klockodo/config.json`

`Configuration.load()` throws if the home directory or config file is missing. Do not add silent fallbacks unless the
task explicitly requires a behavior change.

The config file shape is:

```json
{
  "apiKey": "YOUR_API_KEY",
  "apiUser": "YOUR_EMAIL_ADDRESS",
  "defaultProject": 123,
  "workTimePerDay": "PT8H"
}
```

The app also expects resources to be copied into user-local locations when run outside the build tree. See `README.md`
and `src/nativeMain/resources/`.

## Editing guidance

- Keep core logic in `com.qupaya.klockodo` independent from Gtk/UI code and native API.
- Prefer Kotlin libraries over accessing the C API when possible. For example,
  `org.jetbrains.kotlinx:kotlinx-io-core` is used for file I/O, rather than the POSIX API.
- Keep integrations behind the existing ports in `com.qupaya.klockodo.inboundPorts` and
  `com.qupaya.klockodo.outboundPorts`, with concrete adapters in matching `inbound` or `outbound` packages.
- Prefer adding or updating tests in `src/nativeTest/kotlin/` when changing core business logic behavior.
- Reuse existing patterns for durations, time access, and API abstractions instead of introducing parallel helpers.
- Avoid broad error swallowing. This codebase currently fails explicitly on missing configuration and invalid state.
- Be careful with startup behavior in `main.kt`; it performs real configuration loading, API wiring, notifications, and
  indicator updates.

## Validation expectations

When changing core/domain logic:

- Run `./gradlew nativeTest` at minimum.
- Run `./gradlew build` when the change may affect wiring or compilation more broadly.

When changing native UI or interop code:

- Run `./gradlew build` or the relevant link task.
- Only try the executable in an environment that actually has a GUI session and required native libraries.

For documentation-only changes, verify paths, task names, and file locations against the repository instead of inventing
new commands or structure.

## Notes for future agents

- `README.md` still contains useful setup information, but prefer the current source tree for exact paths. The resources
  live under `src/nativeMain/resources/`.
- Tests are focused on the `Klockodo` domain class. UI behavior is comparatively less automated, so be more careful when
  editing Gtk/AppIndicator code.
- If a task involves running the actual app, confirm whether the environment is headless before attempting it.
