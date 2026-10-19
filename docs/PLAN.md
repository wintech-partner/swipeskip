# Plan

Source: kickoff prompt (`CLAUDE_CODE_PROMPT.md`). Scope of v1: YouTube Shorts on Android only.

## Environment (inspected 2026-10-19)

| Item | State |
|---|---|
| JDK | none on the system; Temurin 17 unpacked to `~/jdk/jdk-17.0.20.1+1` |
| Android SDK | none; cmdline-tools + platform-tools + platform 36 + build-tools 36.1.0 in `~/Android/Sdk` |
| Gradle | none; 9.8.1 unpacked to `~/gradle-dist`, used only to generate the wrapper |
| Emulator / device | none. Instrumented tests cannot run here |
| Disk | about 21 GB free at start |

Shell setup used for every build:

```
export JAVA_HOME=$HOME/jdk/jdk-17.0.20.1+1
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$JAVA_HOME/bin:$PATH
```

## Phases

0. Environment, plan, policy verification (`POLICY_NOTES.md`)
1. Scaffold: Gradle KTS, version catalog, flavors, CI, `CLAUDE.md`, community files
2. `core/*`: models, state machine, session engine, unit tests
3. `platform/youtube`, accessibility service, gestures, fake Shorts app, instrumented test
4. UI: onboarding, Home, Settings, Diagnostics, themes, en + tr
5. Ad monitor integration, notifications, interaction pause, end-of-video spike
6. Remaining locales, billing (`noop`, `play`)
7. Docs, distribution materials, release workflow, fastlane
8. Full verification and final report

Anything that needs a device, store account or human decision goes to `OPEN_ITEMS.md`.
