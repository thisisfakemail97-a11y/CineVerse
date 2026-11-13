package com.example.ui.profile

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPillButton
import com.example.ui.components.InteractiveColorWheel
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FrostWhite
import com.example.ui.theme.IcyBlue
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.SteelBlue
import com.example.ui.theme.ThemeMode
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    currentThemeMode: ThemeMode = ThemeMode.LIQUID_GLASS,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    currentAccentColor: Color = ElectricCyan,
    onAccentColorChange: (Color) -> Unit = {},
    userLibraryCount: Int = 0,
    diaryCount: Int = 0,
    historyCount: Int = 0,
    currentUserId: String? = null,
    onExportBackup: () -> String = { "" },
    onPreviewBackup: (String) -> Triple<Boolean, Int, Int> = { Triple(false, 0, 0) },
    onRestoreBackup: (String) -> Pair<Int, Int> = { Pair(0, 0) }
) {
    val context = LocalContext.current
    val repository = remember { com.example.data.CineverseRepository(context) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val selectedRegion by repository.selectedRegion.collectAsStateWithLifecycle()
    val currentUser by repository.currentUser.collectAsStateWithLifecycle()
    val isSyncing by repository.isSyncing.collectAsStateWithLifecycle()
    val lastSyncTime by repository.lastSyncTime.collectAsStateWithLifecycle()

    val isSignedInWithGoogle = currentUser != null || currentUserId != null
    val googleAccountEmail = currentUser?.email?.takeIf { it.isNotBlank() }
        ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUserId
        ?: ""
    var authBusy by remember { mutableStateOf(false) }

    val availableRegions = listOf("India", "USA", "UK", "Canada", "Australia", "Japan", "Global")

    // Backup & Restore Dialog States
    var showExportDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreInputJson by remember { mutableStateOf("") }
    var restorePreview by remember { mutableStateOf<Triple<Boolean, Int, Int>?>(null) }
    var restoreResultReport by remember { mutableStateOf<String?>(null) }
    var exportedJsonText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(bottom = 120.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                    text = "Personal Hub",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                    color = FrostWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Themes, Vault & Multi-Device Cloud Sync",
                    style = MaterialTheme.typography.bodySmall,
                    color = SteelBlue,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(currentAccentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User Profile",
                    tint = currentAccentColor,
                    modifier = Modifier.requiredSize(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Dedicated "Themes" Section
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
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Themes", style = MaterialTheme.typography.titleMedium, color = FrostWhite)
                        Text(
                            text = "Optical refraction, contour edge glow & glass lens height",
                            style = MaterialTheme.typography.bodySmall,
                            color = SteelBlue
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Themes",
                        tint = currentAccentColor,
                        modifier = Modifier.requiredSize(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        val isSelected = currentThemeMode == mode
                        ThemeOptionRow(
                            mode = mode,
                            isSelected = isSelected,
                            accentColor = currentAccentColor,
                            onClick = { onThemeModeChange(mode) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Interactive Theme Color Wheel
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Accent Color Wheel", style = MaterialTheme.typography.titleMedium, color = FrostWhite)
                        Text("Drag wheel to customize active lighting aura", style = MaterialTheme.typography.bodySmall, color = SteelBlue)
                    }

                    TextButton(onClick = { onAccentColorChange(ElectricCyan) }) {
                        Text("Reset", color = currentAccentColor, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                InteractiveColorWheel(
                    selectedColor = currentAccentColor,
                    onColorSelected = onAccentColorChange
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Preset Swatches (Wrapped cleanly with FlowRow)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Cyan" to Color(0xFF54C8F5),
                        "Emerald" to Color(0xFF00F5D4),
                        "Amethyst" to Color(0xFFE040FB),
                        "Sunset" to Color(0xFFFF007F),
                        "Azure" to Color(0xFF4361EE)
                    ).forEach { (name, color) ->
                        GlassPillButton(
                            text = name,
                            isActive = currentAccentColor == color,
                            onClick = { onAccentColorChange(color) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. Cloud Persistence & Multi-Device Sync Status
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
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text("Cloud Vault & Synchronization", style = MaterialTheme.typography.titleMedium, color = FrostWhite)
                        Text(
                            text = if (!isSignedInWithGoogle) "Local Vault: Sign in with Gmail below to sync across devices"
                            else "Synced with ${googleAccountEmail.ifBlank { "Google Account" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SteelBlue
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isSignedInWithGoogle) SteelBlue.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (!isSignedInWithGoogle) "LOCAL VAULT" else "CLOUD SYNC",
                            color = if (!isSignedInWithGoogle) SteelBlue else ElectricCyan,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f).height(68.dp),
                        cornerRadius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("$userLibraryCount", style = MaterialTheme.typography.titleMedium, color = currentAccentColor, maxLines = 1)
                            Text("Saved Titles", style = MaterialTheme.typography.labelSmall, color = SteelBlue, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    GlassCard(
                        modifier = Modifier.weight(1f).height(68.dp),
                        cornerRadius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("$diaryCount", style = MaterialTheme.typography.titleMedium, color = currentAccentColor, maxLines = 1)
                            Text("Diary Entries", style = MaterialTheme.typography.labelSmall, color = SteelBlue, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    GlassCard(
                        modifier = Modifier.weight(1f).height(68.dp),
                        cornerRadius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("$historyCount", style = MaterialTheme.typography.titleMedium, color = currentAccentColor, maxLines = 1)
                            Text("Rewatch Logs", style = MaterialTheme.typography.labelSmall, color = SteelBlue, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4. Data Backup & Restore (Full JSON Specification with Duplicate Detection)
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
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Data Backup & Restore", style = MaterialTheme.typography.titleMedium, color = FrostWhite)
                        Text("Export or restore library, diary & viewing records", style = MaterialTheme.typography.bodySmall, color = SteelBlue)
                    }
                    Icon(Icons.Default.Security, contentDescription = "Security", tint = currentAccentColor, modifier = Modifier.requiredSize(20.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassPillButton(
                        text = "Export JSON",
                        icon = Icons.Default.FileDownload,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            exportedJsonText = onExportBackup()
                            showExportDialog = true
                        }
                    )
                    GlassPillButton(
                        text = "Restore Vault",
                        icon = Icons.Default.FileUpload,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            restoreInputJson = ""
                            restorePreview = null
                            restoreResultReport = null
                            showRestoreDialog = true
                        }
                    )
                }

                if (restoreResultReport != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = restoreResultReport!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 5. Preferences & Personal Gmail Account Sync
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Preferences & Account Sync", style = MaterialTheme.typography.titleMedium, color = FrostWhite)
                Spacer(modifier = Modifier.height(14.dp))

                // Streaming Region
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Streaming Region", color = FrostWhite, style = MaterialTheme.typography.bodyMedium)
                        Text("Where to Watch provider catalog", color = SteelBlue, style = MaterialTheme.typography.bodySmall)
                    }
                    GlassPillButton(
                        text = selectedRegion,
                        onClick = {
                            val nextIdx = (availableRegions.indexOf(selectedRegion) + 1) % availableRegions.size
                            repository.setSelectedRegion(availableRegions[nextIdx])
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = SteelBlue.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                // Real Google Sign-In & Firestore Multi-Device Sync
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Text("Gmail Sign-In & Cloud Sync", style = MaterialTheme.typography.titleMedium, color = FrostWhite)
                            Text(
                                text = when {
                                    isSyncing -> "Synchronizing library, diary & history..."
                                    isSignedInWithGoogle -> buildString {
                                        append("Connected: $googleAccountEmail")
                                        if (lastSyncTime.isNotBlank()) append(" • Synced $lastSyncTime")
                                    }
                                    else -> "Sign in with your Gmail ID to auto-sync across devices"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSignedInWithGoogle) IcyBlue else SteelBlue
                            )
                        }

                        if (isSignedInWithGoogle) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF00F5D4).copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF00F5D4), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(13.dp),
                                        color = Color(0xFF00F5D4),
                                        strokeWidth = 1.8.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Synced",
                                        tint = Color(0xFF00F5D4),
                                        modifier = Modifier.requiredSize(15.dp)
                                    )
                                }
                                Text(
                                    text = if (isSyncing) "Syncing" else "Synced",
                                    color = Color(0xFF00F5D4),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isSignedInWithGoogle) {
                        GlassPillButton(
                            text = if (authBusy) "Connecting Google..." else "Sign in with Gmail ID",
                            icon = Icons.Default.AccountCircle,
                            isActive = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("google_sign_in_button"),
                            onClick = {
                                if (authBusy) return@GlassPillButton
                                val activity = context.findActivity()
                                if (activity == null) {
                                    Toast.makeText(context, "Unable to launch Google Sign-In UI.", Toast.LENGTH_SHORT).show()
                                    return@GlassPillButton
                                }
                                coroutineScope.launch {
                                    authBusy = true
                                    try {
                                        val webClientId = context.getString(R.string.default_web_client_id)
                                        val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                                        val request = GetCredentialRequest.Builder()
                                            .addCredentialOption(signInOption)
                                            .build()
                                        val credentialManager = CredentialManager.create(activity)
                                        val result = credentialManager.getCredential(activity, request)
                                        val cred = result.credential
                                        if (cred is CustomCredential &&
                                            cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                        ) {
                                            val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
                                            val signInRes = repository.signInWithGoogleIdToken(googleCred.idToken)
                                            signInRes.onSuccess { user ->
                                                Toast.makeText(
                                                    context,
                                                    "Signed in as ${user.email ?: "Google User"}! Sync active.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }.onFailure { err ->
                                                Toast.makeText(
                                                    context,
                                                    "Firebase Auth failed: ${err.localizedMessage ?: "Unknown error"}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    } catch (_: GetCredentialCancellationException) {
                                        // User dismissed Google account picker
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Google Sign-In note: ${e.localizedMessage ?: "Check Google Play Services"}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } finally {
                                        authBusy = false
                                    }
                                }
                            }
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassPillButton(
                                text = if (isSyncing) "Syncing..." else "Sync Now",
                                icon = Icons.Default.Sync,
                                isActive = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sync_now_button"),
                                onClick = {
                                    coroutineScope.launch {
                                        repository.refreshFromBackend()
                                        Toast.makeText(context, "Cloud sync completed.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            GlassPillButton(
                                text = "Sign Out",
                                icon = Icons.Default.Logout,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("google_sign_out_button"),
                                onClick = {
                                    coroutineScope.launch {
                                        withContext(NonCancellable) {
                                            try {
                                                CredentialManager.create(context)
                                                    .clearCredentialState(ClearCredentialStateRequest())
                                            } catch (_: Exception) { }
                                            repository.signOut()
                                        }
                                        Toast.makeText(context, "Signed out of Google Account.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 6. System Architecture Information
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Personal Entertainment Platform • v1.6.0",
                    style = MaterialTheme.typography.labelMedium,
                    color = FrostWhite.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cloud Synchronized • Credential Manager • Gemini Neural Engine",
                    style = MaterialTheme.typography.labelSmall,
                    color = SteelBlue
                )
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = MidnightNavy,
            title = { Text("Vault Backup Snapshot", color = FrostWhite) },
            text = {
                Column {
                    Text(
                        text = "Complete portable JSON backup containing $userLibraryCount titles, $diaryCount diary entries, and $historyCount viewing sessions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SteelBlue
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = exportedJsonText.take(300) + if (exportedJsonText.length > 300) "\n... [${exportedJsonText.length} bytes]" else "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(120.dp),
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
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Cineverse Backup", exportedJsonText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Backup copied to clipboard", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("Copy to Clipboard", color = DeepBlack)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, exportedJsonText)
                            type = "application/json"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Vault Backup JSON"))
                        showExportDialog = false
                    }) {
                        Text("Share JSON", color = IcyBlue)
                    }
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Close", color = FrostWhite)
                    }
                }
            }
        )
    }

    // Restore Dialog with Duplicate Detection & Preview
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            containerColor = MidnightNavy,
            title = { Text("Restore Vault from JSON", color = FrostWhite) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Paste your JSON backup payload below to inspect records before merging.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SteelBlue
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = restoreInputJson,
                        onValueChange = { input ->
                            restoreInputJson = input
                            restorePreview = if (input.isNotBlank()) onPreviewBackup(input) else null
                        },
                        placeholder = { Text("Paste JSON string here...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = SteelBlue,
                            focusedTextColor = FrostWhite,
                            unfocusedTextColor = FrostWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (restorePreview != null) {
                        val (isValid, newItems, dupItems) = restorePreview!!
                        if (isValid) {
                            Text("✓ Valid backup structure detected:", color = ElectricCyan, style = MaterialTheme.typography.labelSmall)
                            Text("• $newItems new titles will be imported.", color = FrostWhite, style = MaterialTheme.typography.bodySmall)
                            Text("• $dupItems duplicate titles detected (will be safely merged with highest ratings/rewatches).", color = IcyBlue, style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("⚠️ Invalid backup JSON format. Please verify the content.", color = Color(0xFFFF5252), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                val canRestore = restorePreview?.first == true
                Button(
                    onClick = {
                        val (imported, merged) = onRestoreBackup(restoreInputJson)
                        restoreResultReport = "✓ Successfully restored $imported new titles and merged $merged existing records."
                        showRestoreDialog = false
                    },
                    enabled = canRestore,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("Restore & Merge Safely", color = DeepBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel", color = FrostWhite)
                }
            }
        )
    }
}

@Composable
private fun ThemeOptionRow(
    mode: ThemeMode,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val previewColors = when (mode) {
        ThemeMode.LIQUID_GLASS -> listOf(Color(0xFF54C8F5), Color(0xFFE040FB), Color(0xFF00F5D4))
        ThemeMode.PRISM_GLASS -> listOf(Color(0xFF00F5D4), Color(0xFFFF007F), Color(0xFF00E5FF))
        ThemeMode.BIOLUMINESCENT_NEON -> listOf(Color(0xFF00FFA3), Color(0xFF00E5FF), Color(0xFF7209B7))
        ThemeMode.SMOKY_OBSIDIAN -> listOf(Color(0xFFE2E8F0), Color(0xFF64748B), Color(0xFF1E293B))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.14f)
                else Color(0xFF0E1826).copy(alpha = 0.55f)
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isSelected) listOf(Color.White, accentColor, previewColors.first())
                    else listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(previewColors))
                    .border(1.2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(13.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = mode.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) FrostWhite else FrostWhite.copy(alpha = 0.9f)
                )
                Text(
                    text = mode.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                    color = if (isSelected) IcyBlue else SteelBlue
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = accentColor,
                unselectedColor = SteelBlue
            )
        )
    }
}
