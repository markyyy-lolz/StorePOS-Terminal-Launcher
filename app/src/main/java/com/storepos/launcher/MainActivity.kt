package com.storepos.launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.storepos.launcher.ui.theme.StoreBlue
import com.storepos.launcher.ui.theme.StoreBlueBright
import com.storepos.launcher.ui.theme.StorePosLauncherTheme
import com.storepos.launcher.ui.theme.StoreTeal

class MainActivity : ComponentActivity() {
    private lateinit var prefs: LauncherPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = LauncherPrefs(this)
        if (prefs.kioskEnabled && KioskController.isDeviceOwner(this)) {
            KioskController.applyKiosk(this, true)
        }
        setContent { StorePosLauncherTheme { LauncherApp() } }
    }
}

private enum class Page { Home, Settings }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LauncherApp() {
    val context = LocalContext.current
    val activity = context as Activity
    val prefs = remember { LauncherPrefs(context) }

    var page by remember { mutableStateOf(Page.Home) }
    var hasPin by remember { mutableStateOf(prefs.hasAdminPin()) }
    var showAdminPin by remember { mutableStateOf(false) }
    var showAdmin by remember { mutableStateOf(false) }
    var showChangePin by remember { mutableStateOf(false) }
    var kioskEnabled by remember { mutableStateOf(prefs.kioskEnabled) }
    var autoOpen by remember { mutableStateOf(prefs.autoOpenStorePos) }
    var status by remember { mutableStateOf(StorePosInspector.inspect(context)) }

    LaunchedEffect(Unit) {
        status = StorePosInspector.inspect(context)
        if (hasPin && autoOpen && status.installed) KioskController.openStorePos(context)
    }

    if (!hasPin) {
        PinSetupScreen {
            prefs.setAdminPin(it)
            hasPin = true
        }
        return
    }

    when (page) {
        Page.Home -> LauncherHome(
            status = status,
            kioskEnabled = kioskEnabled,
            deviceOwner = KioskController.isDeviceOwner(context),
            onStorePos = {
                if (!KioskController.openStorePos(context)) {
                    status = StorePosInspector.inspect(context)
                    toast(context, "StorePOS is not installed.")
                }
            },
            onSettings = { page = Page.Settings },
            onAdmin = { showAdminPin = true },
            onUpdate = { openUrl(context, KioskController.STOREPOS_RELEASE_URL) }
        )
        Page.Settings -> RestrictedSettings(
            onBack = { status = StorePosInspector.inspect(context); page = Page.Home },
            onStorePos = {
                if (!KioskController.openStorePos(context)) toast(context, "Install StorePOS first.")
            },
            onUpdate = { openUrl(context, KioskController.STOREPOS_RELEASE_URL) }
        )
    }

    if (showAdminPin) {
        PinVerifyDialog(
            onDismiss = { showAdminPin = false },
            onVerify = {
                if (prefs.verifyAdminPin(it)) {
                    showAdminPin = false
                    showAdmin = true
                    true
                } else false
            }
        )
    }

    if (showAdmin) {
        AdminDialog(
            deviceOwner = KioskController.isDeviceOwner(context),
            kioskEnabled = kioskEnabled,
            autoOpen = autoOpen,
            onDismiss = { showAdmin = false },
            onDefaultHome = { KioskController.openHomeSettings(context) },
            onFullSettings = { KioskController.openSystemSettings(context) },
            onKioskChange = { enabled ->
                if (KioskController.applyKiosk(activity, enabled)) {
                    kioskEnabled = enabled
                    prefs.kioskEnabled = enabled
                    toast(context, if (enabled) "Kiosk lock enabled." else "Kiosk lock disabled.")
                } else {
                    toast(context, "Device Owner provisioning is required for full kiosk mode.")
                }
            },
            onAutoOpen = { autoOpen = it; prefs.autoOpenStorePos = it },
            onChangePin = { showChangePin = true }
        )
    }

