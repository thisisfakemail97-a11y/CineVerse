package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class CastMember(
    val name: String = "",
    val role: String = "",
    val avatarUrl: String = ""
)

data class RatingSource(
    val sourceName: String = "", // IMDb, Rotten Tomatoes, TMDB, Metacritic
    val ratingValue: String = "", // e.g. "8.8/10", "94%", "87%"
    val reviewCount: String = ""  // e.g. "450k reviews"
)

data class MovieItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val originalTitle: String = "",
    val releaseYear: Int = 2024,
    val releaseDate: String = "", // e.g. "March 1, 2024"
    val contentType: String = "Movie", // Movie, Web Series, TV Show, Documentary, Anime
    val posterUrl: String = "",
    val bannerUrl: String = "",
    val synopsis: String = "",
    val detailedSpoilerPlot: String = "", // Twist/ending explanation protected by Spoiler Shield
    val runtime: String = "", // e.g. "2h 46m" or "4 Seasons • 34 Episodes"
    val genres: List<String> = emptyList(),
    val language: String = "English",
    val country: String = "USA",
    val ageRating: String = "UA 16+",
    val publicRating: Double = 8.5,
    val publicRatingSource: String = "IMDb 8.5 / 10",
    val ratingsList: List<RatingSource> = emptyList(),
    val director: String = "",
    val directorAvatarUrl: String = "",
    val writers: List<String> = emptyList(),
    val mainCast: List<String> = emptyList(),
    val castMembers: List<CastMember> = emptyList(),
    val productionCompanies: List<String> = emptyList(),
    val awards: String = "",
    val trailerUrl: String = "",
    val youtubeFullMovieUrl: String = "",
    val streamingProviders: List<StreamingProvider> = emptyList(),
    val soundtracks: List<Soundtrack> = emptyList(),
    val soundtrackInfo: SoundtrackInfo? = null,
    val inlineQuote: Quote = Quote("", ""),
    val quotesList: List<Quote> = emptyList(),
    val trivia: List<String> = emptyList(),
    val connections: List<String> = emptyList(),
    val similarTitles: List<String> = emptyList(),
    val isTrending: Boolean = false,
    val isUpcoming: Boolean = false,
    val isVerified: Boolean = true,
    val aiSummary: String = "",
    val sourceLink: String = "",
    val userStatus: String = "", // "", "Watchlist", "Watched", "Dropped"
    val userRating: Int = 0, // 0-10
    val userReview: String = "",
    val isUserReviewSpoiler: Boolean = false,
    val rewatchCount: Int = 0,
    val lastWatchedTimestamp: Long = 0L,
    val composer: String = "",
    val signatureColorHex: String = "#54C8F5"
) {
    val effectiveQuotes: List<Quote>
        get() = if (quotesList.isNotEmpty()) quotesList else if (inlineQuote.text.isNotBlank()) listOf(inlineQuote) else emptyList()

    /**
     * Parses verified runtime string into minutes (e.g., "2h 49m" -> 169, "10 Episodes • 24m" -> 240).
     */
    val runtimeMinutes: Int
        get() {
            if (runtime.isBlank()) return 0
            val epMatch = Regex("""(\d+)\s*Episodes?\s*[•·\-]?\s*(\d+)\s*m""", RegexOption.IGNORE_CASE).find(runtime)
            if (epMatch != null) {
                val eps = epMatch.groupValues[1].toIntOrNull() ?: 1
                val mins = epMatch.groupValues[2].toIntOrNull() ?: 24
                return eps * mins
            }
            var total = 0
            val hMatch = Regex("""(\d+)\s*h""", RegexOption.IGNORE_CASE).find(runtime)
            val mMatch = Regex("""(\d+)\s*m""", RegexOption.IGNORE_CASE).find(runtime)
            if (hMatch != null) total += (hMatch.groupValues[1].toIntOrNull() ?: 0) * 60
            if (mMatch != null) total += (mMatch.groupValues[1].toIntOrNull() ?: 0)
            return total
        }
}

data class StreamingProvider(
    val name: String = "", // Netflix, Prime Video, JioCinema, Disney+ Hotstar, Apple TV
    val type: String = "Subscription", // Subscription, Rental, Purchase
    val url: String = "",
    val packageName: String = "", // e.g. "com.netflix.mediaclient", "com.amazon.avod.thirdpartyclient"
    val lastChecked: String = "Nov 12, 2026",
    val regions: List<String> = listOf("India", "USA", "UK", "Global")
) {
    val availabilityType: String get() = type
}

