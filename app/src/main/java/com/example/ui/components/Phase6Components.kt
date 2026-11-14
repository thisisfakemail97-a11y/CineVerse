package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FrostWhite
import com.example.ui.theme.IcyBlue
import com.example.ui.theme.LocalGlassTransparency
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SteelBlue
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Rewatch Symbol Button with the rewatch count rendered directly INSIDE the circular replay symbol itself
 * (never outside it or anywhere else).
 */
@Composable
fun RewatchSymbolCounterButton(
    rewatchCount: Int,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    accentColor: Color = Color(0xFF00F5D4),
    testTagName: String = "rewatch_counter_button",
    isWatched: Boolean = false,
    compact: Boolean = false,
    testTag: String = "",
    onClick: () -> Unit
) {
    val glassTransparency = LocalGlassTransparency.current.coerceIn(0f, 1f)
    val opacityFactor = (1f - glassTransparency).coerceIn(0f, 1f)
    val rimFactor = (0.25f + 0.75f * opacityFactor).coerceIn(0f, 1f)
    val effectiveSize = if (compact) size.coerceAtLeast(42.dp) else size.coerceAtLeast(46.dp)
    val effectiveTestTag = if (testTag.isNotBlank()) testTag else testTagName
    val infiniteTransition = rememberInfiniteTransition(label = "rewatch_breath")
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rewatch_alpha"
    )

    val isActive = isWatched || rewatchCount > 0
    val ringColor = if (isActive) accentColor else IcyBlue
    val squircleCorner = 14.dp
    val squircleShape = RoundedCornerShape(squircleCorner)

    Box(
        modifier = modifier
            .size(effectiveSize)
            .clip(squircleShape)
            .background(
                Brush.verticalGradient(
                    colors = if (isActive) {
                        listOf(
                            accentColor.copy(alpha = (0.36f * opacityFactor).coerceIn(0f, 1f)),
                            Color(0xFF0E1D2F).copy(alpha = (0.94f * opacityFactor).coerceIn(0f, 1f))
                        )
                    } else {
                        listOf(
                            Color(0xFF1E324A).copy(alpha = (0.86f * opacityFactor).coerceIn(0f, 1f)),
                            Color(0xFF0C1624).copy(alpha = (0.94f * opacityFactor).coerceIn(0f, 1f))
                        )
                    }
                )
            )
            .drawWithContent {
                drawContent()
                val strokeW = 1.35.dp.toPx()
                val rPx = squircleCorner.toPx()
                val effAlpha = breathAlpha * rimFactor
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = effAlpha),
                            ringColor.copy(alpha = effAlpha),
                            Color.White.copy(alpha = effAlpha * 0.6f)
                        )
                    ),
                    topLeft = Offset(strokeW / 2f, strokeW / 2f),
                    size = Size(this.size.width - strokeW, this.size.height - strokeW),
                    cornerRadius = CornerRadius(rPx, rPx),
                    style = Stroke(width = strokeW)
                )
            }
            .clickable { onClick() }
            .testTag(effectiveTestTag),
        contentAlignment = Alignment.Center
    ) {
        // Draw circular replay arrow arc around the inner number so the number sits strictly INSIDE the rewatch symbol
        Canvas(modifier = Modifier.size(effectiveSize * 0.72f)) {
            val strokePx = 2.dp.toPx()
            val arcPadding = strokePx * 1.1f
            val arcDiameter = this.size.minDimension - arcPadding * 2f

            drawArc(
                color = ringColor.copy(alpha = (0.78f + 0.22f * breathAlpha).coerceIn(0f, 1f)),
                startAngle = -55f,
                sweepAngle = 295f,
                useCenter = false,
                topLeft = Offset(arcPadding, arcPadding),
                size = Size(arcDiameter, arcDiameter),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Arrowhead at the top-right end of the replay loop (-55 degrees)
            val radius = arcDiameter / 2f
            val centerPt = Offset(this.size.width / 2f, this.size.height / 2f)
            val angleRad = Math.toRadians(-55.0)
            val tipX = (centerPt.x + radius * cos(angleRad)).toFloat()
            val tipY = (centerPt.y + radius * sin(angleRad)).toFloat()

            val arrowHead = Path().apply {
                moveTo(tipX + 3.dp.toPx(), tipY + 1.dp.toPx())
                lineTo(tipX - 3.5.dp.toPx(), tipY - 2.5.dp.toPx())
                lineTo(tipX - 2.dp.toPx(), tipY + 4.dp.toPx())
                close()
            }
            drawPath(
                path = arrowHead,
                color = ringColor
            )
        }

        // Numeric rewatch count strictly inside the circular rewatch symbol
        Text(
            text = rewatchCount.coerceAtLeast(0).toString(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp
            ),
            color = if (isActive) FrostWhite else IcyBlue
        )
    }
}

