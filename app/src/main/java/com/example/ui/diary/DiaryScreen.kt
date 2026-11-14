package com.example.ui.diary

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CinemaWrappedReport
import com.example.data.CineverseRepository
import com.example.data.DiaryEntry
import com.example.data.MovieItem
import com.example.data.MoviePlaylist
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconButton
import com.example.ui.components.GlassPillButton
import com.example.ui.components.MoviePosterImage
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiaryScreen(
    diaryEntries: List<DiaryEntry>,
    userLibrary: List<MovieItem>,
    onMovieClick: (MovieItem) -> Unit,
    onExploreClick: () -> Unit,
    onAddEntry: (DiaryEntry) -> Unit,
    onUpdateEntry: (DiaryEntry) -> Unit,
    onDeleteEntry: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { CineverseRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val refreshVersion by repository.refreshVersion.collectAsStateWithLifecycle()
    val playlists by repository.playlists.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }

    // Mode toggle: "diary", "playlists", "wrapped", "map"
    var activeMode by remember { mutableStateOf("diary") }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<MoviePlaylist?>(null) }

    var wrappedPeriod by remember { mutableStateOf("All Time") }
    var wrappedReport by remember { mutableStateOf<CinemaWrappedReport?>(null) }
    var isGeneratingWrapped by remember { mutableStateOf(false) }

    var filterText by remember { mutableStateOf("") }
    var showNewEntryDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<DiaryEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<DiaryEntry?>(null) }

    LaunchedEffect(activeMode, wrappedPeriod, userLibrary, diaryEntries, refreshVersion) {
        if (activeMode == "wrapped") {
            isGeneratingWrapped = true
            wrappedReport = repository.generateCinemaWrapped(wrappedPeriod)
            isGeneratingWrapped = false
        }
    }

    val filteredEntries = remember(filterText, diaryEntries) {
        if (filterText.isBlank()) {
            diaryEntries
        } else {
            val query = filterText.trim().lowercase()
            diaryEntries.filter { entry ->
                entry.movieTitle.lowercase().contains(query) ||
                    entry.review.lowercase().contains(query) ||
                    entry.thoughts.lowercase().contains(query) ||
                    entry.entryDate.lowercase().contains(query)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Title Header + Action Button
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
                    text = when (activeMode) {
                        "playlists" -> "Movie Playlists"
                        "wrapped" -> "Cinema Wrapped"
                        "map" -> "Movie Map"
                        else -> "Movie Diary"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                    color = FrostWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (activeMode) {
                        "playlists" -> "${playlists.size} custom genre folders & playlists"
                        "wrapped" -> "Personal viewing recap & Gemini insights"
                        "map" -> "Interactive Spider Web & Movie Matrix"
                        else -> "${diaryEntries.size} entries in your personal timeline"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SteelBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            when (activeMode) {
                "diary" -> {
                    GlassIconButton(
                        icon = Icons.Default.Add,
                        contentDescription = "New Diary Entry",
                        size = 44.dp,
                        onClick = { showNewEntryDialog = true },
                        modifier = Modifier.testTag("new_diary_entry_btn")
                    )
                }
                "playlists" -> {
                    GlassIconButton(
                        icon = Icons.Default.CreateNewFolder,
                        contentDescription = "New Playlist Folder",
                        size = 44.dp,
                        onClick = { showCreatePlaylistDialog = true },
                        modifier = Modifier.testTag("new_playlist_folder_btn")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Top Segmented Switcher: Diary, Playlists, Wrapped, and Movie Map
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassPillButton(
                text = "Diary",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                isActive = activeMode == "diary",
                onClick = { activeMode = "diary" },
                modifier = Modifier.testTag("diary_tab_diary")
            )
            GlassPillButton(
                text = "Playlists (${playlists.size})",
                icon = Icons.Default.FolderSpecial,
                isActive = activeMode == "playlists",
                onClick = {
                    activeMode = "playlists"
                },
                modifier = Modifier.testTag("diary_tab_playlists")
            )
            GlassPillButton(
                text = "Wrapped",
                icon = Icons.Default.AutoAwesome,
                isActive = activeMode == "wrapped",
                onClick = { activeMode = "wrapped" },
                modifier = Modifier.testTag("diary_tab_wrapped")
            )
            GlassPillButton(
                text = "Map",
                icon = Icons.Default.Hub,
                isActive = activeMode == "map",
                onClick = { activeMode = "map" },
                modifier = Modifier.testTag("diary_tab_map")
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
            when (activeMode) {
                "playlists" -> {
                    PlaylistsFoldersView(
                        playlists = playlists,
                        userLibrary = userLibrary,
                        selectedPlaylistId = selectedPlaylistId,
                        onSelectPlaylist = { selectedPlaylistId = it },
                        onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                        onDeletePlaylistClick = { playlistToDelete = it },
                        onRemoveMovieFromPlaylist = { plId, movieId ->
                            repository.removeMovieFromPlaylist(plId, movieId)
                        },
                        onToggleMovieInPlaylist = { plId, movie ->
                            repository.toggleMovieInPlaylist(plId, movie)
                        },
                        onMovieClick = onMovieClick
                    )
                }
                "wrapped" -> {
                    CinemaWrappedView(
                        report = wrappedReport,
                        isLoading = isGeneratingWrapped,
                        selectedPeriod = wrappedPeriod,
                        onPeriodChange = { wrappedPeriod = it },
                        onShareRecap = { summaryText ->
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, summaryText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Cinema Wrapped"))
                        }
                    )
                }
                "map" -> {
                    MovieMapSection(
                        userLibrary = userLibrary,
                        diaryEntries = diaryEntries,
                        repository = repository,
                        onMovieClick = onMovieClick
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val diaryOpacity = (1f - com.example.ui.theme.LocalGlassTransparency.current).coerceIn(0f, 1f)
                        OutlinedTextField(
                            value = filterText,
                            onValueChange = { filterText = it },
                            placeholder = { Text("Filter diary notes, films or dates...", color = SteelBlue) },
                            leadingIcon = {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = ElectricCyan)
                            },
                            trailingIcon = {
                                if (filterText.isNotEmpty()) {
                                    IconButton(onClick = { filterText = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = SteelBlue)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MidnightNavy.copy(alpha = (0.75f * diaryOpacity).coerceIn(0f, 1f)),
                                unfocusedContainerColor = MidnightNavy.copy(alpha = (0.55f * diaryOpacity).coerceIn(0f, 1f)),
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = SteelBlue.copy(alpha = 0.4f),
                                focusedTextColor = FrostWhite,
                                unfocusedTextColor = FrostWhite
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (filteredEntries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = "Diary Empty",
                                        tint = SteelBlue,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = if (filterText.isNotBlank()) "No matching diary entries" else "Your Cinema Memoir is Empty",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = FrostWhite,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (filterText.isNotBlank()) "Try a different keyword or date."
                                        else "Log personal reviews, thoughts, and dates for films you've watched, or pull down to sync.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SteelBlue,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    GlassPillButton(
                                        text = "Write First Entry",
                                        icon = Icons.Default.Edit,
                                        isActive = true,
                                        onClick = { showNewEntryDialog = true }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 110.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(filteredEntries, key = { it.id }) { entry ->
                                    DiaryEntryCard(
                                        entry = entry,
                                        onClick = {
                                            val matchedMovie = userLibrary.find {
                                                it.id == entry.titleId || it.title.equals(entry.movieTitle, ignoreCase = true)
                                            }
                                            if (matchedMovie != null) {
                                                onMovieClick(matchedMovie)
                                            } else {
                                                entryToEdit = entry
                                            }
                                        },
                                        onEdit = { entryToEdit = entry },
                                        onDelete = { entryToDelete = entry }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        var folderName by remember { mutableStateOf("") }
        var folderDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = MidnightNavy,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = ElectricCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Playlist / Folder", color = FrostWhite)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Create a custom genre playlist (e.g., Psychological, Mind-Bending, Comfort Movies) to organize movies your own way.",
                        color = SteelBlue,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        placeholder = { Text("Playlist / Folder Name (e.g., Psychological)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("diary_new_playlist_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = SteelBlue,
                            focusedTextColor = FrostWhite,
                            unfocusedTextColor = FrostWhite
                        )
                    )
                    OutlinedTextField(
                        value = folderDesc,
                        onValueChange = { folderDesc = it },
                        placeholder = { Text("Custom genre description (optional)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("diary_new_playlist_desc_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = SteelBlue,
                            focusedTextColor = FrostWhite,
                            unfocusedTextColor = FrostWhite
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            repository.createPlaylist(folderName.trim(), folderDesc.trim())
                            showCreatePlaylistDialog = false
                        }
                    },
                    enabled = folderName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    modifier = Modifier.testTag("diary_confirm_create_playlist_btn")
                ) {
                    Text("Create Folder", color = DeepBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = FrostWhite)
                }
            }
        )
    }

    if (playlistToDelete != null) {
        val targetPl = playlistToDelete!!
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = MidnightNavy,
            title = { Text("Delete Folder \"${targetPl.name}\"?", color = FrostWhite) },
            text = {
                Text(
                    text = "Are you sure you want to delete the \"${targetPl.name}\" playlist folder? Movies inside it will remain in your library.",
                    color = SteelBlue,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedPlaylistId == targetPl.id) {
                            selectedPlaylistId = null
                        }
                        repository.deletePlaylist(targetPl.id)
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    modifier = Modifier.testTag("confirm_delete_playlist_btn")
                ) {
                    Text("Delete Folder", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancel", color = FrostWhite)
                }
            }
        )
    }

    if (showNewEntryDialog) {
        DiaryEntryEditorDialog(
            entry = null,
            userLibrary = userLibrary,
            onDismiss = { showNewEntryDialog = false },
            onSave = { newEntry ->
                onAddEntry(newEntry)
                showNewEntryDialog = false
            }
        )
    }

    if (entryToEdit != null) {
        DiaryEntryEditorDialog(
            entry = entryToEdit,
            userLibrary = userLibrary,
            onDismiss = { entryToEdit = null },
            onSave = { updatedEntry ->
                onUpdateEntry(updatedEntry)
                entryToEdit = null
            }
        )
    }

    if (entryToDelete != null) {
        val entry = entryToDelete!!
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            containerColor = MidnightNavy,
            title = { Text("Delete Diary Entry?", color = FrostWhite) },
            text = {
                Text(
                    text = "Are you sure you want to remove your notes for \"${entry.movieTitle}\" logged on ${entry.entryDate}? This action cannot be undone.",
                    color = SteelBlue,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEntry(entry.id)
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel", color = FrostWhite)
                }
            }
        )
    }
}

@Composable
fun PlaylistsFoldersView(
    playlists: List<MoviePlaylist>,
    userLibrary: List<MovieItem>,
    selectedPlaylistId: String?,
    onSelectPlaylist: (String?) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onDeletePlaylistClick: (MoviePlaylist) -> Unit,
    onRemoveMovieFromPlaylist: (String, String) -> Unit,
    onToggleMovieInPlaylist: (String, MovieItem) -> Unit,
    onMovieClick: (MovieItem) -> Unit
) {
    val activePlaylist = remember(playlists, selectedPlaylistId) {
        playlists.find { it.id == selectedPlaylistId }
    }
    var showAddFromLibraryDialog by remember { mutableStateOf(false) }

    if (activePlaylist != null) {
        val folderMovies = remember(activePlaylist.movieIds, userLibrary) {
            activePlaylist.movieIds.mapNotNull { mId ->
                userLibrary.find { it.id == mId || "title_${it.title.hashCode()}" == mId }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassIconButton(
                            icon = Icons.Default.ArrowBack,
                            contentDescription = "Back to Folders",
                            size = 38.dp,
                            onClick = { onSelectPlaylist(null) },
                            modifier = Modifier.testTag("back_to_playlists_btn")
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activePlaylist.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = FrostWhite
                            )
                            Text(
                                text = if (activePlaylist.description.isNotBlank()) {
                                    "${activePlaylist.description} • ${folderMovies.size} movie(s)"
                                } else {
                                    "${folderMovies.size} movie(s) in this custom playlist"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = ElectricCyan
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassIconButton(
                            icon = Icons.Default.Add,
                            contentDescription = "Add Movie from Library",
                            size = 38.dp,
                            onClick = { showAddFromLibraryDialog = true },
                            modifier = Modifier.testTag("add_movie_to_playlist_btn")
                        )
                        GlassIconButton(
                            icon = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Folder",
                            size = 38.dp,
                            tint = Color(0xFFFF5252),
                            onClick = { onDeletePlaylistClick(activePlaylist) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (folderMovies.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = SteelBlue,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No movies in \"${activePlaylist.name}\" yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = FrostWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Open any movie's detail page and tap the Add to Folder icon at the top, or add from your library below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SteelBlue,
                            textAlign = TextAlign.Center
                        )
                        if (userLibrary.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            GlassPillButton(
                                text = "Add from Library",
                                icon = Icons.Default.Add,
                                isActive = true,
                                onClick = { showAddFromLibraryDialog = true }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(folderMovies, key = { it.id }) { movie ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("playlist_movie_card_${movie.id}"),
                            cornerRadius = 18.dp,
                            onClick = { onMovieClick(movie) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MoviePosterImage(
                                    posterUrl = movie.posterUrl,
                                    title = movie.title,
                                    signatureColorHex = movie.signatureColorHex,
                                    modifier = Modifier
                                        .width(56.dp)
                                        .height(84.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop,
                                    releaseYear = movie.releaseYear,
                                    movieId = movie.id
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = movie.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = FrostWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${movie.releaseYear} • ${movie.contentType} • ${movie.userStatus.ifBlank { "Watchlist" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SteelBlue
                                    )
                                    if (movie.userRating > 0) {
                                        Text(
                                            text = "My Rating: ${movie.userRating}/10",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricCyan
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val safeId = movie.id.ifBlank { "title_${movie.title.hashCode()}" }
                                        onRemoveMovieFromPlaylist(activePlaylist.id, safeId)
                                        onRemoveMovieFromPlaylist(activePlaylist.id, movie.id)
                                    },
                                    modifier = Modifier.testTag("remove_from_playlist_${movie.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove from Folder",
                                        tint = Color(0xFFFF5252)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddFromLibraryDialog) {
            AlertDialog(
                onDismissRequest = { showAddFromLibraryDialog = false },
                containerColor = MidnightNavy,
                title = { Text("Add Movies to \"${activePlaylist.name}\"", color = FrostWhite) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (userLibrary.isEmpty()) {
                            Text("Your library is empty. Search and add movies first.", color = SteelBlue)
                        } else {
                            userLibrary.forEach { libMovie ->
                                val safeId = libMovie.id.ifBlank { "title_${libMovie.title.hashCode()}" }
                                val checked = safeId in activePlaylist.movieIds || libMovie.id in activePlaylist.movieIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onToggleMovieInPlaylist(activePlaylist.id, libMovie) }
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { onToggleMovieInPlaylist(activePlaylist.id, libMovie) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = ElectricCyan,
                                            uncheckedColor = SteelBlue,
                                            checkmarkColor = DeepBlack
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${libMovie.title} (${libMovie.releaseYear})",
                                        color = FrostWhite,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddFromLibraryDialog = false }) {
                        Text("Done", color = ElectricCyan)
                    }
                }
            )
        }
    } else {
        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = "No Playlists",
                        tint = ElectricCyan,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No Custom Folders / Playlists Yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = FrostWhite,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Create folders like \"Psychological\", \"Mind-Bending\", or \"Weekend Classics\" and add movies using the Add to Folder icon on any movie's detail page.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SteelBlue,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    GlassPillButton(
                        text = "Create New Folder",
                        icon = Icons.Default.CreateNewFolder,
                        isActive = true,
                        onClick = onCreatePlaylistClick,
                        modifier = Modifier.testTag("empty_state_create_playlist_btn")
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("diary_playlists_list"),
                contentPadding = PaddingValues(bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(playlists, key = { it.id }) { pl ->
                    val folderMovies = remember(pl.movieIds, userLibrary) {
                        pl.movieIds.mapNotNull { mId ->
                            userLibrary.find { it.id == mId || "title_${it.title.hashCode()}" == mId }
                        }
                    }

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_folder_card_${pl.name.lowercase().replace(" ", "_")}"),
                        cornerRadius = 22.dp,
                        onClick = { onSelectPlaylist(pl.id) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(ElectricCyan.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FolderSpecial,
                                            contentDescription = null,
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = FrostWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (pl.description.isNotBlank()) {
                                                "${pl.description} • ${pl.movieIds.size} movie(s)"
                                            } else {
                                                "Custom Genre Playlist • ${pl.movieIds.size} movie(s)"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SteelBlue,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDeletePlaylistClick(pl) },
                                    modifier = Modifier.testTag("delete_playlist_folder_${pl.name.lowercase().replace(" ", "_")}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Folder",
                                        tint = Color(0xFFFF5252)
                                    )
                                }
                            }

                            if (folderMovies.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(folderMovies.take(6), key = { it.id }) { m ->
                                        MoviePosterImage(
                                            posterUrl = m.posterUrl,
                                            title = m.title,
                                            signatureColorHex = m.signatureColorHex,
                                            modifier = Modifier
                                                .width(52.dp)
                                                .height(76.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { onMovieClick(m) },
                                            contentScale = ContentScale.Crop,
                                            releaseYear = m.releaseYear,
                                            movieId = m.id
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
}

/**
 * PHASE 6 SECTION 2 — CINEMA WRAPPED
 * Personal viewing recap with Monthly, Yearly, and All-Time periods,
 * verified runtime hours, genre/language/director/actor preferences,
 * viewing activity patterns, and Gemini AI observations.
 */
@Composable
fun CinemaWrappedView(
    report: CinemaWrappedReport?,
    isLoading: Boolean,
    selectedPeriod: String,
    onPeriodChange: (String) -> Unit,
    onShareRecap: (String) -> Unit
) {
    val periods = listOf("All Time", "Yearly", "Monthly")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("cinema_wrapped_container")
    ) {
        // Period Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            periods.forEach { period ->
                GlassPillButton(
                    text = period,
                    isActive = selectedPeriod == period,
                    onClick = { onPeriodChange(period) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("wrapped_period_${period.lowercase().replace(" ", "_")}")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isLoading || report == null) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricCyan)
                }
            }
        } else {
            // 1. Hero Shareable Summary Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wrapped_hero_card"),
                cornerRadius = 26.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CINEMA WRAPPED • ${report.period.uppercase()}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp
                                ),
                                color = ElectricCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f Verified Hours", report.totalViewingHours),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = FrostWhite
                            )
                        }

                        GlassIconButton(
                            icon = Icons.Default.Share,
                            contentDescription = "Share Cinema Wrapped",
                            size = 42.dp,
                            onClick = {
                                val shareText = buildString {
                                    appendLine("My Cinema Wrapped (${report.period}):")
                                    appendLine("• Movies Watched: ${report.totalMoviesWatched}")
                                    appendLine("• Series: ${report.totalSeriesWatched} | Docs: ${report.totalDocumentariesWatched} | Anime: ${report.totalAnimeWatched}")
                                    appendLine("• Verified Viewing Time: ${String.format(Locale.US, "%.1f", report.totalViewingHours)} hrs (${report.totalViewingSessions} sessions)")
                                    if (report.topGenres.isNotEmpty()) {
                                        appendLine("• Top Genres: ${report.topGenres.joinToString { it.first }}")
                                    }
                                    if (report.aiObservations.isNotEmpty()) {
                                        appendLine("• Insight: ${report.aiObservations.first()}")
                                    }
                                }
                                onShareRecap(shareText)
                            },
                            modifier = Modifier.testTag("share_cinema_wrapped_btn")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2x3 Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WrappedStatTile("Movies", "${report.totalMoviesWatched}", Modifier.weight(1f))
                        WrappedStatTile("Series", "${report.totalSeriesWatched}", Modifier.weight(1f))
                        WrappedStatTile("Docs", "${report.totalDocumentariesWatched}", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WrappedStatTile("Anime", "${report.totalAnimeWatched}", Modifier.weight(1f))
                        WrappedStatTile("Sessions", "${report.totalViewingSessions}", Modifier.weight(1f))
                        WrappedStatTile(
                            "Hours",
                            String.format(Locale.US, "%.1f", report.totalViewingHours),
                            Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. AI-Generated Observations (Grounded in Actual Records)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wrapped_ai_observations_card"),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Observations",
                            tint = Color(0xFF00F5D4),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI-GENERATED OBSERVATIONS",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF00F5D4)
                        )
                    }

                    if (!report.hasSufficientData) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Insufficient viewing history for deep multi-title pattern synthesis — showing grounded status:",
                            style = MaterialTheme.typography.labelSmall,
                            color = IcyBlue,
                            modifier = Modifier.testTag("wrapped_insufficient_data_notice")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    report.aiObservations.forEach { obs ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("• ", color = ElectricCyan, fontWeight = FontWeight.Bold)
                            Text(
                                text = obs,
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                                color = FrostWhite
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Personal Preferences Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "PERSONAL PREFERENCES",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = ElectricCyan
                    )

                    PreferenceRow(
                        label = "Most-Watched Genres",
                        value = if (report.topGenres.isEmpty()) "Not enough data"
                        else report.topGenres.joinToString(" • ") { "${it.first} (${it.second})" }
                    )
                    PreferenceRow(
                        label = "Favourite Languages",
                        value = if (report.favouriteLanguages.isEmpty()) "Not enough data"
                        else report.favouriteLanguages.joinToString(" • ") { "${it.first} (${it.second})" }
                    )
                    PreferenceRow(
                        label = "Favourite Directors",
                        value = if (report.favouriteDirectors.isEmpty()) "Not enough data"
                        else report.favouriteDirectors.joinToString(" • ") { "${it.first} (${it.second})" }
                    )
                    PreferenceRow(
                        label = "Favourite Actors",
                        value = if (report.favouriteActors.isEmpty()) "Not enough data"
                        else report.favouriteActors.joinToString(" • ") { "${it.first} (${it.second})" }
                    )
                    PreferenceRow(
                        label = "Highest-Rated Titles",
                        value = if (report.highestRatedTitles.isEmpty()) "No rated titles yet"
                        else report.highestRatedTitles.joinToString(" • ") { "${it.title} (${it.userRating}/10)" }
                    )
                    PreferenceRow(
                        label = "Lowest-Rated Titles",
                        value = if (report.lowestRatedTitles.isEmpty()) "Insufficient distinct ratings"
                        else report.lowestRatedTitles.joinToString(" • ") { "${it.title} (${it.userRating}/10)" }
                    )
                    PreferenceRow(
                        label = "Most-Rewatched Titles",
                        value = if (report.mostRewatchedTitles.isEmpty()) "No rewatches logged yet"
                        else report.mostRewatchedTitles.joinToString(" • ") { "${it.title} (${it.rewatchCount}x)" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Viewing Patterns Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "VIEWING PATTERNS & EVOLUTION",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFE040FB)
                    )

                    PreferenceRow(
                        label = "Most Active Viewing Month",
                        value = report.mostActiveMonth
                    )
                    PreferenceRow(
                        label = "Viewing Patterns Over Time",
                        value = report.viewingPatternSummary
                    )
                    PreferenceRow(
                        label = "Rating & Preference Evolution",
                        value = report.ratingEvolutionSummary
                    )

                    if (report.monthlyActivity.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Monthly Viewing Activity",
                            style = MaterialTheme.typography.labelSmall,
                            color = IcyBlue
                        )
                        val maxCount = (report.monthlyActivity.values.maxOrNull() ?: 1).coerceAtLeast(1)
                        report.monthlyActivity.entries.forEach { (month, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = month,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FrostWhite,
                                    modifier = Modifier.width(84.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF122338))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((count.toFloat() / maxCount.toFloat()).coerceIn(0.1f, 1f))
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(ElectricCyan, Color(0xFF00F5D4))
                                                )
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WrappedStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val tileOpacity = (1f - com.example.ui.theme.LocalGlassTransparency.current).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0E1E32).copy(alpha = (0.88f * tileOpacity).coerceIn(0f, 1f)))
            .border(1.dp, ElectricCyan.copy(alpha = 0.30f * (0.25f + 0.75f * tileOpacity)), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = FrostWhite
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = IcyBlue
            )
        }
    }
}

@Composable
private fun PreferenceRow(
    label: String,
    value: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SteelBlue
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = FrostWhite
        )
    }
}

@Composable
fun DiaryEntryCard(
    entry: DiaryEntry,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diary_entry_card_${entry.id}"),
        cornerRadius = 22.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            MoviePosterImage(
                posterUrl = entry.posterUrl,
                title = entry.movieTitle,
                modifier = Modifier
                    .width(74.dp)
                    .height(108.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
                movieId = entry.titleId
            )
            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.movieTitle.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        color = FrostWhite,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SteelBlue, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.entryDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = SteelBlue
                    )

                    if (entry.rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = "Rating", tint = ElectricCyan, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${entry.rating}/10",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Diary entry thoughts & review
                Column {
                    if (entry.thoughts.isNotBlank()) {
                        Text(
                            text = entry.thoughts,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = FrostWhite.copy(alpha = 0.9f),
                            maxLines = 4
                        )
                    }

                    if (entry.review.isNotBlank() && entry.review != entry.thoughts) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${entry.review}\"",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = IcyBlue.copy(alpha = 0.85f),
                            maxLines = 3
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiaryEntryEditorDialog(
    entry: DiaryEntry?,
    userLibrary: List<MovieItem>,
    onDismiss: () -> Unit,
    onSave: (DiaryEntry) -> Unit
) {
    var selectedMovieTitle by remember { mutableStateOf(entry?.movieTitle ?: "") }
    var thoughts by remember { mutableStateOf(entry?.thoughts ?: "") }
    var review by remember { mutableStateOf(entry?.review ?: "") }
    var rating by remember { mutableStateOf(entry?.rating ?: 8) }
    var dateStr by remember {
        mutableStateOf(entry?.entryDate ?: SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date()))
    }

    var selectedPosterUrl by remember { mutableStateOf(entry?.posterUrl ?: "") }
    var selectedTitleId by remember { mutableStateOf(entry?.titleId ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightNavy,
        title = {
            Text(if (entry == null) "Log in Movie Diary" else "Edit Diary Entry", color = FrostWhite)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (entry == null) {
                    Text("Select or enter movie:", color = SteelBlue, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = selectedMovieTitle,
                        onValueChange = { input ->
                            selectedMovieTitle = input
                            val matched = userLibrary.find { it.title.equals(input, ignoreCase = true) }
                            if (matched != null) {
                                selectedPosterUrl = matched.posterUrl
                                selectedTitleId = matched.id
                            }
                        },
                        placeholder = { Text("Movie title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = SteelBlue,
                            focusedTextColor = FrostWhite,
                            unfocusedTextColor = FrostWhite
                        ),
                        singleLine = true
                    )

                    if (userLibrary.isNotEmpty() && selectedMovieTitle.isBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Quick pick from your library:", color = SteelBlue, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            userLibrary.take(3).forEach { libMovie ->
                                GlassPillButton(
                                    text = libMovie.title.take(12),
                                    onClick = {
                                        selectedMovieTitle = libMovie.title
                                        selectedPosterUrl = libMovie.posterUrl
                                        selectedTitleId = libMovie.id
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = entry.movieTitle.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricCyan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val effectiveRating = if (rating in 1..10) rating else 8
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Personal Review Rating:", color = SteelBlue, style = MaterialTheme.typography.labelSmall)
                    Text("$effectiveRating / 10", color = ElectricCyan, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = effectiveRating.toFloat(),
                    onValueChange = { rating = it.roundToInt().coerceIn(1, 10) },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricCyan,
                        activeTrackColor = ElectricCyan,
                        inactiveTrackColor = SteelBlue.copy(alpha = 0.28f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Personal Thoughts & Reflections:", color = SteelBlue, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = thoughts,
                    onValueChange = { thoughts = it },
                    placeholder = { Text("Where did you watch it? How did it make you feel?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = SteelBlue,
                        focusedTextColor = FrostWhite,
                        unfocusedTextColor = FrostWhite
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Review (Optional):", color = SteelBlue, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    placeholder = { Text("Critique of score, acting, or direction...") },
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
                    if (selectedMovieTitle.isNotBlank()) {
                        val finalId = entry?.id ?: "diary_${System.currentTimeMillis()}"
                        val finalTitleId = if (selectedTitleId.isNotBlank()) selectedTitleId else "title_${selectedMovieTitle.hashCode()}"
                        val created = DiaryEntry(
                            id = finalId,
                            userId = entry?.userId ?: "guest",
                            titleId = finalTitleId,
                            movieTitle = selectedMovieTitle,
                            posterUrl = selectedPosterUrl,
                            entryDate = dateStr,
                            rating = rating,
                            review = review,
                            thoughts = thoughts.ifBlank { review },
                            tags = listOf("Personal"),
                            isSpoiler = false
                        )
                        onSave(created)
                    }
                },
                enabled = selectedMovieTitle.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
            ) {
                Text("Save Entry", color = DeepBlack)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = FrostWhite)
            }
        }
    )
}

private enum class MatrixEntityKind(val label: String, val accent: Color) {
    COMPOSER("Soundtracks & Composers", Color(0xFFFFD166)),
    DIRECTOR("Directors", Color(0xFF00E5FF)),
    ACTOR("Actors", Color(0xFF00F5D4)),
    GENRE("Genres", Color(0xFFE040FB))
}

private data class MatrixCreatorNode(
    val id: String,
    val name: String,
    val kind: MatrixEntityKind,
    val movies: List<MovieItem>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MovieMapSection(
    userLibrary: List<MovieItem>,
    diaryEntries: List<DiaryEntry>,
    repository: CineverseRepository,
    onMovieClick: (MovieItem) -> Unit
) {
    var activeMatrixView by remember { mutableStateOf("spider") } // "spider", "matrix", "mindmap"
    var activeKindFilter by remember { mutableStateOf("All") } // "All", "Composers", "Directors", "Actors", "Genres"
    var selectedMovieId by remember { mutableStateOf<String?>(null) }
    var selectedCreatorId by remember { mutableStateOf<String?>(null) }

    val enrichedLibrary = remember(userLibrary, repository) {
        userLibrary.map { movie ->
            val resolvedComposer = movie.composer.ifBlank {
                repository.resolveSoundtrackInfo(movie)?.composer.orEmpty()
            }.takeIf { it.isNotBlank() && !it.contains("Unverified", ignoreCase = true) }.orEmpty()
            movie.copy(composer = resolvedComposer)
        }
    }

    val composerMap = remember(enrichedLibrary) {
        val map = linkedMapOf<String, MutableList<MovieItem>>()
        enrichedLibrary.forEach { movie ->
            val rawComposer = movie.composer
            if (rawComposer.isNotBlank()) {
                rawComposer
                    .split(",", "&", "/")
                    .map { it.trim() }
                    .filter { it.length >= 3 }
                    .ifEmpty { listOf(rawComposer) }
                    .forEach { comp ->
                        map.getOrPut(comp) { mutableListOf() }.add(movie)
                    }
            }
        }
        map.entries.sortedByDescending { it.value.size }.associate { it.key to it.value.distinctBy { m -> m.id } }
    }

    val directorMap = remember(enrichedLibrary) {
        enrichedLibrary
            .filter { it.director.isNotBlank() && it.director != "Filmmaker" }
            .groupBy { it.director }
            .entries
            .sortedByDescending { it.value.size }
            .associate { it.key to it.value.distinctBy { m -> m.id } }
    }

    val actorMap = remember(enrichedLibrary) {
        val map = linkedMapOf<String, MutableList<MovieItem>>()
        enrichedLibrary.forEach { movie ->
            movie.mainCast.take(4).forEach { actor ->
                if (actor.isNotBlank()) {
                    map.getOrPut(actor) { mutableListOf() }.add(movie)
                }
            }
        }
        map.entries.sortedByDescending { it.value.size }.associate { it.key to it.value.distinctBy { m -> m.id } }
    }

    val genreMap = remember(enrichedLibrary) {
        val map = linkedMapOf<String, MutableList<MovieItem>>()
        enrichedLibrary.forEach { movie ->
            movie.genres.forEach { g ->
                if (g.isNotBlank()) {
                    map.getOrPut(g) { mutableListOf() }.add(movie)
                }
            }
        }
        map.entries.sortedByDescending { it.value.size }.associate { it.key to it.value.distinctBy { m -> m.id } }
    }

    val creatorNodes = remember(composerMap, directorMap, actorMap, genreMap, activeKindFilter) {
        val list = mutableListOf<MatrixCreatorNode>()
        if (activeKindFilter == "All" || activeKindFilter == "Composers") {
            composerMap.entries.take(6).forEach { (name, movies) ->
                list.add(MatrixCreatorNode("comp_$name", name, MatrixEntityKind.COMPOSER, movies))
            }
        }
        if (activeKindFilter == "All" || activeKindFilter == "Directors") {
            directorMap.entries.take(5).forEach { (name, movies) ->
                list.add(MatrixCreatorNode("dir_$name", name, MatrixEntityKind.DIRECTOR, movies))
            }
        }
        if (activeKindFilter == "All" || activeKindFilter == "Actors") {
            actorMap.entries.take(6).forEach { (name, movies) ->
                list.add(MatrixCreatorNode("act_$name", name, MatrixEntityKind.ACTOR, movies))
            }
        }
        if (activeKindFilter == "All" || activeKindFilter == "Genres") {
            genreMap.entries.take(5).forEach { (name, movies) ->
                list.add(MatrixCreatorNode("gen_$name", name, MatrixEntityKind.GENRE, movies))
            }
        }
        list.take(14)
    }

    val webMovies = remember(enrichedLibrary) { enrichedLibrary.take(10) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("movie_map_container"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Movie Matrix & Cinema Spider Web",
                    style = MaterialTheme.typography.titleMedium,
                    color = FrostWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Connecting your movies through soundtracks & composers (Hans Zimmer, Ludwig Göransson…), directors, cast & genres.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SteelBlue
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3-Mode Multi-View Toggle: Spider Web | Matrix Chart | Mind Map
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassPillButton(
                        text = "Spider Web",
                        icon = Icons.Default.Hub,
                        isActive = activeMatrixView == "spider",
                        onClick = { activeMatrixView = "spider" },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("movie_map_mode_spider_web")
                    )
                    GlassPillButton(
                        text = "Matrix",
                        icon = Icons.Default.BarChart,
                        isActive = activeMatrixView == "matrix",
                        onClick = { activeMatrixView = "matrix" },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("movie_map_mode_matrix_chart")
                    )
                    GlassPillButton(
                        text = "Mind Map",
                        icon = Icons.Default.AccountTree,
                        isActive = activeMatrixView == "mindmap",
                        onClick = { activeMatrixView = "mindmap" },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("movie_map_mode_mind_map")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Web Strand Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("All", "Composers", "Directors", "Actors", "Genres").forEach { kind ->
                        GlassPillButton(
                            text = kind,
                            isActive = activeKindFilter == kind,
                            onClick = {
                                activeKindFilter = kind
                                selectedCreatorId = null
                            }
                        )
                    }
                }
            }
        }

        if (enrichedLibrary.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Hub, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your Cinema Spider Web is Waiting",
                        style = MaterialTheme.typography.titleMedium,
                        color = FrostWhite,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add or mark movies as Watched to weave posters with music composers, directors, and cast.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SteelBlue,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            when (activeMatrixView) {
                "spider" -> {
                    SpiderWebInteractiveCanvas(
                        movies = webMovies,
                        creatorNodes = creatorNodes,
                        selectedMovieId = selectedMovieId,
                        selectedCreatorId = selectedCreatorId,
                        onSelectMovie = { movie ->
                            selectedMovieId = if (selectedMovieId == movie.id) null else movie.id
                            selectedCreatorId = null
                        },
                        onSelectCreator = { creator ->
                            selectedCreatorId = if (selectedCreatorId == creator.id) null else creator.id
                            selectedMovieId = null
                        }
                    )

                    // Interactive Inspector Card when user taps a poster or creator dot in the Spider Web
                    val inspectedMovie = webMovies.find { it.id == selectedMovieId }
                    val inspectedCreator = creatorNodes.find { it.id == selectedCreatorId }

                    if (inspectedMovie != null) {
                        val linkedCreators = creatorNodes.filter { c -> c.movies.any { it.id == inspectedMovie.id } }
                        val peerMovies = linkedCreators.flatMap { it.movies }.distinctBy { it.id }.filter { it.id != inspectedMovie.id }
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "WEB NODE: ${inspectedMovie.title.uppercase()}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = ElectricCyan,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = buildString {
                                                if (inspectedMovie.composer.isNotBlank()) append("Score: ${inspectedMovie.composer}")
                                                if (inspectedMovie.director.isNotBlank()) {
                                                    if (isNotEmpty()) append(" • ")
                                                    append("Dir: ${inspectedMovie.director}")
                                                }
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = IcyBlue
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    GlassPillButton(
                                        text = "Open",
                                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                                        isActive = true,
                                        compact = true,
                                        onClick = { onMovieClick(inspectedMovie) }
                                    )
                                }

                                if (linkedCreators.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        linkedCreators.forEach { c ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(c.kind.accent.copy(alpha = 0.16f))
                                                    .border(1.dp, c.kind.accent.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        selectedCreatorId = c.id
                                                        selectedMovieId = null
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = "${c.name} (${c.kind.label.dropLast(1)})",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                    color = c.kind.accent
                                                )
                                            }
                                        }
                                    }
                                }

                                if (peerMovies.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Connected Posters via Shared Web Strands:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SteelBlue
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(peerMovies, key = { it.id }) { peer ->
                                            Column(
                                                modifier = Modifier
                                                    .width(76.dp)
                                                    .clickable { onMovieClick(peer) },
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                MoviePosterImage(
                                                    posterUrl = peer.posterUrl,
                                                    title = peer.title,
                                                    modifier = Modifier
                                                        .width(72.dp)
                                                        .height(102.dp)
                                                        .clip(RoundedCornerShape(10.dp)),
                                                    contentScale = ContentScale.Crop,
                                                    releaseYear = peer.releaseYear,
                                                    movieId = peer.id
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = peer.title,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = FrostWhite,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (inspectedCreator != null) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "${inspectedCreator.kind.label.uppercase()} NODE: ${inspectedCreator.name}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = inspectedCreator.kind.accent
                                )
                                Text(
                                    text = "${inspectedCreator.movies.size} linked film(s) in your web — tap a poster to view",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IcyBlue
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(inspectedCreator.movies, key = { it.id }) { m ->
                                        Column(
                                            modifier = Modifier
                                                .width(86.dp)
                                                .clickable { onMovieClick(m) },
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            MoviePosterImage(
                                                posterUrl = m.posterUrl,
                                                title = m.title,
                                                modifier = Modifier
                                                    .width(84.dp)
                                                    .height(120.dp)
                                                    .clip(RoundedCornerShape(12.dp)),
                                                contentScale = ContentScale.Crop,
                                                releaseYear = m.releaseYear,
                                                movieId = m.id
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = m.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = FrostWhite,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Grouped Filmography Carousels: Composers & Soundtracks first, then Directors & Actors
                    if (composerMap.isNotEmpty()) {
                        ConnectionPosterGroupSection(
                            sectionTitle = "Connected by Soundtrack & Music Composer",
                            accentColor = MatrixEntityKind.COMPOSER.accent,
                            icon = Icons.Default.MusicNote,
                            groups = composerMap,
                            onMovieClick = onMovieClick
                        )
                    }
                    if (directorMap.isNotEmpty()) {
                        ConnectionPosterGroupSection(
                            sectionTitle = "Connected by Director",
                            accentColor = MatrixEntityKind.DIRECTOR.accent,
                            icon = Icons.Default.Movie,
                            groups = directorMap,
                            onMovieClick = onMovieClick
                        )
                    }
                    if (actorMap.isNotEmpty()) {
                        ConnectionPosterGroupSection(
                            sectionTitle = "Connected by Cast & Actors",
                            accentColor = MatrixEntityKind.ACTOR.accent,
                            icon = Icons.Default.Person,
                            groups = actorMap.entries.take(6).associate { it.key to it.value },
                            onMovieClick = onMovieClick
                        )
                    }
                }

                "matrix" -> {
                    MatrixGridAndChartsView(
                        movies = webMovies,
                        creatorNodes = creatorNodes,
                        composerMap = composerMap,
                        directorMap = directorMap,
                        genreMap = genreMap,
                        onMovieClick = onMovieClick
                    )
                }

                else -> {
                    MindMapHierarchyView(
                        composerMap = composerMap,
                        directorMap = directorMap,
                        actorMap = actorMap,
                        genreMap = genreMap,
                        onMovieClick = onMovieClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SpiderWebInteractiveCanvas(
    movies: List<MovieItem>,
    creatorNodes: List<MatrixCreatorNode>,
    selectedMovieId: String?,
    selectedCreatorId: String?,
    onSelectMovie: (MovieItem) -> Unit,
    onSelectCreator: (MatrixCreatorNode) -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("spider_web_canvas_card"),
        cornerRadius = 24.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INTERACTIVE SPIDER WEB GRAPH",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = ElectricCyan
                )
                Text(
                    text = "Tap poster or node dot",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = SteelBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MatrixEntityKind.entries.forEach { kind ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(kind.accent)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = kind.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = IcyBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val density = LocalDensity.current
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF0E213A), Color(0xFF060E1A), DeepBlack)
                        )
                    )
                    .border(1.dp, ElectricCyan.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            ) {
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }
                val cx = widthPx / 2f
                val cy = heightPx / 2f
                val minDim = minOf(widthPx, heightPx)

                val innerRadius = minDim * 0.22f
                val midRadius = minDim * 0.31f
                val outerRadius = minDim * 0.42f

                val posterW = 46.dp
                val posterH = 66.dp
                val posterWPx = with(density) { posterW.toPx() }
                val posterHPx = with(density) { posterH.toPx() }

                // Outer orbital coordinates for Movie Posters
                val movieCoords = remember(movies, widthPx, heightPx) {
                    movies.mapIndexed { idx, m ->
                        val angle = (-PI / 2.0) + (2.0 * PI * idx / movies.size.coerceAtLeast(1))
                        val rx = (widthPx * 0.39f) * cos(angle).toFloat()
                        val ry = (heightPx * 0.39f) * sin(angle).toFloat()
                        m.id to Offset(cx + rx, cy + ry)
                    }.toMap()
                }

                // Inner concentric ring coordinates for Composers, Directors, Actors & Genres
                val creatorCoords = remember(creatorNodes, widthPx, heightPx) {
                    creatorNodes.mapIndexed { idx, node ->
                        val angle = (-PI / 2.0 + 0.25) + (2.0 * PI * idx / creatorNodes.size.coerceAtLeast(1))
                        val ringScale = if (idx % 2 == 0) 0.19f else 0.27f
                        val rx = (widthPx * ringScale) * cos(angle).toFloat()
                        val ry = (heightPx * ringScale) * sin(angle).toFloat()
                        node.id to Offset(cx + rx, cy + ry)
                    }.toMap()
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // 1. Spider Web Radial Spokes & Concentric Silk Polygons
                    val webSpokes = 12
                    val ringRadii = listOf(innerRadius * 0.55f, innerRadius, midRadius, outerRadius)
                    val silkColor = ElectricCyan.copy(alpha = 0.12f)

                    for (s in 0 until webSpokes) {
                        val angle = (2.0 * PI * s / webSpokes)
                        val ex = cx + outerRadius * 1.1f * cos(angle).toFloat()
                        val ey = cy + outerRadius * 1.1f * sin(angle).toFloat()
                        drawLine(
                            color = silkColor,
                            start = Offset(cx, cy),
                            end = Offset(ex, ey),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    ringRadii.forEach { r ->
                        val polyPath = Path().apply {
                            for (s in 0..webSpokes) {
                                val angle = (2.0 * PI * (s % webSpokes) / webSpokes)
                                val px = cx + r * cos(angle).toFloat()
                                val py = cy + r * sin(angle).toFloat()
                                if (s == 0) moveTo(px, py) else lineTo(px, py)
                            }
                            close()
                        }
                        drawPath(
                            path = polyPath,
                            color = silkColor,
                            style = Stroke(width = 0.9.dp.toPx())
                        )
                    }

                    // 2. Curved Silk Web Strands from every Creator Dot to matching Movie Posters
                    val anySelected = selectedMovieId != null || selectedCreatorId != null
                    creatorNodes.forEach { cNode ->
                        val cPos = creatorCoords[cNode.id] ?: return@forEach
                        cNode.movies.forEach { linkedMovie ->
                            val mPos = movieCoords[linkedMovie.id] ?: return@forEach
                            val isHighlighted = when {
                                selectedMovieId != null -> linkedMovie.id == selectedMovieId
                                selectedCreatorId != null -> cNode.id == selectedCreatorId
                                else -> true
                            }
                            val strandAlpha = if (!anySelected) 0.52f else if (isHighlighted) 0.95f else 0.09f
                            val strokeW = if (isHighlighted && anySelected) 2.5.dp.toPx() else 1.4.dp.toPx()

                            val ctrl = Offset(
                                (cPos.x + mPos.x) / 2f + (cy - cPos.y) * 0.14f,
                                (cPos.y + mPos.y) / 2f + (cPos.x - cx) * 0.14f
                            )
                            val strand = Path().apply {
                                moveTo(cPos.x, cPos.y)
                                quadraticTo(ctrl.x, ctrl.y, mPos.x, mPos.y)
                            }
                            drawPath(
                                path = strand,
                                color = cNode.kind.accent.copy(alpha = strandAlpha),
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }
                    }

                    // 3. Cross-Strands directly connecting Movie Posters sharing Composer or Director
                    for (i in movies.indices) {
                        for (j in i + 1 until movies.size) {
                            val m1 = movies[i]
                            val m2 = movies[j]
                            val sharesComposer = m1.composer.isNotBlank() && m1.composer.equals(m2.composer, true)
                            val sharesDirector = m1.director.isNotBlank() && m1.director.equals(m2.director, true)
                            if (sharesComposer || sharesDirector) {
                                val p1 = movieCoords[m1.id] ?: continue
                                val p2 = movieCoords[m2.id] ?: continue
                                val highlighted = selectedMovieId == m1.id || selectedMovieId == m2.id
                                val alpha = if (!anySelected) 0.40f else if (highlighted) 0.90f else 0.08f
                                val crossColor = if (sharesComposer) Color(0xFFFFD166) else Color(0xFF00E5FF)
                                drawLine(
                                    color = crossColor.copy(alpha = alpha),
                                    start = p1,
                                    end = p2,
                                    strokeWidth = if (highlighted) 2.dp.toPx() else 1.2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                                )
                            }
                        }
                    }
                }

                // Center Hub Label
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MidnightNavy.copy(alpha = 0.92f))
                        .border(1.5.dp, ElectricCyan.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${movies.size} FILMS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = ElectricCyan
                    )
                }

                // Creator Dots (Composers, Directors, Actors, Genres)
                creatorNodes.forEach { cNode ->
                    val pos = creatorCoords[cNode.id] ?: return@forEach
                    val isSelected = selectedCreatorId == cNode.id
                    val isConnectedToSelectedMovie = selectedMovieId != null && cNode.movies.any { it.id == selectedMovieId }
                    val dotSizePx = with(density) { 18.dp.toPx() }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (pos.x - dotSizePx * 2.2f).roundToInt(),
                                    (pos.y - dotSizePx * 0.7f).roundToInt()
                                )
                            }
                            .widthIn(max = 86.dp)
                            .clickable { onSelectCreator(cNode) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected || isConnectedToSelectedMovie) 18.dp else 13.dp)
                                .clip(CircleShape)
                                .background(cNode.kind.accent)
                                .border(1.5.dp, Color.White, CircleShape)
                        )
                        Text(
                            text = cNode.name.split(" ").lastOrNull() ?: cNode.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                fontWeight = if (isSelected || isConnectedToSelectedMovie) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected || isConnectedToSelectedMovie) cNode.kind.accent else FrostWhite.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DeepBlack.copy(alpha = 0.75f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                // Movie Poster Nodes along Outer Web Orbital
                movies.forEach { m ->
                    val pos = movieCoords[m.id] ?: return@forEach
                    val isSelected = selectedMovieId == m.id
                    val isLinkedToCreator = selectedCreatorId != null &&
                        creatorNodes.find { it.id == selectedCreatorId }?.movies?.any { it.id == m.id } == true

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (pos.x - posterWPx / 2f).roundToInt(),
                                    (pos.y - posterHPx / 2f).roundToInt()
                                )
                            }
                            .width(posterW)
                            .height(posterH)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (isSelected || isLinkedToCreator) 2.2.dp else 1.dp,
                                color = if (isSelected) ElectricCyan else if (isLinkedToCreator) Color(0xFFFFD166) else Color.White.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectMovie(m) }
                    ) {
                        MoviePosterImage(
                            posterUrl = m.posterUrl,
                            title = m.title,
                            signatureColorHex = m.signatureColorHex,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            showFallbackTitle = false,
                            releaseYear = m.releaseYear,
                            movieId = m.id
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixGridAndChartsView(
    movies: List<MovieItem>,
    creatorNodes: List<MatrixCreatorNode>,
    composerMap: Map<String, List<MovieItem>>,
    directorMap: Map<String, List<MovieItem>>,
    genreMap: Map<String, List<MovieItem>>,
    onMovieClick: (MovieItem) -> Unit
) {
    // 1. Visual Matrix Co-Occurrence Table
    val columns = remember(creatorNodes) { creatorNodes.take(8) }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("movie_matrix_grid_card"),
        cornerRadius = 22.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "MOVIE CONNECTION MATRIX",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ElectricCyan
            )
            Text(
                text = "Dot intersections show shared music composers, directors, cast & genres",
                style = MaterialTheme.typography.bodySmall,
                color = SteelBlue
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header columns
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MOVIE TITLE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = IcyBlue,
                        modifier = Modifier.width(126.dp)
                    )
                    columns.forEach { col ->
                        Text(
                            text = col.name.split(" ").lastOrNull() ?: col.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = col.kind.accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(64.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                movies.forEach { movie ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onMovieClick(movie) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.width(126.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MoviePosterImage(
                                posterUrl = movie.posterUrl,
                                title = movie.title,
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop,
                                showFallbackTitle = false,
                                releaseYear = movie.releaseYear,
                                movieId = movie.id
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = movie.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = FrostWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        columns.forEach { col ->
                            val hasEdge = col.movies.any { it.id == movie.id }
                            Box(
                                modifier = Modifier.width(64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (hasEdge) 13.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (hasEdge) col.kind.accent else Color.White.copy(alpha = 0.12f)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. Charts & Breakdown Bars (Composers, Directors, Genres)
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "CREATOR & GENRE DISTRIBUTION CHARTS",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFFFD166)
            )

            MatrixBarGroup("Top Music Composers", composerMap.mapValues { it.value.size }, Color(0xFFFFD166))
            MatrixBarGroup("Top Directors", directorMap.mapValues { it.value.size }, Color(0xFF00E5FF))
            MatrixBarGroup("Top Genres", genreMap.mapValues { it.value.size }, Color(0xFFE040FB))
        }
    }
}

@Composable
private fun MatrixBarGroup(
    title: String,
    counts: Map<String, Int>,
    barColor: Color
) {
    if (counts.isEmpty()) return
    val topItems = counts.entries.take(4)
    val maxVal = (topItems.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = IcyBlue)
        topItems.forEach { (label, count) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = FrostWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(115.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF102033))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((count.toFloat() / maxVal.toFloat()).coerceIn(0.12f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(barColor)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = barColor
                )
            }
        }
    }
}

@Composable
private fun MindMapHierarchyView(
    composerMap: Map<String, List<MovieItem>>,
    directorMap: Map<String, List<MovieItem>>,
    actorMap: Map<String, List<MovieItem>>,
    genreMap: Map<String, List<MovieItem>>,
    onMovieClick: (MovieItem) -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("movie_mind_map_card"),
        cornerRadius = 22.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "CINEMA MIND MAP",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF00F5D4)
            )
            Text(
                text = "Branching tree of Soundtracks, Directors, Cast & Genres linked to your film posters",
                style = MaterialTheme.typography.bodySmall,
                color = SteelBlue
            )

            // Visual Root -> 4 Branch Connector Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                val root = Offset(size.width / 2f, 12f)
                val branchTargets = listOf(0.14f, 0.38f, 0.62f, 0.86f).map { Offset(size.width * it, size.height - 6f) }
                val branchColors = listOf(
                    MatrixEntityKind.COMPOSER.accent,
                    MatrixEntityKind.DIRECTOR.accent,
                    MatrixEntityKind.ACTOR.accent,
                    MatrixEntityKind.GENRE.accent
                )
                branchTargets.forEachIndexed { idx, target ->
                    val path = Path().apply {
                        moveTo(root.x, root.y)
                        cubicTo(
                            root.x, size.height * 0.5f,
                            target.x, size.height * 0.5f,
                            target.x, target.y
                        )
                    }
                    drawPath(
                        path = path,
                        color = branchColors[idx].copy(alpha = 0.85f),
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawCircle(branchColors[idx], radius = 5.dp.toPx(), center = target)
                }
                drawCircle(ElectricCyan, radius = 7.dp.toPx(), center = root)
            }

            val trunks = listOf(
                Triple("Soundtracks & Composers", MatrixEntityKind.COMPOSER.accent, composerMap),
                Triple("Directors", MatrixEntityKind.DIRECTOR.accent, directorMap),
                Triple("Cast & Actors", MatrixEntityKind.ACTOR.accent, actorMap.entries.take(5).associate { it.key to it.value }),
                Triple("Genres", MatrixEntityKind.GENRE.accent, genreMap.entries.take(5).associate { it.key to it.value })
            )

            trunks.forEach { (branchName, branchColor, map) ->
                if (map.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0D1B2E).copy(alpha = 0.72f))
                            .border(1.dp, branchColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = branchName.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = branchColor
                        )
                        map.entries.take(4).forEach { (entityName, movies) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(branchColor.copy(alpha = 0.16f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = entityName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FrostWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(" ─▶ ", color = branchColor, style = MaterialTheme.typography.labelSmall)
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    movies.forEach { m ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MidnightNavy.copy(alpha = 0.85f))
                                                .clickable { onMovieClick(m) }
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            MoviePosterImage(
                                                posterUrl = m.posterUrl,
                                                title = m.title,
                                                modifier = Modifier
                                                    .width(20.dp)
                                                    .height(28.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                contentScale = ContentScale.Crop,
                                                showFallbackTitle = false,
                                                releaseYear = m.releaseYear,
                                                movieId = m.id
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = m.title,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = FrostWhite,
                                                maxLines = 1
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
    }
}

@Composable
private fun ConnectionPosterGroupSection(
    sectionTitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    groups: Map<String, List<MovieItem>>,
    onMovieClick: (MovieItem) -> Unit
) {
    Text(text = sectionTitle, style = MaterialTheme.typography.titleSmall, color = accentColor)
    groups.forEach { (entityName, movies) ->
        if (movies.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.requiredSize(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = entityName,
                            style = MaterialTheme.typography.titleSmall,
                            color = FrostWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${movies.size} films)",
                            style = MaterialTheme.typography.labelSmall,
                            color = SteelBlue,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(movies, key = { "${entityName}_${it.id}" }) { movie ->
                            Column(
                                modifier = Modifier
                                    .width(104.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onMovieClick(movie) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                MoviePosterImage(
                                    posterUrl = movie.posterUrl,
                                    title = movie.title,
                                    modifier = Modifier
                                        .width(104.dp)
                                        .height(144.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop,
                                    releaseYear = movie.releaseYear,
                                    movieId = movie.id
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = movie.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FrostWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
