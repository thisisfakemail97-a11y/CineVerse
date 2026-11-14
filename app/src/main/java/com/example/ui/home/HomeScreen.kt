package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CineverseRepository
import com.example.data.MovieItem
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillButton
import com.example.ui.components.MoviePosterImage
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FrostWhite
import com.example.ui.theme.IcyBlue
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.SteelBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userLibrary: List<MovieItem>,
    onMovieClick: (MovieItem) -> Unit,
    onSearchClick: () -> Unit,
    onMoodClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    onUpdateRating: (String, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val repository = remember(context) { CineverseRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    val refreshVersion by repository.refreshVersion.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }

    // Configurable Home Grid Columns: 2, 3, 4, or 5 in a row
    var gridColumns by remember { mutableIntStateOf(2) }

    // Single & Multi-Select Deletion with Confirmation
    var selectedForDeletionIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var movieToDelete by remember { mutableStateOf<MovieItem?>(null) }
    var showBatchDeleteConfirmDialog by remember { mutableStateOf(false) }

    val watchlistOnly = remember(userLibrary, refreshVersion) {
        userLibrary.filter {
            !it.userStatus.equals("Watched", ignoreCase = true) &&
                !it.userStatus.equals("Dropped", ignoreCase = true)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "home_search_breath")
    val searchGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "home_search_glow_alpha"
    )

    val gridSpacing = when (gridColumns) {
        2 -> 12.dp
        3 -> 9.dp
        4 -> 7.dp
        else -> 5.dp
    }

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
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(gridColumns),
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_watchlist_waterfall_grid"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 120.dp
            ),
            verticalItemSpacing = gridSpacing,
            horizontalArrangement = Arrangement.spacedBy(gridSpacing)
        ) {
            // Minimal Header, Search Bar, and Grid Column Selector (2, 3, 4, 5 in a row)
            item(span = StaggeredGridItemSpan.FullLine) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = "Watchlist",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                ),
                                color = FrostWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${watchlistOnly.size} queued • ${gridColumns} per row",
                                style = MaterialTheme.typography.bodySmall,
                                color = SteelBlue,
                                maxLines = 1
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassPillButton(
                                text = "All (${userLibrary.size})",
                                icon = Icons.Default.Bookmark,
                                isActive = userLibrary.isNotEmpty(),
                                onClick = onWatchlistClick
                            )
                            PlasmaMoodTriggerButton(
                                onClick = onMoodClick
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("home_centered_search_button"),
                        cornerRadius = 18.dp,
                        borderWidth = 1.4.dp,
                        onClick = onSearchClick
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = ElectricCyan,
                                modifier = Modifier
                                    .requiredSize(22.dp)
                                    .graphicsLayer { alpha = searchGlowAlpha }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Search movies, series or reel URL...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = SteelBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Grid Column Selector: 2, 3, 4, or 5 in a row
                    val barOpacity = (1f - com.example.ui.theme.LocalGlassTransparency.current).coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_grid_column_selector"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Grid Columns",
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Grid Layout:",
                                style = MaterialTheme.typography.labelMedium,
                                color = IcyBlue
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(2, 3, 4, 5).forEach { colCount ->
                                val isSelected = gridColumns == colCount
                                Box(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) ElectricCyan.copy(alpha = (0.32f * barOpacity).coerceIn(0f, 1f))
                                            else Color(0xFF142336).copy(alpha = (0.85f * barOpacity).coerceIn(0f, 1f))
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) ElectricCyan else IcyBlue.copy(alpha = 0.28f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { gridColumns = colCount }
                                        .padding(horizontal = 10.dp)
                                        .testTag("home_grid_col_$colCount"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${colCount}x",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) FrostWhite else SteelBlue
                                    )
                                }
                            }
                        }
                    }

                    // Batch Multi-Select Delete Bar
                    AnimatedVisibility(
                        visible = selectedForDeletionIds.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .testTag("home_batch_delete_bar"),
                            cornerRadius = 14.dp
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
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = FrostWhite
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { selectedForDeletionIds = emptySet() }) {
                                        Text("Clear", color = SteelBlue, fontSize = 12.sp)
                                    }
                                    GlassPillButton(
                                        text = "Delete (${selectedForDeletionIds.size})",
                                        icon = Icons.Default.DeleteOutline,
                                        compact = true,
                                        isActive = true,
                                        onClick = { showBatchDeleteConfirmDialog = true },
                                        modifier = Modifier.testTag("home_delete_selected_btn")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            if (watchlistOnly.isEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        cornerRadius = 20.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = "Empty Watchlist",
                                tint = ElectricCyan,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your Watchlist is Empty",
                                style = MaterialTheme.typography.titleMedium,
                                color = FrostWhite,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Search for movies or paste a reel link to start filling your waterfall grid.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SteelBlue,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GlassPillButton(
                                text = "Find Movies",
                                icon = Icons.Default.Search,
                                isActive = true,
                                onClick = onSearchClick
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(watchlistOnly, key = { _, movie -> movie.id }) { index, movie ->
                    val staggerRatio = if (gridColumns == 2) {
                        when (index % 4) {
                            0 -> 0.67f
                            1 -> 0.76f
                            2 -> 0.72f
                            else -> 0.65f
                        }
                    } else {
                        0.68f
                    }
                    val isChecked = movie.id in selectedForDeletionIds
                    WaterfallWatchlistCard(
                        movie = movie,
                        gridColumns = gridColumns,
                        posterAspectRatio = staggerRatio,
                        isSelectedForDeletion = isChecked,
                        onToggleSelectForDeletion = {
                            selectedForDeletionIds = if (isChecked) {
                                selectedForDeletionIds - movie.id
                            } else {
                                selectedForDeletionIds + movie.id
                            }
                        },
                        onClick = { onMovieClick(movie) },
                        onToggleWatchlist = {
                            if (movie.userStatus.equals("Watchlist", ignoreCase = true)) {
                                movieToDelete = movie
                            } else {
                                repository.addToWatchlist(movie)
                            }
                        },
                        onDeleteClick = { movieToDelete = movie },
                        onMarkWatched = {
                            val nextRating = movie.userRating.coerceAtLeast(8)
                            repository.markWatched(movie, rating = nextRating)
                            onUpdateRating(movie.id, nextRating)
                        }
                    )
                }
            }
        }
    }

    // Single Movie Delete Confirmation Dialog
    if (movieToDelete != null) {
        val target = movieToDelete!!
        AlertDialog(
            onDismissRequest = { movieToDelete = null },
            containerColor = MidnightNavy,
            title = { Text("Remove from Watchlist?", color = FrostWhite) },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${target.title}\" from your library?",
                    color = SteelBlue,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.removeFromLibrary(target.id)
                        selectedForDeletionIds = selectedForDeletionIds - target.id
                        movieToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    modifier = Modifier.testTag("home_confirm_remove_btn")
                ) {
                    Text("Remove", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { movieToDelete = null }) {
                    Text("Cancel", color = FrostWhite)
                }
            }
        )
    }

    // Multi-Select Delete Confirmation Dialog
    if (showBatchDeleteConfirmDialog && selectedForDeletionIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirmDialog = false },
            containerColor = MidnightNavy,
            title = { Text("Remove ${selectedForDeletionIds.size} Selected Movie(s)?", color = FrostWhite) },
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
                    modifier = Modifier.testTag("home_confirm_batch_remove_btn")
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
}

