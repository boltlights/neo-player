package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.TrackEntity
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.example.ui.components.ActiveBottomBar
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      ActiveBottomBar(
        track = TrackEntity(
          title = "Celestial Dreamer",
          artist = "Cosmo Ambient Project",
          album = "Unfolding Starfields",
          durationMs = 120000,
          filePath = "",
          artPath = null
        ),
        isPlaying = true,
        progressVal = 0.45f,
        accentColor = Color(0xFF2F80ED),
        surfaceColor = Color(0xFF1C1C1E),
        onBgColor = Color(0xFFF2F2F7),
        onSkipPrevious = {},
        onPlayPauseToggle = {},
        onSkipNext = {},
        onOpenPlayer = {}
      )
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
