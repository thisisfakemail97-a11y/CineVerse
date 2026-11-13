package com.example.ui.mood

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AiRecommendationItem
import com.example.data.CineverseRepository
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
import kotlin.math.cos
import kotlin.math.sin

data class PlasmaOrbSpec(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val secondaryColor: Color
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoodDiscoveryScreen(
    allMovies: List<MovieItem>,
    initialTab: String = "mood", // "mood", "categories"
    onMovieClick: (MovieItem) -> Unit,
    onAddToWatchlist: (MovieItem) -> Unit = {},
    onFeedback: (MovieItem, String) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { CineverseRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val refreshVersion by repository.refreshVersion.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }

    var activeTab by remember(initialTab) {
        mutableStateOf(if (initialTab == "categories") "categories" else "mood")
    }

    // --- TAB 1: 2x2 Plasma Orb Mood State ---
    var selectedMoods by remember { mutableStateOf(listOf("Exciting")) }
    var customMoodText by remember { mutableStateOf("") }
    var selectedContentType by remember { mutableStateOf("All") }
    var selectedSourceFilter by remember { mutableStateOf("All Sources") } // "All Sources", "My Watchlist", "Internet Mainstream"
    var moodRecommendations by remember { mutableStateOf<List<AiRecommendationItem>>(emptyList()) }
    var isMoodLoading by remember { mutableStateOf(false) }

    val watchlistMoodMatches = remember(allMovies, selectedMoods, customMoodText, selectedContentType) {
        repository.filterWatchlistByMood(
            userLibraryList = allMovies,
            selectedMoods = selectedMoods,
            naturalLanguageMood = customMoodText,
            contentTypePreference = selectedContentType
        )
    }

    // --- TAB 2: 7 AI Discovery Categories State ---
    var categoryMap by remember { mutableStateOf<Map<String, List<AiRecommendationItem>>>(emptyMap()) }
    var selectedLanguage by remember { mutableStateOf("Japanese") }
    var selectedPerson by remember { mutableStateOf("Christopher Nolan") }
    var feedbackNotice by remember { mutableStateOf<String?>(null) }

    val plasmaOrbs = remember {
        listOf(
            PlasmaOrbSpec(
                id = "Exciting",
                label = "Exciting",
                icon = Icons.Default.Bolt,
                primaryColor = Color(0xFF00F5D4),
                secondaryColor = Color(0xFF00E5FF)
            ),
            PlasmaOrbSpec(
                id = "Relaxing",
                label = "Relaxing",
                icon = Icons.Default.Bedtime,
                primaryColor = Color(0xFF00E5FF),
                secondaryColor = Color(0xFF00B4D8)
            ),
            PlasmaOrbSpec(
                id = "Thought-Provoking",
                label = "Thought-Provoking",
                icon = Icons.Default.Psychology,
                primaryColor = Color(0xFFD500F9),
                secondaryColor = Color(0xFF9D4EDD)
            ),
            PlasmaOrbSpec(
                id = "Escapist",
                label = "Escapist",
                icon = Icons.Default.RocketLaunch,
                primaryColor = Color(0xFF4361EE),
                secondaryColor = Color(0xFF7209B7)
            )
        )
    }

    val extraMoodNuances = remember {
        listOf(
            "Curious",
            "Nostalgic",
            "Suspenseful",
            "Light-hearted",
            "Adventurous",
            "Emotionally engaging"
        )
    }

    LaunchedEffect(selectedMoods, customMoodText, selectedContentType, refreshVersion) {
        isMoodLoading = true
        moodRecommendations = repository.discoverByMood(
            naturalLanguageMood = customMoodText,
            selectedMoods = selectedMoods,
            contentTypePreference = selectedContentType,
            forceRefresh = false
        )
        isMoodLoading = false
    }

    LaunchedEffect(allMovies, selectedLanguage, selectedPerson, refreshVersion) {
        categoryMap = repository.getDiscoveryCategories(selectedLanguage, selectedPerson)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(bottom = 120.dp)
                .padding(horizontal = 16.dp)
        ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Top Navigation & Mode Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                size = 46.dp,
                onClick = onBack
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassPillButton(
                    text = "Mood Orbs",
                    icon = Icons.Default.Psychology,
                    isActive = activeTab == "mood",
                    onClick = { activeTab = "mood" }
                )
                GlassPillButton(
                    text = "Categories",
                    icon = Icons.Default.Category,
                    isActive = activeTab == "categories",
                    onClick = { activeTab = "categories" }
                )
            }
        }

        if (feedbackNotice != null) {
            Spacer(modifier = Modifier.height(10.dp))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = feedbackNotice!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { feedbackNotice = null }) {
                        Text("Dismiss", color = SteelBlue)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        when (activeTab) {
            "mood" -> {
                // ==================== 1. 2x2 PLASMA ORB MOOD FILTER UI ====================
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 28.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "What are you in the mood for?",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = FrostWhite,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Select a mood orb or nuance below to categorize & filter your Watchlist and mainstream movies",
                            style = MaterialTheme.typography.bodySmall,
                            color = SteelBlue,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        // 2x2 Grid of Swirling Plasma Mood Orbs
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Row 1: Exciting + Relaxing
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                plasmaOrbs.take(2).forEach { orb ->
                                    val isSelected = orb.id in selectedMoods
                                    SwirlingPlasmaMoodOrb(
                                        spec = orb,
                                        isSelected = isSelected,
                                        onClick = {
                                            selectedMoods = listOf(orb.id)
                                        }
                                    )
                                }
                            }

                            // Row 2: Thought-Provoking + Escapist
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                plasmaOrbs.drop(2).forEach { orb ->
                                    val isSelected = orb.id in selectedMoods
                                    SwirlingPlasmaMoodOrb(
                                        spec = orb,
                                        isSelected = isSelected,
                                        onClick = {
                                            selectedMoods = listOf(orb.id)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Secondary Nuance Mood Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            extraMoodNuances.forEach { nuance ->
                                val active = nuance in selectedMoods
                                GlassPillButton(
                                    text = nuance,
                                    isActive = active,
                                    onClick = {
                                        selectedMoods = if (active && selectedMoods.size > 1) {
                                            selectedMoods - nuance
                                        } else {
                                            listOf(nuance)
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Natural-Language Mood Input Bar
                        OutlinedTextField(
                            value = customMoodText,
                            onValueChange = { customMoodText = it },
                            placeholder = {
                                Text(
                                    text = "Or type a mood filter (e.g. cozy, action, mystery)...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SteelBlue
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("natural_mood_input"),
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = SteelBlue.copy(alpha = 0.4f),
                                focusedTextColor = FrostWhite,
                                unfocusedTextColor = FrostWhite
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Source Filter (All Sources / My Watchlist / Internet Mainstream)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All Sources", "My Watchlist", "Internet Mainstream").forEach { src ->
                                GlassPillButton(
                                    text = src,
                                    isActive = selectedSourceFilter == src,
                                    onClick = { selectedSourceFilter = src }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Content Type Filter
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All", "Movie", "Web Series", "Anime", "Documentary").forEach { type ->
                                GlassPillButton(
                                    text = type,
                                    isActive = selectedContentType == type,
                                    onClick = { selectedContentType = type }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                val activeMoodLabel = (selectedMoods + listOfNotNull(customMoodText.takeIf { it.isNotBlank() })).joinToString(" + ")

                // ==================== SECTION A: FILTERED FROM USER'S WATCHLIST ====================
                if (selectedSourceFilter == "All Sources" || selectedSourceFilter == "My Watchlist") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "From Your Watchlist ($activeMoodLabel)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = FrostWhite
                            )
                            Text(
                                text = "${watchlistMoodMatches.size} saved title(s) categorized under $activeMoodLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = IcyBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (watchlistMoodMatches.isEmpty()) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 18.dp
                        ) {
                            Text(
                                text = if (allMovies.isEmpty()) {
                                    "Your Watchlist is currently empty. Add movies from Search or below to filter your personal Watchlist by mood."
                                } else {
                                    "None of your ${allMovies.size} saved Watchlist titles match the \"$activeMoodLabel\" mood filter."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = SteelBlue,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            watchlistMoodMatches.forEach { rec ->
                                val libMatch = allMovies.find { it.id == rec.movie.id || it.title.equals(rec.movie.title, ignoreCase = true) }
                                ExplainableRecommendationCard(
                                    rec = if (libMatch != null) rec.copy(movie = libMatch) else rec,
                                    onMovieClick = { onMovieClick(libMatch ?: rec.movie) },
                                    onAddToWatchlist = {
                                        repository.addToWatchlist(libMatch ?: rec.movie)
                                        onAddToWatchlist(libMatch ?: rec.movie)
                                        feedbackNotice = "${rec.movie.title} updated in Watchlist."
                                    },
                                    onMarkWatched = {
                                        val target = libMatch ?: rec.movie
                                        val r = if (target.userRating > 0) target.userRating else 8
                                        repository.markWatched(target, rating = r, review = target.userReview)
                                        feedbackNotice = "Marked ${target.title} as Watched."
                                    },
                                    onIncrementRewatch = {
                                        val target = libMatch ?: rec.movie
                                        val r = if (target.userRating > 0) target.userRating else 8
                                        if (target.userStatus == "Watched") {
                                            repository.logRewatch(target, sessionRating = r, sessionNotes = "Logged rewatch from Mood Discovery")
                                        } else {
                                            repository.markWatched(target, rating = r, review = target.userReview)
                                        }
                                        feedbackNotice = "Logged rewatch for ${target.title}."
                                    },
                                    onRateChange = { newRating ->
                                        repository.updateRating(rec.movie.id, newRating)
                                        feedbackNotice = "Rated ${rec.movie.title} $newRating/10."
                                    },
                                    onFeedback = { fbType ->
                                        repository.recordRecommendationFeedback(rec.movie, fbType)
                                        onFeedback(rec.movie, fbType)
                                        feedbackNotice = "Recorded \"$fbType\" for ${rec.movie.title}."
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ==================== SECTION B: FILTERED MAINSTREAM MOVIES FROM INTERNET ====================
                if (selectedSourceFilter == "All Sources" || selectedSourceFilter == "Internet Mainstream") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Mainstream Movies from Internet ($activeMoodLabel)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = FrostWhite
                            )
                            Text(
                                text = "Filtered mainstream releases for $activeMoodLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = IcyBlue
                            )
                        }
                        if (isMoodLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = ElectricCyan,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(moodRecommendations, key = { it.movie.id }) { rec ->
                            MoodRecommendationPosterTile(
                                rec = rec,
                                onClick = { onMovieClick(rec.movie) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Why These Mainstream Movies Fit Your Mood",
                        style = MaterialTheme.typography.titleMedium,
                        color = FrostWhite
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        moodRecommendations.forEach { rec ->
                            val libMatch = allMovies.find { it.id == rec.movie.id || it.title.equals(rec.movie.title, ignoreCase = true) }
                            ExplainableRecommendationCard(
                                rec = if (libMatch != null) rec.copy(movie = libMatch) else rec,
                                onMovieClick = { onMovieClick(libMatch ?: rec.movie) },
                                onAddToWatchlist = {
                                    repository.addToWatchlist(libMatch ?: rec.movie)
                                    onAddToWatchlist(libMatch ?: rec.movie)
                                    feedbackNotice = "Added ${rec.movie.title} to your Watchlist."
                                },
                                onMarkWatched = {
                                    val target = libMatch ?: rec.movie
                                    val r = if (target.userRating > 0) target.userRating else 8
                                    repository.markWatched(target, rating = r, review = target.userReview)
                                    feedbackNotice = "Marked ${target.title} as Watched."
                                },
                                onIncrementRewatch = {
                                    val target = libMatch ?: rec.movie
                                    val r = if (target.userRating > 0) target.userRating else 8
                                    if (target.userStatus == "Watched") {
                                        repository.logRewatch(target, sessionRating = r, sessionNotes = "Logged rewatch from Mood Discovery")
                                    } else {
                                        repository.markWatched(target, rating = r, review = target.userReview)
                                    }
                                    feedbackNotice = "Logged rewatch for ${target.title}."
                                },
                                onRateChange = { newRating ->
                                    repository.markWatched(libMatch ?: rec.movie, rating = newRating, review = "")
                                    feedbackNotice = "Rated ${rec.movie.title} $newRating/10."
                                },
                                onFeedback = { fbType ->
                                    repository.recordRecommendationFeedback(rec.movie, fbType)
                                    onFeedback(rec.movie, fbType)
                                    feedbackNotice = "Recorded \"$fbType\" for ${rec.movie.title} — tuning future recommendations."
                                }
                            )
                        }
                    }
                }
            }

            "categories" -> {
                // Interactive Filters for "Discover by Language" & "Discover by Director or Actor"
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Explore by Language", style = MaterialTheme.typography.titleSmall, color = FrostWhite)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Japanese", "Korean", "German", "French", "Spanish", "Hindi", "English").forEach { lang ->
                                GlassPillButton(
                                    text = lang,
                                    isActive = selectedLanguage == lang,
                                    onClick = { selectedLanguage = lang }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Explore by Director or Actor", style = MaterialTheme.typography.titleSmall, color = FrostWhite)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Christopher Nolan",
                                "Denis Villeneuve",
                                "Greta Gerwig",
                                "Christian Bale",
                                "Shane Carruth",
                                "Bong Joon-ho",
                                "Hayao Miyazaki"
                            ).forEach { person ->
                                GlassPillButton(
                                    text = person,
                                    isActive = selectedPerson == person,
                                    onClick = { selectedPerson = person }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // All 7 AI Discovery Categories
                categoryMap.forEach { (categoryTitle, items) ->
                    if (items.isNotEmpty()) {
                        Text(
                            text = categoryTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                            color = ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(items) { rec ->
                                CategoryExplainableCard(
                                    rec = rec,
                                    onClick = { onMovieClick(rec.movie) },
                                    onAdd = {
                                        repository.addToWatchlist(rec.movie)
                                        onAddToWatchlist(rec.movie)
                                        feedbackNotice = "Added ${rec.movie.title} to Watchlist."
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(22.dp))
                    }
                }
            }
        }
        }
    }
}

/**
 * Swirling Glowing Plasma Mood Orb (matching `mood filter.jfif.jpg`)
 * Renders an outer neon halo, double glowing ring, animated internal plasma smoke/wave ribbons,
 * centered minimalist icon, and crisp label inside the circular orb.
 */
@Composable
fun SwirlingPlasmaMoodOrb(
    spec: PlasmaOrbSpec,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "plasma_${spec.id}")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "plasma_p1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = (2f * Math.PI).toFloat(),
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "plasma_p2"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "plasma_rot"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "plasma_pulse"
    )

    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 0.98f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f),
        label = "orb_select_scale"
    )

    Box(
        modifier = Modifier
            .size(142.dp)
            .scale(selectionScale * pulse)
            .testTag("mood_orb_${spec.id.lowercase()}")
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val c = center

            // 1. Outer Atmospheric Neon Bloom Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        spec.primaryColor.copy(alpha = if (isSelected) 0.55f else 0.28f),
                        spec.secondaryColor.copy(alpha = if (isSelected) 0.24f else 0.10f),
                        Color.Transparent
                    ),
                    center = c,
                    radius = radius * 1.24f
                ),
                center = c,
                radius = radius * 1.24f
            )

            // 2. Deep Dark Interior Sphere Base
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        spec.primaryColor.copy(alpha = 0.22f),
                        Color(0xFF0A0E17).copy(alpha = 0.92f),
                        Color(0xFF05070B)
                    ),
                    center = c,
                    radius = radius * 0.88f
                ),
                center = c,
                radius = radius * 0.88f
            )

            // 3. Swirling Internal Plasma / Smoke Ribbons with Rotation
            rotate(rotationAngle, pivot = c) {
                val innerR = radius * 0.82f
                for (waveIdx in 0..2) {
                    val offsetAngle = phase1 + waveIdx * 2.1f
                    val ox = c.x + cos(offsetAngle) * (innerR * 0.35f)
                    val oy = c.y + sin(phase2 + waveIdx) * (innerR * 0.35f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                spec.secondaryColor.copy(alpha = if (isSelected) 0.52f else 0.36f),
                                spec.primaryColor.copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = Offset(ox, oy),
                            radius = innerR * 0.72f
                        ),
                        center = Offset(ox, oy),
                        radius = innerR * 0.72f
                    )
                }

                // Swirling Caustic Wave Path inside the Orb
                val wavePath = Path().apply {
                    val steps = 36
                    for (i in 0..steps) {
                        val t = i.toFloat() / steps * (2f * Math.PI.toFloat())
                        val rMod = innerR * (0.78f + 0.12f * sin(3f * t + phase1) + 0.08f * cos(2f * t - phase2))
                        val px = c.x + rMod * cos(t)
                        val py = c.y + rMod * sin(t)
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                drawPath(
                    path = wavePath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isSelected) 0.85f else 0.55f),
                            spec.primaryColor.copy(alpha = 0.95f),
                            spec.secondaryColor.copy(alpha = 0.75f)
                        )
                    ),
                    style = Stroke(width = if (isSelected) 2.6.dp.toPx() else 1.8.dp.toPx())
                )
            }

            // 4. Outer Crisp Neon Ring Border
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isSelected) 0.95f else 0.55f),
                        spec.primaryColor,
                        spec.secondaryColor,
                        Color.White.copy(alpha = 0.75f)
                    ),
                    start = Offset(c.x + cos(phase1) * radius, c.y + sin(phase1) * radius),
                    end = Offset(c.x - cos(phase1) * radius, c.y - sin(phase1) * radius)
                ),
                center = c,
                radius = radius * 0.88f,
                style = Stroke(width = if (isSelected) 2.8.dp.toPx() else 1.8.dp.toPx())
            )
        }

        // Centered Icon & Mood Label inside the Orb
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(14.dp)
        ) {
            Icon(
                imageVector = spec.icon,
                contentDescription = spec.label,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = spec.label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MoodRecommendationPosterTile(
    rec: AiRecommendationItem,
    onClick: () -> Unit
) {
    val movie = rec.movie
    Column(
        modifier = Modifier
            .width(128.dp)
            .clickable { onClick() }
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp),
            cornerRadius = 18.dp,
            onClick = onClick
        ) {
            MoviePosterImage(
                posterUrl = movie.posterUrl,
                title = movie.title,
                signatureColorHex = movie.signatureColorHex,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = movie.title,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
            color = FrostWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (movie.userStatus == "Watched" && movie.userRating > 0) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "My Rating: ${movie.userRating}/10",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = ElectricCyan
                )
            } else {
                Text(
                    text = "${movie.releaseYear} • ${movie.runtime}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = SteelBlue
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExplainableRecommendationCard(
    rec: AiRecommendationItem,
    onMovieClick: () -> Unit,
    onAddToWatchlist: () -> Unit,
    onMarkWatched: () -> Unit = {},
    onIncrementRewatch: () -> Unit = {},
    onRateChange: (Int) -> Unit = {},
    onFeedback: (String) -> Unit
) {
    val movie = rec.movie
    val isWatched = movie.userStatus == "Watched"
    val isWatchlist = movie.userStatus == "Watchlist"
    var showFeedbackRow by remember { mutableStateOf(false) }
    var sliderRating by remember(movie.id, movie.userRating) {
        mutableStateOf(if (movie.userRating > 0) movie.userRating.toFloat() else 8f)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        onClick = onMovieClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                MoviePosterImage(
                    posterUrl = movie.posterUrl,
                    title = movie.title,
                    signatureColorHex = movie.signatureColorHex,
                    releaseYear = movie.releaseYear,
                    movieId = movie.id,
                    modifier = Modifier
                        .width(84.dp)
                        .height(122.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ElectricCyan.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = rec.categoryTag,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = ElectricCyan
                            )
                        }

                        if (isWatched && movie.userRating > 0) {
                            Text(
                                text = "My Rating: ${movie.userRating}/10 ★",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${movie.title} (${movie.releaseYear})",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                        color = FrostWhite
                    )

                    Text(
                        text = "${movie.contentType} • ${movie.runtime} • ${movie.language}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SteelBlue
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Explainable non-spoiler match reason
                    Text(
                        text = rec.matchReason,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                        color = FrostWhite.copy(alpha = 0.88f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Three Status Buttons + Tune AI in responsive FlowRow so buttons/icons never squish
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassPillButton(
                    text = if (isWatchlist) "Saved" else "Save",
                    icon = if (isWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkAdd,
                    isActive = isWatchlist,
                    onClick = onAddToWatchlist
                )
                GlassPillButton(
                    text = if (isWatched) "Watched" else "Watch",
                    icon = Icons.Default.CheckCircle,
                    isActive = isWatched,
                    onClick = onMarkWatched
                )
                RewatchSymbolCounterButton(
                    rewatchCount = if (isWatched) maxOf(1, movie.rewatchCount) else movie.rewatchCount,
                    isWatched = isWatched,
                    size = 46.dp,
                    onClick = onIncrementRewatch,
                    testTag = "mood_rewatch_btn_${movie.id}"
                )
                GlassPillButton(
                    text = "Tune AI",
                    icon = Icons.Default.Tune,
                    isActive = showFeedbackRow,
                    onClick = { showFeedbackRow = !showFeedbackRow }
                )
            }

            // 1 to 10 Rating Slider ONLY appears after Watched is clicked
            AnimatedVisibility(visible = isWatched) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Rating: ${sliderRating.toInt()} / 10",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = ElectricCyan
                        )
                        Text(
                            text = "1 — 10",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = SteelBlue
                        )
                    }
                    Slider(
                        value = sliderRating,
                        onValueChange = { sliderRating = kotlin.math.round(it) },
                        onValueChangeFinished = { onRateChange(sliderRating.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .testTag("mood_card_rating_slider_${movie.id}"),
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = SteelBlue.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            AnimatedVisibility(visible = showFeedbackRow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("More like this", "Less like this", "Already watched", "Not interested").forEach { fb ->
                        AssistChip(
                            onClick = {
                                onFeedback(fb)
                                showFeedbackRow = false
                            },
                            label = { Text(fb, fontSize = 11.sp, color = FrostWhite) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MidnightNavy.copy(alpha = 0.85f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryExplainableCard(
    rec: AiRecommendationItem,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    val movie = rec.movie
    GlassCard(
        modifier = Modifier
            .width(236.dp)
            .height(276.dp),
        cornerRadius = 22.dp,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
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
                                listOf(Color.Transparent, DeepBlack.copy(alpha = 0.8f))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${movie.title} (${movie.releaseYear})",
                        style = MaterialTheme.typography.titleSmall,
                        color = FrostWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = rec.categoryTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rec.matchReason,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                        color = FrostWhite.copy(alpha = 0.85f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (movie.userStatus == "Watched" && movie.userRating > 0) "My Rating: ${movie.userRating}/10 • ${movie.runtime}" else "${movie.contentType} • ${movie.runtime}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = IcyBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                    )
                    GlassIconButton(
                        icon = Icons.Default.BookmarkAdd,
                        contentDescription = "Add to Watchlist",
                        size = 42.dp,
                        tint = ElectricCyan,
                        onClick = onAdd
                    )
                }
            }
        }
    }
}
