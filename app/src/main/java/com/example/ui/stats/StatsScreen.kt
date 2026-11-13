package com.example.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MovieItem
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillButton
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FrostWhite
import com.example.ui.theme.IcyBlue
import com.example.ui.theme.SteelBlue

@Composable
fun StatsScreen(
    userLibrary: List<MovieItem> = emptyList(),
    onExploreClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var previewMode by remember { mutableStateOf(false) }

    val watchedMovies = remember(userLibrary) {
        userLibrary.filter { it.userStatus.equals("Watched", ignoreCase = true) }
    }

    val displayList = if (previewMode && watchedMovies.isEmpty()) {
        listOf(
            MovieItem(title = "Dune: Part Two", releaseYear = 2024, userRating = 10, genres = listOf("Sci-Fi", "Adventure")),
            MovieItem(title = "Interstellar", releaseYear = 2014, userRating = 9, genres = listOf("Sci-Fi", "Drama")),
            MovieItem(title = "The Batman", releaseYear = 2022, userRating = 9, genres = listOf("Action", "Crime")),
            MovieItem(title = "Inception", releaseYear = 2010, userRating = 9, genres = listOf("Sci-Fi", "Thriller")),
            MovieItem(title = "Fight Club", releaseYear = 1999, userRating = 8, genres = listOf("Drama", "Thriller"))
        )
    } else {
        watchedMovies
    }

    val titlesWatchedCount = displayList.size
    val estimatedHours = displayList.size * 2
    val ratedMovies = displayList.filter { it.userRating > 0 }
    val avgRating = if (ratedMovies.isNotEmpty()) {
        val ratings = ratedMovies.map { it.userRating.toDouble() }
        String.format("%.1f", ratings.average())
    } else "—"

    // Real genre breakdown calculation
    val genreCounts = remember(displayList) {
        val map = mutableMapOf<String, Int>()
        displayList.forEach { movie ->
            movie.genres.forEach { genre ->
                map[genre] = (map[genre] ?: 0) + 1
            }
        }
        val total = map.values.sum().coerceAtLeast(1)
        map.entries.sortedByDescending { it.value }.take(4).map {
            it.key to ((it.value * 100) / total)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(bottom = 120.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                    text = "Cinema Wrapped",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                    color = FrostWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Personal Viewing Insights & Analytics",
                    style = MaterialTheme.typography.bodySmall,
                    color = SteelBlue,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (watchedMovies.isEmpty()) {
                GlassPillButton(
                    text = if (previewMode) "Live Zero State" else "Sample Preview",
                    icon = Icons.Default.AutoGraph,
                    isActive = previewMode,
                    onClick = { previewMode = !previewMode }
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        if (displayList.isEmpty()) {
            // Zero-State
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "No Stats",
                        tint = SteelBlue,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your Cinema Wrapped Awaits",
                        style = MaterialTheme.typography.titleLarge,
                        color = FrostWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Mark movies and series as Watched in your Library to unlock viewing hours, genre breakdown, and your personal Cinema Wrapped recap.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SteelBlue,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    GlassPillButton(
                        text = "Search Movies to Log",
                        icon = Icons.Default.Search,
                        isActive = true,
                        onClick = onExploreClick
                    )
                }
            }
        } else {
            // 1. Primary Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatMetricCard(
                    title = "Titles Watched",
                    value = "$titlesWatchedCount",
                    icon = Icons.Default.Movie,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Watch Time",
                    value = "${estimatedHours}h",
                    icon = Icons.Default.Schedule,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Avg Rating",
                    value = avgRating,
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Top Rated Titles
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Your Top Rated Discoveries",
                        style = MaterialTheme.typography.titleMedium,
                        color = FrostWhite
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    displayList.sortedByDescending { it.userRating }
                        .take(3)
                        .forEachIndexed { index, movie ->
                            if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                            val ratingText = if (movie.userRating > 0) "My Rating: ${movie.userRating}/10 ★" else "Unrated"
                            TopRatedRow(
                                rank = "${index + 1}",
                                title = movie.title.uppercase(),
                                rating = ratingText,
                                year = "${movie.releaseYear}"
                            )
                        }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Favourite Genres Breakdown
            if (genreCounts.isNotEmpty()) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Favourite Genres Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            color = FrostWhite
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        genreCounts.forEachIndexed { index, (genre, pct) ->
                            if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                            GenreProgressRow(genre = genre, percent = pct)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 4. Most Rewatched
            val mostRewatched = displayList.maxByOrNull { it.rewatchCount }
            if (mostRewatched != null) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Rewatch Counter",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = FrostWhite
                                )
                                Text(
                                    text = "Most rewatched masterwork",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SteelBlue
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(ElectricCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mostRewatched.rewatchCount}x",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ElectricCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${mostRewatched.title.uppercase()} (${mostRewatched.releaseYear})",
                            style = MaterialTheme.typography.titleMedium,
                            color = FrostWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.height(115.dp),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.requiredSize(20.dp)
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                    color = FrostWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = SteelBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TopRatedRow(rank: String, title: String, rating: String, year: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(end = 10.dp)
        ) {
            Text(
                text = rank,
                style = MaterialTheme.typography.titleMedium,
                color = ElectricCyan,
                modifier = Modifier.width(24.dp),
                maxLines = 1
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = FrostWhite,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(year, color = SteelBlue, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
        Text(
            text = rating,
            color = IcyBlue,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun GenreProgressRow(genre: String, percent: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(genre, color = FrostWhite, style = MaterialTheme.typography.bodySmall)
            Text("$percent%", color = SteelBlue, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = ElectricCyan,
            trackColor = SteelBlue.copy(alpha = 0.2f),
        )
    }
}