/**
 * Three Uniform Squircle Buttons Directly Below the Movie Name (Save, Watched, Rewatch with count inside)
 * + Inline Expandable 1-to-10 Rating Slider & Review Section that ONLY appears when Watched is active.
 */
@Composable
fun ThreeMovieStatusButtonsWithWatchedRating(
    movieId: String,
    isWatchlist: Boolean,
    isWatched: Boolean,
    rewatchCount: Int,
    userRating: Int,
    userReview: String,
    accentColor: Color = ElectricCyan,
    showReviewEditor: Boolean = true,
    onWatchlistClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onRewatchClick: () -> Unit,
    onRatingAndReviewSaved: (rating: Int, review: String) -> Unit
) {
    val glassTransparency = LocalGlassTransparency.current.coerceIn(0f, 1f)
    val opacityFactor = (1f - glassTransparency).coerceIn(0f, 1f)
    val rimFactor = (0.25f + 0.75f * opacityFactor).coerceIn(0f, 1f)

    var sliderValue by remember(movieId, userRating, isWatched) {
        mutableStateOf(if (userRating > 0) userRating.toFloat() else 8f)
    }
    var reviewDraft by remember(movieId, userReview) {
        mutableStateOf(userReview)
    }
    var savedConfirmation by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "three_btns_breath")
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "three_btns_alpha"
    )

    val squircleCorner = 15.dp
    val squircleShape = RoundedCornerShape(squircleCorner)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Save / Watchlist Squircle Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(squircleShape)
                    .background(
                        if (isWatchlist) {
                            Brush.horizontalGradient(
                                listOf(
                                    ElectricCyan.copy(alpha = (0.38f * opacityFactor).coerceIn(0f, 1f)),
                                    Color(0xFF153450).copy(alpha = (0.94f * opacityFactor).coerceIn(0f, 1f))
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF1B2E44).copy(alpha = (0.86f * opacityFactor).coerceIn(0f, 1f)),
                                    Color(0xFF0E1928).copy(alpha = (0.94f * opacityFactor).coerceIn(0f, 1f))
                                )
                            )
                        }
                    )
                    .drawWithContent {
                        drawContent()
                        val strokeW = if (isWatchlist) 1.5.dp.toPx() else 1.2.dp.toPx()
                        val rPx = squircleCorner.toPx()
                        drawRoundRect(
                            color = if (isWatchlist) ElectricCyan.copy(alpha = breathAlpha * rimFactor) else IcyBlue.copy(alpha = 0.38f * breathAlpha * rimFactor),
                            topLeft = Offset(strokeW / 2f, strokeW / 2f),
                            size = Size(size.width - strokeW, size.height - strokeW),
                            cornerRadius = CornerRadius(rPx, rPx),
                            style = Stroke(width = strokeW)
                        )
                    }
                    .clickable { onWatchlistClick() }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("status_watchlist_btn_$movieId"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isWatchlist) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "Save to Watchlist",
                        tint = if (isWatchlist) ElectricCyan else FrostWhite,
                        modifier = Modifier.requiredSize(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isWatchlist) "Saved" else "Save",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.5.sp,
                            lineHeight = 14.sp
                        ),
                        color = FrostWhite,
                        fontWeight = if (isWatchlist) FontWeight.Bold else FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 2. Watched Squircle Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(squircleShape)
                    .background(
                        if (isWatched) {
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF00F5D4).copy(alpha = (0.40f * opacityFactor).coerceIn(0f, 1f)),
                                    Color(0xFF103538).copy(alpha = (0.94f * opacityFactor).coerceIn(0f, 1f))
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF1B2E44).copy(alpha = (0.86f * opacityFactor).coerceIn(0f, 1f)),
                                    Color(0xFF0E1928).copy(alpha = (0.94f * opacityFactor).coerceIn(0f, 1f))
                                )
                            )
                        }
                    )
                    .drawWithContent {
                        drawContent()
                        val strokeW = if (isWatched) 1.5.dp.toPx() else 1.2.dp.toPx()
                        val rPx = squircleCorner.toPx()
                        drawRoundRect(
                            color = if (isWatched) Color(0xFF00F5D4).copy(alpha = breathAlpha * rimFactor) else IcyBlue.copy(alpha = 0.38f * breathAlpha * rimFactor),
                            topLeft = Offset(strokeW / 2f, strokeW / 2f),
                            size = Size(size.width - strokeW, size.height - strokeW),
                            cornerRadius = CornerRadius(rPx, rPx),
                            style = Stroke(width = strokeW)
                        )
                    }
                    .clickable { onWatchedClick() }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("status_watched_btn_$movieId"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isWatched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = "Mark Watched",
                        tint = if (isWatched) Color(0xFF00F5D4) else FrostWhite,
                        modifier = Modifier.requiredSize(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isWatched) "Watched" else "Watch",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.5.sp,
                            lineHeight = 14.sp
                        ),
                        color = FrostWhite,
                        fontWeight = if (isWatched) FontWeight.Bold else FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3. Rewatch Squircle Symbol Button with the number inside it
            RewatchSymbolCounterButton(
                rewatchCount = if (isWatched) rewatchCount.coerceAtLeast(1) else 0,
                size = 48.dp,
                accentColor = accentColor,
                testTagName = "status_rewatch_btn_$movieId",
                isWatched = isWatched,
                onClick = onRewatchClick
            )
        }

        // Strict Rule: ONLY when Watched is active do the 1-to-10 Rating Slider & Review appear inline below!
        AnimatedVisibility(
            visible = isWatched,
            enter = expandVertically(animationSpec = tween(280)) + fadeIn(animationSpec = tween(240)),
            exit = shrinkVertically(animationSpec = tween(220)) + fadeOut(animationSpec = tween(180))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0B1726).copy(alpha = (0.88f * opacityFactor).coerceIn(0f, 1f)))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF00F5D4).copy(alpha = 0.38f * rimFactor),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(14.dp)
                    .testTag("watched_rating_review_panel_$movieId")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YOUR RATING (1 – 10)",
                        style = MaterialTheme.typography.labelSmall,
                        color = IcyBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color(0xFF00F5D4).copy(alpha = 0.18f))
                            .border(1.dp, Color(0xFF00F5D4).copy(alpha = 0.55f), RoundedCornerShape(11.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${sliderValue.roundToInt()} / 10",
                            style = MaterialTheme.typography.labelMedium,
                            color = FrostWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        savedConfirmation = false
                    },
                    onValueChangeFinished = {
                        val finalScore = sliderValue.roundToInt().coerceIn(1, 10)
                        onRatingAndReviewSaved(finalScore, reviewDraft)
                        savedConfirmation = true
                    },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00F5D4),
                        activeTrackColor = ElectricCyan,
                        inactiveTrackColor = SteelBlue.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("watched_rating_slider_$movieId")
                )

                if (showReviewEditor) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = reviewDraft,
                        onValueChange = {
                            reviewDraft = it
                            savedConfirmation = false
                        },
                        placeholder = {
                            Text(
                                "Write your post-watch review or thoughts...",
                                color = MutedSlate,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("watched_review_input_$movieId"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = SteelBlue.copy(alpha = 0.45f),
                            focusedTextColor = FrostWhite,
                            unfocusedTextColor = FrostWhite,
                            cursorColor = ElectricCyan
                        ),
                        shape = RoundedCornerShape(14.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassPillButton(
                            text = if (savedConfirmation) "Saved ✓" else "Save Review",
                            isActive = true,
                            onClick = {
                                val finalScore = sliderValue.roundToInt().coerceIn(1, 10)
                                onRatingAndReviewSaved(finalScore, reviewDraft)
                                savedConfirmation = true
                            },
                            modifier = Modifier.testTag("save_watched_review_btn_$movieId")
                        )
                    }
                }
            }
        }
    }
}
