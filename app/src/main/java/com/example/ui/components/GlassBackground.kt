package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.theme.LocalThemeMode
import com.example.ui.theme.ThemeMode
import kotlin.math.cos
import kotlin.math.sin

/**
 * Standout 4-Theme Atmospheric Engine (Liquid Glass, Prism Glass, Bioluminescent Neon, Smoky Frosted Obsidian).
 * Completely eradicates Ghost Lights Mode in favor of vibrant optical glass lighting and reactive touch physics.
 */
@Composable
fun LiquidGlassAtmosphere(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF54C8F5),
    content: @Composable () -> Unit
) {
    val themeMode = LocalThemeMode.current

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_multi_lights")

    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t1"
    )

    val t2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t2"
    )

    val t3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t3"
    )

    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse1"
    )

    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 1.35f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )

    val pulse3 by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse3"
    )

    // Touch interaction tracking
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    var isTouching by remember { mutableStateOf(false) }

    val touchAlpha by animateFloatAsState(
        targetValue = if (isTouching) 0.72f else 0f,
        animationSpec = tween(durationMillis = if (isTouching) 140 else 850),
        label = "touch_alpha"
    )

    val animatedTouchX by animateFloatAsState(
        targetValue = touchPos?.x ?: 500f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "touch_x"
    )

    val animatedTouchY by animateFloatAsState(
        targetValue = touchPos?.y ?: 800f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "touch_y"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val change = event.changes.firstOrNull()
                        if (change != null) {
                            if (change.pressed) {
                                touchPos = change.position
                                isTouching = true
                            } else {
                                isTouching = false
                            }
                        } else {
                            isTouching = false
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            when (themeMode) {
                ThemeMode.LIQUID_GLASS -> {
                    drawRect(color = Color(0xFF04070D))

                    val x1 = width * (0.72f + 0.18f * cos(t1))
                    val y1 = height * (0.16f + 0.12f * sin(t1))
                    val r1 = width * 0.62f * pulse1
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE040FB).copy(alpha = 0.22f * pulse1),
                                Color(0xFF7B2CBF).copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(x1, y1),
                            radius = r1
                        ),
                        center = Offset(x1, y1),
                        radius = r1
                    )

                    val x2 = width * (0.20f + 0.18f * sin(t2))
                    val y2 = height * (0.44f + 0.14f * cos(t2))
                    val r2 = width * 0.58f * pulse2
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00F5D4).copy(alpha = 0.20f * pulse2),
                                Color(0xFF0077B6).copy(alpha = 0.13f),
                                Color.Transparent
                            ),
                            center = Offset(x2, y2),
                            radius = r2
                        ),
                        center = Offset(x2, y2),
                        radius = r2
                    )

                    val x3 = width * (0.76f + 0.14f * cos(t3))
                    val y3 = height * (0.74f + 0.11f * sin(t3))
                    val r3 = width * 0.66f * pulse3
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF7209B7).copy(alpha = 0.24f),
                                Color(0xFF3A0CA3).copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(x3, y3),
                            radius = r3
                        ),
                        center = Offset(x3, y3),
                        radius = r3
                    )

                    val x5 = width * (0.46f + 0.15f * sin(t3))
                    val y5 = height * (0.12f + 0.08f * cos(t1))
                    val r5 = width * 0.64f * pulse2
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.26f * pulse2),
                                accentColor.copy(alpha = 0.09f),
                                Color.Transparent
                            ),
                            center = Offset(x5, y5),
                            radius = r5
                        ),
                        center = Offset(x5, y5),
                        radius = r5
                    )
                }

                ThemeMode.PRISM_GLASS -> {
                    drawRect(color = Color(0xFF050811))

                    val cx1 = width * (0.25f + 0.22f * cos(t1))
                    val cy1 = height * (0.22f + 0.15f * sin(t2))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00F5D4).copy(alpha = 0.26f * pulse1),
                                Color(0xFF4361EE).copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(cx1, cy1),
                            radius = width * 0.68f
                        ),
                        center = Offset(cx1, cy1),
                        radius = width * 0.68f
                    )

                    val cx2 = width * (0.78f + 0.18f * sin(t2))
                    val cy2 = height * (0.50f + 0.18f * cos(t1))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF007F).copy(alpha = 0.22f * pulse2),
                                Color(0xFF00E5FF).copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(cx2, cy2),
                            radius = width * 0.62f
                        ),
                        center = Offset(cx2, cy2),
                        radius = width * 0.62f
                    )

                    val cx3 = width * (0.45f + 0.20f * cos(t3))
                    val cy3 = height * (0.82f + 0.10f * sin(t3))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFB5179E).copy(alpha = 0.25f * pulse3),
                                accentColor.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(cx3, cy3),
                            radius = width * 0.70f
                        ),
                        center = Offset(cx3, cy3),
                        radius = width * 0.70f
                    )
                }

                ThemeMode.BIOLUMINESCENT_NEON -> {
                    drawRect(color = Color(0xFF02060D))

                    val bx1 = width * (0.50f + 0.25f * sin(t1))
                    val by1 = height * (0.20f + 0.12f * cos(t2))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00FFA3).copy(alpha = 0.28f * pulse1),
                                Color(0xFF00B4D8).copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = Offset(bx1, by1),
                            radius = width * 0.72f * pulse1
                        ),
                        center = Offset(bx1, by1),
                        radius = width * 0.72f * pulse1
                    )

                    val bx2 = width * (0.22f + 0.18f * cos(t2))
                    val by2 = height * (0.65f + 0.15f * sin(t3))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.26f * pulse2),
                                Color(0xFF7209B7).copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(bx2, by2),
                            radius = width * 0.65f * pulse2
                        ),
                        center = Offset(bx2, by2),
                        radius = width * 0.65f * pulse2
                    )

                    val bx3 = width * (0.80f + 0.14f * sin(t3))
                    val by3 = height * (0.78f + 0.12f * cos(t1))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF007F).copy(alpha = 0.22f * pulse3),
                                accentColor.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(bx3, by3),
                            radius = width * 0.58f
                        ),
                        center = Offset(bx3, by3),
                        radius = width * 0.58f
                    )
                }

                ThemeMode.SMOKY_OBSIDIAN -> {
                    drawRect(color = Color(0xFF06080C))

                    val sx1 = width * (0.28f + 0.12f * cos(t1))
                    val sy1 = height * (0.18f + 0.08f * sin(t1))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFD8E2EF).copy(alpha = 0.15f * pulse1),
                                accentColor.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(sx1, sy1),
                            radius = width * 0.65f
                        ),
                        center = Offset(sx1, sy1),
                        radius = width * 0.65f
                    )

                    val sx2 = width * (0.74f + 0.12f * sin(t2))
                    val sy2 = height * (0.72f + 0.10f * cos(t2))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.18f * pulse2),
                                Color(0xFF1E293B).copy(alpha = 0.22f),
                                Color.Transparent
                            ),
                            center = Offset(sx2, sy2),
                            radius = width * 0.70f
                        ),
                        center = Offset(sx2, sy2),
                        radius = width * 0.70f
                    )
                }
            }

            if (touchAlpha > 0.01f) {
                val touchOffset = Offset(animatedTouchX, animatedTouchY)
                val touchRadius = width * 0.54f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.38f * touchAlpha),
                            Color.White.copy(alpha = 0.16f * touchAlpha),
                            Color(0xFF00F5D4).copy(alpha = 0.12f * touchAlpha),
                            Color.Transparent
                        ),
                        center = touchOffset,
                        radius = touchRadius
                    ),
                    center = touchOffset,
                    radius = touchRadius
                )
            }
        }

        content()
    }
}