    if (showChangePin) {
        PinSetupDialog(
            title = "Change administrator PIN",
            onDismiss = { showChangePin = false },
            onSet = {
                prefs.setAdminPin(it)
                showChangePin = false
                toast(context, "Administrator PIN updated.")
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LauncherHome(
    status: StorePosStatus,
    kioskEnabled: Boolean,
    deviceOwner: Boolean,
    onStorePos: () -> Unit,
    onSettings: () -> Unit,
    onAdmin: () -> Unit,
    onUpdate: () -> Unit
) {
    val isSunmi = Build.MANUFACTURER.contains("sunmi", true) || Build.BRAND.contains("sunmi", true)

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF8FBFF), Color(0xFFF4FAFA))))
            .padding(horizontal = 28.dp, vertical = 22.dp)
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.widthIn(max = 1000.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.storepos_logo_mark),
                    contentDescription = "StorePOS",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(76.dp).combinedClickable(onClick = {}, onLongClick = onAdmin)
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("StorePOS Terminal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("Dedicated retail launcher", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusChip(
                    if (kioskEnabled) "KIOSK LOCKED" else if (deviceOwner) "KIOSK READY" else "STANDARD",
                    kioskEnabled || deviceOwner
                )
            }

            Spacer(Modifier.height(30.dp))
            Text(
                "Ready for checkout.",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                if (isSunmi) "SUNMI terminal detected · printing stays inside StorePOS."
                else "Open the register or manage only the device settings you need.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            AnimatedVisibility(!status.installed || status.outdated) { WarningBanner(status, onUpdate) }
            if (!status.installed || status.outdated) Spacer(Modifier.height(18.dp))

            BoxWithConstraints(Modifier.widthIn(max = 1000.dp).fillMaxWidth()) {
                if (maxWidth >= 700.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        LauncherCard(
                            Modifier.weight(1.35f),
                            Icons.Rounded.PointOfSale,
                            "StorePOS",
                            if (status.installed) "Open Register" else "StorePOS not installed",
                            true,
                            status.installed,
                            onStorePos
                        )
                        LauncherCard(
                            Modifier.weight(1f),
                            Icons.Rounded.Settings,
                            "Device Settings",
                            "Wi-Fi · printer · display · sound",
                            false,
                            true,
                            onSettings
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        LauncherCard(
                            Modifier.fillMaxWidth(),
                            Icons.Rounded.PointOfSale,
                            "StorePOS",
                            if (status.installed) "Open Register" else "StorePOS not installed",
                            true,
                            status.installed,
                            onStorePos
                        )
                        LauncherCard(
                            Modifier.fillMaxWidth(),
                            Icons.Rounded.Settings,
                            "Device Settings",
                            "Wi-Fi · printer · display · sound",
                            false,
                            true,
                            onSettings
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Surface(
                modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip(
                        if (status.installed) "StorePOS " + (status.versionName ?: "installed") else "StorePOS missing",
                        status.installed && !status.outdated
                    )
                    Text(
                        if (isSunmi) "SUNMI compatible" else Build.MANUFACTURER + " · " + Build.MODEL,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(34.dp))
            Text(
                "Long-press the StorePOS logo for administrator access.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "StorePOS Terminal Launcher · v1.1.0",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = StoreBlue
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WarningBanner(status: StorePosStatus, onUpdate: () -> Unit) {
    Surface(
        modifier = Modifier.widthIn(max = 1000.dp).fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFFF8E8),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0D59B))
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.SystemUpdate, null, tint = Color(0xFF9A6500), modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(if (!status.installed) "StorePOS is required" else "StorePOS update recommended", fontWeight = FontWeight.Bold)
                Text(
                    if (!status.installed) "Install com.storepos.app before using this terminal."
                    else "Installed " + (status.versionName ?: "version") + " · supported baseline " + VersionUtils.MIN_STOREPOS_VERSION_NAME,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onUpdate) { Text(if (!status.installed) "Install" else "Update") }
        }
    }
}

@Composable
private fun LauncherCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val container = if (primary) StoreBlue else Color.White
    val content = if (primary) Color.White else MaterialTheme.colorScheme.onSurface
    val secondary = if (primary) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(220.dp).animateContentSize(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = container,
            disabledContainerColor = Color(0xFFE9EEF5),
            disabledContentColor = Color(0xFF8090A5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (primary) 8.dp else 3.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(26.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Surface(
                shape = CircleShape,
                color = if (primary) Color.White.copy(alpha = 0.14f) else Color(0xFFEAF2FF)
            ) {
                Icon(
                    icon, null,
                    tint = if (primary) Color.White else StoreBlueBright,
                    modifier = Modifier.padding(16.dp).size(38.dp)
                )
            }
            Column {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = content)
                Text(subtitle, color = secondary)
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (active) Color(0xFFE7F8F2) else Color(0xFFF0F3F7)
    ) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (active) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                null,
                tint = if (active) Color(0xFF15915D) else Color(0xFF718096),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RestrictedSettings(onBack: () -> Unit, onStorePos: () -> Unit, onUpdate: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF8FAFD)).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
            Spacer(Modifier.width(6.dp))
            Column {
                Text("Device Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Only essential terminal controls are shown.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        SettingsItem(Icons.Rounded.Wifi, "Wi-Fi", "Connect the terminal to your store network") {
            launchSetting(context, Settings.ACTION_WIFI_SETTINGS)
        }
        SettingsItem(Icons.Rounded.Bluetooth, "Bluetooth & printer", "Pair Bluetooth accessories and receipt printers") {
            launchSetting(context, Settings.ACTION_BLUETOOTH_SETTINGS)
        }
        SettingsItem(Icons.Rounded.Print, "Receipt printer", "Open StorePOS for printer setup and test print", onStorePos)
        SettingsItem(Icons.Rounded.Brightness6, "Display & brightness", "Brightness, timeout and display options") {
            launchSetting(context, Settings.ACTION_DISPLAY_SETTINGS)
        }
        SettingsItem(Icons.Rounded.VolumeUp, "Volume", "Adjust terminal media and notification volume") {
            launchSetting(context, Settings.ACTION_SOUND_SETTINGS)
        }
        SettingsItem(Icons.Rounded.Schedule, "Date & time", "Timezone and automatic date/time") {
            launchSetting(context, Settings.ACTION_DATE_SETTINGS)
        }
        SettingsItem(Icons.Rounded.Info, "Device information", Build.MANUFACTURER + " " + Build.MODEL + " · Android " + Build.VERSION.RELEASE) {
            launchSetting(context, Settings.ACTION_DEVICE_INFO_SETTINGS)
        }
        SettingsItem(Icons.Rounded.SystemUpdate, "StorePOS updates", "Check the official StorePOS release channel", onUpdate)

        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Security, null, tint = StoreTeal)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Full Android Settings and kiosk exit are protected by the administrator PIN.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color(0xFFEAF2FF)) {
                Icon(icon, null, tint = StoreBlue, modifier = Modifier.padding(12.dp).size(25.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AdminDialog(
    deviceOwner: Boolean,
    kioskEnabled: Boolean,
    autoOpen: Boolean,
    onDismiss: () -> Unit,
    onDefaultHome: () -> Unit,
    onFullSettings: () -> Unit,
    onKioskChange: (Boolean) -> Unit,
    onAutoOpen: (Boolean) -> Unit,
    onChangePin: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Administrator Access", fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                StatusChip(if (deviceOwner) "DEVICE OWNER ACTIVE" else "STANDARD LAUNCHER MODE", deviceOwner)
                HorizontalDivider()
                ToggleRow(
                    "Kiosk lock",
                    if (deviceOwner) "Lock Home, Recents and system escape paths where Android permits it."
                    else "Provision Device Owner first for full kiosk control.",
                    kioskEnabled, deviceOwner, onKioskChange
                )
                ToggleRow(
                    "Auto-open StorePOS",
                    "Launch the StorePOS register automatically when this launcher starts.",
                    autoOpen, true, onAutoOpen
                )
                HorizontalDivider()
                FilledTonalButton(onClick = onDefaultHome, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Home, null); Spacer(Modifier.width(8.dp)); Text("Set as default Home app")
                }
                FilledTonalButton(onClick = onFullSettings, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.AdminPanelSettings, null); Spacer(Modifier.width(8.dp)); Text("Open full Android Settings")
                }
                if (kioskEnabled) {
                    OutlinedButton(onClick = { onKioskChange(false) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Logout, null); Spacer(Modifier.width(8.dp)); Text("Exit kiosk mode")
                    }
                }
                OutlinedButton(onClick = onChangePin, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Security, null); Spacer(Modifier.width(8.dp)); Text("Change administrator PIN")
                }
                if (!deviceOwner) {
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF6F8FB)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Full kiosk provisioning", fontWeight = FontWeight.Bold)
                            Text("Fresh/reset Android device + ADB:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "adb shell dpm set-device-owner com.storepos.launcher/.admin.StorePosDeviceAdminReceiver",
                                style = MaterialTheme.typography.bodySmall,
                                color = StoreBlue
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun ToggleRow(
    title: String, subtitle: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun PinSetupScreen(onSet: (String) -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF2F8FF)))).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                Modifier.padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(painterResource(R.drawable.storepos_logo_mark), null, modifier = Modifier.size(96.dp))
                Text("StorePOS Terminal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(
                    "Create an administrator PIN before this device is used as a checkout terminal.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PinFields(onSet)
            }
        }
    }
}

@Composable
private fun PinSetupDialog(title: String, onDismiss: () -> Unit, onSet: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { PinFields(onSet) },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PinFields(onSet: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val valid = pin.length in 4..8 && pin.all(Char::isDigit) && pin == confirm

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            pin, { pin = it.filter(Char::isDigit).take(8) },
            label = { Text("Administrator PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            confirm, { confirm = it.filter(Char::isDigit).take(8) },
            label = { Text("Confirm PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Use 4–8 digits.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { onSet(pin) }, enabled = valid, modifier = Modifier.fillMaxWidth()) { Text("Save PIN") }
    }
}

@Composable
private fun PinVerifyDialog(onDismiss: () -> Unit, onVerify: (String) -> Boolean) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Administrator PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    pin, { pin = it.filter(Char::isDigit).take(8); error = false },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
                if (error) Text("Incorrect administrator PIN.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = { error = !onVerify(pin) }, enabled = pin.length >= 4) { Text("Unlock") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun launchSetting(context: Context, action: String) {
    runCatching { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { toast(context, "This setting is not available on this device.") }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { toast(context, "No browser is available. Ask an administrator to install the StorePOS APK.") }
}

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}
