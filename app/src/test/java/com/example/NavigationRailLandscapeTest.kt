package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.ui.theme.WatchPilotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NavigationRailLandscapeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun profileTabRemainsVisibleAndClickableInCompactLandscapeHeight() {
        var currentNav by mutableStateOf(MainNavDestination.LIBRARY)

        composeTestRule.setContent {
            WatchPilotTheme {
                Box(modifier = Modifier.size(width = 890.dp, height = 380.dp)) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        AdaptiveTabletNavigationRail(
                            currentNav = currentNav,
                            availableHeight = 380.dp,
                            onSelectDestination = { currentNav = it }
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("nav_home").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_library").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_profile").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_profile").performClick()
        assertEquals(MainNavDestination.PROFILE, currentNav)
    }

    @Test
    fun profileTabRemainsVisibleAndClickableInStandardTabletLandscapeHeight() {
        var currentNav by mutableStateOf(MainNavDestination.HOME)

        composeTestRule.setContent {
            WatchPilotTheme {
                Box(modifier = Modifier.size(width = 1024.dp, height = 500.dp)) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        AdaptiveTabletNavigationRail(
                            currentNav = currentNav,
                            availableHeight = 500.dp,
                            onSelectDestination = { currentNav = it }
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("nav_profile").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_profile").performClick()
        assertEquals(MainNavDestination.PROFILE, currentNav)
    }
}