/**
 * Adaptive Poster + Title + Squircle Action Bar card for 2, 3, 4, or 5 columns per row,
 * with small Dustbin icon and selection Checkbox.
 */
@Composable
fun WaterfallWatchlistCard(
    movie: MovieItem,
    gridColumns: Int = 2,
    posterAspectRatio: Float = 0.68f,
    isSelectedForDeletion: Boolean = false,
    onToggleSelectForDeletion: () -> Unit = {},
    onClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onDeleteClick: () -> Unit = {},
    onMarkWatched: () -> Unit
) {
    val sigColor = remember(movie.signatureColorHex) {
        try {
            Color(android.graphics.Color.parseColor(movie.signatureColorHex))
        } catch (_: Exception) {
            ElectricCyan
        }
    }

    val isWatched = movie.userStatus.equals("Watched", ignoreCase = true)
    val isInWatchlist = movie.userStatus.equals("Watchlist", ignoreCase = true)
    val glassTransparency = com.example.ui.theme.LocalGlassTransparency.current.coerceIn(0f, 1f)
    val opacityFactor = (1f - glassTransparency).coerceIn(0f, 1f)
    val rimFactor = (0.25f + 0.75f * opacityFactor).coerceIn(0f, 1f)

    val cardCorner = when (gridColumns) {
        2 -> 18.dp
        3 -> 14.dp
        4 -> 11.dp
        else -> 9.dp
    }
    val actionSquircleCorner = when (gridColumns) {
        2 -> 13.dp
        3 -> 10.dp
        else -> 8.dp
    }
    val actionSquircleShape = RoundedCornerShape(actionSquircleCorner)
    val actionHeight = when (gridColumns) {
        2 -> 44.dp
        3 -> 36.dp
        4 -> 30.dp
        else -> 26.dp
    }
    val titleFontSize = when (gridColumns) {
        2 -> 13.5.sp
        3 -> 11.5.sp
        4 -> 10.sp
        else -> 9.sp
    }
    val contentPad = when (gridColumns) {
        2 -> 10.dp
        3 -> 7.dp
        4 -> 5.dp
        else -> 4.dp
    }
    val overlayIconBoxSize = when (gridColumns) {
        2 -> 28.dp
        3 -> 24.dp
        else -> 20.dp
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("watchlist_waterfall_card_${movie.id}"),
        cornerRadius = cardCorner,
        borderWidth = 1.2.dp,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(posterAspectRatio)
            ) {
                MoviePosterImage(
                    posterUrl = movie.posterUrl,
                    title = movie.title,
                    signatureColorHex = movie.signatureColorHex,
                    releaseYear = movie.releaseYear,
                    movieId = movie.id,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, DeepBlack.copy(alpha = 0.58f * opacityFactor)),
                                startY = 140f
                            )
                        )
                )

                // Top-left checkbox + Top-right small dustbin icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(overlayIconBoxSize)
                            .clip(RoundedCornerShape(7.dp))
                            .background(DeepBlack.copy(alpha = 0.65f))
                            .clickable { onToggleSelectForDeletion() }
                            .testTag("home_select_checkbox_${movie.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelectedForDeletion) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = "Select ${movie.title}",
                            tint = if (isSelectedForDeletion) ElectricCyan else FrostWhite,
                            modifier = Modifier.size(if (gridColumns >= 4) 13.dp else 16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(overlayIconBoxSize)
                            .clip(RoundedCornerShape(7.dp))
                            .background(DeepBlack.copy(alpha = 0.65f))
                            .clickable { onDeleteClick() }
                            .testTag("home_dustbin_btn_${movie.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove ${movie.title}",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(if (gridColumns >= 4) 13.dp else 16.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentPad, vertical = contentPad)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = titleFontSize,
                        lineHeight = (titleFontSize.value + 3f).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FrostWhite,
                    maxLines = if (gridColumns >= 4) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(if (gridColumns >= 4) 5.dp else 8.dp))

                // Adaptive Squircle Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (gridColumns >= 4) 4.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Squircle Watched toggle
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(actionHeight)
                            .clip(actionSquircleShape)
                            .background(
                                if (isWatched) Color(0xFF00F5D4).copy(alpha = (0.30f * opacityFactor).coerceIn(0f, 1f))
                                else Color(0xFF17273B).copy(alpha = (0.94f * opacityFactor).coerceIn(0f, 1f))
                            )
                            .drawWithContent {
                                drawContent()
                                val rPx = actionSquircleCorner.toPx()
                                val sw = 1.15.dp.toPx()
                                drawRoundRect(
                                    color = if (isWatched) Color(0xFF00F5D4).copy(alpha = rimFactor) else IcyBlue.copy(alpha = 0.42f * rimFactor),
                                    topLeft = Offset(sw / 2f, sw / 2f),
                                    size = Size(size.width - sw, size.height - sw),
                                    cornerRadius = CornerRadius(rPx, rPx),
                                    style = Stroke(width = sw)
                                )
                            }
                            .clickable { onMarkWatched() }
                            .padding(horizontal = if (gridColumns >= 4) 2.dp else 8.dp)
                            .testTag("card_watched_btn_${movie.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isWatched) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                contentDescription = "Mark Watched",
                                tint = if (isWatched) Color(0xFF00F5D4) else FrostWhite,
                                modifier = Modifier.requiredSize(if (gridColumns >= 4) 13.dp else 17.dp)
                            )
                            if (gridColumns <= 3) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Watch",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = if (gridColumns == 3) 10.sp else 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = FrostWhite,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }

                    // Squircle Save / Saved toggle
                    Box(
                        modifier = Modifier
                            .size(actionHeight)
                            .clip(actionSquircleShape)
                            .background(
                                if (isInWatchlist) sigColor.copy(alpha = (0.28f * opacityFactor).coerceIn(0f, 1f))
                                else Color(0xFF17273B).copy(alpha = (0.94f * opacityFactor).coerceIn(0f, 1f))
                            )
                            .drawWithContent {
                                drawContent()
                                val rPx = actionSquircleCorner.toPx()
                                val sw = 1.15.dp.toPx()
                                drawRoundRect(
                                    color = if (isInWatchlist) ElectricCyan.copy(alpha = rimFactor) else IcyBlue.copy(alpha = 0.42f * rimFactor),
                                    topLeft = Offset(sw / 2f, sw / 2f),
                                    size = Size(size.width - sw, size.height - sw),
                                    cornerRadius = CornerRadius(rPx, rPx),
                                    style = Stroke(width = sw)
                                )
                            }
                            .clickable { onToggleWatchlist() }
                            .testTag("card_watchlist_btn_${movie.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isInWatchlist) "Saved in Watchlist" else "Save to Watchlist",
                            tint = if (isInWatchlist) ElectricCyan else FrostWhite,
                            modifier = Modifier.requiredSize(if (gridColumns >= 4) 14.dp else 20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Distinct Glowing Squircle Plasma Mood Button with `Icons.Default.Psychology`.
 */
@Composable
fun PlasmaMoodTriggerButton(
    onClick: () -> Unit
) {
    val glassTransparency = com.example.ui.theme.LocalGlassTransparency.current.coerceIn(0f, 1f)
    val opacityFactor = (1f - glassTransparency).coerceIn(0f, 1f)
    val rimFactor = (0.25f + 0.75f * opacityFactor).coerceIn(0f, 1f)
    val infiniteTransition = rememberInfiniteTransition(label = "plasma_trigger")
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "plasma_breath_alpha"
    )

    val rotAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "plasma_rot_angle"
    )

    val squircleCorner = 15.dp
    val squircleShape = RoundedCornerShape(squircleCorner)

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(squircleShape)
            .testTag("ai_mood_orb_button")
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rPx = squircleCorner.toPx()
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFD500F9).copy(alpha = 0.45f * breathAlpha * opacityFactor),
                        Color(0xFF00E5FF).copy(alpha = 0.28f * breathAlpha * opacityFactor),
                        Color(0xFF0A111D).copy(alpha = 0.94f * opacityFactor)
                    ),
                    center = center,
                    radius = size.maxDimension * 0.72f
                ),
                cornerRadius = CornerRadius(rPx, rPx)
            )
            rotate(rotAngle, pivot = center) {
                val sw = 2.dp.toPx()
                val effRim = breathAlpha * rimFactor
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00F5D4).copy(alpha = effRim),
                            Color(0xFF00E5FF).copy(alpha = effRim),
                            Color(0xFFD500F9).copy(alpha = effRim),
                            Color(0xFF4361EE).copy(alpha = effRim)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    topLeft = Offset(sw / 2f, sw / 2f),
                    size = Size(size.width - sw, size.height - sw),
                    cornerRadius = CornerRadius(rPx, rPx),
                    style = Stroke(width = sw)
                )
            }
        }
        Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = "AI Mood Orbs",
            tint = Color.White,
            modifier = Modifier
                .requiredSize(24.dp)
                .graphicsLayer { alpha = (0.78f + 0.22f * breathAlpha).coerceIn(0f, 1f) }
        )
    }
}
