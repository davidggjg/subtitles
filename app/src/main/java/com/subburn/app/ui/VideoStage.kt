package com.subburn.app.ui

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.subburn.app.core.BackgroundMode
import com.subburn.app.core.Cue
import com.subburn.app.core.SubtitleStyle
import com.subburn.app.core.asClock
import kotlinx.coroutines.delay

/** מצב הנגן, משותף לנגן עצמו ולשאר המסך. */
class PlaybackState {
    var positionMs by mutableStateOf(0L)
    var durationMs by mutableStateOf(0L)
    var playing by mutableStateOf(false)
    /** הודעה כשהנגן לא מצליח לפתוח את הקובץ; הצריבה עצמה עדיין אפשרית. */
    var error by mutableStateOf<String?>(null)
    internal var view: VideoView? = null

    fun togglePlay() {
        val player = view ?: return
        runCatching {
            if (playing) player.pause() else player.start()
            playing = !playing
        }
    }

    fun pause() {
        view?.pause()
        playing = false
    }

    fun seekTo(ms: Long) {
        val clamped = ms.coerceIn(0L, durationMs.coerceAtLeast(0L))
        positionMs = clamped
        runCatching { view?.seekTo(clamped.toInt()) }
    }

    fun nudge(deltaMs: Long) = seekTo(positionMs + deltaMs)
}

@Composable
fun rememberPlaybackState(): PlaybackState = remember { PlaybackState() }

/**
 * הנגן עצמו: תמונת הווידאו עם הכתובית הפעילה מצוירת מעליה בסגנון שנבחר,
 * כך שרואים בזמן אמת איך זה ייראה אחרי הצריבה.
 */
@Composable
fun VideoStage(
    uri: Uri?,
    activeCue: Cue?,
    style: SubtitleStyle,
    playback: PlaybackState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black)
            .border(1.dp, Neon.copy(alpha = 0.25f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (uri == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(Color(0xFF16203A), Color(0xFF241634)))),
                contentAlignment = Alignment.Center
            ) {
                Text("בחרי וידאו כדי להתחיל", color = TextDim, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    VideoView(context).apply {
                        setOnPreparedListener { media ->
                            media.isLooping = false
                            playback.error = null
                            val reported = media.duration.toLong()
                            if (reported > 0) playback.durationMs = reported
                        }
                        setOnCompletionListener { playback.playing = false }
                        // בלי מאזין שגיאות אנדרואיד פותח חלון שגיאה משלו.
                        setOnErrorListener { _, _, _ ->
                            playback.playing = false
                            playback.error = "לא הצלחתי לנגן את הקובץ הזה בתצוגה המקדימה, אבל אפשר לצרוב אותו"
                            true
                        }
                    }
                },
                update = { view ->
                    playback.view = view
                    if (view.tag != uri) {
                        view.tag = uri
                        view.setVideoURI(uri)
                    }
                }
            )
            // מיקום הנגינה נקרא כל 100 מ"ש בזמן נגינה — מזה נגזרת הכתובית המוצגת.
            LaunchedEffect(playback.playing, uri) {
                while (playback.playing) {
                    playback.view?.let { playback.positionMs = it.currentPosition.toLong() }
                    delay(100)
                }
            }
            DisposableEffect(uri) {
                onDispose {
                    playback.view?.stopPlayback()
                    playback.view = null
                    playback.playing = false
                }
            }
            SubtitleOverlay(activeCue, style)
        }

        playback.error?.let { message ->
            Text(
                message,
                color = Amber,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        if (uri != null) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(if (playback.playing) 0.dp else 62.dp)
                    .clip(RoundedCornerShape(31.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { playback.togglePlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "נגן",
                    tint = Neon,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

/** אותו פונט עברי שנצרב לווידאו, כדי שהתצוגה תשקף את התוצאה. */
@Composable
private fun rememberHebrewFont(): FontFamily {
    val assets = LocalContext.current.assets
    return remember(assets) {
        FontFamily(
            Font("fonts/NotoSansHebrew-Regular.ttf", assets, weight = FontWeight.Normal),
            Font("fonts/NotoSansHebrew-Bold.ttf", assets, weight = FontWeight.Bold)
        )
    }
}

/**
 * הכתובית מעל הווידאו. הקו השחור מצויר באמת (טקסט במילוי קווי מאחור ולבן
 * מלפנים), ולא כצל מטושטש, כדי שמה שנראה כאן יהיה מה שייצרב בפועל.
 */
@Composable
private fun SubtitleOverlay(cue: Cue?, style: SubtitleStyle) {
    if (cue == null) return
    val hebrewFont = rememberHebrewFont()
    val density = LocalDensity.current
    val fontSize = (style.fontSize * 0.8f).coerceIn(12f, 34f)
    val outlinePx = with(density) { (style.outline * 1.6f).dp.toPx() }
    val shadowPx = style.shadow.coerceAtMost(3f) * 1.4f

    val base = TextStyle(
        fontFamily = hebrewFont,
        fontSize = fontSize.sp,
        fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
        textAlign = TextAlign.Center,
        lineHeight = (fontSize * 1.25f).sp
    )

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        val boxed = style.background == BackgroundMode.BOX
        Box(
            Modifier
                .padding(
                    bottom = (style.marginV / 4).coerceIn(6, 56).dp,
                    start = (style.marginH / 2).coerceIn(6, 60).dp,
                    end = (style.marginH / 2).coerceIn(6, 60).dp
                )
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (boxed) Color.Black.copy(alpha = style.backgroundOpacity / 100f) else Color.Transparent
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            val text = cue.text
            if (!boxed && outlinePx > 0f) {
                Text(
                    text,
                    color = Color.Black,
                    style = base.copy(
                        drawStyle = Stroke(
                            width = outlinePx,
                            join = StrokeJoin.Round,
                            cap = StrokeCap.Round
                        ),
                        shadow = if (shadowPx > 0f) {
                            Shadow(Color.Black.copy(alpha = 0.85f), Offset(shadowPx, shadowPx), style.blur * 3f + 1f)
                        } else {
                            null
                        }
                    )
                )
            }
            Text(text, color = Color.White, style = base)
        }
    }
}

/** פס ההתקדמות וכפתורי הנגינה. */
@Composable
fun TransportBar(playback: PlaybackState, enabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Slider(
            value = playback.positionMs.toFloat(),
            onValueChange = { playback.seekTo(it.toLong()) },
            valueRange = 0f..playback.durationMs.coerceAtLeast(1L).toFloat(),
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Neon,
                activeTrackColor = Neon,
                inactiveTrackColor = PanelHigh
            )
        )
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(playback.positionMs.asClock(), color = Neon, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(12.dp))
            RoundButton(Icons.Filled.Replay10, "אחורה 10 שניות", enabled) { playback.nudge(-10_000) }
            Spacer(Modifier.width(8.dp))
            RoundButton(
                if (playback.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (playback.playing) "עצור" else "נגן",
                enabled,
                primary = true
            ) { playback.togglePlay() }
            Spacer(Modifier.width(8.dp))
            RoundButton(Icons.Filled.Forward10, "קדימה 10 שניות", enabled) { playback.nudge(10_000) }
            Spacer(Modifier.weight(1f))
            Text(playback.durationMs.asClock(), color = TextDim, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(if (primary) 48.dp else 40.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (primary) Neon.copy(alpha = 0.18f) else PanelHigh)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (enabled) Neon else TextDim)
    }
}
