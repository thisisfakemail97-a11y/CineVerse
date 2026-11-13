package com.example.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.CastMember
import com.example.data.CineverseRepository
import com.example.data.MovieConnectionItem
import com.example.data.MovieItem
import com.example.data.SoundtrackInfo
import com.example.data.SoundtrackTrack
import com.example.data.StreamingProvider
import com.example.ui.components.AestheticQuoteCarousel
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassPillButton
import com.example.ui.components.MoviePosterImage
import com.example.ui.components.PersonPortraitImage
import com.example.ui.components.ThreeMovieStatusButtonsWithWatchedRating
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FrostWhite
import com.example.ui.theme.IcyBlue
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SteelBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovieDetailScreen(
    movie: MovieItem,
    onBack: () -> Unit,
    isInWatchlist: Boolean = false,
    isWatched: Boolean = false,
    onToggleWatchlist: (MovieItem) -> Unit = {},
    onLogWatched: (MovieItem, Int, String) -> Unit = { _, _, _ -> },
    onLogRewatch: ((MovieItem, Int, String, String) -> Unit)? = null,
    onRelatedMovieClick: ((String) -> Unit)? = null,
    onConnectedMovieSelect: (MovieItem) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { CineverseRepository(context) }
    val scrollState = rememberScrollState()

    val selectedRegion by repository.selectedRegion.collectAsStateWithLifecycle()

    var inWatchlistState by remember(movie.id, isInWatchlist) {
        mutableStateOf(isInWatchlist || movie.userStatus.equals("Watchlist", ignoreCase = true))
    }
    var watchedState by remember(movie.id, isWatched) {
        mutableStateOf(isWatched || movie.userStatus.equals("Watched", ignoreCase = true))
    }
    var userRating by remember(movie.id) { mutableStateOf(movie.userRating) }
    var rewatchCount by remember(movie.id) {
        mutableStateOf(if (isWatched || movie.userStatus.equals("Watched", ignoreCase = true)) movie.rewatchCount.coerceAtLeast(1) else 0)
    }
    var isFavorite by remember { mutableStateOf(false) }
    var showRewatchDialog by remember { mutableStateOf(false) }
    var userReviewText by remember(movie.id) { mutableStateOf(movie.userReview) }
    var rewatchNotesText by remember { mutableStateOf("") }
    var rewatchRating by remember { mutableStateOf(userRating.coerceAtLeast(8)) }
    var showFullSynopsis by remember { mutableStateOf(false) }
    var selectedFeedback by remember(movie.id) { mutableStateOf<String?>(null) }

    // Phase 6 Data Resolutions
    val verifiedQuote = remember(movie.id, movie.title) {
        repository.resolveVerifiedInlineQuote(movie)
    }
    val soundtrackInfo = remember(movie.id, movie.title) {
        repository.resolveSoundtrackInfo(movie)
    }
    val regionalProviders = remember(movie.id, movie.title, selectedRegion) {
        repository.resolveRegionalStreaming(movie, selectedRegion)
    }

    // Resolved poster URL & Movie Connections
    var resolvedPosterUrl by remember(movie.id, movie.posterUrl) { mutableStateOf(movie.posterUrl) }
    var personPortraits by remember(movie.id) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var movieConnections by remember(movie.id) { mutableStateOf<List<MovieConnectionItem>>(emptyList()) }

    LaunchedEffect(movie.id, movie.title, movie.director, movie.mainCast) {
        if (resolvedPosterUrl.isBlank() || resolvedPosterUrl.contains("unsplash.com", ignoreCase = true)) {
            val bestPoster = repository.resolveBestPoster(movie.title, movie.releaseYear, resolvedPosterUrl)
            if (bestPoster.isNotBlank()) {
                resolvedPosterUrl = bestPoster
            }
        }
        val namesToFetch = buildList {
            if (movie.director.isNotBlank()) add(movie.director)
            addAll(movie.mainCast)
            movie.castMembers.forEach { add(it.name) }
        }
        if (namesToFetch.isNotEmpty()) {
            personPortraits = repository.fetchWikipediaPersonPortraits(namesToFetch)
        }
        movieConnections = repository.getMovieConnections(movie)
    }

    val movieAuraColor = remember(movie.signatureColorHex) {
        try {
            Color(android.graphics.Color.parseColor(movie.signatureColorHex))
        } catch (e: Exception) {
            ElectricCyan
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "DetailPosterBreathing")
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingAlpha"
    )

    var touchPos by remember { mutableStateOf(Offset(300f, 400f)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        touchPos = offset
                    }
                )
            }
    ) {
        // 1. FULL SCREEN ORIGINAL THEATRICAL POSTER BACKDROP
        MoviePosterImage(
            posterUrl = resolvedPosterUrl.ifBlank { movie.bannerUrl },
            title = movie.title,
            signatureColorHex = movie.signatureColorHex,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            showFallbackTitle = false,
            releaseYear = movie.releaseYear,
            movieId = movie.id
        )

        // 2. Cinematic Translucent Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x3505080D),
                            Color(0x7505080D),
                            Color(0xCE05080D),
                            DeepBlack
                        )
                    )
                )
        )

        // 3. Dynamic Atmospheric Breathing Light
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            movieAuraColor.copy(alpha = breathingAlpha),
                            movieAuraColor.copy(alpha = breathingAlpha * 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(400f, 600f),
                        radius = 950f
                    )
                )
        )

        // 4. Subtle Interactive Touch Aura
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            movieAuraColor.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = touchPos,
                        radius = 650f
                    )
                )
        )

        // 5. Scrollable Foreground Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(bottom = 120.dp)
        ) {
            // Top Floating Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    size = 46.dp,
                    onClick = onBack
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassIconButton(
                        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        size = 46.dp,
                        onClick = { isFavorite = !isFavorite }
                    )

                    GlassIconButton(
                        icon = Icons.Default.Share,
                        contentDescription = "Share",
                        size = 46.dp,
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Check out ${movie.title} (${movie.releaseYear})!")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Movie"))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(95.dp))

            // 6. Main Liquid Glass Info Card:
            //    - Movie Title
            //    - Verified Single Inline Quote directly below Title + Speaker immediately below Quote (No Quotes heading)
            //    - Three Status Buttons below Movie Title (Watchlist, Watched, Rewatch with number inside it)
            //    - Inline 1-to-10 Rating Slider & Review Editor that ONLY appears when Watched is clicked!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 28.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = movie.title.uppercase(),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 22.sp,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = FrostWhite,
                                    modifier = Modifier.testTag("detail_movie_title")
                                )

                                // Auto-Advancing Swipeable Quote Carousel (2s Interval)
                                val carouselQuotes = remember(movie.id, movie.title, verifiedQuote) {
                                    (movie.effectiveQuotes +
                                        repository.lookupQuotes(movie.title) +
                                        listOfNotNull(verifiedQuote.takeIf { it.text.isNotBlank() }))
                                        .filter { it.text.isNotBlank() }
                                        .distinctBy { it.text.trim() }
                                }
                                if (carouselQuotes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("detail_inline_quote_block")
                                    ) {
                                        AestheticQuoteCarousel(
                                            quotes = carouselQuotes,
                                            textColor = FrostWhite.copy(alpha = 0.88f),
                                            speakerColor = movieAuraColor,
                                            maxLines = 4
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${movie.releaseYear} • ${movie.contentType.uppercase()} • ${movie.runtime.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                                    color = movieAuraColor
                                )

                                if (movie.originalTitle.isNotBlank() && !movie.originalTitle.equals(movie.title, ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "ORIGINAL: ${movie.originalTitle.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = IcyBlue
                                    )
                                }

                                if (movie.director.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = "DIRECTED BY ${movie.director.uppercase()}",
                                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                                        color = SteelBlue
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Floating Original Poster Thumbnail
                            MoviePosterImage(
                                posterUrl = resolvedPosterUrl,
                                title = movie.title,
                                signatureColorHex = movie.signatureColorHex,
                                modifier = Modifier
                                    .width(96.dp)
                                    .height(144.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop,
                                releaseYear = movie.releaseYear,
                                movieId = movie.id
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // THREE BUTTONS DIRECTLY BELOW MOVIE TITLE & QUOTE:
                        // 1. Watchlist Button
                        // 2. Watched Button
                        // 3. Rewatch Symbol Button with the rewatch count number INSIDE it
                        // + Inline 1-to-10 Rating Slider & Review that ONLY expands when Watched is clicked!
                        ThreeMovieStatusButtonsWithWatchedRating(
                            movieId = movie.id,
                            isWatchlist = inWatchlistState,
                            isWatched = watchedState,
                            rewatchCount = rewatchCount,
                            userRating = userRating,
                            userReview = userReviewText,
                            accentColor = movieAuraColor,
                            showReviewEditor = true,
                            onWatchlistClick = {
                                inWatchlistState = !inWatchlistState
                                if (inWatchlistState) watchedState = false
                                onToggleWatchlist(movie)
                            },
                            onWatchedClick = {
                                watchedState = !watchedState
                                if (watchedState) {
                                    inWatchlistState = false
                                    if (rewatchCount < 1) rewatchCount = 1
                                    if (userRating < 1) userRating = 8
                                    repository.markWatched(movie, userRating, userReviewText)
                                    onLogWatched(movie, userRating, userReviewText)
                                } else {
                                    inWatchlistState = true
                                    repository.updateStatus(movie.id, "Watchlist")
                                }
                            },
                            onRewatchClick = {
                                if (!watchedState) {
                                    watchedState = true
                                    inWatchlistState = false
                                    rewatchCount = 1
                                    if (userRating < 1) userRating = 8
                                    repository.markWatched(movie, userRating, userReviewText)
                                    onLogWatched(movie, userRating, userReviewText)
                                } else {
                                    showRewatchDialog = true
                                }
                            },
                            onRatingAndReviewSaved = { newRating, newReview ->
                                userRating = newRating
                                userReviewText = newReview
                                watchedState = true
                                if (rewatchCount < 1) rewatchCount = 1
                                repository.markWatched(movie, newRating, newReview)
                                onLogWatched(movie, newRating, newReview)
                            }
                        )
                    }
                }

                // Saved Personal Review Display (only when watched and review exists)
                if (watchedState && userReviewText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "YOUR PERSONAL REVIEW",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ElectricCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = userReviewText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = FrostWhite
                            )
                        }
                    }
                }

                // 7. Personal Rating Summary (Only My Rating when Watched — Zero Public Ratings)
                if (watchedState && userRating > 0) {
                    Text(
                        text = "My Rating",
                        style = MaterialTheme.typography.titleMedium,
                        color = FrostWhite
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp),
                        cornerRadius = 18.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = movieAuraColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "My Rating: $userRating / 10",
                                    color = movieAuraColor,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                text = "Rewatches: ${rewatchCount.coerceAtLeast(1)}x",
                                style = MaterialTheme.typography.labelMedium,
                                color = IcyBlue
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (movie.awards.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Awards & Recognition: ${movie.awards}",
                        style = MaterialTheme.typography.bodySmall,
                        color = IcyBlue
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 8. Synopsis
                Text("Synopsis", style = MaterialTheme.typography.titleMedium, color = FrostWhite)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (showFullSynopsis) movie.synopsis else movie.synopsis.take(220) + (if (movie.synopsis.length > 220) "..." else ""),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = FrostWhite.copy(alpha = 0.88f)
                )
                if (movie.synopsis.length > 220) {
                    TextButton(
                        onClick = { showFullSynopsis = !showFullSynopsis },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(if (showFullSynopsis) "Read less" else "Read full synopsis", color = movieAuraColor)
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 9. Director First, Followed by Cast
                val crewList = remember(movie, personPortraits) {
                    val list = mutableListOf<CastMember>()
                    if (movie.director.isNotBlank() && movie.director != "Filmmaker") {
                        val dirPhoto = movie.directorAvatarUrl.ifBlank { personPortraits[movie.director] ?: "" }
                        list.add(
                            CastMember(
                                name = movie.director,
                                role = "Director",
                                avatarUrl = dirPhoto
                            )
                        )
                    }
                    if (movie.castMembers.isNotEmpty()) {
                        movie.castMembers.forEach { cm ->
                            if (list.none { it.name.equals(cm.name, ignoreCase = true) }) {
                                val photo = cm.avatarUrl.ifBlank { personPortraits[cm.name] ?: "" }
                                list.add(cm.copy(avatarUrl = photo))
                            }
                        }
                    } else {
                        movie.mainCast.forEach { actorName ->
                            if (list.none { it.name.equals(actorName, ignoreCase = true) }) {
                                val photo = personPortraits[actorName] ?: ""
                                list.add(
                                    CastMember(
                                        name = actorName,
                                        role = "Cast",
                                        avatarUrl = photo
                                    )
                                )
                            }
                        }
                    }
                    list
                }

                if (crewList.isNotEmpty()) {
                    Text("Director & Cast", style = MaterialTheme.typography.titleMedium, color = FrostWhite)
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(crewList) { member ->
                            CastMemberAvatar(
                                member = member,
                                accentColor = movieAuraColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 10. PHASE 6 SECTION 7 — WHERE TO WATCH (Region-Aware Streaming Availability)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Where to Watch",
                        style = MaterialTheme.typography.titleMedium,
                        color = FrostWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF142840))
                            .border(1.dp, ElectricCyan.copy(alpha = 0.50f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("where_to_watch_region_badge")
                    ) {
                        Text(
                            text = "Region: $selectedRegion",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("where_to_watch_card"),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (regionalProviders.isEmpty()) {
                            Text(
                                text = "Regional streaming availability is currently unverified for $selectedRegion. Use legitimate provider search below:",
                                style = MaterialTheme.typography.bodySmall,
                                color = IcyBlue,
                                modifier = Modifier.testTag("where_to_watch_unavailable_notice")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        } else {
                            // Detailed verified regional provider list with Subscription vs Rent/Buy & Last Checked date
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                regionalProviders.forEach { provider ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF0E1D30).copy(alpha = 0.72f))
                                            .clickable { launchStreamingApp(context, provider, movie.title) }
                                            .padding(10.dp)
                                            .testTag("streaming_provider_${provider.name.lowercase().replace(" ", "_")}"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            StreamingAppIconBadge(
                                                provider = provider,
                                                onClick = { launchStreamingApp(context, provider, movie.title) }
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = provider.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = FrostWhite,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${provider.availabilityType} • Checked: ${provider.lastChecked}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = IcyBlue,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = "Open ${provider.name}",
                                            tint = ElectricCyan,
                                            modifier = Modifier.requiredSize(18.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Quick Launch Row for Google & YouTube Official Trailers / Full Movie
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GoogleAppIconBadge(
                                onClick = {
                                    val googleUrl = "https://www.google.com/search?q=" + Uri.encode("${movie.title} ${movie.releaseYear} watch online $selectedRegion")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(googleUrl)))
                                }
                            )

                            val isFullMovieOnYoutube = movie.youtubeFullMovieUrl.isNotBlank()
                            YouTubePlatformOption(
                                isFullMovie = isFullMovieOnYoutube,
                                onClick = {
                                    val targetUrl = if (isFullMovieOnYoutube) {
                                        movie.youtubeFullMovieUrl
                                    } else if (movie.trailerUrl.isNotBlank()) {
                                        movie.trailerUrl
                                    } else {
                                        "https://www.youtube.com/results?search_query=" + Uri.encode("${movie.title} ${movie.releaseYear} official trailer")
                                    }
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 11. PHASE 6 SECTION 4 — SOUNDTRACK EXPLORER
                SoundtrackExplorerSection(
                    soundtrack = soundtrackInfo,
                    movieTitle = movie.title,
                    accentColor = movieAuraColor,
                    onPlayTrack = { track ->
                        repository.playSoundtrackTrack(track, soundtrackInfo, movie)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 12. Personalisation Feedback Controls (Phase 5 Section 9)
                Text("Personalise AI Taste Profile", style = MaterialTheme.typography.titleMedium, color = FrostWhite)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("More like this", "Less like this", "Already watched", "Not interested").forEach { fb ->
                        val isChosen = selectedFeedback == fb
                        GlassPillButton(
                            text = fb,
                            isActive = isChosen,
                            onClick = {
                                selectedFeedback = fb
                                repository.recordRecommendationFeedback(movie, fb)
                                if (fb == "Already watched") {
                                    watchedState = true
                                    if (rewatchCount < 1) rewatchCount = 1
                                }
                            }
                        )
                    }
                }
                if (selectedFeedback != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "✓ Recorded \"$selectedFeedback\" for ${movie.title} — future AI recommendations updated.",
                        style = MaterialTheme.typography.labelSmall,
                        color = movieAuraColor
                    )
                }

                // 13. Movie Connections (Phase 5 Section 6: Verified Factual vs AI-Assessed Creative Similarities)
                val factualConnections = remember(movieConnections) {
                    movieConnections.filter { it.connectionCategory == "FACTUAL" }
                }
                val creativeConnections = remember(movieConnections) {
                    movieConnections.filter { it.connectionCategory == "CREATIVE" }
                }

                if (factualConnections.isNotEmpty() || creativeConnections.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(26.dp))
                    Text(
                        text = "Movie Connections",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        color = FrostWhite
                    )
                    Text(
                        text = "Tap any connected title to explore its detail page directly",
                        style = MaterialTheme.typography.bodySmall,
                        color = SteelBlue
                    )

                    if (factualConnections.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF00F5D4).copy(alpha = 0.18f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "VERIFIED FACTUAL CONNECTIONS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF00F5D4)
                                )
                            }
                            Text(
                                text = "Director • Cast • Writer • Franchise",
                                style = MaterialTheme.typography.labelSmall,
                                color = SteelBlue,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            factualConnections.forEach { conn ->
                                MovieConnectionCard(
                                    item = conn,
                                    isFactual = true,
                                    onClick = {
                                        onConnectedMovieSelect(conn.movie)
                                        onRelatedMovieClick?.invoke(conn.movie.title)
                                    }
                                )
                            }
                        }
                    }

                    if (creativeConnections.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE040FB).copy(alpha = 0.18f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "AI-ASSESSED CREATIVE SIMILARITIES",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFE040FB)
                                )
                            }
                            Text(
                                text = "Themes • Atmosphere • Narrative Structure",
                                style = MaterialTheme.typography.labelSmall,
                                color = SteelBlue,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            creativeConnections.forEach { conn ->
                                MovieConnectionCard(
                                    item = conn,
                                    isFactual = false,
                                    onClick = {
                                        onConnectedMovieSelect(conn.movie)
                                        onRelatedMovieClick?.invoke(conn.movie.title)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Automated Rewatch Dialog
    if (showRewatchDialog) {
        val todayStr = remember { SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date()) }
        var selectedDate by remember { mutableStateOf(todayStr) }

        AlertDialog(
            onDismissRequest = { showRewatchDialog = false },
            containerColor = MidnightNavy,
            title = { Text("Log Rewatch #${rewatchCount + 1}", color = FrostWhite) },
            text = {
                Column {
                    Text("Recording another viewing session for ${movie.title}:", color = SteelBlue, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { selectedDate = it },
                        label = { Text("Watch Date", color = SteelBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = movieAuraColor,
                            unfocusedBorderColor = SteelBlue,
                            focusedTextColor = FrostWhite,
                            unfocusedTextColor = FrostWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = rewatchNotesText,
                        onValueChange = { rewatchNotesText = it },
                        placeholder = { Text("Notes for this rewatch (e.g. IMAX screening, with friends)...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = movieAuraColor,
                            unfocusedBorderColor = SteelBlue,
                            focusedTextColor = FrostWhite,
                            unfocusedTextColor = FrostWhite
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showRewatchDialog = false
                    rewatchCount += 1
                    onLogRewatch?.invoke(movie, rewatchRating, rewatchNotesText, selectedDate)
                }) {
                    Text("Confirm Rewatch", color = movieAuraColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRewatchDialog = false }) {
                    Text("Cancel", color = SteelBlue)
                }
            }
        )
    }
}

/**
 * PHASE 6 SECTION 4 — SOUNDTRACK EXPLORER
 * Displays verified soundtrack albums, background score, composer information,
 * recognisable songs, official music videos, and authorised streaming links.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SoundtrackExplorerSection(
    soundtrack: SoundtrackInfo?,
    movieTitle: String,
    accentColor: Color,
    onPlayTrack: ((SoundtrackTrack) -> Unit)? = null
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("soundtrack_explorer_section")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Album,
                contentDescription = "Soundtrack Explorer",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Soundtrack Explorer",
                style = MaterialTheme.typography.titleMedium,
                color = FrostWhite
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (soundtrack == null) {
                    Text(
                        text = "No verified soundtrack album or score metadata is currently indexed for $movieTitle. Unverified song claims are omitted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IcyBlue,
                        modifier = Modifier.testTag("soundtrack_unavailable_notice")
                    )
                } else {
                    Text(
                        text = soundtrack.albumTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = FrostWhite,
                        modifier = Modifier.testTag("soundtrack_album_title")
                    )

                    if (soundtrack.composer.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Composer: ${soundtrack.composer}",
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor,
                            modifier = Modifier.testTag("soundtrack_composer")
                        )
                    }

                    if (soundtrack.scoreDescription.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = soundtrack.scoreDescription,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = FrostWhite.copy(alpha = 0.85f)
                        )
                    }

                    if (soundtrack.recognisableSongs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "External Streaming Links",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = IcyBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlassPillButton(
                                text = "Spotify",
                                icon = Icons.AutoMirrored.Filled.OpenInNew,
                                onClick = {
                                    val url = "https://open.spotify.com/search/" + Uri.encode("$movieTitle ${soundtrack.albumTitle}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            )
                            GlassPillButton(
                                text = "Apple Music",
                                icon = Icons.AutoMirrored.Filled.OpenInNew,
                                onClick = {
                                    val url = "https://music.apple.com/us/search?term=" + Uri.encode("$movieTitle ${soundtrack.albumTitle}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            )
                            GlassPillButton(
                                text = "YouTube Music",
                                icon = Icons.AutoMirrored.Filled.OpenInNew,
                                onClick = {
                                    val url = "https://music.youtube.com/search?q=" + Uri.encode("$movieTitle ${soundtrack.albumTitle}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "VERIFIED RECOGNISABLE TRACKS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = IcyBlue
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            soundtrack.recognisableSongs.forEach { track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF0D1B2E).copy(alpha = 0.72f))
                                        .clickable {
                                            val songUrl = track.officialVideoUrl.ifBlank {
                                                "https://www.youtube.com/results?search_query=" + android.net.Uri.encode("$movieTitle ${track.title} ${track.artist}")
                                            }
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(songUrl)))
                                            } catch (_: Exception) {}
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = FrostWhite
                                        )
                                        Text(
                                            text = "${track.artist} (${track.duration})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SteelBlue
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = "Open External Link",
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Official brand vector emblems for streaming platforms & search in Where to Watch.
 */
@Composable
private fun StreamingAppIconBadge(
    provider: StreamingProvider,
    onClick: () -> Unit
) {
    val name = provider.name.lowercase()
    val (bgBrush, borderCol) = when {
        name.contains("prime") -> Brush.linearGradient(listOf(Color(0xFF0A1E32), Color(0xFF061320))) to Color(0xFF00A8E1)
        name.contains("hotstar") || name.contains("disney") -> Brush.linearGradient(listOf(Color(0xFF091B4C), Color(0xFF030A1F))) to Color(0xFF3E8BFF)
        name.contains("netflix") -> Brush.linearGradient(listOf(Color(0xFF140203), Color(0xFF070707))) to Color(0xFFE50914)
        name.contains("youtube") -> Brush.linearGradient(listOf(Color(0xFF1D090B), Color(0xFF100709))) to Color(0xFFFF0000)
        name.contains("google") -> Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF3F6FA))) to Color(0xFFD0DCEB)
        name.contains("jio") -> Brush.radialGradient(listOf(Color(0xFFFF1A9B), Color(0xFFD9008D), Color(0xFF6A004B))) to Color(0xFFFF47B6)
        name.contains("apple") -> Brush.linearGradient(listOf(Color(0xFF232529), Color(0xFF0E0F12))) to Color(0xFFE2E8F0)
        else -> Brush.linearGradient(listOf(Color(0xFF1E3A5F), Color(0xFF0F1E36))) to ElectricCyan
    }

    Box(
        modifier = Modifier
            .requiredSize(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgBrush)
            .border(1.4.dp, borderCol.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when {
            name.contains("netflix") -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_brand_netflix),
                    contentDescription = "Netflix",
                    modifier = Modifier.size(30.dp),
                    contentScale = ContentScale.Fit
                )
            }
            name.contains("prime") -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_brand_prime_video),
                    contentDescription = "Prime Video",
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Fit
                )
            }
            name.contains("hotstar") || name.contains("disney") -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_brand_hotstar),
                    contentDescription = "Disney+ Hotstar",
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Fit
                )
            }
            name.contains("youtube") -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_brand_youtube),
                    contentDescription = "YouTube",
                    modifier = Modifier.size(32.dp),
                    contentScale = ContentScale.Fit
                )
            }
            name.contains("google") -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_brand_google),
                    contentDescription = "Google",
                    modifier = Modifier.size(28.dp),
                    contentScale = ContentScale.Fit
                )
            }
            name.contains("apple") -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(modifier = Modifier.size(15.dp)) {
                        drawCircle(
                            color = Color.White,
                            radius = size.minDimension * 0.34f,
                            center = Offset(size.width * 0.5f, size.height * 0.62f)
                        )
                        drawOval(
                            color = Color.White,
                            topLeft = Offset(size.width * 0.48f, size.height * 0.06f),
                            size = Size(size.width * 0.24f, size.height * 0.24f)
                        )
                    }
                    Text(
                        text = "tv",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
            name.contains("jio") -> {
                Canvas(modifier = Modifier.size(30.dp)) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.28f),
                        radius = size.minDimension * 0.46f,
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                    val play = Path().apply {
                        moveTo(size.width * 0.40f, size.height * 0.30f)
                        lineTo(size.width * 0.72f, size.height * 0.50f)
                        lineTo(size.width * 0.40f, size.height * 0.70f)
                        close()
                    }
                    drawPath(play, color = Color.White)
                }
            }
            else -> {
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawRoundRect(
                        color = FrostWhite.copy(alpha = 0.2f),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    val play = Path().apply {
                        moveTo(size.width * 0.36f, size.height * 0.28f)
                        lineTo(size.width * 0.72f, size.height * 0.50f)
                        lineTo(size.width * 0.36f, size.height * 0.72f)
                        close()
                    }
                    drawPath(play, color = FrostWhite)
                }
            }
        }
    }
}

