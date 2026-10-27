# SANRO POS Public Android — FREE Local-First V0 (Owner baseline)
Date: 2026-10-27
Status: SCOPE LOCKED / IMPLEMENTATION NOT YET QA PASSED

## Goal
Build an Android Public POS with user experience and Bluetooth thermal receipt quality at least equivalent to Owner-tested SANRO POS Tunas Baru APK 1.0.7, adding actual device-local SQLite/Room operation required by FREE.

## Source & isolation
- Repository: arioguswara-oss/Sanro-Android-Public
- Development branch: feature/pos-free-local-first-v0
- New standalone app module: SanroPOSFree (never modify SanroSuperadmin).
- Unique applicationId (not com.sanrotech.pos) and endpoint pos2.sanrotech.com; keep production Tunas Baru source and secrets out of this public repo.
- Reference source (private Drive, READ ONLY): folder SANRO_TUNASBARU_ANDROID_SOURCE_V1.0.7_TESTING_BT_CUT_R3, folder ID 1Hv760fNhLV-P1ZtqcnVkgVX4A2D-d4Xt.
- Printer source components: BluetoothReceiptPrinter.java, BluetoothJobWriter.java, native SPP bridge in MainActivity.java, and receipt HTML R5-CUT2. Port a reviewed, minimal implementation, never wholesale-copy Tunas Baru's hosted WebView and authority.

## FREE operational mode
1. Register and verify email at POS2; authenticate online as one operational session, verify signed/authoritative FREE entitlement and bind device. Avoid exposing credentials in logs.
2. Room SQLite is operational source of truth for Master Barang, stock, sale+items, cash payment, transaction history and simple daily cash close. Operations persist through app restart; offline after initial activation.
3. Keep one active session/device; session recovery server-mediated, no silent simultaneous logins.
4. Never write FREE operational transactions to direct-cloud /api/pos/*; preserve CLOUD_PLAN_DENIED policy.
5. Do not infer offline expiry or backup behavior: implement safe anti-clock-rollback and entitlement grace policy after control-plane contract is defined.
6. Purchase/return/finance and advanced screens remain entitlement-scoped. No falsified parity with Tunas Baru if module not implemented.

## MUST PASS — Bluetooth Panda PRJ-80BT + auto cutter
- Android Bluetooth Classic SPP connection to already-paired printer, runtime permissions Android 12+, select printer + visible connection status.
- 80 mm ESC/POS receipt: sale ID, store identity, items, qty, price, total, payment, change, footer. Complete physical print before cutting.
- Auto cutter after receipt when user taps PRINT. Conditional ESC/POS full-cut GS V 00 after trailing feed, no cash drawer pulse ESC p.
- Audited source version supports small serial chunks (96 bytes), gap (45 ms) and post-send drain (1200 ms) as TESTING values, not universally guaranteed timings.
- Exactly one active print job; prevent double-press duplicate prints. Never auto-resend after ambiguous Bluetooth stream failure.
- Printing reads a previously committed local sale; no stock/payments modifications from retry or printer failure.
- Independent hardware UAT required on Panda PRJ-80BT: receipt/footer complete; one cut at correct location; no duplicate print; works with internet disabled; failure/reconnect handled.
- If device lacks cutter/GS V support, explicit capability and truthful UI; don't claim auto cut PASS.

## Release gates
- Android Studio build PASS and APK installed on physical Android.
- Local Room schema, migrations, tenant separation, atomic checkout and persistent history tested.
- Auth/session/entitlement integration tested with SR00001 (non-production sample data; no secrets in repo).
- Panda physical printer + cutter test PASS and Owner acceptance.
- No modifications to POS2 LIVE beyond separately approved rollout, no modifications to sanrotech.com/superadmin or pos.sanrotech.com.
