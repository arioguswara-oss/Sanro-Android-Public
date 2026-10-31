# Debug local Free MVP

Current stage and limitations: [FUNCTIONAL_MVP_V1.md](FUNCTIONAL_MVP_V1.md).

GitHub Actions on `feature/pos-free-functional-mvp-v1` runs unit tests, lintDebug, assembleDebug and uploads the sandbox APK plus verification reports. No release signing, keystore or AAB. Hardware retest by Owner is required.

Local toolchain if available: Java 17, Gradle 8.9, Android SDK 35; run in `SanroPOSFree`:

```sh
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

The debug sandbox is not POS2 activation. Release remains locked pending verified auth/device/entitlement and offline policy contracts.
