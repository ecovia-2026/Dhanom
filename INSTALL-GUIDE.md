# Dhan-OM — How to install the APK (fixes "App not installed")

The APK itself is valid and correctly signed (verified with apksigner). When
Android shows **"App not installed"** with no other message, it is almost always
one of these 4 things. Follow ALL steps in order.

## Step 0 — Remove the OLD app from the PHONE (not just the file)

Deleting the downloaded `.apk` FILE does NOT remove the installed app.
1. Open **Settings → Apps → See all apps**.
2. Find **Dhan-OM** (or "Dhanom AI") → tap it.
3. Tap **Uninstall**.
4. Also check for any app named **Dhan-OM / Dhanom AI** under Settings → Apps.
   If you see more than one, uninstall them all.

> Why: older builds used a different signing key. If even one old copy is still
> installed, Android silently blocks the new APK ("App not installed").

## Step 1 — Download correctly

1. In your browser (Chrome recommended), sign in at **github.com**.
2. Open the **Actions** run I gave you → **Artifacts** → tap
   **`DhanomAI-Finance-debug-apk`**. This downloads a **.zip** (~25 MB).
3. Unzip it (Files app → tap the zip → Extract). Inside is **`app-debug.apk`**.

> If you try to install the **.zip** directly (or rename it to .apk), it will
> always fail. You MUST extract the `app-debug.apk` from inside the zip first.

## Step 2 — Install

1. Tap **`app-debug.apk`**.
2. If Android asks "Allow from this source?" → **Allow**.
3. If **Google Play Protect** shows *"Blocked"* → tap **More details** →
   **Install anyway**.
4. Tap **Install**.

## Step 3 — If it STILL says "App not installed" (device-specific)

The most common silent blocker on Indian phones is the OEM installer:

**Xiaomi / Redmi / POCO (MIUI/HyperOS)**
- Settings → Apps → ⋮ → **Special access** → **Install unknown apps** → allow
  your browser/file manager.
- Disable **MIUI optimization**: Settings → Developer options → turn OFF
  "MIUI optimization" (then try again).
- If "Install via USB" interferes, toggle it off.

**Realme / Oppo / OnePlus (ColorOS)**
- Settings → Security → **Install unknown apps** → allow the file manager.
- Turn OFF **"Verify apps over USB"** / **"Enhance app installation"** if shown.

**Samsung (One UI)**
- Settings → Apps → **Google Play Protect** → turn OFF **"Scan apps with Play
  Protect"** temporarily, then install.

**General**
- Free up a little space (the app needs ~4 GB for the offline brain + working
  space, but install itself only needs ~100 MB).
- If you see "Storage full" anywhere, clear some space.

## Step 4 — Get the REAL error (if nothing above works)

Connect the phone to a PC with USB debugging on and run:

```
adb install app-debug.apk
```

It prints the exact reason, e.g.:
- `INSTALL_FAILED_UPDATE_INCOMPATIBLE` → an old copy is still installed (Step 0).
- `INSTALL_FAILED_NO_MATCHING_ABIS` → your device can't run the native libs
  (tell me your phone model and I'll fix the build).
- `INSTALL_PARSE_FAILED_...` → the downloaded file is corrupt (re-download).

Paste that exact line back to me and I'll fix it precisely.
