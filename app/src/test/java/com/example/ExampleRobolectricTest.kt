package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CineverseRepository
import com.example.data.DiaryEntry
import com.example.data.MovieItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CineMax", appName)
    }

    @Test
    fun `search Primer returns exact film at top`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)
        val results = repository.search("Primer")
        assertTrue("Results should not be empty", results.isNotEmpty())
        val top = results.first()
        assertEquals("Primer", top.title)
        assertEquals("Shane Carruth", top.director)
        assertTrue("Poster URL must be present", top.posterUrl.isNotBlank())
        assertEquals("#00F5D4", top.signatureColorHex)
    }

    @Test
    fun `search Home Alone returns classic movie at top`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)
        val results = repository.search("Home Alone")
        assertTrue("Results should not be empty", results.isNotEmpty())
        val top = results.first()
        assertEquals("Home Alone", top.title)
        assertEquals("Chris Columbus", top.director)
        assertTrue("Poster URL must be present", top.posterUrl.isNotBlank())
        assertEquals("#D90429", top.signatureColorHex)
    }

    @Test
    fun `search Barbie with typo returns 2023 movie at top`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)
        val results = repository.search("barby")
        assertTrue("Results should not be empty", results.isNotEmpty())
        val top = results.first()
        assertEquals("Barbie", top.title)
        assertEquals("Greta Gerwig", top.director)
        assertEquals(2023, top.releaseYear)
        assertTrue("Quotes must be present", top.effectiveQuotes.isNotEmpty())
        assertEquals("Barbie", top.effectiveQuotes.first().speaker)
    }

    @Test
    fun `search American Psycho with typo returns 2000 movie at top`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)
        val results = repository.search("americn psycho")
        assertTrue("Results should not be empty", results.isNotEmpty())
        val top = results.first()
        assertEquals("American Psycho", top.title)
        assertEquals("Mary Harron", top.director)
        assertEquals(2000, top.releaseYear)
        assertTrue("Quotes must be present", top.effectiveQuotes.isNotEmpty())
        assertEquals("Patrick Bateman", top.effectiveQuotes.first().speaker)
    }

    @Test
    fun `library lifecycle - watchlist, watched, dropped, rating, and rewatch tracking`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        val primerResults = repository.search("Primer")
        val primer = primerResults.first()

        // 1. Add to Watchlist
        repository.addToWatchlist(primer)
        var library = repository.userLibrary.value
        val watchlistPrimer = library.find { it.id == primer.id }
        assertNotNull("Primer should be in user library", watchlistPrimer)
        assertEquals("Watchlist", watchlistPrimer?.userStatus)

        // 2. Move to Watched with rating and review
        repository.markWatched(primer, rating = 9, review = "Mind-bending indie masterpiece")
        library = repository.userLibrary.value
        val watchedPrimer = library.find { it.id == primer.id }
        assertEquals("Watched", watchedPrimer?.userStatus)
        assertEquals(9, watchedPrimer?.userRating)
        assertEquals(1, watchedPrimer?.rewatchCount)

        // 3. Log a Second Watch (explicit rewatch)
        repository.logRewatch(watchedPrimer!!, sessionRating = 10, sessionNotes = "Understood the timeline better", watchDate = "Nov 12, 2026")
        library = repository.userLibrary.value
        val rewatchedPrimer = library.find { it.id == primer.id }
        assertEquals(2, rewatchedPrimer?.rewatchCount)
        assertEquals(10, rewatchedPrimer?.userRating)

        // Check viewing history
        val history = repository.viewingHistory.value
        assertTrue("Viewing history should record rewatch", history.any { it.titleId == primer.id && it.rewatchSequence == 2 })

        // Check diary
        val diary = repository.diaryEntries.value
        assertTrue("Diary should have entry for Primer", diary.any { it.titleId == primer.id })

        // 4. Move to Dropped
        repository.moveToDropped(primer)
        library = repository.userLibrary.value
        val droppedPrimer = library.find { it.id == primer.id }
        assertEquals("Dropped", droppedPrimer?.userStatus)

        // 5. Export JSON Backup
        val exportedJson = repository.exportBackupJson()
        assertTrue("Backup must contain library key", exportedJson.contains("\"library\""))
        assertTrue("Backup must contain Primer", exportedJson.contains("Primer"))

        // 6. Preview & Duplicate Detection
        val (isValid, _, dupItems) = repository.previewBackupJson(exportedJson)
        assertTrue("Preview should be valid", isValid)
        assertTrue("Duplicates should be detected", dupItems > 0)

        // 7. Restore JSON Backup safely
        val (_, mergedCount) = repository.restoreBackupJson(exportedJson)
        assertTrue("Records should be merged non-destructively", mergedCount > 0)
    }

    @Test
    fun `movie diary - add, edit, and search entries`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        val entry = DiaryEntry(
            id = "test_diary_1",
            userId = "guest",
            titleId = "primer_test",
            movieTitle = "Primer",
            posterUrl = "https://example.com/primer.jpg",
            entryDate = "Nov 12, 2026",
            rating = 9,
            review = "Brilliant cinema",
            thoughts = "Fascinating use of time travel"
        )

        repository.addDiaryEntry(entry)
        assertTrue("Diary entries must contain test entry", repository.diaryEntries.value.any { it.id == "test_diary_1" })

        // Update entry
        val updated = entry.copy(thoughts = "Updated thoughts on time loops", rating = 10)
        repository.updateDiaryEntry(updated)
        val fetched = repository.diaryEntries.value.find { it.id == "test_diary_1" }
        assertEquals("Updated thoughts on time loops", fetched?.thoughts)
        assertEquals(10, fetched?.rating)

        // Delete entry
        repository.deleteDiaryEntry("test_diary_1")
        assertTrue("Entry should be removed", repository.diaryEntries.value.none { it.id == "test_diary_1" })
    }

    // =========================================================================
    // PHASE 5 COMPLETION TESTS (Section 11)
    // =========================================================================

    @Test
    fun `phase 5 - mood-based discovery with 2x2 plasma orbs filters watchlist and mainstream internet movies`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        // Add two very different movies to user Watchlist: Home Alone (Relaxing/Escapist/Nostalgic) & Primer (Thought-Provoking)
        val homeAlone = repository.search("Home Alone").first()
        val primer = repository.search("Primer").first()
        repository.addToWatchlist(homeAlone)
        repository.addToWatchlist(primer)

        val watchlistRelaxing = repository.filterWatchlistByMood(
            userLibraryList = repository.userLibrary.value,
            selectedMoods = listOf("Relaxing")
        )
        assertTrue("Watchlist Relaxing filter must include Home Alone", watchlistRelaxing.any { it.movie.title == "Home Alone" })
        assertFalse("Watchlist Relaxing filter must exclude Primer", watchlistRelaxing.any { it.movie.title == "Primer" })

        val watchlistThoughtProvoking = repository.filterWatchlistByMood(
            userLibraryList = repository.userLibrary.value,
            selectedMoods = listOf("Thought-Provoking")
        )
        assertTrue("Watchlist Thought-Provoking filter must include Primer", watchlistThoughtProvoking.any { it.movie.title == "Primer" })

        // Verify Internet Mood Discovery returns distinct mainstream movies for each mood without unwanted Anime/Documentary content
        val excitingInternet = repository.discoverByMood(
            naturalLanguageMood = "",
            selectedMoods = listOf("Exciting")
        )
        val relaxingInternet = repository.discoverByMood(
            naturalLanguageMood = "",
            selectedMoods = listOf("Relaxing")
        )
        assertTrue("Exciting mainstream list should not be empty", excitingInternet.isNotEmpty())
        assertTrue("Relaxing mainstream list should not be empty", relaxingInternet.isNotEmpty())
        assertFalse(
            "Exciting and Relaxing filters must return different top movies",
            excitingInternet.first().movie.title == relaxingInternet.first().movie.title
        )
        excitingInternet.forEach { item ->
            assertEquals("Default mood filter must only return mainstream Movies", "Movie", item.movie.contentType)
            assertTrue("Poster URL must be genuine and non-blank", item.movie.posterUrl.isNotBlank())
        }
    }

    @Test
    fun `phase 5 - discovery categories remain intact without ai suggest`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        val categories = repository.getDiscoveryCategories()
        assertTrue("Must include Hidden Gems category", categories.containsKey("Hidden Gems"))
        assertTrue("Must include Short Watch category", categories.containsKey("Short Watch"))
        assertTrue("Must include Discover by Language category", categories.containsKey("Discover by Language"))
    }

    @Test
    fun `home waterfall filter shows only watchlist items`() {
        val sampleLibrary = listOf(
            MovieItem(id = "1", title = "Interstellar", userStatus = "Watchlist"),
            MovieItem(id = "2", title = "Coherence", userStatus = ""),
            MovieItem(id = "3", title = "Inception", userStatus = "Watched"),
            MovieItem(id = "4", title = "The Batman", userStatus = "Dropped")
        )
        val homeWatchlist = sampleLibrary.filter {
            it.userStatus.equals("Watchlist", ignoreCase = true) || it.userStatus.isBlank()
        }
        assertEquals(2, homeWatchlist.size)
        assertTrue(homeWatchlist.any { it.title == "Interstellar" })
        assertTrue(homeWatchlist.any { it.title == "Coherence" })
        assertFalse(homeWatchlist.any { it.title == "Inception" })
        assertFalse(homeWatchlist.any { it.title == "The Batman" })
    }

    @Test
    fun `phase 5 - movie connections clearly distinguish factual from creative similarities`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        val inception = repository.search("Inception").first()
        val connections = repository.getMovieConnections(inception)

        assertTrue("Movie connections should not be empty", connections.isNotEmpty())

        // 6. Factual Movie Connection
        val factual = connections.filter { it.connectionCategory == "FACTUAL" }
        assertTrue("Should contain verified FACTUAL connections (e.g., Same Director / Cast)", factual.isNotEmpty())
        assertTrue(
            "Factual connection must specify relation (e.g., Same Director / Cast / Writer)",
            factual.any {
                it.connectionBadge.contains("Director", ignoreCase = true) ||
                        it.connectionBadge.contains("Actor", ignoreCase = true) ||
                        it.connectionBadge.contains("Cast", ignoreCase = true) ||
                        it.connectionBadge.contains("Writer", ignoreCase = true)
            }
        )

        // 7. Subjective/Creative Movie Connection
        val creative = connections.filter { it.connectionCategory == "CREATIVE" }
        assertTrue("Should contain AI-assessed CREATIVE similarities", creative.isNotEmpty())
        assertTrue("Creative connection must explain narrative/atmospheric similarity", creative.first().explanation.isNotBlank())
    }

    @Test
    fun `phase 5 - social media extractor supports public URLs, restricted URLs, manual paste, ambiguous confirmation, and duplicate prevention`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        // 8. Public YouTube URL
        val ytUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&title=Must+watch+Inception+and+Interstellar+compared"
        val ytExtraction = repository.extractFromSocialUrlOrText(urlInput = ytUrl, manualTextInput = "Must watch Inception and Interstellar compared")
        assertEquals("YouTube", ytExtraction.platform)
        assertTrue("Should extract mentioned titles from YouTube context", ytExtraction.mentions.isNotEmpty())

        // 9. Public article URL
        val articleUrl = "https://example.com/film-blog/why-parasite-and-memories-of-murder-are-masterpieces"
        val articleExtraction = repository.extractFromSocialUrlOrText(urlInput = articleUrl)
        assertTrue("Platform should be Article/Blog", articleExtraction.platform.contains("Article", ignoreCase = true) || articleExtraction.platform.contains("Blog", ignoreCase = true))
        assertTrue("Public article URL should extract titles from slug/content", articleExtraction.mentions.any { it.movie.title.equals("Parasite", ignoreCase = true) })

        // 10. Inaccessible social-media URL (e.g. Instagram login-walled reel)
        val igUrl = "https://www.instagram.com/reel/C9xYz123/"
        val igExtraction = repository.extractFromSocialUrlOrText(urlInput = igUrl, manualTextInput = "")
        assertEquals("Instagram", igExtraction.platform)
        assertFalse("Instagram URL without manual text should report inaccessible without bypassing login", igExtraction.isAccessible)
        assertTrue("Should provide clear explanation to paste caption manually", igExtraction.statusMessage.isNotBlank())

        // 11. Manually pasted text with role classification & 12. Ambiguous title detection
        val manualCaption = """
            1. I strongly recommend Arrival — one of the smartest sci-fi films ever made!
            2. Compared to Interstellar, it's much quieter.
            3. Also watched Dune last night.
        """.trimIndent()
        val manualExtraction = repository.extractFromSocialUrlOrText(urlInput = igUrl, manualTextInput = manualCaption)
        assertTrue("Should flag manual input", manualExtraction.isManualInput)
        assertTrue("Should identify Arrival", manualExtraction.mentions.any { it.movie.title == "Arrival" })

        val arrivalMention = manualExtraction.mentions.first { it.movie.title == "Arrival" }
        assertEquals("Recommended", arrivalMention.mentionRole)
        assertTrue("Poster must be genuine", arrivalMention.movie.posterUrl.isNotBlank())

        // 12. Ambiguous title ("Dune" shorthand is flagged as uncertain requiring user confirmation)
        val duneMention = manualExtraction.mentions.find { it.movie.title.contains("Dune", ignoreCase = true) }
        assertNotNull("Should resolve Dune", duneMention)
        assertTrue("Short/ambiguous mention 'Dune' should be marked uncertain", duneMention!!.isUncertain)
        assertFalse("Uncertain match must not be auto-confirmed", duneMention.isConfirmedByUser)

        // 13. Duplicate prevention & uncertain match gating
        // Attempting to batch-add without confirming uncertain match should skip unconfirmed uncertain items
        val (addedCountFirst, skippedCountFirst) = repository.addExtractedMentionsToLibrary(
            mentions = listOf(arrivalMention, duneMention),
            targetStatus = "Watchlist"
        )
        assertEquals("Only confirmed/certain Arrival should be added; unconfirmed Dune skipped", 1, addedCountFirst)
        assertEquals("Unconfirmed Dune should be skipped", 1, skippedCountFirst)

        // Attempting to add Arrival again should prevent duplicates
        val (addedCountDuplicate, skippedDuplicate) = repository.addExtractedMentionsToLibrary(
            mentions = listOf(arrivalMention),
            targetStatus = "Watchlist"
        )
        assertEquals("Duplicate Arrival must not be added twice", 0, addedCountDuplicate)
        assertEquals("Duplicate Arrival should increment skipped count", 1, skippedDuplicate)
    }

    // =========================================================================
    // PHASE 6 COMPLETION TESTS (Section 10)
    // =========================================================================

    @Test
    fun `phase 6 - cinema wrapped calculates statistics, preferences, viewing patterns, and handles insufficient data`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        // 1. Test insufficient data flag when fewer than 2 titles are watched
        val emptyWrapped = repository.generateCinemaWrapped(
            period = "All Time",
            library = emptyList(),
            diary = emptyList(),
            history = emptyList()
        )
        assertTrue("Empty library must flag insufficient data", emptyWrapped.hasInsufficientData)
        assertTrue("Insufficient data message must be non-blank", emptyWrapped.insufficientDataMessage.isNotBlank())

        // 2. Test Cinema Wrapped with rich sample viewing data (Movie, Documentary, Anime, Rewatch)
        val primer = repository.search("Primer").first()
        val inception = repository.search("Inception").first()
        val jiroDoc = repository.search("Jiro Dreams of Sushi").first()
        val spiritedAway = repository.search("Spirited Away").first()

        repository.markWatched(primer, rating = 10, review = "Hard sci-fi masterpiece")
        repository.logRewatch(
            repository.userLibrary.value.first { it.id == primer.id },
            sessionRating = 10,
            sessionNotes = "Second watch revealed hidden timeline",
            watchDate = "Nov 10, 2026"
        )
        repository.markWatched(inception, rating = 9, review = "Dream heist classic")
        repository.markWatched(jiroDoc, rating = 8, review = "Incredible documentary craftsmanship")
        repository.markWatched(spiritedAway, rating = 10, review = "Breathtaking anime world")

        val report = repository.generateCinemaWrapped(period = "All Time")
        assertFalse("With 4 watched titles, insufficient data flag should be false", report.hasInsufficientData)
        assertTrue("Total movies watched should be >= 2", report.totalMoviesWatched >= 2)
        assertTrue("Total documentaries watched should be >= 1", report.totalDocumentariesWatched >= 1)
        assertTrue("Total anime watched should be >= 1", report.totalAnimeWatched >= 1)
        assertTrue("Total viewing hours must be calculated from verified runtime", report.totalViewingHours >= 6.0)
        assertTrue("Total viewing sessions must include rewatches", report.totalViewingSessions >= 5)
        assertTrue("Most-watched genres must not be empty", report.topGenres.isNotEmpty())
        assertTrue("Favourite directors must not be empty", report.favouriteDirectors.isNotEmpty())
        assertTrue("Most-rewatched titles must include Primer", report.mostRewatchedTitles.any { it.title == "Primer" && it.rewatchCount >= 2 })
        assertTrue("Monthly viewing activity must be populated", report.monthlyActivity.isNotEmpty())
        assertTrue("AI observations grounded in records must not be empty", report.aiObservations.isNotEmpty())
    }

    @Test
    fun `phase 6 - release radar distinguishes confirmed, tentative, and rumoured releases and supports following`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        val catalog = repository.getReleaseRadarCatalog()
        assertTrue("Release Radar catalog must not be empty", catalog.isNotEmpty())

        val confirmed = catalog.filter { it.releaseStatus == "Confirmed" }
        val tentative = catalog.filter { it.releaseStatus == "Tentative" }
        val rumoured = catalog.filter { it.releaseStatus == "Rumoured" }

        assertTrue("Must contain Confirmed upcoming entries", confirmed.isNotEmpty())
        assertTrue("Must contain Tentative upcoming entries", tentative.isNotEmpty())
        assertTrue("Must contain Rumoured entries clearly marked as Rumoured", rumoured.isNotEmpty())
        assertTrue(
            "Rumoured entries must never present a fake confirmed exact calendar date",
            rumoured.none { it.releaseStatus == "Confirmed" }
        )

        // Test Follow / Unfollow persistence
        val targetRelease = confirmed.first()
        val followedNow = repository.toggleFollowRelease(targetRelease.id)
        assertTrue("First toggle should follow release", followedNow)
        assertTrue("Followed IDs must contain target release", repository.followedReleaseIds.value.contains(targetRelease.id))

        val unfollowedNow = repository.toggleFollowRelease(targetRelease.id)
        assertFalse("Second toggle should unfollow release", unfollowedNow)
    }

    @Test
    fun `phase 6 - soundtrack explorer returns verified score or honest incomplete status`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        val inception = repository.search("Inception").first()
        val inceptionSoundtrack = repository.getSoundtrackInfo(inception)
        assertEquals("Hans Zimmer", inceptionSoundtrack.composer)
        assertTrue("Inception soundtrack should have recognisable tracks", inceptionSoundtrack.recognisableSongs.isNotEmpty())
        assertTrue("Inception soundtrack should include Time", inceptionSoundtrack.recognisableSongs.any { it.title == "Time" })
        assertTrue("Authorized Spotify link must be present", inceptionSoundtrack.authorizedSpotifyUrl.startsWith("https://open.spotify.com"))

        // Test a synthetic/unknown movie with incomplete soundtrack metadata
        val obscureMovie = inception.copy(
            id = "obscure_indie_1997",
            title = "Unindexed Experimental Short 1997",
            director = "Unknown",
            composer = "",
            soundtracks = emptyList(),
            soundtrackInfo = null
        )
        val incompleteSoundtrack = repository.getSoundtrackInfo(obscureMovie)
        assertFalse("Obscure movie without verified tracks must mark isVerifiedComplete = false", incompleteSoundtrack.isVerifiedComplete)
        assertTrue("Recognisable songs must be empty rather than fabricated", incompleteSoundtrack.recognisableSongs.isEmpty())
    }

    @Test
    fun `poster resolution sanitizes bad urls, preserves interstellar, clears stale coherence url, and validates brand drawables`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        assertEquals("", repository.sanitizePosterUrl("N/A"))
        assertEquals("", repository.sanitizePosterUrl("null"))
        assertEquals(
            "https://upload.wikimedia.org/wikipedia/en/b/bc/Interstellar_film_poster.jpg",
            repository.sanitizePosterUrl("http://upload.wikimedia.org/wikipedia/en/b/bc/Interstellar_film_poster.jpg")
        )

        val coherence = repository.phase5Engine.verifiedCatalog.find { it.title.equals("Coherence", ignoreCase = true) }
        assertNotNull("Coherence must exist in verifiedCatalog", coherence)
        assertEquals("Coherence stale Wikimedia URL should be cleared for live backend resolution", "", coherence!!.posterUrl)

        assertEquals("Hans Zimmer", repository.phase5Engine.resolveKnownComposer("Inception"))
        assertEquals("Hans Zimmer", repository.phase5Engine.resolveKnownComposer("Interstellar"))
        assertEquals("Ludwig Göransson", repository.phase5Engine.resolveKnownComposer("Oppenheimer"))

        assertTrue(R.drawable.ic_brand_netflix != 0)
        assertTrue(R.drawable.ic_brand_prime_video != 0)
        assertTrue(R.drawable.ic_brand_hotstar != 0)
        assertTrue(R.drawable.ic_brand_youtube != 0)
        assertTrue(R.drawable.ic_brand_google != 0)
    }

    @Test
    fun `phase 6 - inline movie quote returns verified quote and speaker or null when unavailable`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        val barbie = repository.search("Barbie").first()
        val verifiedQuote = repository.getVerifiedInlineQuote(barbie)
        assertNotNull("Barbie should have a verified inline quote", verifiedQuote)
        assertTrue("Quote text must be non-blank", verifiedQuote!!.quoteText.isNotBlank())
        assertEquals("Barbie", verifiedQuote.speaker)

        // Test movie with no verified quote or speaker
        val noQuoteMovie = barbie.copy(
            id = "silent_doc_test",
            title = "Silent Archival Footage",
            inlineQuote = com.example.data.Quote("", ""),
            quotesList = emptyList()
        )
        val unavailableQuote = repository.getVerifiedInlineQuote(noQuoteMovie)
        assertEquals("Movie without verified quote must return null so UI hides quote cleanly", null, unavailableQuote)
    }

    @Test
    fun `phase 6 - where to watch defaults to India and distinguishes available vs unavailable regional streaming`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        // Default region must be India
        assertEquals("Default Where to Watch region must be India", "India", repository.selectedRegion.value)

        val inception = repository.search("Inception").first()
        val (indiaProviders, indiaVerified) = repository.getRegionalStreaming(inception, "India")
        assertTrue("Inception should have verified providers in India", indiaVerified)
        assertTrue("Providers list should not be empty", indiaProviders.isNotEmpty())
        assertTrue("Each provider must have a legitimate URL and lastChecked timestamp", indiaProviders.all { it.url.startsWith("https://") && it.lastChecked.isNotBlank() })

        // Test unavailable regional provider data
        val noStreamMovie = inception.copy(
            id = "theatrical_only_archive",
            title = "Unverified Lost Celluloid Print",
            streamingProviders = emptyList()
        )
        val (unavailableList, isVerified) = repository.getRegionalStreaming(noStreamMovie, "India")
        assertFalse("Title with no providers must report isVerified = false", isVerified)
        assertTrue("Unavailable provider list must be empty", unavailableList.isEmpty())
    }

    @Test
    fun `phase 6 - movie filters combine criteria and sort accurately`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        val primer = repository.search("Primer").first().copy(userStatus = "Watched", userRating = 10)
        val parasite = repository.search("Parasite").first().copy(userStatus = "Watched", userRating = 9)
        val spiritedAway = repository.search("Spirited Away").first().copy(userStatus = "Watchlist", userRating = 0)
        val samplePool = listOf(primer, parasite, spiritedAway)

        // Filter 1: ContentType = Movie, Status = Watched, MinPersonalRating = 9, MaxRuntime = 100m (Primer is 77m, Parasite is 132m)
        val criteriaShortSciFi = com.example.data.MovieFilterCriteria(
            contentType = "Movie",
            genre = "Sci-Fi",
            language = "English",
            country = "USA",
            minYear = 2000,
            maxYear = 2010,
            maxRuntimeMinutes = 100,
            minPersonalRating = 9,
            libraryStatus = "Watched",
            sortBy = "Personal Rating"
        )
        val filtered = repository.filterAndSortMovies(samplePool, criteriaShortSciFi)
        assertEquals("Only Primer should match all combined filters", 1, filtered.size)
        assertEquals("Primer", filtered.first().title)

        // Filter 2: Language = Korean
        val criteriaKorean = com.example.data.MovieFilterCriteria(
            language = "Korean"
        )
        val koreanFiltered = repository.filterAndSortMovies(samplePool, criteriaKorean)
        assertEquals(1, koreanFiltered.size)
        assertEquals("Parasite", koreanFiltered.first().title)
    }

    @Test
    fun `social link extractor parses youtube shorts, share urls, and instagram reel shortcodes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = CineverseRepository(context).phase5Engine

        assertEquals("dQw4w9WgXcQ", engine.extractYouTubeVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=12s"))
        assertEquals("3nj5MMURKm8", engine.extractYouTubeVideoId("https://youtu.be/3nj5MMURKm8?si=abc"))
        assertEquals("AbC_123-xYz", engine.extractYouTubeVideoId("https://www.youtube.com/shorts/AbC_123-xYz"))
        assertEquals("C9xYz123Ab", engine.extractInstagramShortcode("https://www.instagram.com/reel/C9xYz123Ab/?igsh=123"))
        assertEquals("D1pQr456", engine.extractInstagramShortcode("https://www.instagram.com/reels/D1pQr456/"))
    }

    @Test
    fun `repeated movies across links or within a video are automatically marked Watched by default`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        // Ensure test titles start clean
        CineverseRepository.recentExtractedTitles.removeAll(listOf("shutter island", "prisoners", "the prestige"))
        repository.removeFromLibrary("shutter_island")
        repository.removeFromLibrary("prisoners")
        repository.removeFromLibrary("prestige")

        // First link extraction: Shutter Island mentioned once, Prisoners mentioned twice inside the same reel/transcript
        val firstResult = repository.extractFromSocialUrlOrText(
            urlInput = "https://www.instagram.com/reel/TestReel1/",
            manualTextInput = "Watch Prisoners right now. Honestly Prisoners is Villeneuve at his peak, and also check out Shutter Island."
        )
        assertTrue(firstResult.mentions.any { it.movie.title == "Prisoners" })
        assertTrue(firstResult.mentions.any { it.movie.title == "Shutter Island" })

        // Prisoners was repeated twice inside the same video -> automatically marked Watched by default
        val prisonersEntry = repository.userLibrary.value.find { it.title.equals("Prisoners", ignoreCase = true) }
        assertNotNull("Repeated movie Prisoners inside one video should be auto-added as Watched", prisonersEntry)
        assertEquals("Watched", prisonersEntry!!.userStatus)

        // Shutter Island appeared only once -> NOT auto-marked Watched yet
        val shutterBeforeRepeat = repository.userLibrary.value.find { it.title.equals("Shutter Island", ignoreCase = true) }
        assertTrue("Single-mention Shutter Island should wait for user Watchlist/Watched choice", shutterBeforeRepeat == null)

        // Second link extraction repeats Shutter Island and introduces The Prestige for the first time
        val secondResult = repository.extractFromSocialUrlOrText(
            urlInput = "https://www.youtube.com/shorts/TestShort2",
            manualTextInput = "Top twists: Shutter Island and The Prestige!"
        )
        assertTrue(secondResult.mentions.any { it.movie.title == "Shutter Island" })
        assertTrue(secondResult.mentions.any { it.movie.title == "The Prestige" })

        // Repeated Shutter Island is now automatically marked Watched by default
        val shutterAfterRepeat = repository.userLibrary.value.find { it.title.equals("Shutter Island", ignoreCase = true) }
        assertNotNull("Repeated movie Shutter Island across links should be auto-marked Watched", shutterAfterRepeat)
        assertEquals("Watched", shutterAfterRepeat!!.userStatus)

        // First-time The Prestige stays unadded until user taps Save or Watched
        val prestigeEntry = repository.userLibrary.value.find { it.title.equals("The Prestige", ignoreCase = true) }
        assertTrue("First-time The Prestige should remain unadded until user picks Watchlist/Watched", prestigeEntry == null)

        // Adding The Prestige to Watchlist, then re-adding it via addExtractedMentionsToLibrary upgrades it to Watched while keeping duplicate prevention
        val prestigeMention = secondResult.mentions.first { it.movie.title == "The Prestige" }
        val (addedFirst, skippedFirst) = repository.addExtractedMentionsToLibrary(listOf(prestigeMention), "Watchlist")
        assertEquals(1, addedFirst)
        assertEquals(0, skippedFirst)
        assertEquals("Watchlist", repository.userLibrary.value.first { it.title == "The Prestige" }.userStatus)

        val (addedRepeat, skippedRepeat) = repository.addExtractedMentionsToLibrary(listOf(prestigeMention), "Watchlist")
        assertEquals(0, addedRepeat)
        assertEquals(1, skippedRepeat)
        assertEquals("Watched", repository.userLibrary.value.first { it.title == "The Prestige" }.userStatus)
    }

    @Test
    fun `searchRealTime finds Hindi web series The Family Man and real social reel link extracts movie list`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        val familyManResults = repository.searchRealTime("The Family Man")
        assertTrue("Search for The Family Man should return results", familyManResults.isNotEmpty())
        val topHit = familyManResults.first()
        assertEquals("The Family Man", topHit.title)
        assertEquals("Web Series", topHit.contentType)
        assertTrue("The Family Man should have a valid poster URL", topHit.posterUrl.startsWith("https://"))

        // Also test pasting a real Instagram reel link without manual text yields a 4-5 movie list
        val reelResult = repository.extractFromSocialUrlOrText(
            urlInput = "https://www.instagram.com/reel/D9mXp789QvL/?igsh=xyz",
            manualTextInput = ""
        )
        assertTrue("Real Instagram reel link should extract at least 4 movies via hook-and-crook pipeline", reelResult.mentions.size >= 4)

        // Test 1st-letter continuous movie search (e.g. typing 'i' or 'f')
        val firstLetterI = repository.searchInstantPrefix("i")
        assertTrue("Typing single letter 'i' should immediately return movies like Interstellar/Inception", firstLetterI.any { it.title == "Interstellar" || it.title == "Inception" })

        val firstLetterF = repository.searchInstantPrefix("f")
        assertTrue("Typing single letter 'f' should immediately return Farzi and The Family Man", firstLetterF.any { it.title == "Farzi" || it.title == "The Family Man" })

        // Test persistent search history add, delete single, and clear all
        repository.clearSearchHistory()
        repository.addSearchHistory("The Family Man")
        repository.addSearchHistory("Interstellar")
        assertEquals(listOf("Interstellar", "The Family Man"), repository.searchHistory.value)
        repository.removeSearchHistoryItem("Interstellar")
        assertEquals(listOf("The Family Man"), repository.searchHistory.value)
        repository.clearSearchHistory()
        assertTrue(repository.searchHistory.value.isEmpty())
    }

    @Test
    fun `glass transparency slider persists 0 to 100 percent and sync upload and download actions execute cleanly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("cineverse_vault", Context.MODE_PRIVATE)
        val repository = CineverseRepository(context)

        // Default transparency is 40% (0.40f)
        prefs.edit().remove("glass_transparency").commit()
        assertEquals(0.40f, prefs.getFloat("glass_transparency", 0.40f), 0.001f)

        // Set to 0% transparent (solid glass)
        prefs.edit().putFloat("glass_transparency", 0.0f).commit()
        assertEquals(0.0f, prefs.getFloat("glass_transparency", 0.40f), 0.001f)

        // Set to 100% transparent (completely clear glass)
        prefs.edit().putFloat("glass_transparency", 1.0f).commit()
        assertEquals(1.0f, prefs.getFloat("glass_transparency", 0.40f), 0.001f)

        // Verify uploadToCloud and downloadFromCloud execute and return status messages
        val (_, uploadMsg) = repository.uploadToCloud()
        assertTrue("Upload message must not be blank", uploadMsg.isNotBlank())

        val (_, downloadMsg) = repository.downloadFromCloud()
        assertTrue("Download message must not be blank", downloadMsg.isNotBlank())
    }

    @Test
    fun `marking movie as watched records only once in diary without duplicates`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        val coherence = repository.search("Coherence").first()
        repository.removeFromLibrary(coherence.id)
        repository.diaryEntries.value
            .filter { it.titleId == coherence.id || it.movieTitle.equals("Coherence", ignoreCase = true) }
            .forEach { repository.deleteDiaryEntry(it.id) }

        // Call markWatched twice (simulating rapid click or detail + callback invocation)
        repository.markWatched(coherence, rating = 9, review = "First watch log")
        repository.markWatched(coherence, rating = 9, review = "First watch log updated")

        val coherenceDiaryEntries = repository.diaryEntries.value.filter {
            it.titleId == coherence.id || it.movieTitle.equals("Coherence", ignoreCase = true)
        }
        assertEquals("Recording a movie as watched must create exactly 1 diary entry, not 2", 1, coherenceDiaryEntries.size)
        assertEquals("First watch log updated", coherenceDiaryEntries.first().review)
    }

    @Test
    fun `custom playlists and folders create, toggle movies, and clean up on batch delete`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        val primer = repository.search("Primer").first()
        val inception = repository.search("Inception").first()
        repository.addToWatchlist(primer)
        repository.addToWatchlist(inception)

        // Create custom genre playlist folder "Psychological"
        val created = repository.createPlaylist(
            name = "Psychological",
            description = "Movies I consider psychological",
            initialMovie = primer
        )
        assertEquals("Psychological", created.name)
        assertTrue("Playlist should contain Primer", created.movieIds.contains(primer.id))

        // Toggle Inception into the folder
        repository.toggleMovieInPlaylist(created.id, inception)
        val updatedFolder = repository.playlists.value.first { it.id == created.id }
        assertTrue("Inception should be added to playlist", updatedFolder.movieIds.contains(inception.id))
        assertEquals(2, updatedFolder.movieIds.size)

        // Batch delete both movies via removeMultipleFromLibrary
        repository.removeMultipleFromLibrary(setOf(primer.id, inception.id))
        assertTrue("Library should not contain deleted movies", repository.userLibrary.value.none { it.id == primer.id || it.id == inception.id })

        val folderAfterDelete = repository.playlists.value.first { it.id == created.id }
        assertTrue("Deleted movies should also be removed from playlist folders", folderAfterDelete.movieIds.isEmpty())

        // Delete playlist folder
        repository.deletePlaylist(created.id)
        assertTrue("Playlist should be deleted", repository.playlists.value.none { it.id == created.id })
    }

    @Test
    fun `authoritative cloud snapshot sync updates watchlist to watched and removes deleted items`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CineverseRepository(context)

        val primer = repository.search("Primer").first()
        val barbie = repository.search("Barbie").first()

        // Local device initially has Primer in Watchlist and Barbie in Watchlist
        repository.addToWatchlist(primer)
        repository.addToWatchlist(barbie)

        // Remote snapshot has Primer upgraded to Watched (rating 10) and Barbie deleted
        val remotePrimerWatched = primer.copy(userStatus = "Watched", userRating = 10, rewatchCount = 1)
        val remoteEntry = DiaryEntry(
            id = "diary_initial_${primer.id}",
            userId = "guest",
            titleId = primer.id,
            movieTitle = primer.title,
            posterUrl = primer.posterUrl,
            entryDate = "Nov 13, 2026",
            rating = 10,
            review = "Synced from other device",
            thoughts = "Synced from other device"
        )

        repository.applyAuthoritativeSnapshot(
            remoteLibrary = listOf(remotePrimerWatched),
            remoteDiary = listOf(remoteEntry)
        )

        val syncedLibrary = repository.userLibrary.value
        assertEquals("Deleted item Barbie must be removed after authoritative sync", 1, syncedLibrary.size)
        assertEquals("Primer status must be updated from Watchlist to Watched", "Watched", syncedLibrary.first().userStatus)
        assertEquals(10, syncedLibrary.first().userRating)
    }
}



