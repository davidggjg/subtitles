package com.subburn.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subburn.app.core.Cue
import com.subburn.app.core.asClock
import kotlinx.coroutines.flow.distinctUntilChanged

/** כמה מקום על המסך תופסת שנייה אחת של וידאו. */
private val SECOND_WIDTH = 46.dp

/**
 * פס הזמן: גוללים אותו ימינה ושמאלה, והקו הקבוע במרכז הוא המקום שמתנגן.
 * הכתוביות מופיעות כמלבנים לאורך הפס, בדיוק במקום שבו הן מופיעות בסרט.
 */
@Composable
fun TimelineTrack(
    cues: List<Cue>,
    playback: PlaybackState,
    enabled: Boolean,
    onCueClick: (index: Int) -> Unit,
    onAddCue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scroll = rememberScrollState()
    val pxPerMs = remember(density) { with(density) { SECOND_WIDTH.toPx() } / 1000f }
    // פס הזמן תמיד רץ משמאל לימין, כמו בכל עורך וידאו, גם בממשק עברי.
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val halfWidth = maxWidth / 2
        val trackWidth = with(density) { (playback.durationMs * pxPerMs).toDp() }

        // גרירה של הפס = הזזת מקום הנגינה.
        LaunchedEffect(scroll, pxPerMs) {
            snapshotFlow { scroll.value to scroll.isScrollInProgress }
                .distinctUntilChanged()
                .collect { (value, dragging) ->
                    if (dragging && enabled) {
                        playback.pause()
                        playback.seekTo((value / pxPerMs).toLong())
                    }
                }
        }
        // בזמן נגינה הפס זז לבד מתחת לקו.
        LaunchedEffect(playback.positionMs, playback.playing) {
            if (playback.playing && !scroll.isScrollInProgress) {
                scroll.scrollTo((playback.positionMs * pxPerMs).toInt())
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scroll)
            ) {
                Row {
                    Spacer(Modifier.width(halfWidth))
                    Column(
                        Modifier.width(trackWidth.coerceAtLeast(1.dp)),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SubtitleLane(cues, pxPerMs, enabled, onCueClick, onAddCue)
                        FilmLane(playback.durationMs)
                        Ruler(playback.durationMs)
                    }
                    Spacer(Modifier.width(halfWidth))
                }
            }
        }

        // הקו הקבוע במרכז שמסמן את המקום המתנגן.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .width(2.dp)
                .height(112.dp)
                .background(Neon)
        )
    }
}

/** מסלול הכתוביות — כל כתובית מלבן במקום שלה. */
@Composable
private fun SubtitleLane(
    cues: List<Cue>,
    pxPerMs: Float,
    enabled: Boolean,
    onCueClick: (Int) -> Unit,
    onAddCue: () -> Unit
) {
    val density = LocalDensity.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PanelHigh.copy(alpha = 0.5f))
    ) {
        if (cues.isEmpty()) {
            Row(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 10.dp)
                    .clickable(enabled = enabled, onClick = onAddCue),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Neon)
                Spacer(Modifier.width(6.dp))
                Text("הוספת כתובית", color = TextDim, style = MaterialTheme.typography.labelSmall)
            }
        }
        cues.forEachIndexed { index, cue ->
            val start = with(density) { (cue.startMs * pxPerMs).toDp() }
            val width = with(density) { (cue.durationMs * pxPerMs).toDp() }
            Box(
                Modifier
                    .padding(start = start, top = 4.dp, bottom = 4.dp)
                    .width(width.coerceAtLeast(18.dp))
                    .height(36.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Violet.copy(alpha = 0.35f))
                    .clickable(enabled = enabled) { onCueClick(index) }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    cue.text.replace('\n', ' '),
                    color = TextHigh,
                    fontSize = 11.sp,
                    maxLines = 2
                )
            }
        }
    }
}

/** רצועת הווידאו — פס דקורטיבי שנותן תחושת אורך לסרט. */
@Composable
private fun FilmLane(durationMs: Long) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF16203A), Color(0xFF23304F), Color(0xFF16203A))
                )
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            "  ${durationMs.asClock()}",
            color = TextDim,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** סרגל שניות מתחת לרצועות. */
@Composable
private fun Ruler(durationMs: Long) {
    val totalSeconds = (durationMs / 1000).toInt()
    val step = when {
        totalSeconds > 3600 -> 60
        totalSeconds > 600 -> 30
        else -> 5
    }
    Box(Modifier.fillMaxWidth().height(16.dp)) {
        var second = 0
        while (second <= totalSeconds) {
            Text(
                (second * 1000L).asClock(),
                color = TextDim,
                fontSize = 9.sp,
                modifier = Modifier.padding(start = SECOND_WIDTH * second)
            )
            second += step
        }
    }
}
