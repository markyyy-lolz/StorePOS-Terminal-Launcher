package com.storepos.launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
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
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private lateinit var prefs: LauncherPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = LauncherPrefs(this)
        if (prefs.kioskEnabled && KioskController.isDeviceOwner(this)) {
            KioskController.applyKiosk(this, true, prefs.hideEscapeApps)
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

    var starting by remember { mutableStateOf(true) }
    var page by remember { mutableStateOf(Page.Home) }
    var hasPin by remember { mutableStateOf(prefs.hasAdminPin()) }
    var showAdminPin by remember { mutableStateOf(false) }
    var showAdmin by remember { mutableStateOf(false) }
    var showChangePin by remember { mutableStateOf(false) }
    var showVolume by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var kioskEnabled by remember { mutableStateOf(prefs.kioskEnabled) }
    var autoOpen by remember { mutableStateOf(prefs.autoOpenStorePos) }
    var hideEscapeApps by remember { mutableStateOf(prefs.hideEscapeApps) }
    var status by remember { mutableStateOf(StorePosInspector.inspect(context)) }

    LaunchedEffect(Unit) {
        status = StorePosInspector.inspect(context)
        delay(650)
        starting = false
        if (hasPin && autoOpen && status.installed) KioskController.openStorePos(context)
    }

    if (starting) {
        StartingScreen()
        return
    }

    if (!hasPin) {
        PinSetupScreen {
            prefs.setAdminPin(it)
            hasPin = true
        }
        return
    }

    if (showAdmin) {
        AdminPanel(
            deviceOwner = KioskController.isDeviceOwner(context),
            kioskEnabled = kioskEnabled,
            autoOpen = autoOpen,
            hideEscapeApps = hideEscapeApps,
            onBack = { showAdmin = false },
            onDefaultHome = { KioskController.openHomeSettings(context) },
            onFullSettings = { KioskController.openSystemSettings(context) },
            onKioskChange = { enabled ->
                if (KioskController.applyKiosk(activity, enabled, hideEscapeApps)) {
                    kioskEnabled = enabled
                    prefs.kioskEnabled = enabled
                    toast(context, if (enabled) "Kiosk lock enabled." else "Kiosk lock disabled.")
                } else {
                    toast(context, "Device Owner provisioning is required for full kiosk mode.")
                }
            },
            onAutoOpen = {
                autoOpen = it
                prefs.autoOpenStorePos = it
            },
            onHideEscapeApps = {
                hideEscapeApps = it
                prefs.hideEscapeApps = it
                if (KioskController.isDeviceOwner(context) && kioskEnabled) {
                    KioskController.setEscapeAppsHidden(context, it)
                }
            },
            onChangePin = { showChangePin = true },
            onRestart = { activity.recreate() }
        )
    } else {
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
                onInstallOrUpdate = {
                    if (kioskEnabled) toast(context, "Administrator access is required to install updates while kiosk mode is active.")
                    else openUrl(context, KioskController.STOREPOS_RELEASE_URL)
                },
                onWifi = { launchSetting(context, Settings.ACTION_WIFI_SETTINGS) },
                onPrinter = {
                    if (!KioskController.openStorePos(context)) toast(context, "Install StorePOS first.")
                },
                onVolume = { showVolume = true },
                onHelp = { showHelp = true }
            )
            Page.Settings -> TerminalSettingsScreen(
                status = status,
                kioskEnabled = kioskEnabled,
                onBack = {
                    status = StorePosInspector.inspect(context)
                    page = Page.Home
                },
                onStorePos = {
                    if (!KioskController.openStorePos(context)) toast(context, "Install StorePOS first.")
                },
                onUpdate = {
                    if (kioskEnabled) toast(context, "Administrator access is required to install updates while kiosk mode is active.")
                    else openUrl(context, KioskController.STOREPOS_RELEASE_URL)
                }
            )
        }
    }

    if (showAdminPin) {
        AdminPinDialog(
            onDismiss = { showAdminPin = false },
            onVerify = { pin ->
                if (prefs.verifyAdminPin(pin)) {
                    showAdminPin = false
                    showAdmin = true
                    true
                } else false
            }
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

    if (showVolume) VolumeDialog(onDismiss = { showVolume = false })
    if (showHelp) HelpDialog(onDismiss = { showHelp = false })
}

@Composable
private fun StartingScreen() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF173D98), StoreBlueBright, Color(0xFF23BFD5), Color(0xFF6DE0D4))
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(32.dp), color = Color.White, shadowElevation = 8.dp) {
                Image(
                    painter = painterResource(R.drawable.storepos_logo_mark),
                    contentDescription = "StorePOS",
                    modifier = Modifier.padding(22.dp).size(104.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "Starting StorePOS Terminal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                modifier = Modifier.width(220.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.22f)
            )
        }
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
    onInstallOrUpdate: () -> Unit,
    onWifi: () -> Unit,
    onPrinter: () -> Unit,
    onVolume: () -> Unit,
    onHelp: () -> Unit
) {
    val context = LocalContext.current
    var clock by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            clock = LocalTime.now()
            delay(30_000)
        }
    }

    val battery = batteryPercent(context)
    val wifiConnected = isWifiConnected(context)
    val isSunmi = Build.MANUFACTURER.contains("sunmi", true) || Build.BRAND.contains("sunmi", true)

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF173D98), Color(0xFF0A73E8), Color(0xFF28C4D5), Color(0xFFDFF8F3))
            )
        )
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.widthIn(max = 980.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    Modifier.weight(1f).combinedClickable(onClick = {}, onLongClick = onAdmin),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.98f)) {
                        Image(
                            painter = painterResource(R.drawable.storepos_logo_mark),
                            contentDescription = "StorePOS",
                            modifier = Modifier.padding(9.dp).size(46.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("StorePOS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = 0.18f)) {
                                Text(
                                    "TERMINAL",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                        Text(
                            if (kioskEnabled) "Dedicated checkout · Kiosk active" else "Dedicated checkout device",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    HeaderIndicator(if (wifiConnected) Icons.Rounded.Wifi else Icons.Rounded.WifiOff, if (wifiConnected) "Wi-Fi" else "Offline")
                    HeaderIndicator(Icons.Rounded.BatteryFull, "$battery%")
                    HeaderIndicator(Icons.Rounded.Schedule, clock.format(DateTimeFormatter.ofPattern("h:mm")))
                }
            }

            Spacer(Modifier.height(30.dp))
            Column(Modifier.widthIn(max = 980.dp).fillMaxWidth()) {
                Text("Select an option", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
                Text("Everything your cashier needs, without the Android clutter.", color = Color.White.copy(alpha = 0.84f))
            }
            Spacer(Modifier.height(20.dp))

            BoxWithConstraints(Modifier.widthIn(max = 980.dp).fillMaxWidth()) {
                if (maxWidth >= 700.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Box(Modifier.weight(1.25f)) {
                            if (status.installed) {
                                MainActionCard(
                                    icon = Icons.Rounded.PointOfSale,
                                    title = "Open Register",
                                    subtitle = "Start selling with StorePOS",
                                    detail = "StorePOS " + (status.versionName ?: "installed") + " · " + if (status.outdated) "Update available" else "Ready",
                                    badge = if (status.outdated) "UPDATE AVAILABLE" else null,
                                    onClick = onStorePos
                                )
                            } else MissingStorePosCard(onInstallOrUpdate)
                        }
                        Box(Modifier.weight(1f)) {
                            MainActionCard(
                                icon = Icons.Rounded.Settings,
                                title = "Device Settings",
                                subtitle = "Manage network, printer and terminal controls",
                                detail = if (isSunmi) "SUNMI terminal detected" else Build.MANUFACTURER + " " + Build.MODEL,
                                onClick = onSettings
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (status.installed) {
                            MainActionCard(
                                icon = Icons.Rounded.PointOfSale,
                                title = "Open Register",
                                subtitle = "Start selling with StorePOS",
                                detail = "StorePOS " + (status.versionName ?: "installed") + " · " + if (status.outdated) "Update available" else "Ready",
                                badge = if (status.outdated) "UPDATE AVAILABLE" else null,
                                onClick = onStorePos
                            )
                        } else MissingStorePosCard(onInstallOrUpdate)

                        MainActionCard(
                            icon = Icons.Rounded.Settings,
                            title = "Device Settings",
                            subtitle = "Manage network, printer and terminal controls",
                            detail = if (isSunmi) "SUNMI terminal detected" else Build.MANUFACTURER + " " + Build.MODEL,
                            onClick = onSettings
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Surface(
                modifier = Modifier.widthIn(max = 980.dp).fillMaxWidth(),
                color = Color.White.copy(alpha = 0.96f),
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 5.dp
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Quick Access", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(14.dp))
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        if (maxWidth >= 620.dp) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                QuickTile(Modifier.weight(1f), Icons.Rounded.Wifi, "Wi-Fi", onWifi)
                                QuickTile(Modifier.weight(1f), Icons.Rounded.Print, "Printer", onPrinter)
                                QuickTile(Modifier.weight(1f), Icons.Rounded.VolumeUp, "Volume", onVolume)
                                QuickTile(Modifier.weight(1f), Icons.Rounded.HelpOutline, "Help", onHelp)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    QuickTile(Modifier.weight(1f), Icons.Rounded.Wifi, "Wi-Fi", onWifi)
                                    QuickTile(Modifier.weight(1f), Icons.Rounded.Print, "Printer", onPrinter)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    QuickTile(Modifier.weight(1f), Icons.Rounded.VolumeUp, "Volume", onVolume)
                                    QuickTile(Modifier.weight(1f), Icons.Rounded.HelpOutline, "Help", onHelp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                modifier = Modifier.widthIn(max = 980.dp).fillMaxWidth(),
                color = Color.White.copy(alpha = 0.90f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactStatus(
                            if (status.installed && !status.outdated) "StorePOS Ready"
                            else if (status.installed) "Update Available"
                            else "StorePOS Missing",
                            status.installed && !status.outdated
                        )
                        if (kioskEnabled || deviceOwner) CompactStatus(if (kioskEnabled) "Kiosk Active" else "Kiosk Ready", true)
                    }
                    Text("Launcher v1.2.0", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Powered by StorePOS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF124C96))
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeaderIndicator(icon: ImageVector, text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = 0.16f)) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun MainActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    detail: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 178.dp).animateContentSize(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(22.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(shape = RoundedCornerShape(19.dp), color = Color(0xFFEAF3FF)) {
                    Icon(icon, null, tint = StoreBlueBright, modifier = Modifier.padding(14.dp).size(34.dp))
                }
                Spacer(Modifier.weight(1f))
                if (badge != null) {
                    Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFFFF0D8)) {
                        Text(
                            badge,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF9B5D00)
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(20.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(detail, style = MaterialTheme.typography.labelMedium, color = StoreTeal, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MissingStorePosCard(onInstall: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 178.dp),
        shape = RoundedCornerShape(30.dp),
        color = Color.White,
        shadowElevation = 7.dp
    ) {
        Column(Modifier.padding(22.dp)) {
            Surface(shape = RoundedCornerShape(19.dp), color = Color(0xFFFFF0E8)) {
                Icon(Icons.Rounded.DownloadForOffline, null, tint = Color(0xFFD7672E), modifier = Modifier.padding(14.dp).size(34.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text("StorePOS isn't installed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("Install StorePOS to start accepting transactions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onInstall) {
                Icon(Icons.Rounded.Download, null)
                Spacer(Modifier.width(8.dp))
                Text("Install StorePOS")
            }
        }
    }
}

@Composable
private fun QuickTile(modifier: Modifier, icon: ImageVector, label: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFF)),
        border = BorderStroke(1.dp, Color(0xFFE7EEF8))
    ) {
        Column(
            Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = StoreBlueBright, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CompactStatus(text: String, active: Boolean) {
    Surface(shape = RoundedCornerShape(999.dp), color = if (active) Color(0xFFE5F8F1) else Color(0xFFFFF0E4)) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (active) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                null,
                tint = if (active) Color(0xFF11845A) else Color(0xFFB46727),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TerminalSettingsScreen(
    status: StorePosStatus,
    kioskEnabled: Boolean,
    onBack: () -> Unit,
    onStorePos: () -> Unit,
    onUpdate: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity

    Column(Modifier.fillMaxSize().background(Color(0xFFF4F7FB))) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.horizontalGradient(listOf(Color(0xFF173D98), StoreBlueBright, Color(0xFF18B7C7)))
            ).padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.16f)) {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White) }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Device Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
                    Text("Terminal configuration", color = Color.White.copy(alpha = 0.82f))
                }
            }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection("Connectivity") {
                SettingsItem(Icons.Rounded.Wifi, "Wi-Fi", if (isWifiConnected(context)) "Connected" else "Not connected") {
                    launchSetting(context, Settings.ACTION_WIFI_SETTINGS)
                }
                SettingsItem(Icons.Rounded.Bluetooth, "Bluetooth", "Pair receipt printers and accessories") {
                    launchSetting(context, Settings.ACTION_BLUETOOTH_SETTINGS)
                }
            }

            SettingsSection("Terminal") {
                BrightnessControl(activity)
                VolumeControl()
                SettingsItem(Icons.Rounded.Schedule, "Date & time", "Timezone and automatic date/time") {
                    launchSetting(context, Settings.ACTION_DATE_SETTINGS)
                }
            }

            SettingsSection("StorePOS") {
                SettingsItem(Icons.Rounded.Print, "Receipt Printer", "Configure printing and run test prints inside StorePOS", onStorePos)
                SettingsItem(
                    Icons.Rounded.SystemUpdate,
                    "StorePOS Update",
                    if (status.outdated) "Update recommended" else "Installed " + (status.versionName ?: "version"),
                    onUpdate
                )
                SettingsItem(
                    Icons.Rounded.Info,
                    "Device Information",
                    Build.MANUFACTURER + " " + Build.MODEL + " · Android " + Build.VERSION.RELEASE
                ) {
                    launchSetting(context, Settings.ACTION_DEVICE_INFO_SETTINGS)
                }
            }

            Surface(shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE3EAF4))) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, null, tint = StoreTeal)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (kioskEnabled)
                            "Kiosk mode is active. Full Android Settings and software installation require administrator access."
                        else
                            "Full Android Settings and kiosk controls are protected by the administrator PIN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = StoreBlue,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), color = Color.White, shadowElevation = 2.dp) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
        }
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(15.dp), color = Color(0xFFEAF3FF)) {
                Icon(icon, null, tint = StoreBlue, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFF9AA8BA))
        }
    }
}

@Composable
private fun BrightnessControl(activity: Activity) {
    val initial = activity.window.attributes.screenBrightness.let { if (it < 0f) 0.65f else it.coerceIn(0.1f, 1f) }
    var brightness by remember { mutableFloatStateOf(initial) }

    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(15.dp), color = Color(0xFFEAF3FF)) {
                Icon(Icons.Rounded.Brightness6, null, tint = StoreBlue, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Brightness", fontWeight = FontWeight.Bold)
                Text("Launcher screen brightness", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(((brightness * 100).toInt()).toString() + "%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = brightness,
            onValueChange = {
                brightness = it.coerceIn(0.1f, 1f)
                val params = activity.window.attributes
                params.screenBrightness = brightness
                activity.window.attributes = params
            },
            valueRange = 0.1f..1f
        )
    }
}

@Composable
private fun VolumeControl() {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(AudioManager::class.java) }
    val max = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var current by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }

    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(15.dp), color = Color(0xFFEAF3FF)) {
                Icon(Icons.Rounded.VolumeUp, null, tint = StoreBlue, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Volume", fontWeight = FontWeight.Bold)
                Text("Media and StorePOS sound level", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(((current / max * 100).toInt()).toString() + "%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = current,
            onValueChange = {
                current = it
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, it.toInt(), 0)
            },
            valueRange = 0f..max.toFloat()
        )
    }
}

@Composable
private fun AdminPanel(
    deviceOwner: Boolean,
    kioskEnabled: Boolean,
    autoOpen: Boolean,
    hideEscapeApps: Boolean,
    onBack: () -> Unit,
    onDefaultHome: () -> Unit,
    onFullSettings: () -> Unit,
    onKioskChange: (Boolean) -> Unit,
    onAutoOpen: (Boolean) -> Unit,
    onHideEscapeApps: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    onRestart: () -> Unit
) {
    var confirmExit by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color(0xFFF4F7FB))) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.horizontalGradient(listOf(Color(0xFF173D98), StoreBlueBright, StoreTeal))
            ).padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.16f)) {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White) }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Terminal Administration", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
                    Text("Protected StorePOS controls", color = Color.White.copy(alpha = 0.82f))
                }
            }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection("Kiosk") {
                AdminToggleRow(
                    Icons.Rounded.Lock,
                    "Kiosk Mode",
                    if (deviceOwner) "Restrict Home, Recents, status bar and unapproved apps."
                    else "Device Owner provisioning is required for full kiosk mode.",
                    kioskEnabled,
                    deviceOwner,
                    onKioskChange
                )
                AdminToggleRow(
                    Icons.Rounded.RocketLaunch,
                    "Auto-open StorePOS",
                    "Launch the register automatically after terminal startup.",
                    autoOpen,
                    true,
                    onAutoOpen
                )
                AdminToggleRow(
                    Icons.Rounded.Block,
                    "Block external apps",
                    "Hide Play Store and common browsers while full kiosk mode is active.",
                    hideEscapeApps,
                    deviceOwner,
                    onHideEscapeApps
                )
            }

            SettingsSection("System") {
                SettingsItem(Icons.Rounded.Home, "Set as default Home", "Choose StorePOS Terminal as Android Home app", onDefaultHome)
                SettingsItem(Icons.Rounded.AdminPanelSettings, "Full Android Settings", "Open unrestricted Android system settings", onFullSettings)
                SettingsItem(Icons.Rounded.RestartAlt, "Restart launcher", "Reload StorePOS Terminal Launcher", onRestart)
            }

            SettingsSection("Security") {
                SettingsItem(Icons.Rounded.Password, "Change Admin PIN", "Replace the protected administrator PIN", onChangePin)
            }

            if (!deviceOwner) {
                Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFFFFF7E8), border = BorderStroke(1.dp, Color(0xFFF3D49B))) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Device Owner not provisioned", fontWeight = FontWeight.Black)
                        Text(
                            "For full kiosk controls, provision a freshly reset device through ADB:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "adb shell dpm set-device-owner com.storepos.launcher/.admin.StorePosDeviceAdminReceiver",
                            style = MaterialTheme.typography.bodySmall,
                            color = StoreBlue
                        )
                    }
                }
            }

            if (kioskEnabled) {
                OutlinedButton(
                    onClick = { confirmExit = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Icon(Icons.Rounded.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Exit StorePOS Terminal Kiosk")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            icon = { Icon(Icons.Rounded.WarningAmber, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Exit kiosk mode?") },
            text = { Text("Android navigation and hidden external apps will be restored where supported.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmExit = false
                        onKioskChange(false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Exit kiosk") }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(15.dp), color = Color(0xFFEAF3FF)) {
            Icon(icon, null, tint = StoreBlue, modifier = Modifier.padding(10.dp).size(24.dp))
        }
        Spacer(Modifier.width(13.dp))
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
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF173D98), StoreBlueBright, Color(0xFF5BDED3)))
        ).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            Column(
                Modifier.padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(painterResource(R.drawable.storepos_logo_mark), null, modifier = Modifier.size(92.dp))
                Text("Secure this terminal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(
                    "Create an administrator PIN before the launcher is used as a checkout terminal.",
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
        title = { Text(title, fontWeight = FontWeight.Black) },
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
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(8) },
            label = { Text("Administrator PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it.filter(Char::isDigit).take(8) },
            label = { Text("Confirm PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Use 4–8 digits.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { onSet(pin) }, enabled = valid, modifier = Modifier.fillMaxWidth()) {
            Text("Save administrator PIN")
        }
    }
}

@Composable
private fun AdminPinDialog(onDismiss: () -> Unit, onVerify: (String) -> Boolean) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Administrator Access", fontWeight = FontWeight.Black) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Enter your PIN to manage this terminal.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Text(
                    if (pin.isEmpty()) "○ ○ ○ ○" else "• ".repeat(pin.length).trim(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = if (error) MaterialTheme.colorScheme.error else StoreBlue
                )
                AnimatedVisibility(error) {
                    Text("Incorrect administrator PIN", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(16.dp))

                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { digit ->
                            KeypadButton(Modifier.weight(1f), digit) {
                                if (pin.length < 8) pin += digit
                                error = false
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeypadIconButton(Modifier.weight(1f), Icons.Rounded.Backspace) {
                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                        error = false
                    }
                    KeypadButton(Modifier.weight(1f), "0") {
                        if (pin.length < 8) pin += "0"
                        error = false
                    }
                    Button(
                        onClick = {
                            if (pin.length >= 4) {
                                val ok = onVerify(pin)
                                error = !ok
                                if (!ok) pin = ""
                            }
                        },
                        enabled = pin.length >= 4,
                        modifier = Modifier.weight(1f).height(62.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) { Icon(Icons.Rounded.ArrowForward, null) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun KeypadButton(modifier: Modifier, label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(62.dp), shape = RoundedCornerShape(18.dp)) {
        Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun KeypadIconButton(modifier: Modifier, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(62.dp), shape = RoundedCornerShape(18.dp)) {
        Icon(icon, "Backspace")
    }
}

@Composable
private fun VolumeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.VolumeUp, null, tint = StoreBlue) },
        title = { Text("Terminal Volume", fontWeight = FontWeight.Black) },
        text = { VolumeControl() },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.HelpOutline, null, tint = StoreBlue) },
        title = { Text("StorePOS Terminal Help", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Open Register starts the StorePOS cashier app.")
                Text("Printer setup and test printing are handled inside StorePOS.")
                Text("Long-press the StorePOS logo to open protected administrator controls.")
                Text("For full kiosk restrictions, provision this launcher as Android Device Owner.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private fun batteryPercent(context: Context): Int {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    if (level < 0 || scale <= 0) return 0
    return ((level * 100f) / scale).toInt().coerceIn(0, 100)
}

private fun isWifiConnected(context: Context): Boolean {
    return runCatching {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork ?: return@runCatching false
        val capabilities = manager.getNetworkCapabilities(network) ?: return@runCatching false
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }.getOrDefault(false)
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
