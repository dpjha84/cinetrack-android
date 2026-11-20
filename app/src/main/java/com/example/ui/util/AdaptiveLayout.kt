package com.example.ui.util

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AdaptiveWidthClass {
    COMPACT,   // < 600.dp (Phones portrait)
    MEDIUM,    // 600.dp .. 839.dp (Foldables unfolded / 7-8" tablets / landscape phones)
    EXPANDED   // >= 840.dp (Tablets landscape & large tablets portrait)
}

@Immutable
data class AdaptiveLayoutInfo(
    val widthClass: AdaptiveWidthClass = AdaptiveWidthClass.COMPACT,
    val availableWidth: Dp = 360.dp,
    val availableHeight: Dp = 800.dp
) {
    val isCompact: Boolean get() = widthClass == AdaptiveWidthClass.COMPACT
    val isTabletOrWide: Boolean get() = widthClass != AdaptiveWidthClass.COMPACT
    val isExpanded: Boolean get() = widthClass == AdaptiveWidthClass.EXPANDED

    /**
     * Number of poster columns for browse grids (HomeScreen catalog, etc.).
     * Keeps Compact phones strictly at 2 columns.
     */
    val posterGridColumns: Int
        get() = when {
            widthClass == AdaptiveWidthClass.COMPACT -> 2
            widthClass == AdaptiveWidthClass.MEDIUM -> 3
            availableWidth >= 1120.dp -> 5
            else -> 4
        }

    /**
     * Number of wide card columns (Trailers, Library, Search results, Filmography, Episodes).
     * Keeps Compact phones strictly at 1 column.
     */
    val cardGridColumns: Int
        get() = when {
            widthClass == AdaptiveWidthClass.COMPACT -> 1
            widthClass == AdaptiveWidthClass.EXPANDED && availableWidth >= 1180.dp -> 3
            else -> 2
        }

    /** Whether two-pane detail/welcome splits should activate (>= 840dp). */
    val useTwoPaneLayout: Boolean
        get() = widthClass == AdaptiveWidthClass.EXPANDED && availableWidth >= 840.dp
}

val LocalAdaptiveLayoutInfo = compositionLocalOf { AdaptiveLayoutInfo() }

@Composable
fun AdaptiveLayoutProvider(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val widthClass = when {
            w < 600.dp -> AdaptiveWidthClass.COMPACT
            w < 840.dp -> AdaptiveWidthClass.MEDIUM
            else -> AdaptiveWidthClass.EXPANDED
        }
        val info = remember(widthClass, w, h) {
            AdaptiveLayoutInfo(
                widthClass = widthClass,
                availableWidth = w,
                availableHeight = h
            )
        }
        CompositionLocalProvider(LocalAdaptiveLayoutInfo provides info) {
            content()
        }
    }
}