data class Soundtrack(
    val title: String = "",
    val composer: String = "",
    val duration: String = ""
)

data class SoundtrackTrack(
    val title: String = "",
    val artistOrComposer: String = "",
    val duration: String = "",
    val isRecognisableSong: Boolean = false,
    val officialVideoUrl: String = "",
    val sceneDescription: String = ""
) {
    val artist: String get() = artistOrComposer
}

data class SoundtrackInfo(
    val albumTitle: String = "",
    val composer: String = "",
    val backgroundScoreSummary: String = "",
    val tracks: List<SoundtrackTrack> = emptyList(),
    val spotifyUrl: String = "",
    val appleMusicUrl: String = "",
    val youtubeMusicUrl: String = "",
    val officialMusicVideoUrl: String = "",
    val isComplete: Boolean = true
) {
    val scoreDescription: String get() = backgroundScoreSummary
    val recognisableSongs: List<SoundtrackTrack> get() = tracks
    val authorizedSpotifyUrl: String get() = spotifyUrl
    val authorizedAppleMusicUrl: String get() = appleMusicUrl
    val authorizedYoutubeMusicUrl: String get() = youtubeMusicUrl
    val isVerifiedComplete: Boolean get() = isComplete && tracks.isNotEmpty()
}

data class ActiveSoundtrackPlayState(
    val track: SoundtrackTrack? = null,
    val soundtrackInfo: SoundtrackInfo? = null,
    val movieTitle: String = "",
    val moviePosterUrl: String = "",
    val isPlaying: Boolean = false,
    val progressMs: Long = 0L,
    val durationMs: Long = 180000L
)

data class Quote(
    val text: String = "",
    val speaker: String = "",
    val isSpoiler: Boolean = false
) {
    val quoteText: String get() = text
}

data class LibraryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val titleId: String = "",
    val title: String = "",
    val posterUrl: String = "",
    val releaseYear: Int = 2024,
    val contentType: String = "Movie",
    val status: String = "Watchlist", // Watchlist, Watched, Dropped
    val personalRating: Int = 0, // out of 10
    val personalReview: String = "",
    val notes: String = "",
    val isFavorite: Boolean = false,
    val rewatchCount: Int = 1,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ViewingHistory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val titleId: String = "",
    val movieTitle: String = "",
    val posterUrl: String = "",
    val watchDate: String = "",
    val rewatchSequence: Int = 1,
    val sessionRating: Int = 0,
    val sessionNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class DiaryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val titleId: String = "",
    val movieTitle: String = "",
    val posterUrl: String = "",
    val entryDate: String = "",
    val rating: Int = 0,
    val review: String = "",
    val thoughts: String = "",
    val isSpoiler: Boolean = false,
    val tags: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// ==================== PHASE 5: AI DISCOVERY, CONNECTIONS & SOCIAL EXTRACTION ====================

data class AiRecommendationItem(
    val movie: MovieItem = MovieItem(),
    val matchReason: String = "",
    val categoryTag: String = "Recommended",
    val sourceAnchorTitle: String = "",
    val confidenceScore: Double = 0.94
)

data class MovieConnectionItem(
    val movie: MovieItem = MovieItem(),
    val connectionCategory: String = "FACTUAL", // "FACTUAL" or "CREATIVE"
    val connectionBadge: String = "",           // e.g. "Same Director", "Same Actor", "Shared Universe", "Similar Themes", "Similar Storytelling"
    val explanation: String = ""
)

data class ExtractedMediaMention(
    val id: String = "",
    val movie: MovieItem = MovieItem(),
    val mentionRole: String = "Recommended", // Recommended, Reviewed, Criticised, Compared, Casually mentioned, Unclear
    val extractedContext: String = "",
    val whyMentioned: String = "",
    val confidence: Double = 0.92,
    val isUncertain: Boolean = false,
    val isConfirmedByUser: Boolean = false,
    val mentionCount: Int = 1
)

data class SocialExtractorResult(
    val url: String = "",
    val platform: String = "", // Instagram, YouTube, YouTube Shorts, Reddit, X, Facebook, TikTok, Public Article, Manual Text
    val urlType: String = "",  // Video, Short, Reel/Post, Thread, Article, Manual Paste
    val isAccessible: Boolean = true,
    val isManualInput: Boolean = false,
    val statusMessage: String = "",
    val extractedRawText: String = "",
    val mentions: List<ExtractedMediaMention> = emptyList()
)

