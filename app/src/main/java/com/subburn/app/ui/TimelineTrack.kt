package com.subburn.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subburn.app.core.Cue
import com.subburn.app.core.TimelineMath
import com.subburn.app.core.asClock
import kotlinx.coroutines.flow.distinctUntilChanged

/** כמה מקום על המסך תופסת שנייה אחת של וידאו. */
private val SECOND_WIDTH = 46.dp

// אורכי המקטעים והחישובים עצמם יושבים ב־TimelineMath, כדי שיהיו בדוקים.
private const val CHUNK_SECONDS = TimelineMath.CHUNK_SECONDS
private const val CHUNK_MS = TimelineMath.CHUNK_MS

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
    val listState = rememberLazyListState()
    val pxPerMs = with(density) { SECOND_WIDTH.toPx() } / 1000f
    val chunkPx = TimelineMath.chunkWidthPx(with(density) { SECOND_WIDTH.toPx() })
    val chunkCount = remember(playback.durationMs) { TimelineMath.chunkCount(playback.durationMs) }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val halfWidth = maxWidth / 2

        // גרירה של הפס = הזזת מקום הנגינה.
        LaunchedEffect(listState, pxPerMs, enabled) {
            snapshotFlow {
                Triple(
                    listState.firstVisibleItemIndex,
                    listState.firstVisibleItemScrollOffset,
                    listState.isScrollInProgress
                )
            }
                .distinctUntilChanged()
                .collect { (index, offset, dragging) ->
                    if (dragging && enabled) {
                        playback.pause()
                        playback.seekTo(TimelineMath.positionMs(index, offset, chunkPx, pxPerMs))
                    }
                }
        }
        // בזמן נגינה הפס זז לבד מתחת לקו.
        LaunchedEffect(playback.positionMs, playback.playing) {
            if (playback.playing && !listState.isScrollInProgress) {
                val scrolled = TimelineMath.scrollPx(playback.positionMs, pxPerMs)
                listState.scrollToItem(
                    (scrolled / chunkPx).toInt(),
                    (scrolled % chunkPx).toInt()
                )
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            LazyRow(
                state = listState,
                userScrollEnabled = enabled,
                // ריפוד בחצי מסך משני הצדדים, כדי שההתחלה והסוף יגיעו עד הקו שבמרכז.
                contentPadding = PaddingValues(horizontal = halfWidth),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(chunkCount) { chunk ->
                    TimelineChunk(
                        chunkIndex = chunk,
                        cues = cues,
                        enabled = enabled,
                        onCueClick = onCueClick
                    )
                }
            }
        }

        // הקו הקבוע במרכז שמסמן את המקום המתנגן.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .width(2.dp)
                .height(104.dp)
                .background(Neon)
        )

        if (cues.isEmpty() && playback.durationMs > 0) {
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Panel.copy(alpha = 0.85f))
                    .clickable(enabled = enabled, onClick = onAddCue)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Neon)
                Spacer(Modifier.width(6.dp))
                Text("הוספת כתובית", color = TextHigh, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** מקטע אחד של עשר שניות: רצועת כתוביות, רצועת וידאו וסרגל זמן. */
@Composable
private fun TimelineChunk(
    chunkIndex: Int,
    cues: List<Cue>,
    enabled: Boolean,
    onCueClick: (Int) -> Unit
) {
    val density = LocalDensity.current
    val chunkStart = chunkIndex * CHUNK_MS
    val chunkEnd = chunkStart + CHUNK_MS
    val width = SECOND_WIDTH * CHUNK_SECONDS

    Column(
        Modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PanelHigh.copy(alpha = 0.5f))
        ) {
            // כל כתובית מצוירת בכל מקטע שהיא חוצה, כדי שלא תיעלם בגלילה.
            cues.forEachIndexed { index, cue ->
                if (cue.endMs < chunkStart || cue.startMs > chunkEnd) return@forEachIndexed
                val offsetDp = with(density) {
                    ((cue.startMs - chunkStart) * (SECOND_WIDTH.toPx() / 1000f)).toDp()
                }
                val cueWidth = with(density) {
                    (cue.durationMs * (SECOND_WIDTH.toPx() / 1000f)).toDp()
                }
                Box(
                    Modifier
                        .offset(x = offsetDp)
                        .padding(vertical = 4.dp)
                        .width(cueWidth.coerceIn(18.dp, SECOND_WIDTH * 60))
                        .height(36.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(CueBlock)
                        .clickable(enabled = enabled) { onCueClick(index) }
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (cue.startMs >= chunkStart) {
                        Text(
                            cue.text.replace('\n', ' '),
                            color = TextHigh,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF16203A), Color(0xFF23304F)))
                )
        )

        Text(
            chunkStart.asClock(),
            color = TextDim,
            fontSize = 9.sp,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}
