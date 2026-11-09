# Android CI — Emulator QA setelah build APK

**Purpose:** After the existing Android debug build passes unit tests, lint and assembleDebug, an additional emulator-smoke job downloads the exact same APK artifact from that workflow run. No APK rebuild for QA, no VPS emulator, new paid service, signing, production or Play upload.

## Checks (API 35, synthetic sandbox only)

- Install debug APK, launch and confirm native login fail-closed without real POS2 staging. Never use customer credentials.
- Open debug sandbox, inspect navigation with UIAutomator XML bounds.
- Create a synthetic test SKU/product, make a local cash sale and verify saved receipt.
- Force-stop; switch off emulator Wi-Fi and data; restart and verify saved Room transaction history.
- Check app process, crashes and upload test-only screenshots plus summary JSON with APK SHA256.
- CI blocks on emulator smoke failure, with a separate QA evidence artifact retained 7 days.

## Limits

The ChatGPT @Test Android Apps skill provides an ADB testing methodology, while the automated emulator runs in GitHub Actions using reactivecircus/android-emulator-runner v2, not inside the Hermes VPS. Emulator PASS is not Panda PRJ-80BT physical UAT, cutter, dual-cashier Bluetooth, genuine POS2 login/entitlement, billing, Play publishing or release quality approval.

No real tenant credentials, customer data, production access, unredacted logcat or release signing. Red/failed QA must be fixed via development PR; never silently skip it to make CI green.

Deployment: this PR only adds source and a workflow job. It becomes the default post-build QA for qualifying Android development builds after Owner-approved merge.