data class SocialDiscoveryRecord(
    val id: String = "",
    val originalUrl: String = "",
    val platform: String = "",
    val dateSaved: String = "",
    val extractedExcerpt: String = "",
    val matchedTitles: List<ExtractedMediaMention> = emptyList(),
    val selectedActions: Map<String, String> = emptyMap() // titleId -> "Watchlist" / "Watched" / "Skipped"
)

data class RecommendationFeedback(
    val titleId: String = "",
    val movieTitle: String = "",
    val feedbackType: String = "", // "More like this", "Less like this", "Not interested", "Already watched"
    val timestamp: Long = System.currentTimeMillis()
)

// ==================== PHASE 6: CINEMA WRAPPED, RELEASE RADAR & MULTI-CRITERIA FILTERS ====================

data class ReleaseRadarItem(
    val id: String = "",
    val title: String = "",
    val posterUrl: String = "",
    val contentType: String = "Movie", // Movie, Web Series, TV Show, Documentary, Anime
    val releaseDate: String = "",
    val releaseYear: Int = 2026,
    val region: String = "India & Global",
    val trailerUrl: String = "",
    val releaseStatus: String = "Confirmed", // "Confirmed", "Tentative", "Rumoured"
    val sourceLink: String = "",
    val sourceName: String = "Official Studio Announcement",
    val lastChecked: String = "Nov 12, 2026",
    val synopsis: String = "",
    val director: String = "",
    val genres: List<String> = emptyList(),
    val isFollowed: Boolean = false
) {
    val sourceUrl: String get() = sourceLink
}

data class CinemaWrappedReport(
    val period: String = "All Time", // "Monthly", "Yearly", "All Time"
    val totalMoviesWatched: Int = 0,
    val totalSeriesWatched: Int = 0,
    val totalDocumentariesWatched: Int = 0,
    val totalAnimeWatched: Int = 0,
    val totalViewingHours: Double = 0.0,
    val totalViewingMinutes: Int = 0,
    val totalViewingSessions: Int = 0,
    val topGenres: List<Pair<String, Int>> = emptyList(),
    val favouriteLanguages: List<Pair<String, Int>> = emptyList(),
    val favouriteDirectors: List<Pair<String, Int>> = emptyList(),
    val favouriteActors: List<Pair<String, Int>> = emptyList(),
    val highestRatedTitles: List<MovieItem> = emptyList(),
    val lowestRatedTitles: List<MovieItem> = emptyList(),
    val mostRewatchedTitles: List<MovieItem> = emptyList(),
    val monthlyActivity: Map<String, Int> = emptyMap(),
    val mostActiveMonth: String = "Insufficient Data",
    val ratingTrendSummary: String = "",
    val aiObservations: List<String> = emptyList(),
    val hasSufficientData: Boolean = false
) {
    val viewingPatternSummary: String
        get() = if (mostActiveMonth != "Insufficient Data") {
            "Most active month: $mostActiveMonth ($totalViewingSessions viewing sessions logged)."
        } else {
            "Log more watched titles to unlock monthly viewing activity trends."
        }
    val ratingEvolutionSummary: String get() = ratingTrendSummary
    val hasInsufficientData: Boolean get() = !hasSufficientData
    val insufficientDataMessage: String =
        "Insufficient viewing data (${totalMoviesWatched + totalSeriesWatched + totalDocumentariesWatched + totalAnimeWatched} watched). Log at least 2 watched titles with ratings to unlock AI viewing pattern observations."
}

data class MovieFilterCriteria(
    val contentType: String = "All", // All, Movie, Web Series, TV Show, Documentary, Anime
    val genre: String = "All",
    val language: String = "All",
    val country: String = "All",
    val minYear: Int = 1950,
    val maxYear: Int = 2026,
    val maxRuntimeMinutes: Int = 300, // 300 = Any
    val minPersonalRating: Int = 0,   // 0 = Any
    val minPublicRating: Double = 0.0,
    val minExternalRating: Double = 0.0,
    val externalRatingSource: String = "All", // All, IMDb, Rotten Tomatoes
    val libraryStatus: String = "All", // All, Watchlist, Watched, Dropped
    val sortBy: String = "Title" // Title, Release Year, Personal Rating, External Rating, Runtime
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (contentType != "All") count++
            if (genre != "All") count++
            if (language != "All") count++
            if (country != "All") count++
            if (minYear > 1950 || maxYear < 2026) count++
            if (maxRuntimeMinutes < 300) count++
            if (minPersonalRating > 0) count++
            if (minPublicRating > 0.0 || minExternalRating > 0.0 || externalRatingSource != "All") count++
            if (libraryStatus != "All") count++
            return count
        }

    fun hasActiveFilters(): Boolean = activeFilterCount > 0
}
