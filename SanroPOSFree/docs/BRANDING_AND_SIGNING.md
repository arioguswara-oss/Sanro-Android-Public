# SANRO POS Android Public — Branding & APK Signing

Owner-approved 27 Oktober 2026.

- Android launcher display name (android:label): **SANRO POS**.
- Launcher icon: the exact blue SANRO shopping-cart / white SR mark provided by Owner in chat on 27 Oktober 2026. Preserve its identity and colors; use density/adaptive launcher resources, not the older Tunas Baru icon by assumption.
- Use independent Android applicationId so SANRO POS Public and Tunas Baru (com.sanrotech.pos) can coexist.
- The Owner already has an APK signing keystore. **Do not generate or replace their signing identity without an explicit decision.** Owner selects appropriate private key and alias at release time. Keep certificate consistent for every update of the same Android applicationId.
- Never commit/upload keystores, signing passwords, signing config containing secrets, private key material, or signed credentials to this public repository.
- This source branch currently has NO physically validated Android Public APK. Launcher resources and release signing are distinct from build/UAT gates.
- Bluetooth Panda PRJ-80BT printing and auto cutter remain release-blocking requirements.
