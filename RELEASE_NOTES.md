# StorePOS Terminal Launcher v1.1.0 — White Edition

- Modern white-first tablet UI with blue/teal StorePOS branding.
- StorePOS — Open Register and Device Settings primary actions.
- Responsive portrait/landscape layouts.
- Detects main StorePOS package com.storepos.app and warns when missing/outdated.
- Restricted Wi-Fi, Bluetooth/printer, display, volume, date/time, device info and update shortcuts.
- Long-press StorePOS logo for PIN-protected administrator access.
- Default Home support, auto-open StorePOS and boot handling.
- Device Owner + Lock Task kiosk mode.
- Home/Recents/status-bar/keyguard restrictions where Android permits them.
- Play Store/common browsers hidden during full Device Owner kiosk mode.
- Regular Android tablet and SUNMI-class compatibility.

Full kiosk provisioning on a freshly reset device:

    adb shell dpm set-device-owner com.storepos.launcher/.admin.StorePosDeviceAdminReceiver
