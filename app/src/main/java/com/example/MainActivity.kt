package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddCustomMediaDialog
import com.example.ui.screens.GoogleTvScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MediaDetailScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TrendingScreen
import com.example.ui.theme.CineTrackTheme
import com.example.ui.theme.CinemaGold
import com.example.ui.viewmodel.MediaViewModel

enum class MainNavDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DISCOVER("Discover", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_discover"),
    TRENDING("Trending", Icons.AutoMirrored.Filled.TrendingUp, Icons.AutoMirrored.Outlined.TrendingUp, "nav_trending"),
    SEARCH("Search", Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
    LIBRARY("Watchlist", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary, "nav_library"),
    STATS("Stats", Icons.Filled.Insights, Icons.Outlined.Insights, "nav_stats")
}

class MainActivity : ComponentActivity() {
    private val viewModel: MediaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CineTrackTheme {
                CineTrackApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CineTrackApp(viewModel: MediaViewModel) {
    var currentNav by remember { mutableStateOf(MainNavDestination.DISCOVER) }
    var showAddDialog by remember { mutableStateOf(false) }

    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val customCollections by viewModel.customCollections.collectAsStateWithLifecycle()
    val selectedMediaId by viewModel.selectedMediaId.collectAsStateWithLifecycle()
    val effectiveRegion by viewModel.effectiveRegionCode.collectAsStateWithLifecycle()
    val isGoogleTvMode by viewModel.isGoogleTvMode.collectAsStateWithLifecycle()

    val selectedMedia = remember(selectedMediaId, allMedia) {
        allMedia.find { it.id == selectedMediaId }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (selectedMedia == null && !isGoogleTvMode) {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav_bar"),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = MaterialTheme.colorScheme.surfaceVariant.let { 4.dp }
                ) {
                    MainNavDestination.entries.forEach { destination ->
                        val selected = currentNav == destination
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentNav = destination },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.title
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = CinemaGold.copy(alpha = 0.25f),
                                selectedIconColor = CinemaGold,
                                selectedTextColor = CinemaGold,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(destination.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedMedia,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screenTransition",
            modifier = Modifier.padding(innerPadding)
        ) { activeMedia ->
            if (activeMedia != null) {
                MediaDetailScreen(
                    media = activeMedia,
                    viewModel = viewModel,
                    availableCollections = customCollections,
                    effectiveRegionCode = effectiveRegion,
                    onBack = { viewModel.selectMedia(null) }
                )
            } else if (isGoogleTvMode) {
                GoogleTvScreen(
                    viewModel = viewModel,
                    onMediaClick = { id -> viewModel.selectMedia(id) },
                    onSwitchToClassic = { viewModel.setGoogleTvMode(false) },
                    onNavigateToSearch = {
                        currentNav = MainNavDestination.SEARCH
                        viewModel.setGoogleTvMode(false)
                    }
                )
            } else {
                if (currentNav != MainNavDestination.DISCOVER) {
                    BackHandler {
                        currentNav = MainNavDestination.DISCOVER
                    }
                }

                when (currentNav) {
                    MainNavDestination.DISCOVER -> HomeScreen(
                        viewModel = viewModel,
                        onMediaClick = { id -> viewModel.selectMedia(id) },
                        onOpenAddDialog = { showAddDialog = true },
                        onNavigateToSearch = { currentNav = MainNavDestination.SEARCH },
                        onNavigateToTrending = { currentNav = MainNavDestination.TRENDING }
                    )

                    MainNavDestination.TRENDING -> TrendingScreen(
                        viewModel = viewModel,
                        onMediaClick = { id -> viewModel.selectMedia(id) }
                    )

                    MainNavDestination.SEARCH -> SearchScreen(
                        viewModel = viewModel,
                        onMediaClick = { id -> viewModel.selectMedia(id) }
                    )

                    MainNavDestination.LIBRARY -> LibraryScreen(
                        viewModel = viewModel,
                        onMediaClick = { id -> viewModel.selectMedia(id) },
                        onExploreClick = { currentNav = MainNavDestination.DISCOVER }
                    )

                    MainNavDestination.STATS -> StatsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomMediaDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, mediaType, releaseYear, genres, runtimeMinutes, totalSeasons, totalEpisodes, overview, director, cast, status, initialRating, posterUrl, language, originCountry ->
                viewModel.addCustomMedia(
                    title = title,
                    mediaType = mediaType,
                    releaseYear = releaseYear,
                    genres = genres,
                    runtimeMinutes = runtimeMinutes,
                    totalSeasons = totalSeasons,
                    totalEpisodes = totalEpisodes,
                    overview = overview,
                    director = director,
                    cast = cast,
                    status = status,
                    initialRating = initialRating,
                    posterUrl = posterUrl,
                    language = language,
                    originCountry = originCountry
                )
                showAddDialog = false
            }
        )
    }
}
