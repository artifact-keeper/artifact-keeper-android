# Android Test Plan

## Overview

The artifact-keeper Android app uses Kotlin 2.1 with Jetpack Compose and Hilt DI. Testing infrastructure needs to be built from scratch.

## Test Inventory

| Test Type | Framework | Count | CI Job | Status |
|-----------|-----------|-------|--------|--------|
| Build | Gradle | Full app | `build` | Active |
| Lint | Gradle | Full app | `lint` | Active |
| Unit | JUnit/Mockk | 0 | `test` | No tests |
| UI | Compose UI Test | 0 | - | Missing |
| Instrumented | AndroidJUnitRunner | 0 | - | Missing |
| Screenshot | (none) | 0 | - | Missing |

## How to Run

### Build
```bash
./gradlew assembleDebug
```

### Lint
```bash
./gradlew lint
```

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests (requires emulator or device)
```bash
./gradlew connectedAndroidTest
```

## CI Pipeline

```
PR opened/pushed
  -> lint (Gradle lint)
  -> build (assembleDebug)
  -> test (unit tests)

Merge to main
  -> All above + nightly build
```

## Gaps and Roadmap

| Gap | Recommendation | Priority |
|-----|---------------|----------|
| No unit tests | Add JUnit + Mockk for API client, ViewModels, data layer | P1 |
| No UI tests | Add Compose UI tests for critical screens | P2 |
| No instrumented tests | Add AndroidJUnitRunner tests on CI emulator | P2 |
| No screenshot tests | Add Paparazzi or Roborazzi for Compose screenshots | P3 |
