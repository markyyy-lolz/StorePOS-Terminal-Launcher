package com.storepos.launcher

object VersionUtils {
    const val MIN_STOREPOS_VERSION_CODE = 14L
    const val MIN_STOREPOS_VERSION_NAME = "1.4.0"
    fun isOutdated(versionCode: Long): Boolean =
        versionCode > 0L && versionCode < MIN_STOREPOS_VERSION_CODE
}
