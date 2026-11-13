package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FrostWhite
import com.example.ui.theme.SteelBlue
import kotlin.math.*

/**
 * Interactive circular HSV Color Wheel composable.
 * Allows intuitive touch & drag color picking with live preview.
 */
@Composable
fun InteractiveColorWheel(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var indicatorOffset by remember { mutableStateOf<Offset?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = size.width / 2f
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val dist = sqrt(dx * dx + dy * dy)
                            if (dist <= radius) {
                                val angle = (atan2(dy, dx) * 180f / PI.toFloat() + 360f) % 360f
                                val saturation = (dist / radius).coerceIn(0f, 1f)
                                val color = hsvToColor(angle, saturation, 1.0f)
                                indicatorOffset = offset
                                onColorSelected(color)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = size.width / 2f
                            val dx = change.position.x - center.x
                            val dy = change.position.y - center.y
                            val dist = sqrt(dx * dx + dy * dy).coerceAtMost(radius)
                            val angle = (atan2(dy, dx) * 180f / PI.toFloat() + 360f) % 360f
                            val saturation = (dist / radius).coerceIn(0.1f, 1f)
                            val color = hsvToColor(angle, saturation, 1.0f)

                            val clampedX = center.x + (dx / sqrt(dx * dx + dy * dy).coerceAtLeast(1f)) * dist
                            val clampedY = center.y + (dy / sqrt(dx * dx + dy * dy).coerceAtLeast(1f)) * dist
                            indicatorOffset = Offset(clampedX, clampedY)
                            onColorSelected(color)
                        }
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                // Draw hue sweep gradient
                val sweepBrush = Brush.sweepGradient(
                    listOf(
                        Color(0xFFFF0000), // Red
                        Color(0xFFFFFF00), // Yellow
                        Color(0xFF00FF00), // Green
                        Color(0xFF00FFFF), // Cyan
                        Color(0xFF0000FF), // Blue
                        Color(0xFFFF00FF), // Magenta
                        Color(0xFFFF0000)  // Red
                    ),
                    center = center
                )
                drawCircle(brush = sweepBrush, radius = radius, center = center)

                // White radial gradient in center for saturation
                val radialSaturation = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
                    center = center,
                    radius = radius
                )
                drawCircle(brush = radialSaturation, radius = radius, center = center)

                // Outer glass ring border
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Draw draggable indicator target
                val currentIndicator = indicatorOffset ?: center
                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = 16.dp.toPx(),
                    center = currentIndicator
                )
                drawCircle(
                    color = Color.White,
                    radius = 14.dp.toPx(),
                    center = currentIndicator,
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = selectedColor,
                    radius = 10.dp.toPx(),
                    center = currentIndicator
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Selected Color Preview Capsule
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(selectedColor)
                    .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            )
            Text(
                text = "Active Accent Color",
                style = MaterialTheme.typography.bodySmall,
                color = FrostWhite
            )
        }
    }
}

/**
 * Converts HSV coordinates into a Jetpack Compose Color.
 */
fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
    val hsv = floatArrayOf(hue, saturation, value)
    val colorInt = AndroidColor.HSVToColor(hsv)
    return Color(colorInt)
}
