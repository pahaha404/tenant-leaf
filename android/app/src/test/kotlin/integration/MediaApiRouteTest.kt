package com.seipseip.app.integration

import android.Manifest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaApiRouteTest {
    @Test
    fun mediaPermissionsIncludePhotosAndVideosFromAndroid13() {
        assertArrayEquals(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            mediaRuntimePermissions(sdkInt = 32),
        )
        assertArrayEquals(
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO),
            mediaRuntimePermissions(sdkInt = 33),
        )
        assertArrayEquals(
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            ),
            mediaRuntimePermissions(sdkInt = 34),
        )
    }

    @Test
    fun android14SelectedMediaAccessIsUsable() {
        assertTrue(hasMediaAccess(sdkInt = 34) { it == Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED })
        assertFalse(hasMediaAccess(sdkInt = 34) { false })
    }
}
