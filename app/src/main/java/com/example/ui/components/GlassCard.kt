package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FrostWhite
import com.example.ui.theme.IcyBlue
import com.example.ui.theme.LocalThemeMode
import com.example.ui.theme.ThemeMode

/**
 * Standout Optical Glass Card with clean static contour glow, top lens refraction,
 * and glowing glass edges.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.25.dp,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit
) {
    val themeMode = LocalThemeMode.current
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "glass_scale"
    )

    val glowIntensity by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.52f,
        animationSpec = tween(durationMillis = 220),
        label = "glass_glow"
    )

    val shape = RoundedCornerShape(cornerRadius)

    val backgroundBrush = when (themeMode) {
        ThemeMode.LIQUID_GLASS -> {
            val baseAlpha = if (isPressed) 0.84f else 0.58f
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF284264).copy(alpha = baseAlpha),
                    Color(0xFF112034).copy(alpha = baseAlpha + 0.12f),
                    Color(0xFF070F1B).copy(alpha = baseAlpha + 0.18f)
                )
            )
        }
        ThemeMode.PRISM_GLASS -> {
            val baseAlpha = if (isPressed) 0.82f else 0.56f
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF23395B).copy(alpha = baseAlpha),
                    Color(0xFF19223C).copy(alpha = baseAlpha + 0.10f),
                    Color(0xFF2B193D).copy(alpha = baseAlpha + 0.08f),
                    Color(0xFF09111E).copy(alpha = baseAlpha + 0.16f)
                ),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        }
        ThemeMode.BIOLUMINESCENT_NEON -> {
            val baseAlpha = if (isPressed) 0.86f else 0.64f
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0B2B3C).copy(alpha = baseAlpha),
                    Color(0xFF061726).copy(alpha = baseAlpha + 0.12f),
                    Color(0xFF030B14).copy(alpha = baseAlpha + 0.20f)
                )
            )
        }
        ThemeMode.SMOKY_OBSIDIAN -> {
            val baseAlpha = if (isPressed) 0.90f else 0.76f
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1E2430).copy(alpha = baseAlpha),
                    Color(0xFF11151E).copy(alpha = baseAlpha + 0.08f),
                    Color(0xFF090C12).copy(alpha = baseAlpha + 0.14f)
                )
            )
        }
    }

    val contourGlowColors = when (themeMode) {
        ThemeMode.LIQUID_GLASS -> listOf(
            Color.White.copy(alpha = 0.28f * glowIntensity),
            ElectricCyan.copy(alpha = 0.34f * glowIntensity),
            Color(0xFFE040FB).copy(alpha = 0.22f * glowIntensity),
            ElectricCyan.copy(alpha = 0.14f * glowIntensity)
        )
        ThemeMode.PRISM_GLASS -> listOf(
            Color(0xFF00F5D4).copy(alpha = 0.36f * glowIntensity),
            Color(0xFFFF007F).copy(alpha = 0.30f * glowIntensity),
            Color(0xFF00E5FF).copy(alpha = 0.22f * glowIntensity),
            Color(0xFF4361EE).copy(alpha = 0.28f * glowIntensity)
        )
        ThemeMode.BIOLUMINESCENT_NEON -> listOf(
            Color(0xFF00FFA3).copy(alpha = 0.42f * glowIntensity),
            Color(0xFF00E5FF).copy(alpha = 0.36f * glowIntensity),
            Color(0xFF7209B7).copy(alpha = 0.24f * glowIntensity),
            Color(0xFF00FFA3).copy(alpha = 0.18f * glowIntensity)
        )
        ThemeMode.SMOKY_OBSIDIAN -> listOf(
            Color.White.copy(alpha = 0.24f * glowIntensity),
            Color(0xFF94A3B8).copy(alpha = 0.20f * glowIntensity),
            ElectricCyan.copy(alpha = 0.12f * glowIntensity),
            Color.Transparent
        )
    }

    val rimStrokeColors = when (themeMode) {
        ThemeMode.LIQUID_GLASS -> listOf(
            Color.White.copy(alpha = 0.90f),
            ElectricCyan.copy(alpha = 0.72f),
            Color(0xFFE040FB).copy(alpha = 0.40f),
            Color.White.copy(alpha = 0.15f),
            ElectricCyan.copy(alpha = 0.55f)
        )
        ThemeMode.PRISM_GLASS -> listOf(
            Color.White.copy(alpha = 0.92f),
            Color(0xFF00F5D4).copy(alpha = 0.80f),
            Color(0xFFFF007F).copy(alpha = 0.65f),
            Color(0xFF00E5FF).copy(alpha = 0.50f),
            Color(0xFF4361EE).copy(alpha = 0.75f)
        )
        ThemeMode.BIOLUMINESCENT_NEON -> listOf(
            Color(0xFF00FFA3).copy(alpha = 0.92f),
            Color(0xFF00E5FF).copy(alpha = 0.80f),
            Color(0xFF00B4D8).copy(alpha = 0.35f),
            Color(0xFF00FFA3).copy(alpha = 0.65f)
        )
        ThemeMode.SMOKY_OBSIDIAN -> listOf(
            Color.White.copy(alpha = 0.85f),
            Color(0xFFCBD5E1).copy(alpha = 0.45f),
            Color(0xFF334155).copy(alpha = 0.20f),
            Color.White.copy(alpha = 0.45f)
        )
    }

    var boxModifier = modifier
        .scale(scale)
        .drawBehind {
            val radiusPx = cornerRadius.toPx()
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = contourGlowColors,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
                size = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()),
                cornerRadius = CornerRadius(radiusPx + 2.dp.toPx(), radiusPx + 2.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }
        .clip(shape)
        .background(backgroundBrush)
        .drawWithContent {
            drawContent()

            val radiusPx = cornerRadius.toPx()
            val lensHeight = (size.height * 0.28f).coerceAtMost(36.dp.toPx())

            // Top Optical Lens Height Refraction
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = lensHeight
                ),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(size.width - 2.dp.toPx(), lensHeight),
                cornerRadius = CornerRadius(radiusPx, radiusPx)
            )

            // Outer Glowing Glass Edge Contour
            val strokePx = if (isPressed && onClick != null) (borderWidth + 0.6.dp).toPx() else borderWidth.toPx()
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = rimStrokeColors,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                topLeft = Offset(strokePx / 2f, strokePx / 2f),
                size = Size(
                    (size.width - strokePx).coerceAtLeast(0f),
                    (size.height - strokePx).coerceAtLeast(0f)
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = strokePx)
            )
        }

    if (onClick != null) {
        boxModifier = boxModifier.clickable(
            interactionSource = interactionSource,
            indication = null
        ) { onClick() }
    }

    Box(
        modifier = boxModifier,
        content = content
    )
}

/**
 * Uniform Squircle Action / Filter Button with breathing glass rim, visible symbol,
 * and clean wrapped typography that never shrinks or clips.
 */
