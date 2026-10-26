# Build and private delivery

## Android Studio local (preferred)
1. Open the standalone `SanroSuperadmin/` folder as a new project. Use Android Studio with JDK 17 and Android SDK 35.
2. Configure Gradle 8.9 and Gradle Android Plugin 8.7.3. Source tree has no committed Gradle wrapper JAR; Android Studio may offer Gradle setup. Never trust arbitrary wrapper downloads.
3. Sync project, run `testDebugUnitTest`, `lintDebug`, then `assembleDebug`.
4. Install `app/build/outputs/apk/debug/app-debug.apk` on a test device. Its debug package is `com.sanrotech.superadmin.debug`.

## Private CI build
- Opening/updating the pull request triggers a restricted GitHub Actions build for `SanroSuperadmin/**` changes, if Actions is enabled for the repository.
- CI provisions Java 17, Android SDK 35, Gradle 8.9; runs unit tests + lint + assembles debug.
- Retrieve `sanro-superadmin-v1.0.1-debug` from workflow run artifacts **only after SUCCESS**.
- The manual `workflow_dispatch` option is intended for after the workflow file reaches the default branch; do not rely on dispatch before merge.
- Do not distribute debug APK as a production release.

## Release signing (Owner only)
- Use Android Studio **Build > Generate Signed Bundle / APK > APK**, creating a separate keystore for `com.sanrotech.superadmin`.
- Keep keystore and credentials outside GitHub/Drive shared folders, back up securely offline, record SHA-256 certificate fingerprint privately.
- Install test signed APK on test device and rerun QA. Never reuse a POS or Stock keystore without an explicit signing-policy decision.
- For upgrades, retain the same package ID/signing key and increment `versionCode`.

## Blockers before private signed APK approval
- Production dashboard login/logout in Android WebView must PASS with real authorized test credentials (do not put credentials in issues/logs).
- Validate form POST `Origin` behavior, HttpOnly/SameSite cookie propagation and CSRF on real WebView.
- Check dashboard tenants, read-only settings, User Management and Audit Performance views.
- Test Android 13/14/15/16, rotation, insets/keyboard, poor network, SSL certificate error, Android Back and logout/reopen.
- Confirm security and accessibility of any new Superadmin web features added after repo baseline.
