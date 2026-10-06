package com.storepos.launcher

import android.content.Context
import android.os.Build

data class StorePosStatus(
    val installed: Boolean,
    val versionName: String?,
    val versionCode: Long,
    val outdated: Boolean
)

object StorePosInspector {
    fun inspect(context: Context): StorePosStatus {
        return try {
            val info = context.packageManager.getPackageInfo(KioskController.STOREPOS_PACKAGE, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
            else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            StorePosStatus(true, info.versionName, code, VersionUtils.isOutdated(code))
        } catch (_: Exception) {
            StorePosStatus(false, null, 0L, false)
        }
    }
}
