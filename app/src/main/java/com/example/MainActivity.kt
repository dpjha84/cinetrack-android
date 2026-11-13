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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MediaDetailScreen
import com.example.ui.screens.PersonDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SeasonDetailScreen
import com.example.ui.screens.TrailersScreen
import com.example.ui.theme.MovieverseTheme
import com.example.ui.theme.CinemaGold
import com.example.ui.viewmodel.MediaViewModel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.PlayCircle

enum class MainNavDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    TRAILERS("Trailers", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle, "nav_trailers"),
    SEARCH("Search", Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
    LIBRARY("Library", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary, "nav_library"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
}

class MainActivity : ComponentActivity() {
    private val viewModel: MediaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MovieverseTheme {
                MovieverseApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MovieverseApp(viewModel: MediaViewModel) {
    var currentNav by remember { mutableStateOf(MainNavDestination.HOME) }

    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val customCollections by viewModel.customCollections.collectAsStateWithLifecycle()
    val selectedMediaId by viewModel.selectedMediaId.collectAsStateWithLifecycle()
    val effectiveRegion by viewModel.effectiveRegionCode.collectAsStateWithLifecycle()
    val selectedSeasonForDetail by viewModel.selectedSeasonForDetail.collectAsStateWithLifecycle()

    val trendingItems by viewModel.trendingItems.collectAsStateWithLifecycle()
    val currentTopRatedList by viewModel.currentTopRatedList.collectAsStateWithLifecycle()
    val favoritePeople by viewModel.favoritePeople.collectAsStateWithLifecycle()
    val selectedPerson by viewModel.selectedPersonForDetail.collectAsStateWithLifecycle()
    val personDetail by viewModel.personDetailData.collectAsStateWithLifecycle()
    val isLoadingPersonDetail by viewModel.isLoadingPersonDetail.collectAsStateWithLifecycle()

    val selectedMedia = remember(selectedMediaId, allMedia, trendingItems, currentTopRatedList) {
        allMedia.find { it.id == selectedMediaId }
            ?: trendingItems.find { it.id == selectedMediaId }
            ?: currentTopRatedList.find { it.id == selectedMediaId }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (selectedMedia == null && selectedPerson == null) {
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
                            alwaysShowLabel = true,
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.title
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
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
            targetState = Triple(selectedPerson, selectedMedia, selectedSeasonForDetail),
            contentKey = { (person, media, season) ->
                Triple(person != null, media?.id, season?.seasonNumber)
            },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screenTransition",
            modifier = Modifier.padding(innerPadding)
        ) { (activePerson, activeMedia, activeSeason) ->
            if (activePerson != null) {
                val resolvedId = personDetail?.id?.takeIf { it > 0L } ?: activePerson.id
                val resolvedName = personDetail?.name?.takeIf { it.isNotBlank() } ?: activePerson.name
                val isPersonFavorite = remember(resolvedId, resolvedName, favoritePeople) {
                    favoritePeople.any { fav ->
                        (resolvedId > 0L && fav.personId == resolvedId) ||
                            fav.name.equals(resolvedName, ignoreCase = true)
                    }
                }
                PersonDetailScreen(
                    person = activePerson,
                    detailDto = personDetail,
                    isLoading = isLoadingPersonDetail,
                    isFavorite = isPersonFavorite,
                    trackedMedia = allMedia,
                    onBack = { viewModel.selectPersonForDetail(null) },
                    onToggleFavorite = { viewModel.toggleFavoritePerson(activePerson, personDetail) },
                    onMediaClick = { media ->
                        viewModel.selectPersonForDetail(null)
                        viewModel.openMediaFromSearch(media)
                    },
                    onToggleWatchlist = { media ->
                        viewModel.toggleWatchlistQuick(media)
                    }
                )
            } else if (activeMedia != null) {
                if (activeSeason != null) {
                    SeasonDetailScreen(
                        media = activeMedia,
                        season = activeSeason,
                        viewModel = viewModel,
                        effectiveRegionCode = effectiveRegion,
                        onBack = { viewModel.closeSeasonDetail() }
                    )
                } else {
                    MediaDetailScreen(
                        media = activeMedia,
                        viewModel = viewModel,
                        availableCollections = customCollections,
                        effectiveRegionCode = effectiveRegion,
                        onBack = { viewModel.selectMedia(null) }
                    )
                }
            } else {
                if (currentNav != MainNavDestination.HOME) {
                    BackHandler {
                        currentNav = MainNavDestination.HOME
                    }
                }

                when (currentNav) {
                    MainNavDestination.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onMediaClick = { id -> viewModel.selectMedia(id) },
                        onNavigateToSearch = { currentNav = MainNavDestination.SEARCH }
                    )

                    MainNavDestination.TRAILERS -> TrailersScreen(
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
                        onExploreClick = { currentNav = MainNavDestination.HOME }
                    )

                    MainNavDestination.PROFILE -> ProfileScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
