package com.storepos.launcher

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.UserManager
import android.provider.Settings
import com.storepos.launcher.admin.StorePosDeviceAdminReceiver

object KioskController {
    const val STOREPOS_PACKAGE = "com.storepos.app"
    const val STOREPOS_RELEASE_URL = "https://github.com/markyyy-lolz/StorePOS-Android/releases/latest"

    private fun manager(context: Context) =
        context.getSystemService(DevicePolicyManager::class.java)

    fun adminComponent(context: Context) =
        ComponentName(context, StorePosDeviceAdminReceiver::class.java)

    fun isDeviceOwner(context: Context): Boolean =
        manager(context).isDeviceOwnerApp(context.packageName)

    fun openStorePos(context: Context): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(STOREPOS_PACKAGE) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
        return true
    }

    fun openHomeSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openSystemSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun applyKiosk(activity: Activity, enabled: Boolean): Boolean {
        val dpm = manager(activity)
        if (!dpm.isDeviceOwnerApp(activity.packageName)) return false
        val admin = adminComponent(activity)

        if (enabled) {
            val packages = mutableListOf(activity.packageName)
            if (StorePosInspector.inspect(activity).installed) packages.add(STOREPOS_PACKAGE)
            dpm.setLockTaskPackages(admin, packages.toTypedArray())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
            }

            runCatching { dpm.addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT) }
            runCatching { dpm.addUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET) }
            runCatching { dpm.addUserRestriction(admin, UserManager.DISALLOW_ADD_USER) }
            runCatching { dpm.addUserRestriction(admin, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA) }
            runCatching { dpm.setStatusBarDisabled(admin, true) }
            runCatching { dpm.setKeyguardDisabled(admin, true) }

            val homeFilter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            runCatching {
                dpm.addPersistentPreferredActivity(
                    admin,
                    homeFilter,
                    ComponentName(activity, MainActivity::class.java)
                )
            }
            setEscapeAppsHidden(activity, dpm, admin, true)

            if (dpm.isLockTaskPermitted(activity.packageName)) {
                runCatching { activity.startLockTask() }
            }
        } else {
            runCatching { activity.stopLockTask() }
            runCatching { dpm.setStatusBarDisabled(admin, false) }
            runCatching { dpm.setKeyguardDisabled(admin, false) }
            runCatching { dpm.setLockTaskPackages(admin, emptyArray()) }
            runCatching { dpm.clearPackagePersistentPreferredActivities(admin, activity.packageName) }
            runCatching { dpm.clearUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT) }
            runCatching { dpm.clearUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET) }
            runCatching { dpm.clearUserRestriction(admin, UserManager.DISALLOW_ADD_USER) }
            runCatching { dpm.clearUserRestriction(admin, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA) }
            setEscapeAppsHidden(activity, dpm, admin, false)
        }
        return true
    }

    private fun setEscapeAppsHidden(
        context: Context,
        dpm: DevicePolicyManager,
        admin: ComponentName,
        hidden: Boolean
    ) {
        listOf("com.android.vending", "com.android.chrome", "com.google.android.apps.chrome", "org.mozilla.firefox")
            .forEach { pkg ->
                if (runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess) {
                    runCatching { dpm.setApplicationHidden(admin, pkg, hidden) }
                }
            }
    }
}
