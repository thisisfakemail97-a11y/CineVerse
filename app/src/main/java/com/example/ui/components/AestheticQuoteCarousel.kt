package com.example.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Quote
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FrostWhite
import com.example.ui.theme.SteelBlue
import kotlinx.coroutines.delay

@Composable
fun AestheticQuoteCarousel(
    quotes: List<Quote>,
    modifier: Modifier = Modifier,
    textColor: Color = FrostWhite.copy(alpha = 0.85f),
    speakerColor: Color = ElectricCyan,
    maxLines: Int = 3
) {
    val cleanQuotes = quotes.filter { it.text.isNotBlank() }.distinctBy { it.text.trim() }
    if (cleanQuotes.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { cleanQuotes.size })

    // Auto-advance quotes every 2.0 seconds, pausing whenever user manually swipes
    LaunchedEffect(cleanQuotes.size, pagerState.isScrollInProgress) {
        if (cleanQuotes.size > 1 && !pagerState.isScrollInProgress) {
            while (true) {
                delay(2000L)
                if (!pagerState.isScrollInProgress) {
                    try {
                        val nextPage = (pagerState.currentPage + 1) % cleanQuotes.size
                        pagerState.animateScrollToPage(
                            page = nextPage,
                            animationSpec = tween(durationMillis = 520)
                        )
                    } catch (_: Exception) {
                        // Ignore cancellation when user swipes manually
                    }
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val quote = cleanQuotes[page]
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "“${quote.text}”",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        letterSpacing = 0.25.sp
                    ),
                    color = textColor,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis
                )
                if (quote.speaker.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "— ${quote.speaker}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.4.sp
                        ),
                        color = speakerColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Swipeable indicator bars when multiple quotes exist
        if (cleanQuotes.size > 1) {
            Spacer(modifier = Modifier.height(7.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                cleanQuotes.indices.forEach { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .width(if (isSelected) 16.dp else 6.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(if (isSelected) speakerColor else SteelBlue.copy(alpha = 0.45f))
                    )
                }
            }
        }
    }
}
