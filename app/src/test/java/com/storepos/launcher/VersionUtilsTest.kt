package com.storepos.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionUtilsTest {
    @Test fun olderBuildIsOutdated() = assertTrue(VersionUtils.isOutdated(13))
    @Test fun baselineBuildIsSupported() = assertFalse(VersionUtils.isOutdated(14))
    @Test fun newerBuildIsSupported() = assertFalse(VersionUtils.isOutdated(99))
}
