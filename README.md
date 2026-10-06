# StorePOS Terminal Launcher

A dedicated Android Home/Launcher application for **StorePOS** terminals. This project is intentionally separate from the main `markyyy-lolz/StorePOS-Android` application.

## Design

Version 1.1.0 uses a **white-first tablet interface** with StorePOS blue/teal accents. The home screen is deliberately simple:

- **StorePOS — Open Register**
- **Device Settings**

There is no normal app drawer in the launcher UI.

## Target devices

- Android tablets
- Android-based retail terminals
- SUNMI-class devices
- Android 8.0+ / API 26+
- Portrait and landscape layouts

Main StorePOS package: `com.storepos.app`

Launcher package: `com.storepos.launcher`

## Key features

- Default Android Home/Launcher capability.
- Large StorePOS register launch card.
- StorePOS installed/version detection and update warning.
- Restricted device settings page:
  - Wi-Fi
  - Bluetooth / accessories
  - receipt-printer handoff to StorePOS
  - launcher brightness
  - media volume
  - date/time
  - device information
  - StorePOS and launcher update links
- Hidden administrator access by long-pressing the StorePOS logo.
- Administrator PIN required for:
  - full Android Settings
  - Bluetooth/date-time system screens while locked down
  - leaving kiosk mode
  - external install/update pages while in kiosk mode
- Salted PBKDF2-HMAC-SHA256 administrator PIN storage.
- Android Device Owner / Lock Task support.
- Optional hiding of Google Play Store and common browsers during full kiosk mode.
- Persistent preferred Home assignment in Device Owner mode.
- Optional StorePOS auto-open after launcher startup.
- Boot receiver for provisioned dedicated devices.
- SUNMI detection. Built-in printer support remains in the main StorePOS app.

## Standard launcher mode

Install the APK and open **Administrator Access** by long-pressing the StorePOS logo. Tap **Set as default Home app**, then select **StorePOS Terminal Launcher** as the Android Home app.

In standard mode, the launcher replaces the normal Home screen, but Android cannot guarantee that Home, Recents, notification shade, and other escape paths are completely blocked.

## Full kiosk / Device Owner mode

Full dedicated-device restrictions require Android Device Owner provisioning. This generally must be done on a freshly reset device before normal user setup/accounts are added.

1. Enable USB debugging.
2. Install the launcher APK.
3. Run:

```bash
adb shell dpm set-device-owner com.storepos.launcher/.admin.StorePosDeviceAdminReceiver
```

4. Open StorePOS Terminal Launcher.
5. Long-press the StorePOS logo.
6. Enter the administrator PIN.
7. Enable **Kiosk lock**.

When enabled, the launcher:

- allowlists the launcher and `com.storepos.app` for Lock Task mode;
- disables the status bar/keyguard where the device permits it;
- prevents safe boot, user creation, factory reset, and physical-media mounting through Device Owner restrictions;
- can hide the Play Store and common browsers;
- registers itself as the persistent Home app.

## StorePOS update detection

The launcher checks the installed package version and uses a supported baseline (`versionCode >= 14`, StorePOS 1.4.0 at the time this project was prepared). It also reads the StorePOS GitHub release list and ignores release tags containing `launcher`, because the original launcher v1.0.0 was temporarily released from the main StorePOS repository.

## Printer handling

The launcher does not implement SUNMI/ESC-POS receipt printing itself. The **Receipt printer** shortcut hands the user back to StorePOS, where the actual printer settings and print workflow belong.

## Build

The repository is configured for Gradle 8.11.1 / Android Gradle Plugin 8.10.1 / Kotlin 2.2.21.

```bash
gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions workflows are included for CI and releases.

## Release signing note

The supplied GitHub release workflow creates and caches a stable debug-style signing key **inside this standalone repository** so repeated GitHub releases can update each other.

The original launcher-v1.0.0 APK was signed from the `StorePOS-Android` repository. A brand-new standalone repository cannot recover that private signing key from the old repository cache. If Android reports a signature mismatch when moving from the old v1.0.0 APK to this standalone build, uninstall the old launcher once before installing the new standalone build, or migrate the original signing key manually if you control it.

For production distribution, replace the CI debug-style signing setup with a dedicated release keystore stored in GitHub Actions secrets.
