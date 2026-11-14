package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LocalGlassTransparency
import com.example.ui.theme.SteelBlue

sealed class Screen(val route: String, val description: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Search : Screen("search", "Search", Icons.Default.Search)
    object Library : Screen("library", "Library", Icons.Default.Bookmark)
    object Diary : Screen("diary", "Movie Diary", Icons.AutoMirrored.Filled.MenuBook)
    object Profile : Screen("profile", "Settings", Icons.Default.Person)
}

/**
 * Floating Liquid Glass capsule dock with:
 * - Dynamic transparency controlled by LocalGlassTransparency (0% to 100% transparent).
 * - Slow, relaxed breathing animation on tab borders and icons.
 * - Fluid Search Button Dock Morphing: When on the Home screen, the Search button lives in the center
 *   of the Home screen (and is collapsed in the dock so there is never a duplicate search button).
 *   Whenever switching to any other tab, the Search button fluidly glides and docks into the bottom bar
 *   using spring physics.
 */
@Composable
fun CineverseBottomNavBar(
    currentRoute: String,
    accentColor: Color = ElectricCyan,
    onNavigate: (String) -> Unit
) {
    val glassTransparency = LocalGlassTransparency.current.coerceIn(0f, 1f)
    val opacityFactor = (1f - glassTransparency).coerceIn(0f, 1f)
    val rimFactor = (0.22f + 0.78f * opacityFactor).coerceIn(0f, 1f)

    val items = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Library,
        Screen.Diary,
        Screen.Profile
    )

    val isOnHome = currentRoute == Screen.Home.route

    // Fluid spring physics for Search button docking when leaving Home screen
    val searchSlotWidth by animateDpAsState(
        targetValue = if (isOnHome) 0.dp else 52.dp,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 260f
        ),
        label = "search_dock_slot_width"
    )

    val searchVerticalOffsetDp by animateDpAsState(
        targetValue = if (isOnHome) (-165).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.70f,
            stiffness = 240f
        ),
        label = "search_dock_vertical_glide"
    )

    val searchDockScale by animateFloatAsState(
        targetValue = if (isOnHome) 0.35f else 1f,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = 280f
        ),
        label = "search_dock_scale"
    )

    val searchDockAlpha by animateFloatAsState(
        targetValue = if (isOnHome) 0f else 1f,
        animationSpec = tween(durationMillis = if (isOnHome) 180 else 320, easing = FastOutSlowInEasing),
        label = "search_dock_alpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "dock_slow_breathing")
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dock_breath_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 14.dp)
            .testTag("bottom_nav_dock"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF284566).copy(alpha = (0.86f * opacityFactor).coerceIn(0f, 1f)),
                            Color(0xFF16283E).copy(alpha = (0.92f * opacityFactor).coerceIn(0f, 1f)),
                            Color(0xFF0A1422).copy(alpha = (0.96f * opacityFactor).coerceIn(0f, 1f))
                        )
                    )
                )
                .drawBehind {
                    if (opacityFactor > 0.01f) {
                        // Frosted glass sheen + top icy frost reflection scaled by opacityFactor
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.22f * opacityFactor),
                                    Color(0xFFB8E8FF).copy(alpha = 0.12f * opacityFactor),
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.12f * opacityFactor)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            )
                        )
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.26f * opacityFactor),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.42f
                            )
                        )
                    }
                }
                .drawWithContent {
                    drawContent()

                    val strokeW = 1.25.dp.toPx()
                    val rPx = 24.dp.toPx()
                    val effectiveRim = breathAlpha * rimFactor
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (effectiveRim * 0.90f).coerceIn(0f, 1f)),
                                accentColor.copy(alpha = (effectiveRim * 0.72f).coerceIn(0f, 1f)),
                                Color.White.copy(alpha = (effectiveRim * 0.35f).coerceIn(0f, 1f)),
                                accentColor.copy(alpha = (effectiveRim * 0.55f).coerceIn(0f, 1f))
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
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    val selected = currentRoute == screen.route
                    if (screen == Screen.Search) {
                        if (searchSlotWidth > 1.dp || !isOnHome) {
                            Box(
                                modifier = Modifier
                                    .width(searchSlotWidth)
                                    .height(48.dp)
                                    .graphicsLayer {
                                        translationY = searchVerticalOffsetDp.toPx()
                                        scaleX = searchDockScale
                                        scaleY = searchDockScale
                                        alpha = searchDockAlpha
                                    }
                                    .testTag("dock_search_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                IconOnlyDockItem(
                                    screen = screen,
                                    selected = selected,
                                    accentColor = accentColor,
                                    opacityFactor = opacityFactor,
                                    rimFactor = rimFactor,
                                    breathAlphaProvider = { breathAlpha },
                                    onClick = { onNavigate(screen.route) }
                                )
                            }
                        }
                    } else {
                        IconOnlyDockItem(
                            screen = screen,
                            selected = selected,
                            accentColor = accentColor,
                            opacityFactor = opacityFactor,
                            rimFactor = rimFactor,
                            breathAlphaProvider = { breathAlpha },
                            onClick = { onNavigate(screen.route) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconOnlyDockItem(
    screen: Screen,
    selected: Boolean,
    accentColor: Color,
    opacityFactor: Float,
    rimFactor: Float,
    breathAlphaProvider: () -> Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label = "nav_dock_scale"
    )

    val baseIconColor by animateColorAsState(
        targetValue = if (selected) accentColor else SteelBlue,
        label = "nav_dock_color"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .drawBehind {
                if ((selected || isPressed) && opacityFactor > 0.01f) {
                    val bAlpha = breathAlphaProvider() * opacityFactor
                    val r = (size.width * 0.65f).coerceAtLeast(1f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.36f * bAlpha),
                                accentColor.copy(alpha = 0.10f * bAlpha),
                                Color.Transparent
                            ),
                            center = center,
                            radius = r
                        ),
                        center = center,
                        radius = r
                    )
                }
            }
            .testTag("nav_tab_${screen.route}"),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = (0.24f * opacityFactor).coerceIn(0f, 1f)))
                    .drawWithContent {
                        drawContent()
                        val bAlpha = breathAlphaProvider() * rimFactor
                        val rPx = 14.dp.toPx()
                        val strokeW = 1.3.dp.toPx()
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = bAlpha),
                                    accentColor.copy(alpha = bAlpha),
                                    Color.White.copy(alpha = bAlpha * 0.35f)
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
            )
        }

        Icon(
            imageVector = screen.icon,
            contentDescription = screen.description,
            tint = baseIconColor,
            modifier = Modifier
                .size(23.dp)
                .graphicsLayer {
                    val bAlpha = breathAlphaProvider()
                    val base = if (selected) 0.75f else 0.65f
                    alpha = (base + 0.25f * bAlpha).coerceIn(0f, 1f)
                }
        )
    }
}