@Composable
fun GlassPillButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isActive: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val themeMode = LocalThemeMode.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pill_slow_breathing")
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pill_border_breath"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 450f),
        label = "pill_press"
    )

    val cornerDp = if (compact) 13.dp else 15.dp
    val shape = RoundedCornerShape(cornerDp)
    val minHeightDp = if (compact) 42.dp else 46.dp

    val backgroundBrush = if (isActive || isPressed) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2D5F88).copy(alpha = 0.90f),
                Color(0xFF132D48).copy(alpha = 0.95f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF223850).copy(alpha = 0.72f),
                Color(0xFF0E1A29).copy(alpha = 0.86f)
            )
        )
    }

    val primaryRim = when (themeMode) {
        ThemeMode.PRISM_GLASS -> Color(0xFF00F5D4)
        ThemeMode.BIOLUMINESCENT_NEON -> Color(0xFF00FFA3)
        else -> ElectricCyan
    }

    val secondaryRim = when (themeMode) {
        ThemeMode.PRISM_GLASS -> Color(0xFFFF007F)
        ThemeMode.BIOLUMINESCENT_NEON -> Color(0xFF00E5FF)
        else -> IcyBlue
    }

    Box(
        modifier = modifier
            .heightIn(min = minHeightDp)
            .scale(pressScale)
            .clip(shape)
            .background(backgroundBrush)
            .drawWithContent {
                drawContent()

                val strokeW = if (isActive || isPressed) 1.6.dp.toPx() else 1.2.dp.toPx()
                val effectiveAlpha = if (isActive || isPressed) (breathAlpha * 1.05f).coerceIn(0.55f, 1f) else breathAlpha
                val rPx = cornerDp.toPx()
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = effectiveAlpha),
                            primaryRim.copy(alpha = effectiveAlpha),
                            secondaryRim.copy(alpha = effectiveAlpha * 0.65f),
                            Color.White.copy(alpha = effectiveAlpha * 0.8f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    topLeft = Offset(strokeW / 2f, strokeW / 2f),
                    size = Size(size.width - strokeW, size.height - strokeW),
                    cornerRadius = CornerRadius(rPx, rPx),
                    style = Stroke(width = strokeW)
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(
                horizontal = if (compact) 11.dp else 14.dp,
                vertical = if (compact) 7.dp else 9.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                val baseIconTint = if (isActive || isPressed) primaryRim else FrostWhite
                val minAlpha = if (isActive || isPressed) 0.78f else 0.75f
                val deltaAlpha = if (isActive || isPressed) 0.22f else 0.25f
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = baseIconTint,
                    modifier = Modifier
                        .requiredSize(if (compact) 16.dp else 18.dp)
                        .graphicsLayer {
                            alpha = (minAlpha + deltaAlpha * breathAlpha).coerceIn(0f, 1f)
                        }
                )
                Spacer(modifier = Modifier.width(if (compact) 5.dp else 7.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = if (compact) 11.5.sp else 12.5.sp,
                    lineHeight = if (compact) 13.5.sp else 15.sp
                ),
                color = if (isActive || isPressed) FrostWhite else FrostWhite.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Uniform Squircle Glass Icon Button with breathing rim and crisp centered icon.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "icon_slow_breathing")
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.36f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_border_breath"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 450f),
        label = "icon_scale"
    )

    val activeTint = tint ?: ElectricCyan
    val baseIconTint = tint ?: if (isPressed) ElectricCyan else FrostWhite
    val squircleCorner = 15.dp
    val squircleShape = RoundedCornerShape(squircleCorner)

    Box(
        modifier = modifier
            .size(size)
            .scale(pressScale)
            .drawBehind {
                val r = (size.toPx() * 0.72f).coerceAtLeast(1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeTint.copy(alpha = 0.24f * breathAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        radius = r
                    ),
                    center = center,
                    radius = r
                )
            }
            .clip(squircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2B4769).copy(alpha = if (isPressed) 0.92f else 0.72f),
                        Color(0xFF0F1E2E).copy(alpha = if (isPressed) 0.96f else 0.88f)
                    )
                )
            )
            .drawWithContent {
                drawContent()

                val strokeW = if (isPressed) 1.7.dp.toPx() else 1.3.dp.toPx()
                val rPx = squircleCorner.toPx()
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = breathAlpha),
                            activeTint.copy(alpha = breathAlpha),
                            IcyBlue.copy(alpha = breathAlpha * 0.5f),
                            Color.White.copy(alpha = breathAlpha * 0.8f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(this.size.width, this.size.height)
                    ),
                    topLeft = Offset(strokeW / 2f, strokeW / 2f),
                    size = Size(this.size.width - strokeW, this.size.height - strokeW),
                    cornerRadius = CornerRadius(rPx, rPx),
                    style = Stroke(width = strokeW)
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = baseIconTint,
            modifier = Modifier
                .requiredSize(22.dp)
                .graphicsLayer { alpha = (0.76f + 0.24f * breathAlpha).coerceIn(0f, 1f) }
        )
    }
}
