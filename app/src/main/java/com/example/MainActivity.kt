package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.example.data.CineverseRepository
import com.example.data.MovieItem
import com.example.ui.detail.MovieDetailScreen
import com.example.ui.diary.DiaryScreen
import com.example.ui.home.HomeScreen
import com.example.ui.library.LibraryScreen
import com.example.ui.mood.MoodDiscoveryScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.search.SearchScreen
import com.example.ui.stats.StatsScreen
import com.example.ui.components.CineverseBottomNavBar
import com.example.ui.components.LiquidGlassAtmosphere
import com.example.ui.theme.CineverseTheme
import com.example.ui.theme.ThemeMode
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"
private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        // 1. Ensure FirebaseApp is initialized
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            // 2. Debug: Read debug token from Intent extra
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)

            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                // CRITICAL: MUST use commit() instead of apply() so the secret is written
                // synchronously before DebugAppCheckProviderFactory reads it.
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            // 3. Release: Look up recaptcha_site_key safely without throwing Resources.NotFoundException
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        // Log explicitly to keep App Check attestation failures visible
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val repository = CineverseRepository(this)
        val prefs = getSharedPreferences("cineverse_vault", Context.MODE_PRIVATE)
        val savedThemeName = prefs.getString("selected_theme_mode", ThemeMode.LIQUID_GLASS.name) ?: ThemeMode.LIQUID_GLASS.name
        val initialThemeMode = try {
            ThemeMode.valueOf(savedThemeName)
        } catch (e: Exception) {
            ThemeMode.LIQUID_GLASS
        }
        val initialGlassTransparency = prefs.getFloat("glass_transparency", 0.40f).coerceIn(0f, 1f)

        setContent {
            var currentThemeMode by remember { mutableStateOf(initialThemeMode) }
            var glassTransparency by remember { mutableFloatStateOf(initialGlassTransparency) }
            var customAccentColor by remember { mutableStateOf(Color(0xFF54C8F5)) }
            var currentRoute by remember { mutableStateOf("home") }
            var selectedMovie by remember { mutableStateOf<MovieItem?>(null) }
            var showMoodScreen by remember { mutableStateOf(false) }
            var moodInitialTab by remember { mutableStateOf("mood") }

            // Live persistent user library, diary timeline, viewing history, and Firebase user
            val userLibrary by repository.userLibrary.collectAsStateWithLifecycle()
            val diaryEntries by repository.diaryEntries.collectAsStateWithLifecycle()
            val viewingHistory by repository.viewingHistory.collectAsStateWithLifecycle()
            val activeSoundtrack by repository.activeSoundtrack.collectAsStateWithLifecycle()
            val currentFirebaseUser by repository.currentUser.collectAsStateWithLifecycle()

            // Silent auto-login only after first frame + SurfaceSyncGroup settles and only if user previously opted in
            LaunchedEffect(Unit) {
                if (repository.getCurrentUserId() == null && prefs.getBoolean("google_auto_signin_opted_in", false)) {
                    withFrameNanos { }
                    delay(1500)
                    if (repository.getCurrentUserId() == null) {
                        try {
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(true)
                                .setServerClientId(getString(R.string.default_web_client_id))
                                .setAutoSelectEnabled(true)
                                .build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()
                            val result = CredentialManager.create(this@MainActivity)
                                .getCredential(this@MainActivity, request)
                            val cred = result.credential
                            if (cred is CustomCredential &&
                                cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            ) {
                                val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
                                repository.signInWithGoogleIdToken(googleCred.idToken)
                            }
                        } catch (_: Exception) {
                            // Silent attempt only; user can explicitly sign in via Personal Hub
                        }
                    }
                }
            }

            // Handle system back gesture
            BackHandler(enabled = selectedMovie != null || showMoodScreen) {
                if (selectedMovie != null) {
                    selectedMovie = null
                } else if (showMoodScreen) {
                    showMoodScreen = false
                }
            }

            CineverseTheme(
                themeMode = currentThemeMode,
                glassTransparency = glassTransparency
            ) {
                LiquidGlassAtmosphere(accentColor = customAccentColor) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 680.dp)
                                .fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                when {
                                    selectedMovie != null -> {
                                        val movie = selectedMovie!!
                                        val inWatchlist = userLibrary.any {
                                            (it.id == movie.id || it.title.equals(movie.title, ignoreCase = true)) &&
                                                    (it.userStatus.equals("Watchlist", ignoreCase = true) || it.userStatus.isBlank())
                                        }
                                        val isWatched = userLibrary.any {
                                            (it.id == movie.id || it.title.equals(movie.title, ignoreCase = true)) &&
                                                    it.userStatus.equals("Watched", ignoreCase = true)
                                        }

                                        MovieDetailScreen(
                                            movie = movie,
                                            onBack = { selectedMovie = null },
                                            isInWatchlist = inWatchlist,
                                            isWatched = isWatched,
                                            onToggleWatchlist = { target ->
                                                val existing = userLibrary.find { it.id == target.id || it.title.equals(target.title, ignoreCase = true) }
                                                if (existing != null && existing.userStatus == "Watchlist") {
                                                    repository.removeFromLibrary(existing.id)
                                                } else {
                                                    repository.addToWatchlist(target)
                                                }
                                            },
                                            onLogWatched = { target, rating, review ->
                                                repository.markWatched(target, rating, review)
                                            },
                                            onLogRewatch = { target, rating, notes, date ->
                                                repository.logRewatch(target, rating, notes, date)
                                            },
                                            onRelatedMovieClick = { title ->
                                                val found = userLibrary.find { it.title.equals(title, ignoreCase = true) }
                                                if (found != null) selectedMovie = found
                                            },
                                            onConnectedMovieSelect = { connected ->
                                                selectedMovie = connected
                                            }
                                        )
                                    }
                                    showMoodScreen -> {
                                        MoodDiscoveryScreen(
                                            allMovies = userLibrary,
                                            onMovieClick = { selectedMovie = it },
                                            onAddToWatchlist = { repository.addToWatchlist(it) },
                                            onFeedback = { movie, fb -> repository.recordRecommendationFeedback(movie, fb) },
                                            onBack = { showMoodScreen = false },
                                            initialTab = moodInitialTab
                                        )
                                    }
                                    else -> {
                                        when (currentRoute) {
                                            "home" -> HomeScreen(
                                                userLibrary = userLibrary,
                                                onMovieClick = { selectedMovie = it },
                                                onSearchClick = { currentRoute = "search" },
                                                onMoodClick = {
                                                    moodInitialTab = "mood"
                                                    showMoodScreen = true
                                                },
                                                onWatchlistClick = { currentRoute = "library" }
                                            )
                                            "search" -> SearchScreen(
                                                userLibrary = userLibrary,
                                                onMovieClick = { selectedMovie = it },
                                                onAddToWatchlist = { repository.addToWatchlist(it) },
                                                onMarkWatched = { repository.markWatched(it, rating = 8, review = "Watched via Social Extractor") }
                                            )
                                            "library" -> LibraryScreen(
                                                userLibrary = userLibrary,
                                                onMovieClick = { selectedMovie = it },
                                                onExploreClick = { currentRoute = "search" },
                                                onUpdateStatus = { id, status -> repository.updateStatus(id, status) },
                                                onRemoveMovie = { id -> repository.removeFromLibrary(id) },
                                                onLogRewatch = { movie, rating, notes, date ->
                                                    repository.logRewatch(movie, rating, notes, date)
                                                },
                                                onUpdateRating = { id, rating -> repository.updateRating(id, rating) }
                                            )
                                            "diary" -> DiaryScreen(
                                                diaryEntries = diaryEntries,
                                                userLibrary = userLibrary,
                                                onMovieClick = { selectedMovie = it },
                                                onExploreClick = { currentRoute = "search" },
                                                onAddEntry = { repository.addDiaryEntry(it) },
                                                onUpdateEntry = { repository.updateDiaryEntry(it) },
                                                onDeleteEntry = { repository.deleteDiaryEntry(it) }
                                            )
                                            "stats" -> StatsScreen(
                                                userLibrary = userLibrary,
                                                onExploreClick = { currentRoute = "search" }
                                            )
                                            "profile" -> ProfileScreen(
                                                currentThemeMode = currentThemeMode,
                                                onThemeModeChange = { newMode ->
                                                    currentThemeMode = newMode
                                                    prefs.edit().putString("selected_theme_mode", newMode.name).apply()
                                                },
                                                currentAccentColor = customAccentColor,
                                                onAccentColorChange = { customAccentColor = it },
                                                glassTransparency = glassTransparency,
                                                onGlassTransparencyChange = { newTransparency ->
                                                    val clamped = newTransparency.coerceIn(0f, 1f)
                                                    glassTransparency = clamped
                                                    prefs.edit().putFloat("glass_transparency", clamped).apply()
                                                },
                                                userLibraryCount = userLibrary.size,
                                                diaryCount = diaryEntries.size,
                                                historyCount = viewingHistory.size,
                                                currentUserId = currentFirebaseUser?.uid,
                                                onExportBackup = { repository.exportBackupJson() },
                                                onPreviewBackup = { repository.previewBackupJson(it) },
                                                onRestoreBackup = { repository.restoreBackupJson(it) }
                                            )
                                        }
                                    }
                                }
                            }

                            if (selectedMovie == null && !showMoodScreen) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                ) {
                                    CineverseBottomNavBar(
                                        currentRoute = currentRoute,
                                        accentColor = customAccentColor,
                                        onNavigate = { route ->
                                            currentRoute = route
                                            selectedMovie = null
                                            showMoodScreen = false
                                        }
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
