package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.local.MediaEntity
import com.example.data.model.MediaType
import com.example.data.model.TmdbSearchResultItem
import com.example.data.model.WatchStatus
import com.example.data.remote.tmdb.TmdbClient
import com.example.data.remote.tmdb.TmdbKnownForDto
import com.example.data.remote.tmdb.TmdbPersonDetailDto
import com.example.ui.theme.CinemaCyan
import com.example.ui.theme.CinemaGold
import com.example.ui.theme.CinemaRed
import com.example.ui.theme.WatchPilotColors
import com.example.ui.util.LocalAdaptiveLayoutInfo

private enum class CreditFilter(val label: String) {
    ALL("All"),
    MOVIES("Movies"),
    TV_SHOWS("Series")
}

@Composable
fun PersonDetailScreen(
    person: TmdbSearchResultItem.Person,
    detailDto: TmdbPersonDetailDto?,
    isLoading: Boolean,
    isFavorite: Boolean,
    trackedMedia: List<MediaEntity>,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMediaClick: (MediaEntity) -> Unit,
    onToggleWatchlist: (MediaEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var isBioExpanded by remember(person.id, person.name) { mutableStateOf(false) }
    var selectedFilter by remember(person.id, person.name) { mutableStateOf(CreditFilter.ALL) }

    val displayName = detailDto?.name?.takeIf { it.isNotBlank() } ?: person.name
    val department = (detailDto?.knownForDepartment ?: person.department).ifBlank { "Acting" }
    val profileUrl = detailDto?.profilePath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: person.profileUrl

    val allCredits = remember(person.knownFor, detailDto) {
        val creditEntities = detailDto?.combinedCredits?.cast
            .orEmpty()
            .filter {
                (it.mediaType == "movie" || it.mediaType == "tv") &&
                    (!it.title.isNullOrBlank() || !it.name.isNullOrBlank())
            }
            .sortedWith(
                compareByDescending<TmdbKnownForDto> { !it.posterPath.isNullOrBlank() }
                    .thenByDescending { it.voteAverage ?: 0.0 }
            )
            .map { TmdbClient.knownForToEntity(it) }

        (person.knownFor + creditEntities)
            .distinctBy { it.id }
            .take(30)
    }

    val hasMovies = remember(allCredits) { allCredits.any { it.getMediaTypeEnum() == MediaType.MOVIE } }
    val hasTvShows = remember(allCredits) { allCredits.any { it.getMediaTypeEnum() == MediaType.TV_SHOW } }

    val filteredCredits = remember(allCredits, selectedFilter) {
        when (selectedFilter) {
            CreditFilter.ALL -> allCredits
            CreditFilter.MOVIES -> allCredits.filter { it.getMediaTypeEnum() == MediaType.MOVIE }
            CreditFilter.TV_SHOWS -> allCredits.filter { it.getMediaTypeEnum() == MediaType.TV_SHOW }
        }
    }

    val trackedMap = remember(trackedMedia) { trackedMedia.associateBy { it.id } }
    val adaptive = LocalAdaptiveLayoutInfo.current
    val gridColumns = adaptive.cardGridColumns
    val bio = detailDto?.biography?.takeIf { it.isNotBlank() }

    if (adaptive.isTabletOrWide) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("person_detail_screen"),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(span = { GridItemSpan(gridColumns) }) {
                PersonHeroBanner(
                    displayName = displayName,
                    department = department,
                    profileUrl = profileUrl,
                    detailDto = detailDto,
                    isFavorite = isFavorite,
                    isTablet = true,
                    onBack = onBack,
                    onToggleFavorite = onToggleFavorite
                )
            }

            if (isLoading) {
                item(span = { GridItemSpan(gridColumns) }) {
                    PersonLoadingRow()
                }
            }

            if (bio != null) {
                item(span = { GridItemSpan(gridColumns) }) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        PersonBiographySection(
                            bio = bio,
                            isBioExpanded = isBioExpanded,
                            onToggleExpand = { isBioExpanded = !isBioExpanded },
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 1040.dp)
                        )
                    }
                }
            }

            item(span = { GridItemSpan(gridColumns) }) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PersonFilmographyHeader(
                        totalCreditsCount = allCredits.size,
                        filteredCreditsCount = filteredCredits.size,
                        hasMovies = hasMovies,
                        hasTvShows = hasTvShows,
                        selectedFilter = selectedFilter,
                        onSelectFilter = { selectedFilter = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 1040.dp)
                    )
                }
            }

            if (filteredCredits.isNotEmpty()) {
                gridItems(filteredCredits, key = { it.id }) { media ->
                    val currentEntity = trackedMap[media.id] ?: media
                    val isInWatchlist = currentEntity.getWatchStatusEnum() != WatchStatus.NONE

                    PersonCreditCard(
                        media = currentEntity,
                        isInWatchlist = isInWatchlist,
                        onCardClick = { onMediaClick(currentEntity) },
                        onToggleWatchlist = { onToggleWatchlist(currentEntity) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (!isLoading) {
                item(span = { GridItemSpan(gridColumns) }) {
                    PersonEmptyFilmography()
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("person_detail_screen"),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            // Hero Portrait & Header Banner
            item {
                PersonHeroBanner(
                    displayName = displayName,
                    department = department,
                    profileUrl = profileUrl,
                    detailDto = detailDto,
                    isFavorite = isFavorite,
                    isTablet = false,
                    onBack = onBack,
                    onToggleFavorite = onToggleFavorite
                )
            }

            // Loading indicator
            if (isLoading) {
                item {
                    PersonLoadingRow()
                }
            }

            // Biography Section
            if (bio != null) {
                item {
                    PersonBiographySection(
                        bio = bio,
                        isBioExpanded = isBioExpanded,
                        onToggleExpand = { isBioExpanded = !isBioExpanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Filmography Header + Optional Filter Chips
            item {
                PersonFilmographyHeader(
                    totalCreditsCount = allCredits.size,
                    filteredCreditsCount = filteredCredits.size,
                    hasMovies = hasMovies,
                    hasTvShows = hasTvShows,
                    selectedFilter = selectedFilter,
                    onSelectFilter = { selectedFilter = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)
                )
            }

            // Filmography List
            if (filteredCredits.isNotEmpty()) {
                items(filteredCredits, key = { it.id }) { media ->
                    val currentEntity = trackedMap[media.id] ?: media
                    val isInWatchlist = currentEntity.getWatchStatusEnum() != WatchStatus.NONE

                    PersonCreditCard(
                        media = currentEntity,
                        isInWatchlist = isInWatchlist,
                        onCardClick = { onMediaClick(currentEntity) },
                        onToggleWatchlist = { onToggleWatchlist(currentEntity) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            } else if (!isLoading) {
                item {
                    PersonEmptyFilmography()
                }
            }
        }
    }
}

@Composable
private fun PersonHeroBanner(
    displayName: String,
    department: String,
    profileUrl: String?,
    detailDto: TmdbPersonDetailDto?,
    isFavorite: Boolean,
    isTablet: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(if (isTablet) RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp) else RoundedCornerShape(0.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
    ) {
                // Atmospheric ambient backdrop
                Image(
                    painter = painterResource(id = R.drawable.banner_cinema),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.28f),
                    contentScale = ContentScale.Crop
                )
                if (!profileUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(profileUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.18f),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.65f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isTablet) Modifier.widthIn(max = 1040.dp).align(Alignment.Center) else Modifier)
                        .statusBarsPadding()
                        .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = if (isTablet) 18.dp else 12.dp)
                ) {
                    // Top Bar with Back and Favorite Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                    .testTag("person_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                .testTag("person_fav_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                                tint = if (isFavorite) CinemaRed else Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Portrait + Actor Identity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isTablet) 128.dp else 108.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.dp, CinemaGold.copy(alpha = 0.85f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!profileUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(profileUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = displayName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = displayName,
                                    tint = CinemaGold,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CinemaGold.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, CinemaGold.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = department.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CinemaGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            if (!detailDto?.birthday.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = CinemaCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Born ${detailDto?.birthday}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (!detailDto?.placeOfBirth.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = CinemaCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = detailDto?.placeOfBirth.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
}

@Composable
private fun PersonLoadingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = CinemaGold,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Loading...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PersonBiographySection(
    bio: String,
    isBioExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.animateContentSize()
    ) {
        Text(
            text = "Biography",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = bio,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
            maxLines = if (isBioExpanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis
        )
        if (bio.length > 260) {
            TextButton(
                onClick = onToggleExpand,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                modifier = Modifier.testTag("btn_toggle_person_bio")
            ) {
                Text(
                    text = if (isBioExpanded) "Show less" else "Read full biography",
                    color = CinemaCyan,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun PersonFilmographyHeader(
    totalCreditsCount: Int,
    filteredCreditsCount: Int,
    hasMovies: Boolean,
    hasTvShows: Boolean,
    selectedFilter: CreditFilter,
    onSelectFilter: (CreditFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filmography",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            if (totalCreditsCount > 0) {
                Text(
                    text = "$filteredCreditsCount titles",
                    style = MaterialTheme.typography.labelMedium,
                    color = WatchPilotColors.current.goldAccent,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        if (hasMovies && hasTvShows) {
            Spacer(modifier = Modifier.height(8.dp))
            val chipColors = WatchPilotColors.current
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CreditFilter.entries.forEach { filter ->
                    val selected = selectedFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectFilter(filter) },
                        label = {
                            Text(
                                text = filter.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipColors.goldChipContainer,
                            selectedLabelColor = chipColors.goldAccent
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            selectedBorderColor = chipColors.goldChipBorder
                        ),
                        modifier = Modifier.testTag("person_filter_${filter.name.lowercase()}")
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonEmptyFilmography() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No credits available.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PersonCreditCard(
    media: MediaEntity,
    isInWatchlist: Boolean,
    onCardClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMovie = media.getMediaTypeEnum() == MediaType.MOVIE

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("person_filmography_${media.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 66.dp, height = 98.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (media.posterUrl.isNotBlank()) {
                    AsyncImage(
                        model = media.posterUrl,
                        contentDescription = media.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (isMovie) Icons.Default.Movie else Icons.Default.Tv,
                        contentDescription = media.title,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                val creditColors = WatchPilotColors.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isMovie) creditColors.goldChipContainer else creditColors.cyanChipContainer
                    ) {
                        Text(
                            text = if (isMovie) "MOVIE" else "SERIES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMovie) creditColors.goldAccent else creditColors.cyanAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = media.releaseYear.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = creditColors.goldAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = String.format("%.1f", media.communityRating),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = creditColors.goldAccent
                        )
                    }
                }

                if (media.overview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = media.overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onToggleWatchlist,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("person_credit_watchlist_${media.id}")
            ) {
                Icon(
                    imageVector = if (isInWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isInWatchlist) "Remove from Watchlist" else "Add to Watchlist",
                    tint = if (isInWatchlist) CinemaGold else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
