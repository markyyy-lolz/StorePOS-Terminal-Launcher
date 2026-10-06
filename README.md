# StorePOS Terminal Launcher

Standalone Android Home/Launcher for dedicated StorePOS checkout tablets and Android POS terminals.

## v1.2.0 — Terminal UI Update

StorePOS Terminal now uses a payment-terminal inspired interface built specifically for cashier devices:

- Blue → cyan → teal terminal gradient
- Large floating white action cards
- **Open Register** and **Device Settings** as the two primary actions
- Quick Access for Wi-Fi, Printer, Volume and Help
- Live terminal status for Wi-Fi, battery and time
- Missing/outdated StorePOS states
- Responsive compact-terminal and tablet layouts
- Payment-terminal style numeric administrator PIN keypad
- Full-screen protected administration panel
- StorePOS startup/loading screen
- Launcher brightness and media-volume controls

## Packages

- Launcher: `com.storepos.launcher`
- Main StorePOS app: `com.storepos.app`

## Kiosk features

- Default Android Home/Launcher support
- Optional StorePOS auto-launch
- Android Device Owner + Lock Task kiosk mode
- Home, Recents, status-bar and keyguard restrictions where supported
- Optional Play Store/common browser hiding during full kiosk mode
- Proper restoration of hidden apps when kiosk mode is disabled
- Boot handling
- SUNMI-class Android compatibility
- Printer setup remains primarily inside StorePOS

## Administrator access

Long-press the StorePOS logo to open the PIN keypad.

The admin panel controls:

- Kiosk mode
- Auto-open StorePOS
- External-app blocking
- Default Home assignment
- Full Android Settings
- Launcher restart
- Admin PIN changes
- Kiosk exit

Existing v1.1.0 administrator PINs are migrated automatically to stronger PBKDF2-HMAC-SHA256 storage after a successful unlock.

## Full kiosk setup

On a freshly reset Android device:

    adb shell dpm set-device-owner com.storepos.launcher/.admin.StorePosDeviceAdminReceiver

Then long-press the StorePOS logo and enable **Kiosk Mode**.

## Build

CI uses JDK 17, Gradle 8.11.1, Android Gradle Plugin 8.10.1 and Kotlin 2.2.21.

    gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
