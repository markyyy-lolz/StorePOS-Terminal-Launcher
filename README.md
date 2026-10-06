# StorePOS Terminal Launcher

Standalone Android Home/Launcher for dedicated StorePOS checkout tablets and Android POS terminals.

## v1.1.0 White Edition

The home screen is intentionally focused on two actions:

- **StorePOS — Open Register**
- **Device Settings**

The launcher uses a clean white UI with blue/teal StorePOS branding and responsive tablet layouts.

## Packages

- Launcher: \`com.storepos.launcher\`
- Main StorePOS app: \`com.storepos.app\`

## Features

- Default Android Home/Launcher support
- StorePOS installed/version detection
- Missing/outdated app warning
- Restricted device settings
- Hidden admin access by long-pressing the StorePOS logo
- Admin PIN protection for full Settings and kiosk exit
- Optional StorePOS auto-launch
- Android Device Owner / Lock Task kiosk mode
- Home, Recents, status-bar and keyguard restrictions where supported
- Play Store/browser hiding during full kiosk mode
- Boot handling
- SUNMI-class Android compatibility
- Printer setup remains primarily inside StorePOS

## Full kiosk setup

On a freshly reset Android device:

    adb shell dpm set-device-owner com.storepos.launcher/.admin.StorePosDeviceAdminReceiver

Then long-press the StorePOS logo and enable **Kiosk lock**.

## Build

CI uses JDK 17, Gradle 8.11.1, Android Gradle Plugin 8.10.1 and Kotlin 2.2.21.

    gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