@Composable
private fun GoogleAppIconBadge(
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(15.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("google_watch_badge")
    ) {
        Box(
            modifier = Modifier
                .requiredSize(52.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White)
                .border(1.4.dp, Color.White.copy(alpha = 0.75f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_brand_google),
                contentDescription = "Google",
                modifier = Modifier.size(30.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Google",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            ),
            color = FrostWhite,
            maxLines = 1
        )
        Text(
            text = "Search Watch",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
            color = IcyBlue,
            maxLines = 1
        )
    }
}

@Composable
private fun YouTubePlatformOption(
    isFullMovie: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(15.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("youtube_watch_badge")
    ) {
        Box(
            modifier = Modifier
                .requiredSize(52.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Color(0xFF190A0D))
                .border(1.4.dp, Color(0xFFFF0000).copy(alpha = 0.65f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_brand_youtube),
                contentDescription = "YouTube",
                modifier = Modifier.size(34.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "YouTube",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            ),
            color = FrostWhite,
            maxLines = 1
        )
        Text(
            text = if (isFullMovie) "Full Movie" else "Trailers",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = if (isFullMovie) Color(0xFF00F5D4) else IcyBlue,
            maxLines = 1
        )
    }
}

private fun launchStreamingApp(context: Context, provider: StreamingProvider, movieTitle: String) {
    val pkg = when {
        provider.name.contains("Netflix", true) -> "com.netflix.mediaclient"
        provider.name.contains("Prime", true) -> "com.amazon.avod.thirdpartyclient"
        provider.name.contains("Hotstar", true) || provider.name.contains("Disney", true) -> "in.startv.hotstar"
        provider.name.contains("JioCinema", true) -> "com.jio.media.ondemand"
        provider.name.contains("Apple", true) -> "com.apple.atve.androidtv.appletv"
        provider.packageName.isNotBlank() -> provider.packageName
        else -> null
    }

    if (pkg != null) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
            return
        }
    }

    if (provider.url.isNotBlank()) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(provider.url))
            context.startActivity(intent)
        } catch (e: Exception) {
            val googleSearchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode("$movieTitle stream watch online")))
            context.startActivity(googleSearchIntent)
        }
    } else {
        val googleSearchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode("$movieTitle stream watch online")))
        context.startActivity(googleSearchIntent)
    }
}

