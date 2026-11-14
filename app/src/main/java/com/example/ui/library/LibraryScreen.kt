package com.example.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CineverseRepository
import com.example.data.MovieFilterCriteria
import com.example.data.MovieItem
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassPillButton
import com.example.ui.components.MoviePosterImage
import com.example.ui.components.RewatchSymbolCounterButton
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FrostWhite
import com.example.ui.theme.IcyBlue
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.SteelBlue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class LibraryViewMode(val label: String, val icon: ImageVector) {
    DETAIL("Detail View", Icons.Default.ViewAgenda),
    LIST("List View", Icons.AutoMirrored.Filled.ViewList),
    LARGE_ICONS("Large Icons", Icons.Default.GridView),
    MEDIUM_ICONS("Medium Icons", Icons.Default.ViewModule),
    SMALL_ICONS("Small Icons", Icons.Default.Apps)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    userLibrary: List<MovieItem>,
    onMovieClick: (MovieItem) -> Unit,
    onExploreClick: () -> Unit,
    onUpdateStatus: (String, String) -> Unit = { _, _ -> },
    onRemoveMovie: (String) -> Unit = {},
    onLogRewatch: (MovieItem, Int, String, String) -> Unit = { _, _, _, _ -> },
    onUpdateRating: (String, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val repository = remember { CineverseRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    var filterCriteria by remember { mutableStateOf(MovieFilterCriteria(libraryStatus = "Watchlist")) }
    var searchQuery by remember { mutableStateOf("") }
    var showAdvancedFilterPanel by remember { mutableStateOf(false) }

    // Windows-Style Multiple View Modes
    var currentViewMode by remember { mutableStateOf(LibraryViewMode.DETAIL) }
    var showViewModeBar by remember { mutableStateOf(false) }

    // Single & Multi-Select Deletion with Confirmation
    var selectedForDeletionIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showBatchDeleteConfirmDialog by remember { mutableStateOf(false) }
    var movieToRemove by remember { mutableStateOf<MovieItem?>(null) }
    var rewatchTargetMovie by remember { mutableStateOf<MovieItem?>(null) }

    val filteredMovies = remember(filterCriteria, searchQuery, userLibrary) {
        repository.filterAndSortMovies(
            movies = userLibrary,
            criteria = filterCriteria,
            searchQuery = searchQuery
        )
    }

    val watchlistCount = remember(userLibrary) { userLibrary.count { it.userStatus.equals("Watchlist", true) || it.userStatus.isBlank() } }
    val watchedCount = remember(userLibrary) { userLibrary.count { it.userStatus.equals("Watched", true) } }
    val droppedCount = remember(userLibrary) { userLibrary.count { it.userStatus.equals("Dropped", true) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Title Header + Windows View Mode Button Beside Filter Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                Text(
                    text = "Your Library",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    color = FrostWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${filteredMovies.size} of ${userLibrary.size} titles matching criteria • ${currentViewMode.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SteelBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("library_result_count_label")
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View Mode Button right beside Filter button
                GlassIconButton(
                    icon = currentViewMode.icon,
                    contentDescription = "Change View Mode (${currentViewMode.label})",
                    size = 44.dp,
                    tint = if (showViewModeBar || currentViewMode != LibraryViewMode.DETAIL) ElectricCyan else FrostWhite,
                    onClick = { showViewModeBar = !showViewModeBar },
                    modifier = Modifier.testTag("toggle_library_view_mode_btn")
                )

                GlassIconButton(
                    icon = Icons.Default.Tune,
                    contentDescription = "Filters & Sort",
                    size = 44.dp,
                    tint = if (filterCriteria.hasActiveFilters() || showAdvancedFilterPanel) ElectricCyan else FrostWhite,
                    onClick = { showAdvancedFilterPanel = !showAdvancedFilterPanel },
                    modifier = Modifier.testTag("toggle_library_filters_btn")
                )
            }
        }

        // Windows-Style View Mode Switcher Bar
        AnimatedVisibility(
            visible = showViewModeBar,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .horizontalScroll(rememberScrollState())
                    .testTag("library_view_mode_bar"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LibraryViewMode.entries.forEach { mode ->
                    GlassPillButton(
                        text = mode.label,
                        icon = mode.icon,
                        compact = true,
                        isActive = currentViewMode == mode,
                        onClick = { currentViewMode = mode },
                        modifier = Modifier.testTag("library_view_mode_${mode.name.lowercase()}")
                    )
                }
            }
        }

        // Batch Multi-Select Deletion Action Bar
        AnimatedVisibility(
            visible = selectedForDeletionIds.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("library_batch_delete_bar"),
                cornerRadius = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedForDeletionIds.size} selected",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = FrostWhite
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                selectedForDeletionIds = filteredMovies.map { it.id }.toSet()
                            }
                        ) {
                            Text("Select All", color = IcyBlue, fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = { selectedForDeletionIds = emptySet() }
                        ) {
                            Text("Clear", color = SteelBlue, fontSize = 12.sp)
                        }
                        GlassPillButton(
                            text = "Delete (${selectedForDeletionIds.size})",
                            icon = Icons.Default.DeleteOutline,
                            compact = true,
                            isActive = true,
                            onClick = { showBatchDeleteConfirmDialog = true },
                            modifier = Modifier.testTag("lib_delete_selected_btn")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // In-Library Filter Input
        val libOpacity = (1f - com.example.ui.theme.LocalGlassTransparency.current).coerceIn(0f, 1f)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter titles, directors, cast or year...", color = SteelBlue) },
            leadingIcon = {
                Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = ElectricCyan)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = SteelBlue)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("library_filter_text_input"),
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MidnightNavy.copy(alpha = (0.75f * libOpacity).coerceIn(0f, 1f)),
                unfocusedContainerColor = MidnightNavy.copy(alpha = (0.55f * libOpacity).coerceIn(0f, 1f)),
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = SteelBlue.copy(alpha = 0.4f),
                focusedTextColor = FrostWhite,
                unfocusedTextColor = FrostWhite
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Status Tabs: Watchlist, Watched, Dropped, All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val statusItems = listOf(
                Triple("Watchlist", Icons.Default.Bookmark, watchlistCount),
                Triple("Watched", Icons.Default.CheckCircle, watchedCount),
                Triple("Dropped", Icons.Default.DeleteOutline, droppedCount),
                Triple("All", Icons.Default.VideoLibrary, userLibrary.size)
            )

            statusItems.forEach { (status, icon, count) ->
                val isSelected = filterCriteria.libraryStatus.equals(status, ignoreCase = true)
                GlassPillButton(
                    text = "$status ($count)",
                    icon = icon,
                    isActive = isSelected,
                    onClick = {
                        filterCriteria = filterCriteria.copy(libraryStatus = status)
                    },
                    modifier = Modifier.testTag("library_status_tab_${status.lowercase()}")
                )
            }
        }

        // COMPREHENSIVE 9-DIMENSION MOVIE FILTERS & SORT PANEL
        AnimatedVisibility(
            visible = showAdvancedFilterPanel,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            MovieFiltersPanel(
                criteria = filterCriteria,
                resultCount = filteredMovies.size,
                onCriteriaChange = { filterCriteria = it },
                onClearFilters = {
                    filterCriteria = MovieFilterCriteria(libraryStatus = filterCriteria.libraryStatus)
                    searchQuery = ""
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    try {
                        repository.refreshFromBackend()
                    } finally {
                        isRefreshing = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (filteredMovies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 48.dp)
                            .testTag("library_empty_result_state")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Empty",
                            tint = SteelBlue,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || filterCriteria.hasActiveFilters()) {
                                "No titles match your active filters"
                            } else {
                                "No titles in ${filterCriteria.libraryStatus}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = FrostWhite,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || filterCriteria.hasActiveFilters()) {
                                "Try clearing one or more filter dimensions to broaden your results."
                            } else {
                                "Use the Search tab in the bottom dock to discover and add movies or series, or pull down to sync."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = SteelBlue,
                            textAlign = TextAlign.Center
                        )
                        if (searchQuery.isNotBlank() || filterCriteria.hasActiveFilters()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            GlassPillButton(
                                text = "Clear Filters",
                                icon = Icons.Default.ClearAll,
                                isActive = true,
                                onClick = {
                                    filterCriteria = MovieFilterCriteria(libraryStatus = "All")
                                    searchQuery = ""
                                },
                                modifier = Modifier.testTag("empty_state_clear_filters_btn")
                            )
                        }
                    }
                }
            } else {
                when (currentViewMode) {
                    LibraryViewMode.DETAIL, LibraryViewMode.LIST -> {
                        val isCompactList = currentViewMode == LibraryViewMode.LIST
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 110.dp),
                            verticalArrangement = Arrangement.spacedBy(if (isCompactList) 8.dp else 14.dp)
                        ) {
                            items(filteredMovies, key = { it.id }) { movie ->
                                val isChecked = movie.id in selectedForDeletionIds
                                LibraryItemCard(
                                    movie = movie,
                                    isCompactList = isCompactList,
                                    isSelectedForDeletion = isChecked,
                                    onToggleSelectForDeletion = {
                                        selectedForDeletionIds = if (isChecked) {
                                            selectedForDeletionIds - movie.id
                                        } else {
                                            selectedForDeletionIds + movie.id
                                        }
                                    },
                                    onClick = { onMovieClick(movie) },
                                    onMoveWatched = { onUpdateStatus(movie.id, "Watched") },
                                    onMoveWatchlist = { onUpdateStatus(movie.id, "Watchlist") },
                                    onMoveDropped = { onUpdateStatus(movie.id, "Dropped") },
                                    onRewatch = { rewatchTargetMovie = movie },
                                    onSliderRatingChange = { newRating ->
                                        onUpdateRating(movie.id, newRating)
                                    },
                                    onRemove = { movieToRemove = movie }
                                )
                            }
                        }
                    }
                    LibraryViewMode.LARGE_ICONS, LibraryViewMode.MEDIUM_ICONS, LibraryViewMode.SMALL_ICONS -> {
                        val columns = when (currentViewMode) {
                            LibraryViewMode.LARGE_ICONS -> 2
                            LibraryViewMode.MEDIUM_ICONS -> 3
                            LibraryViewMode.SMALL_ICONS -> 4
                            else -> 2
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("library_icon_grid_$columns"),
                            contentPadding = PaddingValues(bottom = 110.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredMovies, key = { it.id }) { movie ->
                                val isChecked = movie.id in selectedForDeletionIds
                                LibraryIconGridCard(
                                    movie = movie,
                                    columns = columns,
                                    isSelectedForDeletion = isChecked,
                                    onToggleSelectForDeletion = {
                                        selectedForDeletionIds = if (isChecked) {
                                            selectedForDeletionIds - movie.id
                                        } else {
                                            selectedForDeletionIds + movie.id
                                        }
                                    },
                                    onClick = { onMovieClick(movie) },
                                    onMoveWatched = { onUpdateStatus(movie.id, "Watched") },
                                    onMoveWatchlist = { onUpdateStatus(movie.id, "Watchlist") },
                                    onRemove = { movieToRemove = movie }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Single Movie Delete Confirmation Dialog
    if (movieToRemove != null) {
        val target = movieToRemove!!
        AlertDialog(
            onDismissRequest = { movieToRemove = null },
            containerColor = MidnightNavy,
            title = { Text("Remove from Library?", color = FrostWhite) },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${target.title}\" from your personal library?",
                    color = SteelBlue,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveMovie(target.id)
                        selectedForDeletionIds = selectedForDeletionIds - target.id
                        movieToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    modifier = Modifier.testTag("confirm_remove_movie_btn")
                ) {
                    Text("Remove", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { movieToRemove = null }) {
                    Text("Cancel", color = FrostWhite)
                }
            }
        )
    }

    // Batch Multi-Select Delete Confirmation Dialog
    if (showBatchDeleteConfirmDialog && selectedForDeletionIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirmDialog = false },
            containerColor = MidnightNavy,
            title = { Text("Remove ${selectedForDeletionIds.size} Selected Title(s)?", color = FrostWhite) },
            text = {
                Text(
                    text = "Are you sure you want to remove the ${selectedForDeletionIds.size} selected movie(s) from your library?",
                    color = SteelBlue,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.removeMultipleFromLibrary(selectedForDeletionIds)
                        selectedForDeletionIds = emptySet()
                        showBatchDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    modifier = Modifier.testTag("confirm_batch_remove_btn")
                ) {
                    Text("Delete Selected", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirmDialog = false }) {
                    Text("Cancel", color = FrostWhite)
                }
            }
        )
    }

    if (rewatchTargetMovie != null) {
        val target = rewatchTargetMovie!!
        var sessionSliderValue by remember(target.id) {
            mutableFloatStateOf((if (target.userRating in 1..10) target.userRating else 8).toFloat())
        }
        val sessionRating = sessionSliderValue.roundToInt().coerceIn(1, 10)
        var sessionNotes by remember { mutableStateOf("") }
        val dateToday = remember { SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date()) }

        AlertDialog(
            onDismissRequest = { rewatchTargetMovie = null },
            containerColor = MidnightNavy,
            title = {
                Text("Log Rewatch #${target.rewatchCount.coerceAtLeast(1) + 1}", color = FrostWhite)
            },
            text = {
                Column {
                    Text(
                        text = "Recording a new viewing session for ${target.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SteelBlue
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Session Rating:", color = FrostWhite, style = MaterialTheme.typography.labelMedium)
                        Text("$sessionRating / 10", color = ElectricCyan, style = MaterialTheme.typography.titleMedium)
                    }
                    Slider(
                        value = sessionSliderValue,
                        onValueChange = { sessionSliderValue = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = SteelBlue.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Viewing Notes (Optional):", color = FrostWhite, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = sessionNotes,
                        onValueChange = { sessionNotes = it },
                        placeholder = { Text("Did you notice something new this viewing?") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = SteelBlue,
                            focusedTextColor = FrostWhite,
                            unfocusedTextColor = FrostWhite
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLogRewatch(target, sessionRating, sessionNotes, dateToday)
                        rewatchTargetMovie = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("Record Rewatch", color = DeepBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { rewatchTargetMovie = null }) {
                    Text("Cancel", color = FrostWhite)
                }
            }
        )
    }
}

/**
 * Windows-Style Icon Grid Card (Large Icons = 2 cols, Medium Icons = 3 cols, Small Icons = 4 cols)
 */
@Composable
fun LibraryIconGridCard(
    movie: MovieItem,
    columns: Int,
    isSelectedForDeletion: Boolean,
    onToggleSelectForDeletion: () -> Unit,
    onClick: () -> Unit,
    onMoveWatched: () -> Unit,
    onMoveWatchlist: () -> Unit,
    onRemove: () -> Unit
) {
    val isWatched = movie.userStatus.equals("Watched", ignoreCase = true)
    val isInWatchlist = movie.userStatus.equals("Watchlist", ignoreCase = true) || movie.userStatus.isBlank()
    val btnOpacity = (1f - com.example.ui.theme.LocalGlassTransparency.current).coerceIn(0f, 1f)
    val titleFontSize = when (columns) {
        2 -> 13.5.sp
        3 -> 11.5.sp
        else -> 10.sp
    }
    val actionHeight = when (columns) {
        2 -> 36.dp
        3 -> 30.dp
        else -> 26.dp
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("library_item_card_${movie.id}"),
        cornerRadius = if (columns >= 4) 12.dp else 16.dp,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
            ) {
                MoviePosterImage(
                    posterUrl = movie.posterUrl,
                    title = movie.title,
                    signatureColorHex = movie.signatureColorHex,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    releaseYear = movie.releaseYear,
                    movieId = movie.id
                )

                // Top-left selection checkbox + Top-right small dustbin icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DeepBlack.copy(alpha = 0.65f))
                            .clickable { onToggleSelectForDeletion() }
                            .testTag("lib_select_checkbox_${movie.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelectedForDeletion) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Select ${movie.title}",
                            tint = if (isSelectedForDeletion) ElectricCyan else FrostWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DeepBlack.copy(alpha = 0.65f))
                            .clickable { onRemove() }
                            .testTag("lib_dustbin_btn_${movie.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete ${movie.title}",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (columns >= 4) 6.dp else 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FrostWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(actionHeight)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isInWatchlist) ElectricCyan.copy(alpha = (0.30f * btnOpacity).coerceIn(0f, 1f))
                                else Color(0xFF15253A).copy(alpha = (0.90f * btnOpacity).coerceIn(0f, 1f))
                            )
                            .clickable { onMoveWatchlist() }
                            .testTag("lib_watchlist_btn_${movie.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Watchlist",
                            tint = if (isInWatchlist) ElectricCyan else FrostWhite,
                            modifier = Modifier.size(if (columns >= 4) 13.dp else 16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(actionHeight)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isWatched) Color(0xFF00F5D4).copy(alpha = (0.30f * btnOpacity).coerceIn(0f, 1f))
                                else Color(0xFF15253A).copy(alpha = (0.90f * btnOpacity).coerceIn(0f, 1f))
                            )
                            .clickable { onMoveWatched() }
                            .testTag("lib_watched_btn_${movie.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isWatched) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = "Watched",
                            tint = if (isWatched) Color(0xFF00F5D4) else FrostWhite,
                            modifier = Modifier.size(if (columns >= 4) 13.dp else 16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * MULTI-DIMENSION MOVIE FILTERS & SORT PANEL
 */
@Composable
fun MovieFiltersPanel(
    criteria: MovieFilterCriteria,
    resultCount: Int,
    onCriteriaChange: (MovieFilterCriteria) -> Unit,
    onClearFilters: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .testTag("movie_filters_panel"),
        cornerRadius = 22.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MULTI-DIMENSION FILTERS ($resultCount matches)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ElectricCyan
                )
                TextButton(
                    onClick = onClearFilters,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.testTag("clear_all_filters_btn")
                ) {
                    Text("Clear Filters", color = IcyBlue, style = MaterialTheme.typography.labelSmall)
                }
            }

            // 1. Content Type
            FilterChipRow(
                label = "Content Type",
                options = listOf("All", "Movie", "Web Series", "TV Show", "Documentary", "Anime"),
                selected = criteria.contentType,
                onSelect = { onCriteriaChange(criteria.copy(contentType = it)) }
            )

            // 2. Genre
            FilterChipRow(
                label = "Genre",
                options = listOf("All", "Sci-Fi", "Drama", "Thriller", "Action", "Mystery", "Comedy", "Adventure", "Crime"),
                selected = criteria.genre,
                onSelect = { onCriteriaChange(criteria.copy(genre = it)) }
            )

            // 3. Language
            FilterChipRow(
                label = "Language",
                options = listOf("All", "English", "Japanese", "Korean", "German", "French", "Hindi", "Spanish"),
                selected = criteria.language,
                onSelect = { onCriteriaChange(criteria.copy(language = it)) }
            )

            // 4. Country
            FilterChipRow(
                label = "Country",
                options = listOf("All", "USA", "UK", "Japan", "South Korea", "Germany", "France", "India", "Australia"),
                selected = criteria.country,
                onSelect = { onCriteriaChange(criteria.copy(country = it)) }
            )

            // 5. Release Year Range
            val yearLabel = when {
                criteria.minYear == 2020 -> "2020s"
                criteria.minYear == 2010 && criteria.maxYear == 2019 -> "2010s"
                criteria.minYear == 2000 && criteria.maxYear == 2009 -> "2000s"
                criteria.maxYear == 1999 -> "Pre-2000"
                else -> "All Years"
            }
            FilterChipRow(
                label = "Release Year",
                options = listOf("All Years", "2020s", "2010s", "2000s", "Pre-2000"),
                selected = yearLabel,
                onSelect = { sel ->
                    val (minY, maxY) = when (sel) {
                        "2020s" -> 2020 to 2026
                        "2010s" -> 2010 to 2019
                        "2000s" -> 2000 to 2009
                        "Pre-2000" -> 1950 to 1999
                        else -> 1950 to 2026
                    }
                    onCriteriaChange(criteria.copy(minYear = minY, maxYear = maxY))
                }
            )

            // 6. Runtime
            val runtimeLabel = when (criteria.maxRuntimeMinutes) {
                100 -> "Under 100m"
                150 -> "Under 150m"
                else -> "Any Runtime"
            }
            FilterChipRow(
                label = "Runtime",
                options = listOf("Any Runtime", "Under 100m", "Under 150m"),
                selected = runtimeLabel,
                onSelect = { sel ->
                    val maxR = when (sel) {
                        "Under 100m" -> 100
                        "Under 150m" -> 150
                        else -> 300
                    }
                    onCriteriaChange(criteria.copy(maxRuntimeMinutes = maxR))
                }
            )

            // 7. My Rating
            val ratingLabel = when {
                criteria.minPersonalRating >= 9 -> "My Rating 9+"
                criteria.minPersonalRating >= 8 -> "My Rating 8+"
                criteria.minPersonalRating >= 7 -> "My Rating 7+"
                else -> "Any Rating"
            }
            FilterChipRow(
                label = "My Rating",
                options = listOf("Any Rating", "My Rating 7+", "My Rating 8+", "My Rating 9+"),
                selected = ratingLabel,
                onSelect = { sel ->
                    when (sel) {
                        "My Rating 7+" -> onCriteriaChange(criteria.copy(minPersonalRating = 7, minPublicRating = 0.0))
                        "My Rating 8+" -> onCriteriaChange(criteria.copy(minPersonalRating = 8, minPublicRating = 0.0))
                        "My Rating 9+" -> onCriteriaChange(criteria.copy(minPersonalRating = 9, minPublicRating = 0.0))
                        else -> onCriteriaChange(criteria.copy(minPersonalRating = 0, minPublicRating = 0.0))
                    }
                }
            )

            // 8. Sort Option
            val sortDisplay = if (criteria.sortBy == "Personal Rating") "My Rating" else criteria.sortBy
            FilterChipRow(
                label = "Sort By",
                options = listOf("Title", "Year", "My Rating", "Runtime"),
                selected = sortDisplay,
                onSelect = { sel ->
                    val mappedSort = if (sel == "My Rating") "Personal Rating" else sel
                    onCriteriaChange(criteria.copy(sortBy = mappedSort))
                }
            )
        }
    }
}

@Composable
private fun FilterChipRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val chipOpacity = (1f - com.example.ui.theme.LocalGlassTransparency.current).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SteelBlue
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { opt ->
                val isSelected = selected.equals(opt, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) ElectricCyan.copy(alpha = (0.34f * chipOpacity).coerceIn(0f, 1f))
                            else Color(0xFF132235).copy(alpha = (0.92f * chipOpacity).coerceIn(0f, 1f))
                        )
                        .border(
                            1.dp,
                            if (isSelected) ElectricCyan else IcyBlue.copy(alpha = 0.28f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelect(opt) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (isSelected) FrostWhite else IcyBlue,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Library Movie Card supporting Detail View & Compact List View, plus:
 * - Small Dustbin Icon & Selection Checkbox for fast single or multi-select removal with confirmation.
 * - Three buttons directly below the movie name: Watchlist, Watched, and Rewatch.
 * - Inline 1-to-10 Rating Slider that ONLY appears when Watched is clicked (`isWatched == true`).
 */
@Composable
fun LibraryItemCard(
    movie: MovieItem,
    isCompactList: Boolean = false,
    isSelectedForDeletion: Boolean = false,
    onToggleSelectForDeletion: () -> Unit = {},
    onClick: () -> Unit,
    onMoveWatched: () -> Unit,
    onMoveWatchlist: () -> Unit,
    onMoveDropped: () -> Unit,
    onRewatch: () -> Unit,
    onSliderRatingChange: (Int) -> Unit = {},
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var sliderVal by remember(movie.id, movie.userRating) {
        mutableFloatStateOf((if (movie.userRating in 1..10) movie.userRating else 8).toFloat())
    }

    val isWatched = movie.userStatus.equals("Watched", ignoreCase = true)
    val isInWatchlist = movie.userStatus.equals("Watchlist", ignoreCase = true) || movie.userStatus.isBlank()

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("library_item_card_${movie.id}"),
        cornerRadius = if (isCompactList) 16.dp else 22.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompactList) 8.dp else 12.dp),
            verticalAlignment = if (isCompactList) Alignment.CenterVertically else Alignment.Top
        ) {
            MoviePosterImage(
                posterUrl = movie.posterUrl,
                title = movie.title,
                signatureColorHex = movie.signatureColorHex,
                modifier = Modifier
                    .width(if (isCompactList) 54.dp else 90.dp)
                    .height(if (isCompactList) 80.dp else 136.dp)
                    .clip(RoundedCornerShape(if (isCompactList) 10.dp else 16.dp)),
                contentScale = ContentScale.Crop,
                releaseYear = movie.releaseYear,
                movieId = movie.id
            )

            Spacer(modifier = Modifier.width(if (isCompactList) 10.dp else 12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = movie.title.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = if (isCompactList) 13.5.sp else 15.sp),
                        color = FrostWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Selection Checkbox for multi-delete
                        IconButton(
                            onClick = onToggleSelectForDeletion,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("lib_select_checkbox_${movie.id}")
                        ) {
                            Icon(
                                imageVector = if (isSelectedForDeletion) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = "Select ${movie.title}",
                                tint = if (isSelectedForDeletion) ElectricCyan else SteelBlue,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // Small Dustbin Icon for direct fast removal
                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("lib_dustbin_btn_${movie.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete ${movie.title}",
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = SteelBlue, modifier = Modifier.size(17.dp))
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            ) {
                                if (!movie.userStatus.equals("Dropped", true)) {
                                    DropdownMenuItem(
                                        text = { Text("Move to Dropped") },
                                        onClick = {
                                            showMenu = false
                                            onMoveDropped()
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Remove from Library", color = Color(0xFFFF5252)) },
                                    onClick = {
                                        showMenu = false
                                        onRemove()
                                    }
                                )
                            }
                        }
                    }
                }

                if (!isCompactList && movie.inlineQuote.text.isNotBlank()) {
                    Text(
                        text = "“${movie.inlineQuote.text}”",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = FontStyle.Italic,
                            fontSize = 10.5.sp
                        ),
                        color = IcyBlue.copy(alpha = 0.80f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${movie.releaseYear} • ${movie.contentType.uppercase()} • ${movie.runtime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SteelBlue
                )

                Spacer(modifier = Modifier.height(if (isCompactList) 6.dp else 8.dp))

                val btnOpacity = (1f - com.example.ui.theme.LocalGlassTransparency.current).coerceIn(0f, 1f)
                val btnRim = (0.25f + 0.75f * btnOpacity).coerceIn(0f, 1f)
                val btnMinHeight = if (isCompactList) 36.dp else 44.dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Watchlist Squircle Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = btnMinHeight)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isInWatchlist) ElectricCyan.copy(alpha = (0.32f * btnOpacity).coerceIn(0f, 1f))
                                else Color(0xFF15253A).copy(alpha = (0.94f * btnOpacity).coerceIn(0f, 1f))
                            )
                            .border(
                                1.1.dp,
                                if (isInWatchlist) ElectricCyan.copy(alpha = btnRim) else IcyBlue.copy(alpha = 0.34f * btnRim),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onMoveWatchlist() }
                            .padding(horizontal = 8.dp, vertical = if (isCompactList) 5.dp else 8.dp)
                            .testTag("lib_watchlist_btn_${movie.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isInWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Watchlist",
                                tint = if (isInWatchlist) ElectricCyan else FrostWhite,
                                modifier = Modifier.requiredSize(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isInWatchlist) "Saved" else "Save",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 14.sp
                                ),
                                color = FrostWhite,
                                maxLines = 2,
                                softWrap = true,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 2. Watched Squircle Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = btnMinHeight)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isWatched) Color(0xFF00F5D4).copy(alpha = (0.32f * btnOpacity).coerceIn(0f, 1f))
                                else Color(0xFF15253A).copy(alpha = (0.94f * btnOpacity).coerceIn(0f, 1f))
                            )
                            .border(
                                1.1.dp,
                                if (isWatched) Color(0xFF00F5D4).copy(alpha = btnRim) else IcyBlue.copy(alpha = 0.34f * btnRim),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onMoveWatched() }
                            .padding(horizontal = 8.dp, vertical = if (isCompactList) 5.dp else 8.dp)
                            .testTag("lib_watched_btn_${movie.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isWatched) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                contentDescription = "Watched",
                                tint = if (isWatched) Color(0xFF00F5D4) else FrostWhite,
                                modifier = Modifier.requiredSize(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isWatched) "Watched" else "Watch",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 14.sp
                                ),
                                color = FrostWhite,
                                maxLines = 2,
                                softWrap = true,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 3. Rewatch Squircle Button with the number inside it
                    RewatchSymbolCounterButton(
                        rewatchCount = if (isWatched) movie.rewatchCount.coerceAtLeast(1) else 0,
                        size = btnMinHeight,
                        accentColor = ElectricCyan,
                        testTagName = "lib_rewatch_btn_${movie.id}",
                        isWatched = isWatched,
                        onClick = {
                            if (!isWatched) {
                                onMoveWatched()
                            } else {
                                onRewatch()
                            }
                        }
                    )
                }

                // Only when Watched is clicked (`isWatched == true`) does the 1-to-10 Rating Slider show up
                AnimatedVisibility(
                    visible = isWatched && !isCompactList,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Your Rating: ${sliderVal.roundToInt().coerceIn(1, 10)} / 10",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IcyBlue
                                )
                            }
                        }

                        Slider(
                            value = sliderVal,
                            onValueChange = { newVal ->
                                sliderVal = newVal
                            },
                            onValueChangeFinished = {
                                onSliderRatingChange(sliderVal.roundToInt().coerceIn(1, 10))
                            },
                            valueRange = 1f..10f,
                            steps = 8,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .testTag("movie_list_review_slider_${movie.id}"),
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = ElectricCyan,
                                inactiveTrackColor = SteelBlue.copy(alpha = 0.28f)
                            )
                        )
                    }
                }
            }
        }
    }
}
