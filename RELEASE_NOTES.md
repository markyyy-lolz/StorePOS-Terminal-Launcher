# StorePOS Terminal Launcher v1.2.0 — Terminal UI Update

## New terminal interface
- Rebuilt the launcher around a premium payment-terminal style.
- Blue/cyan/teal gradient background with large floating white action cards.
- **Open Register** and **Device Settings** remain the primary cashier actions.
- Added a responsive Quick Access area for Wi-Fi, Printer, Volume and Help.
- Added Wi-Fi, battery and clock indicators in the terminal header.
- Added compact and wide tablet layouts.

## StorePOS states
- Dedicated missing-StorePOS install state.
- StorePOS version/ready status on the register card.
- Update-available badge when the installed StorePOS build is below the supported baseline.
- Software install/update links are blocked from normal kiosk use and require admin handling.

## Device Settings
- New grouped Connectivity, Terminal and StorePOS settings layout.
- Built-in launcher brightness slider.
- Built-in media-volume slider.
- Wi-Fi, Bluetooth, date/time and device-info shortcuts.
- Printer configuration continues to hand off to StorePOS.

## Administration
- Payment-terminal style numeric administrator PIN keypad.
- Full-screen Terminal Administration panel.
- Configurable full kiosk mode.
- Auto-open StorePOS toggle.
- Optional Play Store/common-browser hiding.
- Default Home, full Android Settings, launcher restart and PIN change controls.
- Kiosk exit confirmation restores supported Android navigation and hidden apps.

## Security
- New PINs use salted PBKDF2-HMAC-SHA256.
- Existing v1.1.0 SHA-256 PINs remain compatible and migrate automatically after successful verification.

## Compatibility
- Package: `com.storepos.launcher`
- Version: `1.2.0`
- Version code: `3`
- Android 8.0+ / API 26+
- Regular Android tablets and Android POS terminals
- SUNMI-class devices

Full kiosk provisioning on a freshly reset device:

    adb shell dpm set-device-owner com.storepos.launcher/.admin.StorePosDeviceAdminReceiver
