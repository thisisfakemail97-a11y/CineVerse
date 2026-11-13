package com.example.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CineverseRepository
import com.example.data.ExtractedMediaMention
import com.example.data.MovieItem
import com.example.data.SocialDiscoveryRecord
import com.example.data.SocialExtractorResult
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    userLibrary: List<MovieItem> = emptyList(),
    onMovieClick: (MovieItem) -> Unit,
    onAddToWatchlist: (MovieItem) -> Unit = {},
    onMarkWatched: (MovieItem) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { CineverseRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val socialDiscoveries by repository.socialDiscoveries.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var selectedContentType by remember { mutableStateOf("All") }
    var selectedGenre by remember { mutableStateOf("All") }
    var searchHistory by remember {
        mutableStateOf(emptyList<String>())
    }
    var searchResults by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    // Phase 5 Social Media URL & Caption Extractor State
    var showManualTextBox by remember { mutableStateOf(false) }
    var manualCaptionInput by remember { mutableStateOf("") }
    var isExtractingSocial by remember { mutableStateOf(false) }
    var socialExtractionResult by remember { mutableStateOf<SocialExtractorResult?>(null) }
    var extractedMentions by remember { mutableStateOf<List<ExtractedMediaMention>>(emptyList()) }
    var selectedMentionIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var skippedMentionIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var mentionActionLog by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var socialStatusBanner by remember { mutableStateOf<String?>(null) }
    var showSocialDiscoveriesVault by remember { mutableStateOf(false) }

    // Detect if input is a social media or web URL
    val isSocialUrl = remember(query) {
        val lower = query.trim().lowercase()
        lower.startsWith("http://") || lower.startsWith("https://") ||
            listOf(
                "instagram.com",
                "youtube.com",
                "youtu.be",
                "reddit.com",
                "x.com",
                "twitter.com",
                "tiktok.com",
                "facebook.com",
                "fb.watch"
            ).any { domain -> lower.contains(domain) }
    }

    // Real-time search automatically triggered while typing (when not a raw URL)
    LaunchedEffect(query, selectedContentType, selectedGenre, isSocialUrl) {
        val trimmed = query.trim()
        if (isSocialUrl) {
            isSearching = false
            return@LaunchedEffect
        }
        if (trimmed.isEmpty() && selectedContentType == "All" && selectedGenre == "All") {
            searchResults = emptyList()
            isSearching = false
            searchError = null
            return@LaunchedEffect
        }

        if (trimmed.length < 2 && selectedContentType == "All" && selectedGenre == "All") {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }

        isSearching = true
        searchError = null
        delay(260)

        try {
            val results = repository.searchRealTime(trimmed, selectedContentType, selectedGenre)
            searchResults = results
        } catch (e: Exception) {
            searchError = e.localizedMessage ?: "Network search error"
            searchResults = emptyList()
        } finally {
            isSearching = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = 100.dp)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Main Prominent Liquid Glass Search & Social URL Bar
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSocialUrl) Icons.Default.Link else Icons.Default.Search,
                    contentDescription = "Search or Paste URL",
                    tint = ElectricCyan,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                TextField(
                    value = query,
                    onValueChange = {
                        query = it
                        socialStatusBanner = null
                    },
                    placeholder = {
                        Text(
                            text = "Search titles or paste YouTube / Reddit / Instagram / Article URL...",
                            color = SteelBlue,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("main_search_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = FrostWhite,
                        unfocusedTextColor = FrostWhite
                    ),
                    singleLine = true
                )

                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        socialExtractionResult = null
                        extractedMentions = emptyList()
                        socialStatusBanner = null
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = SteelBlue
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Utility Row: Paste Caption / Social Discoveries Collection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassPillButton(
                text = if (showManualTextBox) "Hide Text" else "Paste Text",
                icon = Icons.Default.ContentPaste,
                isActive = showManualTextBox,
                onClick = { showManualTextBox = !showManualTextBox }
            )

            GlassPillButton(
                text = "Vault (${socialDiscoveries.size})",
                icon = Icons.Default.CollectionsBookmark,
                isActive = showSocialDiscoveriesVault,
                onClick = { showSocialDiscoveriesVault = !showSocialDiscoveriesVault }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ==================== SOCIAL MEDIA URL & CAPTION EXTRACTOR ====================
        AnimatedVisibility(visible = isSocialUrl || showManualTextBox || socialExtractionResult != null) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                cornerRadius = 22.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val (detectedPlatform, detectedType) = remember(query) {
                        repository.phase5Engine.detectPlatformAndType(query)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Social Extractor",
                                tint = ElectricCyan,
                                modifier = Modifier.requiredSize(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isSocialUrl) "$detectedPlatform • $detectedType Detected" else "Social Reel & Caption Analyser",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = FrostWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isSocialUrl) "Watch Reel & extract all movies mentioned • Gemini AI" else "Paste any reel link, caption, review, or transcript",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SteelBlue,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        GlassPillButton(
                            text = if (isExtractingSocial) "Working..." else "Extract",
                            icon = Icons.Default.AutoAwesome,
                            isActive = true,
                            modifier = Modifier.testTag("analyse_with_ai_button"),
                            onClick = {
                                coroutineScope.launch {
                                    isExtractingSocial = true
                                    socialStatusBanner = null
                                    val result = repository.extractFromSocialUrlOrText(
                                        urlInput = query,
                                        manualTextInput = manualCaptionInput
                                    )
                                    socialExtractionResult = result
                                    extractedMentions = result.mentions
                                    skippedMentionIds = emptySet()
                                    selectedMentionIds = result.mentions
                                        .filter { !it.isUncertain || it.isConfirmedByUser }
                                        .map { it.id }
                                        .toSet()
                                    if (!result.isAccessible && result.mentions.isEmpty()) {
                                        showManualTextBox = true
                                    }
                                    if (result.mentions.isNotEmpty()) {
                                        repository.saveSocialDiscovery(result, mentionActionLog)
                                    }
                                    isExtractingSocial = false
                                }
                            }
                        )
                    }

                    // Show Manual Text Input when requested OR when URL is inaccessible (e.g. Instagram/TikTok login wall)
                    if (showManualTextBox || (socialExtractionResult != null && !socialExtractionResult!!.isAccessible)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (socialExtractionResult != null && !socialExtractionResult!!.isAccessible) {
                            Text(
                                text = "🔒 ${socialExtractionResult!!.statusMessage}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF00E5FF)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedTextField(
                            value = manualCaptionInput,
                            onValueChange = { manualCaptionInput = it },
                            label = {
                                Text(
                                    text = "Manual Input: Paste Caption, Transcript, Post Text, or Article Content",
                                    color = IcyBlue,
                                    fontSize = 11.sp
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "e.g., Must watch Primer and Coherence! Barbie was reviewed 9/10 while American Psycho was compared to Nightcrawler...",
                                    fontSize = 12.sp,
                                    color = SteelBlue
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 84.dp)
                                .testTag("manual_social_text_input"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = SteelBlue.copy(alpha = 0.45f),
                                focusedTextColor = FrostWhite,
                                unfocusedTextColor = FrostWhite
                            )
                        )
                    }

                    // Source Mode Indicator Banner (Clearly distinguishes Manual Input vs Public URL Retrieval)
                    if (socialExtractionResult != null) {
                        val res = socialExtractionResult!!
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (res.isManualInput) Color(0xFF7209B7).copy(alpha = 0.25f)
                                    else if (res.isAccessible) ElectricCyan.copy(alpha = 0.16f)
                                    else Color(0xFF4361EE).copy(alpha = 0.25f)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (res.isManualInput) "SOURCE: MANUAL TEXT INPUT (${res.platform})"
                                else if (res.isAccessible) "SOURCE: PUBLIC URL METADATA (${res.platform} • ${res.urlType})"
                                else "SOURCE RESTRICTED (${res.platform}) — PASTE TEXT ABOVE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = FrostWhite
                            )
                            Text(
                                text = "${extractedMentions.size} Titles",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                    }

                    if (socialStatusBanner != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = socialStatusBanner!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                    }

                    // Batch Controls: Select All + Add Selected
                    val activeVisibleMentions = extractedMentions.filter { it.id !in skippedMentionIds }
                    if (activeVisibleMentions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassPillButton(
                                text = "Select All (${activeVisibleMentions.size})",
                                icon = Icons.Default.SelectAll,
                                onClick = {
                                    selectedMentionIds = activeVisibleMentions.map { it.id }.toSet()
                                }
                            )
                            GlassPillButton(
                                text = "Add Selected (${selectedMentionIds.size})",
                                icon = Icons.Default.PlaylistAddCheck,
                                isActive = selectedMentionIds.isNotEmpty(),
                                onClick = {
                                    val selectedItems = activeVisibleMentions.filter { it.id in selectedMentionIds }
                                    val (added, skipped) = repository.addExtractedMentionsToLibrary(selectedItems, "Watchlist")
                                    val updatedActions = mentionActionLog.toMutableMap()
                                    selectedItems.forEach { m ->
                                        if (!m.isUncertain || m.isConfirmedByUser) {
                                            updatedActions[m.movie.id] = "Watchlist"
                                        }
                                    }
                                    mentionActionLog = updatedActions
                                    socialExtractionResult?.let { repository.saveSocialDiscovery(it, updatedActions) }
                                    socialStatusBanner = "Added $added title(s) to Watchlist" +
                                        if (skipped > 0) " ($skipped duplicate or unconfirmed match skipped)." else "."
                                }
                            )
                        }
                    }
                }
            }
        }

        // ==================== SOCIAL DISCOVERIES COLLECTION VAULT ====================
        AnimatedVisibility(visible = showSocialDiscoveriesVault) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                cornerRadius = 22.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Social Discoveries Vault", style = MaterialTheme.typography.titleMedium, color = FrostWhite)
                            Text("Revisit extracted links, excerpts & saved titles", style = MaterialTheme.typography.bodySmall, color = SteelBlue)
                        }
                        IconButton(onClick = { showSocialDiscoveriesVault = false }) {
                            Icon(Icons.Default.ExpandLess, contentDescription = "Close", tint = ElectricCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (socialDiscoveries.isEmpty()) {
                        Text(
                            text = "No social discoveries saved yet. Paste a Reel, YouTube, Reddit, or article link above and tap 'Watch & Extract'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SteelBlue
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            socialDiscoveries.take(5).forEach { record ->
                                SocialDiscoveryRecordItem(
                                    record = record,
                                    onReopen = {
                                        extractedMentions = record.matchedTitles
                                        socialExtractionResult = SocialExtractorResult(
                                            url = record.originalUrl,
                                            platform = record.platform,
                                            urlType = "Saved Discovery",
                                            isAccessible = true,
                                            isManualInput = record.originalUrl == "Manual Paste",
                                            statusMessage = "Loaded from Social Discoveries (${record.dateSaved})",
                                            extractedRawText = record.extractedExcerpt,
                                            mentions = record.matchedTitles
                                        )
                                        showSocialDiscoveriesVault = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Filter Pills Row (Content Type & Genres)
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

            listOf("Sci-Fi", "Action", "Thriller", "Drama", "Mystery").forEach { genre ->
                GlassPillButton(
                    text = genre,
                    isActive = selectedGenre == genre,
                    onClick = { selectedGenre = if (selectedGenre == genre) "All" else genre }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ==================== MAIN CONTENT AREA ====================
        val activeMentions = extractedMentions.filter { it.id !in skippedMentionIds }
        if (activeMentions.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(activeMentions, key = { it.id }) { mention ->
                    val existingItem = userLibrary.find {
                        it.id == mention.movie.id || it.title.equals(mention.movie.title, ignoreCase = true)
                    }
                    val libraryStatus = existingItem?.userStatus ?: "Not in Library"
                    val isSelected = mention.id in selectedMentionIds

                    ExtractedMentionCard(
                        mention = mention,
                        libraryStatus = libraryStatus,
                        isSelected = isSelected,
                        onToggleSelect = {
                            selectedMentionIds = if (isSelected) selectedMentionIds - mention.id else selectedMentionIds + mention.id
                        },
                        onConfirmUncertain = {
                            extractedMentions = extractedMentions.map {
                                if (it.id == mention.id) it.copy(isConfirmedByUser = true) else it
                            }
                            selectedMentionIds = selectedMentionIds + mention.id
                        },
                        onAddToWatchlist = {
                            if (mention.isUncertain && !mention.isConfirmedByUser) {
                                socialStatusBanner = "Please tap 'Confirm Match' on uncertain title ${mention.movie.title} first."
                            } else {
                                val (added, _) = repository.addExtractedMentionsToLibrary(listOf(mention.copy(isConfirmedByUser = true)), "Watchlist")
                                onAddToWatchlist(mention.movie)
                                val updated = mentionActionLog + (mention.movie.id to "Watchlist")
                                mentionActionLog = updated
                                socialExtractionResult?.let { repository.saveSocialDiscovery(it, updated) }
                                socialStatusBanner = if (added > 0) "Added ${mention.movie.title} to Watchlist." else "${mention.movie.title} is already in your library (duplicate prevented)."
                            }
                        },
                        onMarkWatched = {
                            repository.markWatched(mention.movie, rating = 8, review = "Discovered via ${socialExtractionResult?.platform ?: "Social Link"}")
                            onMarkWatched(mention.movie)
                            val updated = mentionActionLog + (mention.movie.id to "Watched")
                            mentionActionLog = updated
                            socialExtractionResult?.let { repository.saveSocialDiscovery(it, updated) }
                            socialStatusBanner = "Marked ${mention.movie.title} as Watched."
                        },
                        onSkip = {
                            skippedMentionIds = skippedMentionIds + mention.id
                            selectedMentionIds = selectedMentionIds - mention.id
                            val updated = mentionActionLog + (mention.movie.id to "Skipped")
                            mentionActionLog = updated
                            socialExtractionResult?.let { repository.saveSocialDiscovery(it, updated) }
                        },
                        onViewDetails = {
                            onMovieClick(mention.movie)
                        }
                    )
                }
            }
        } else if (query.isNotEmpty() && !isSocialUrl || selectedContentType != "All" || selectedGenre != "All") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSearching) "Searching real-time internet..." else "Live Results (${searchResults.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = SteelBlue
                )

                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = ElectricCyan,
                        strokeWidth = 2.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (!isSearching && searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "No Results",
                            tint = SteelBlue,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No live matches found for \"$query\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = FrostWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try searching full title name, director, or paste a social link to extract recommendations",
                            style = MaterialTheme.typography.bodySmall,
                            color = SteelBlue,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(searchResults) { movie ->
                        val existingItem = userLibrary.find { it.id == movie.id || it.title.equals(movie.title, ignoreCase = true) }
                        val isSaved = existingItem != null
                        SearchResultCard(
                            movie = existingItem ?: movie,
                            isSaved = isSaved,
                            onClick = {
                                if (query.isNotBlank() && !searchHistory.contains(query)) {
                                    searchHistory = listOf(query) + searchHistory.take(5)
                                }
                                onMovieClick(existingItem ?: movie)
                            },
                            onAdd = {
                                repository.addToWatchlist(existingItem ?: movie)
                                onAddToWatchlist(existingItem ?: movie)
                            },
                            onMarkWatched = {
                                val target = existingItem ?: movie
                                val r = if (target.userRating > 0) target.userRating else 8
                                repository.markWatched(target, rating = r, review = target.userReview)
                                onMarkWatched(target)
                            },
                            onIncrementRewatch = {
                                val target = existingItem ?: movie
                                val r = if (target.userRating > 0) target.userRating else 8
                                if (target.userStatus == "Watched") {
                                    repository.logRewatch(target, sessionRating = r, sessionNotes = "Logged rewatch from Search")
                                } else {
                                    repository.markWatched(target, rating = r, review = target.userReview)
                                }
                            },
                            onRateChange = { newRating ->
                                if (existingItem != null) {
                                    repository.updateRating(existingItem.id, newRating)
                                } else {
                                    repository.markWatched(movie, rating = newRating, review = "")
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // Zero-State: Search History & Sample Social URLs / Queries
            Column {
                if (searchHistory.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Searches", style = MaterialTheme.typography.titleMedium, color = FrostWhite)
                        TextButton(onClick = { searchHistory = emptyList() }) {
                            Text("Clear All", color = SteelBlue)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        searchHistory.forEach { item ->
                            GlassPillButton(
                                text = item,
                                onClick = { query = item }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieFilter,
                            contentDescription = "Search or Paste Link",
                            tint = ElectricCyan,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Search Any Movie or Paste Your Own Link",
                            style = MaterialTheme.typography.titleMedium,
                            color = FrostWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Type a movie title in the search bar above, or paste your own YouTube, Instagram, Reddit, or article URL to extract titles with AI.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SteelBlue,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExtractedMentionCard(
    mention: ExtractedMediaMention,
    libraryStatus: String,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onConfirmUncertain: () -> Unit,
    onAddToWatchlist: () -> Unit,
    onMarkWatched: () -> Unit,
    onSkip: () -> Unit,
    onViewDetails: () -> Unit
) {
    val movie = mention.movie
    val confidencePct = (mention.confidence * 100).toInt()
    val roleColor = when (mention.mentionRole) {
        "Recommended" -> Color(0xFF00F5D4)
        "Reviewed" -> ElectricCyan
        "Criticised" -> Color(0xFFFF5252)
        "Compared" -> Color(0xFF7209B7)
        else -> IcyBlue
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        onClick = onViewDetails
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ElectricCyan,
                        uncheckedColor = SteelBlue
                    )
                )

                MoviePosterImage(
                    posterUrl = movie.posterUrl,
                    title = movie.title,
                    signatureColorHex = movie.signatureColorHex,
                    modifier = Modifier
                        .width(78.dp)
                        .height(114.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                    releaseYear = movie.releaseYear,
                    movieId = movie.id
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Role Badge + Match Confidence + Library Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(roleColor.copy(alpha = 0.18f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = mention.mentionRole.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
                                color = roleColor,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "$confidencePct% Match • $libraryStatus",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (mention.isUncertain && !mention.isConfirmedByUser) ElectricCyan else IcyBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${movie.title} (${movie.releaseYear})",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp),
                        color = FrostWhite
                    )

                    Text(
                        text = "${movie.contentType} • Directed by ${movie.director}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SteelBlue
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (mention.extractedContext.isNotBlank()) {
                        Text(
                            text = mention.extractedContext,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = FontStyle.Italic,
                                fontSize = 11.5.sp
                            ),
                            color = FrostWhite.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = mention.whyMentioned,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = IcyBlue
                    )
                }
            }

            // Uncertain match confirmation bar
            if (mention.isUncertain && !mention.isConfirmedByUser) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricCyan.copy(alpha = 0.16f))
                        .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️ Ambiguous / Uncertain match ($confidencePct%) — Confirm before saving",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00F5D4),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onConfirmUncertain) {
                        Text("Confirm Match", color = ElectricCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Add to Watchlist, Mark as Watched, Skip, View Details
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassPillButton(
                    text = "Save",
                    icon = Icons.Default.BookmarkAdd,
                    isActive = libraryStatus == "Watchlist",
                    onClick = onAddToWatchlist
                )
                GlassPillButton(
                    text = "Watched",
                    icon = Icons.Default.CheckCircle,
                    isActive = libraryStatus == "Watched",
                    onClick = onMarkWatched
                )
                GlassPillButton(
                    text = "Skip",
                    icon = Icons.Default.Close,
                    onClick = onSkip
                )
                GlassPillButton(
                    text = "Open",
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    onClick = onViewDetails
                )
            }
        }
    }
}

@Composable
private fun SocialDiscoveryRecordItem(
    record: SocialDiscoveryRecord,
    onReopen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MidnightNavy.copy(alpha = 0.7f))
            .border(1.dp, ElectricCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable { onReopen() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${record.platform} • ${record.dateSaved}",
                style = MaterialTheme.typography.labelMedium,
                color = ElectricCyan
            )
            Text(
                text = record.originalUrl,
                style = MaterialTheme.typography.bodySmall,
                color = FrostWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Matched: ${record.matchedTitles.joinToString(", ") { it.movie.title }}",
                style = MaterialTheme.typography.labelSmall,
                color = IcyBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("Revisit →", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
    }
}

@Composable
fun SearchResultCard(
    movie: MovieItem,
    isSaved: Boolean = false,
    onClick: () -> Unit,
    onAdd: () -> Unit = {},
    onMarkWatched: () -> Unit = {},
    onIncrementRewatch: () -> Unit = {},
    onRateChange: (Int) -> Unit = {}
) {
    val sigColor = remember(movie.signatureColorHex) {
        try {
            Color(android.graphics.Color.parseColor(movie.signatureColorHex))
        } catch (e: Exception) {
            ElectricCyan
        }
    }
    val isWatched = movie.userStatus == "Watched"
    val isWatchlist = movie.userStatus == "Watchlist" || (isSaved && !isWatched)
    var sliderVal by remember(movie.id, movie.userRating) {
        mutableStateOf(if (movie.userRating > 0) movie.userRating.toFloat() else 8f)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp),
        cornerRadius = 24.dp,
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            MoviePosterImage(
                posterUrl = movie.posterUrl,
                title = movie.title,
                signatureColorHex = movie.signatureColorHex,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.16f,
                showFallbackTitle = false,
                releaseYear = movie.releaseYear,
                movieId = movie.id
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MidnightNavy.copy(alpha = 0.95f),
                                MidnightNavy.copy(alpha = 0.85f),
                                sigColor.copy(alpha = 0.12f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoviePosterImage(
                    posterUrl = movie.posterUrl,
                    title = movie.title,
                    signatureColorHex = movie.signatureColorHex,
                    modifier = Modifier
                        .width(92.dp)
                        .height(136.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop,
                    releaseYear = movie.releaseYear,
                    movieId = movie.id
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 4.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = movie.title.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, letterSpacing = 0.8.sp),
                            color = FrostWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(sigColor.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = movie.contentType,
                                color = sigColor,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${movie.releaseYear} • ${if (movie.director.isNotBlank()) movie.director else movie.language} • ${movie.genres.take(2).joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = SteelBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Three Buttons Directly Below Movie Title: Watchlist, Watched, Rewatch (with count inside)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassPillButton(
                            text = if (isWatchlist) "Saved" else "Save",
                            icon = if (isWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkAdd,
                            isActive = isWatchlist,
                            compact = true,
                            onClick = onAdd,
                            modifier = Modifier.weight(1f)
                        )
                        GlassPillButton(
                            text = if (isWatched) "Watched" else "Watch",
                            icon = Icons.Default.CheckCircle,
                            isActive = isWatched,
                            compact = true,
                            onClick = onMarkWatched,
                            modifier = Modifier.weight(1f)
                        )
                        RewatchSymbolCounterButton(
                            rewatchCount = if (isWatched) maxOf(1, movie.rewatchCount) else movie.rewatchCount,
                            size = 44.dp,
                            isWatched = isWatched,
                            compact = true,
                            onClick = onIncrementRewatch,
                            testTag = "search_rewatch_btn_${movie.id}"
                        )
                    }

                    // 1 to 10 Rating Slider ONLY appears after Watched is clicked
                    AnimatedVisibility(visible = isWatched) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "My Rating: ${sliderVal.toInt()}/10",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                                    color = ElectricCyan
                                )
                                Text(
                                    text = "1 — 10",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = IcyBlue
                                )
                            }
                            Slider(
                                value = sliderVal,
                                onValueChange = { sliderVal = kotlin.math.round(it) },
                                onValueChangeFinished = { onRateChange(sliderVal.toInt()) },
                                valueRange = 1f..10f,
                                steps = 8,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(22.dp)
                                    .testTag("search_card_rating_slider_${movie.id}"),
                                colors = SliderDefaults.colors(
                                    thumbColor = ElectricCyan,
                                    activeTrackColor = ElectricCyan,
                                    inactiveTrackColor = SteelBlue.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

