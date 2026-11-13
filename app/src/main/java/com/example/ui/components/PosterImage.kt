package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.data.CineverseRepository
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FrostWhite
import com.example.ui.theme.IcyBlue
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.SteelBlue

/**
 * Reliable Movie Poster Loader that:
 * 1. Attaches Wikimedia/iTunes compliant User-Agent headers.
 * 2. Auto-heals missing or failed poster URLs in real time via repository.resolveBestPoster().
 * 3. Renders a clean loading state during Coil fetch and a consistent 'IMAGE NOT AVAILABLE'
 *    fallback graphic when no poster is available or Coil returns an error.
 *    Fallback UI is strictly suppressed while Coil State.Success is active.
 */
@Composable
fun MoviePosterImage(
    posterUrl: String,
    title: String,
    modifier: Modifier = Modifier,
    signatureColorHex: String = "#54C8F5",
    contentScale: ContentScale = ContentScale.Crop,
    alpha: Float = 1f,
    showFallbackTitle: Boolean = true,
    releaseYear: Int = 0,
    movieId: String = ""
) {
    val context = LocalContext.current
    val repository = remember(context) { CineverseRepository(context) }
    val sharedLoader = remember(context) { CineverseRepository.sharedImageLoader(context) }

    val sigColor = remember(signatureColorHex) {
        try {
            Color(android.graphics.Color.parseColor(signatureColorHex))
        } catch (_: Exception) {
            ElectricCyan
        }
    }

    val initialUrl = remember(posterUrl, repository) {
        val clean = repository.sanitizePosterUrl(posterUrl)
        if (clean in CineverseRepository.failedPosterUrls) "" else clean
    }

    var resolvedUrl by remember(initialUrl, title, releaseYear) { mutableStateOf(initialUrl) }
    var attemptedCount by remember(initialUrl, title, releaseYear) { mutableIntStateOf(0) }
    var isHealing by remember(initialUrl, title, releaseYear) { mutableStateOf(false) }

    LaunchedEffect(initialUrl, title, releaseYear) {
        if (initialUrl.isBlank() && title.isNotBlank() && attemptedCount == 0) {
            attemptedCount = 1
            isHealing = true
            val healed = repository.healFailedPoster(
                movieId = movieId,
                title = title,
                releaseYear = if (releaseYear > 1900) releaseYear else 2024,
                failedUrl = posterUrl
            )
            if (healed.isNotBlank()) {
                resolvedUrl = healed
            }
            isHealing = false
        }
    }

    val imageRequest = remember(resolvedUrl) {
        ImageRequest.Builder(context)
            .data(resolvedUrl.ifBlank { null })
            .crossfade(true)
            .build()
    }

    val painter = rememberAsyncImagePainter(model = imageRequest, imageLoader = sharedLoader)
    val state = painter.state

    // If the given URL fails to decode/fetch, record it as failed and heal from alternative backend sources
    LaunchedEffect(state, resolvedUrl, title, releaseYear) {
        if (state is AsyncImagePainter.State.Error && attemptedCount < 2 && title.isNotBlank()) {
            attemptedCount += 1
            isHealing = true
            val failed = resolvedUrl
            val healed = repository.healFailedPoster(
                movieId = movieId,
                title = title,
                releaseYear = if (releaseYear > 1900) releaseYear else 2024,
                failedUrl = failed
            )
            if (healed.isNotBlank() && healed != failed) {
                resolvedUrl = healed
            }
            isHealing = false
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val showLoadingState = isHealing || (resolvedUrl.isNotBlank() && state is AsyncImagePainter.State.Loading)
        val showErrorFallback = !showLoadingState && (resolvedUrl.isBlank() || state is AsyncImagePainter.State.Error)

        if (showLoadingState) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                sigColor.copy(alpha = 0.24f * alpha),
                                MidnightNavy.copy(alpha = 0.94f * alpha),
                                Color(0xFF060A12).copy(alpha = alpha)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (showFallbackTitle) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = sigColor.copy(alpha = 0.85f),
                        strokeWidth = 2.dp
                    )
                }
            }
        } else if (showErrorFallback && state !is AsyncImagePainter.State.Success) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                sigColor.copy(alpha = 0.26f * alpha),
                                MidnightNavy.copy(alpha = 0.94f * alpha),
                                Color(0xFF060A12).copy(alpha = alpha)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (showFallbackTitle) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(sigColor.copy(alpha = 0.14f))
                                .border(1.dp, sigColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = "Image not available",
                                tint = sigColor.copy(alpha = 0.92f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "IMAGE NOT AVAILABLE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = IcyBlue.copy(alpha = 0.90f),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (title.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = title.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.4.sp
                                ),
                                color = FrostWhite.copy(alpha = 0.80f),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (resolvedUrl.isNotBlank() && state !is AsyncImagePainter.State.Error) {
            Image(
                painter = painter,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                alpha = alpha
            )
        }
    }
}

/**
 * Reliable Wikipedia / Wikimedia Commons Portrait Loader for Directors and Actors.
 * Attaches a compliant User-Agent so Wikimedia Commons thumbnails never return HTTP 403.
 */
@Composable
fun PersonPortraitImage(
    avatarUrl: String,
    name: String,
    modifier: Modifier = Modifier,
    accentColor: Color = ElectricCyan,
    isDirector: Boolean = false
) {
    val context = LocalContext.current
    val sharedLoader = remember(context) { CineverseRepository.sharedImageLoader(context) }
    val sanitizedUrl = remember(avatarUrl) {
        avatarUrl.trim().replace("http://", "https://")
    }

    val imageRequest = remember(sanitizedUrl) {
        ImageRequest.Builder(context)
            .data(sanitizedUrl.ifBlank { null })
            .crossfade(true)
            .build()
    }

    val painter = rememberAsyncImagePainter(model = imageRequest, imageLoader = sharedLoader)
    val state = painter.state

    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (isDirector) 0.35f else 0.22f),
                    MidnightNavy
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        if (state !is AsyncImagePainter.State.Success || sanitizedUrl.isBlank()) {
            val initials = remember(name) {
                name.trim()
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.take(1).uppercase() }
                    .ifBlank { "?" }
            }
            if (initials != "?") {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = if (isDirector) accentColor else FrostWhite
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = name,
                    tint = SteelBlue,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        if (sanitizedUrl.isNotBlank() && state !is AsyncImagePainter.State.Error) {
            Image(
                painter = painter,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
