# SANRO POS Public — Android debug prototype

This is a **debug-only, offline smoke-test scaffold**, not a functioning commercial Free POS APK. The buttons use TEST_TENANT placeholder and debug-only locally seeded product/sale data. Real POS2 login, verified tenant-bound entitlement, expiry/recovery, migration, and the fully specified production cash-close workflow remain OPEN.

GitHub Actions: workflow SANRO POS Public Android - Debug Prototype (push on development branch, or manual workflow_dispatch once available under Actions). Download Actions artifact 'sanro-pos-public-free-debug-PROTOTYPE', install on a spare Android phone, and test Panda PRJ-80BT (already paired in Android Bluetooth settings). The debug applicationId is com.sanrotech.pospublic.debug and does not overwrite Tunas Baru com.sanrotech.pos or a future signed public release.

Bluetooth send uses one job at a time and an explicit ESC/POS GS V 00 cut with four LF, modeled on audited Tunas Baru 1.0.7 source, but **hardware UAT in the Public APK has not been performed**. Stream success is not a physical printer acknowledgment; never auto-repeat an uncertain print. Test only synthetic receipts; do not use for real customers.

Compile: Java 17, AGP 8.7.3, Gradle 8.9, Android SDK 35, Room 2.6.1. No keystore or production credentials required or checked in.

Release gates: real auth/session, tenant binding & data isolation, Room migrations, production rules, Bluetooth physical UAT, offline expiry policy, installer signing, Owner approval. Do not merge or call production ready solely based on CI build.