@Composable
fun CastMemberAvatar(
    member: CastMember,
    accentColor: Color = ElectricCyan
) {
    val isDirector = member.role.equals("Director", ignoreCase = true)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(82.dp)
    ) {
        PersonPortraitImage(
            avatarUrl = member.avatarUrl,
            name = member.name,
            accentColor = accentColor,
            isDirector = isDirector,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(
                    width = if (isDirector) 2.dp else 1.dp,
                    color = if (isDirector) accentColor else Color.White.copy(alpha = 0.28f),
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = member.name,
            color = FrostWhite,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isDirector) FontWeight.Bold else FontWeight.Medium
            ),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Text(
            text = member.role,
            color = if (isDirector) accentColor else SteelBlue,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MovieConnectionCard(
    item: MovieConnectionItem,
    isFactual: Boolean,
    onClick: () -> Unit
) {
    val badgeColor = if (isFactual) Color(0xFF00F5D4) else Color(0xFFE040FB)
    val connectedMovie = item.movie

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MoviePosterImage(
                posterUrl = connectedMovie.posterUrl,
                title = connectedMovie.title,
                signatureColorHex = connectedMovie.signatureColorHex,
                modifier = Modifier
                    .width(64.dp)
                    .height(94.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                releaseYear = connectedMovie.releaseYear,
                movieId = connectedMovie.id
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.connectionBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFactual) "FACTUAL" else "AI SIMILARITY",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = SteelBlue,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${connectedMovie.title} (${connectedMovie.releaseYear})",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    color = FrostWhite
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = item.explanation,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                    color = FrostWhite.copy(alpha = 0.85f)
                )
            }
        }
    }
}
