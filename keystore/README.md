# Dhan-OM sideload keystore

`dhanom-sideload.jks` is a PKCS12 keystore (alias `androiddebugkey`,
password `android`) committed on purpose.

Every CI `assembleDebug` APK is signed with this **same** key using APK
Signature Scheme v1 + v2 + v3 so:

- updates overwrite the previous sideload install instead of showing
  **App not installed** (signature mismatch)
- Xiaomi / Vivo / Oppo / Realme / Samsung installers that still require
  JAR (v1) signing accept the APK

This is **not** a Play Store upload key. Do not use it for Play publishing.
