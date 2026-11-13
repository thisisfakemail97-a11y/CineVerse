package com.example.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
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
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_watchlist_waterfall_grid"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 120.dp
            ),
            verticalItemSpacing = 12.dp,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Minimal Header & Search Bar
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
                                text = "${watchlistOnly.size} queued",
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
                    val staggerRatio = when (index % 4) {
                        0 -> 0.67f
                        1 -> 0.76f
                        2 -> 0.72f
                        else -> 0.65f
                    }
                    WaterfallWatchlistCard(
                        movie = movie,
                        posterAspectRatio = staggerRatio,
                        onClick = { onMovieClick(movie) },
                        onToggleWatchlist = {
                            if (movie.userStatus.equals("Watchlist", ignoreCase = true)) {
                                repository.removeFromLibrary(movie.id)
                            } else {
                                repository.addToWatchlist(movie)
                            }
                        },
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
}

/**
 * Minimal Poster + Title + Uniform Squircle Action Bar card for the 2x2 waterfall Watchlist.
 */
@Composable
fun WaterfallWatchlistCard(
    movie: MovieItem,
    posterAspectRatio: Float = 0.68f,
    onClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
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
    val actionSquircleCorner = 13.dp
    val actionSquircleShape = RoundedCornerShape(actionSquircleCorner)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("watchlist_waterfall_card_${movie.id}"),
        cornerRadius = 18.dp,
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
                                colors = listOf(Color.Transparent, DeepBlack.copy(alpha = 0.58f)),
                                startY = 140f
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = FrostWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Uniform Squircle Action Bar with clearly legible icon + text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Squircle Watched toggle
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(actionSquircleShape)
                            .background(
                                if (isWatched) Color(0xFF00F5D4).copy(alpha = 0.24f)
                                else Color(0xFF17273B).copy(alpha = 0.92f)
                            )
                            .drawWithContent {
                                drawContent()
                                val rPx = actionSquircleCorner.toPx()
                                val sw = 1.15.dp.toPx()
                                drawRoundRect(
                                    color = if (isWatched) Color(0xFF00F5D4) else IcyBlue.copy(alpha = 0.42f),
                                    topLeft = Offset(sw / 2f, sw / 2f),
                                    size = Size(size.width - sw, size.height - sw),
                                    cornerRadius = CornerRadius(rPx, rPx),
                                    style = Stroke(width = sw)
                                )
                            }
                            .clickable { onMarkWatched() }
                            .padding(horizontal = 8.dp)
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
                                modifier = Modifier.requiredSize(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Watch",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = FrostWhite,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }

                    // Squircle Save / Saved toggle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(actionSquircleShape)
                            .background(
                                if (isInWatchlist) sigColor.copy(alpha = 0.22f)
                                else Color(0xFF17273B).copy(alpha = 0.92f)
                            )
                            .drawWithContent {
                                drawContent()
                                val rPx = actionSquircleCorner.toPx()
                                val sw = 1.15.dp.toPx()
                                drawRoundRect(
                                    color = if (isInWatchlist) ElectricCyan else IcyBlue.copy(alpha = 0.42f),
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
                            modifier = Modifier.requiredSize(20.dp)
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
                        Color(0xFFD500F9).copy(alpha = 0.45f * breathAlpha),
                        Color(0xFF00E5FF).copy(alpha = 0.28f * breathAlpha),
                        Color(0xFF0A111D)
                    ),
                    center = center,
                    radius = size.maxDimension * 0.72f
                ),
                cornerRadius = CornerRadius(rPx, rPx)
            )
            rotate(rotAngle, pivot = center) {
                val sw = 2.dp.toPx()
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00F5D4).copy(alpha = breathAlpha),
                            Color(0xFF00E5FF).copy(alpha = breathAlpha),
                            Color(0xFFD500F9).copy(alpha = breathAlpha),
                            Color(0xFF4361EE).copy(alpha = breathAlpha)
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
