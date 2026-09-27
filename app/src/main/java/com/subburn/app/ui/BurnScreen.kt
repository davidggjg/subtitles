package com.subburn.app.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subburn.app.core.BackgroundMode
import com.subburn.app.core.PickedMedia
import com.subburn.app.core.PickedSubtitle
import com.subburn.app.core.RenderState
import com.subburn.app.core.SubtitleStyle
import com.subburn.app.core.asClock
import com.subburn.app.core.asReadableSize
import java.io.File
import kotlin.math.roundToInt

/** כל מה שהמשתמשת יכולה לשנות לפני שהצריבה מתחילה. */
data class BurnSettings(
    val style: SubtitleStyle = SubtitleStyle(),
    val crf: Int = 20,
    val preset: String = "medium",
    val fontsDir: String = ""
)

private val presets = listOf(
    "ultrafast" to "הכי מהיר",
    "veryfast" to "מהיר מאוד",
    "fast" to "מהיר",
    "medium" to "מאוזן",
    "slow" to "איטי ויפה"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurnScreen(
    video: PickedMedia?,
    subtitle: PickedSubtitle?,
    settings: BurnSettings,
    renderState: RenderState,
    logLines: List<String>,
    commandPreview: String,
    previewFrame: File?,
    previewLoading: Boolean,
    previewError: String?,
    previewPositionMs: Long,
    onPreviewPositionChange: (Long) -> Unit,
    onPreviewRefresh: () -> Unit,
    onPickVideo: () -> Unit,
    onPickSubtitle: () -> Unit,
    onSettingsChange: (BurnSettings) -> Unit,
    onReset: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    val running = renderState is RenderState.Running
    Scaffold(
        containerColor = Void,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("צריבת כתוביות", color = TextHigh, fontWeight = FontWeight.SemiBold)
                        Text(
                            "הכתוביות נצרבות לתוך התמונה · דחיסה ל־H.265",
                            color = TextDim,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onReset, enabled = !running) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = "איפוס", tint = TextDim)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Panel)
            )
        }
    ) { insets ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(insets)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF070B16), Void, Color(0xFF0A0717)))
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Section(
                number = "1",
                title = "בחירת הקבצים",
                explanation = "בוחרים את הווידאו ואת קובץ הכתוביות. הווידאו נקרא ישירות מהאחסון — הוא לא מועתק, כך שאין בזבוז מקום.",
                accent = Neon
            ) {
                SourceRow(
                    label = "קובץ הווידאו",
                    icon = Icons.Filled.Movie,
                    accent = Neon,
                    primary = video?.displayName ?: "לחצי לבחירת וידאו",
                    secondary = video?.let {
                        "${it.sizeBytes.asReadableSize()} · אורך ${it.durationMs.asClock()}"
                    } ?: "MKV · MP4 · AVI · TS",
                    enabled = !running,
                    onClick = onPickVideo
                )
                SourceRow(
                    label = "קובץ הכתוביות",
                    icon = Icons.Filled.ClosedCaption,
                    accent = Violet,
                    primary = subtitle?.displayName ?: "לחצי לבחירת קובץ כתוביות",
                    secondary = subtitle?.let { "${it.cueCount} שורות כתוביות" } ?: "SRT · ASS · SSA",
                    enabled = !running,
                    onClick = onPickSubtitle
                )
            }

            Section(
                number = "2",
                title = "פס הווידאו ותצוגה מקדימה",
                explanation = "מזיזים את הפס כדי להתקדם בווידאו. התמונה למטה היא פריים אמיתי מהסרט עם הכתוביות עליו — אותו מסנן שישמש בצריבה, כך שמה שרואים הוא מה שיתקבל.",
                accent = Neon
            ) {
                PreviewStage(
                    frame = previewFrame,
                    loading = previewLoading,
                    error = previewError,
                    hasVideo = video != null,
                    hasSubtitle = subtitle != null,
                    style = settings.style
                )
                Timeline(
                    positionMs = previewPositionMs,
                    durationMs = video?.durationMs ?: 0L,
                    enabled = video != null && !running,
                    onPositionChange = onPreviewPositionChange,
                    onRefresh = onPreviewRefresh
                )
            }

            Section(
                number = "3",
                title = "עיצוב הכתוביות",
                explanation = "כל שינוי כאן מתעדכן בתצוגה המקדימה למעלה.",
                accent = Violet
            ) {
                StyleControls(settings, running, onSettingsChange)
            }

            Section(
                number = "4",
                title = "דחיסה ואיכות",
                explanation = "הווידאו מקודד מחדש ב־H.265 (HEVC) — זה מה שמקטין את הקובץ משמעותית. הפסקול מועתק כמו שהוא, בלי איבוד איכות.",
                accent = Amber
            ) {
                QualityControls(settings, running, onSettingsChange)
            }

            CommandPanel(commandPreview)

            when (renderState) {
                is RenderState.Running -> ProgressPanel(renderState, onCancel)
                is RenderState.Done -> StatusPanel(
                    "הצריבה הושלמה",
                    listOfNotNull(
                        renderState.savedTo?.let { "נשמר ב־$it" },
                        "גודל הקובץ החדש: ${renderState.sizeBytes.asReadableSize()}"
                    ).joinToString("\n"),
                    Neon
                )
                is RenderState.Failed -> StatusPanel("הצריבה נכשלה", renderState.message, Danger)
                RenderState.Cancelled -> StatusPanel("בוטל", "לא נשמר קובץ.", Amber)
                RenderState.Idle -> Unit
            }

            StartButton(
                enabled = video != null && subtitle != null && !running,
                running = running,
                onClick = onStart
            )

            if (logLines.isNotEmpty()) LogPanel(logLines)
            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ---------- מסגרות ובלוקים ---------- */

@Composable
private fun Section(
    number: String,
    title: String,
    explanation: String,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Panel)
            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(number, color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(title, color = TextHigh, style = MaterialTheme.typography.titleMedium)
        }
        Text(explanation, color = TextDim, style = MaterialTheme.typography.bodyMedium)
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(accent.copy(alpha = 0.12f))
        )
        content()
    }
}

@Composable
private fun Panel(accent: Color = Neon, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Panel)
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun SourceRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    primary: String,
    secondary: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PanelHigh)
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = accent)
            Text(primary, style = MaterialTheme.typography.titleMedium, color = TextHigh, maxLines = 2)
            Text(secondary, style = MaterialTheme.typography.bodyMedium, color = TextDim)
        }
    }
}

/* ---------- תצוגה מקדימה ופס הווידאו ---------- */

@Composable
private fun PreviewStage(
    frame: File?,
    loading: Boolean,
    error: String?,
    hasVideo: Boolean,
    hasSubtitle: Boolean,
    style: SubtitleStyle
) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(1.dp, Neon.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = remember(frame?.absolutePath, frame?.lastModified()) {
            frame?.let { runCatching { BitmapFactory.decodeFile(it.absolutePath) }.getOrNull() }
        }
        when {
            bitmap != null -> Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "תצוגה מקדימה של הכתוביות",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            // בלי וידאו אין פריים אמיתי, אז מוצגת הדגמה של הסגנון עצמו.
            else -> MockPreview(style)
        }
        if (loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Neon, strokeWidth = 2.dp)
                    Spacer(Modifier.height(8.dp))
                    Text("מכין תצוגה מקדימה…", color = TextHigh, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        val notice = when {
            error != null -> error
            !hasVideo -> "בחרי וידאו כדי לראות פריים אמיתי"
            !hasSubtitle -> "בחרי קובץ כתוביות כדי לראות אותן על הפריים"
            else -> null
        }
        if (notice != null && !loading) {
            Text(
                notice,
                color = TextDim,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/** הדגמת סגנון כשאין עוד וידאו נבחר. */
@Composable
private fun MockPreview(style: SubtitleStyle) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF16203A), Color(0xFF241634)))),
        contentAlignment = Alignment.BottomCenter
    ) {
        val boxed = style.background == BackgroundMode.BOX
        Box(
            Modifier
                .padding(bottom = (style.marginV / 5).coerceIn(4, 40).dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (boxed) Color.Black.copy(alpha = style.backgroundOpacity / 100f) else Color.Transparent
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                "שלום עולם — כתובית לדוגמה",
                color = Color.White,
                fontSize = (style.fontSize * 0.75f).coerceIn(11f, 30f).sp,
                fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black,
                        offset = androidx.compose.ui.geometry.Offset(
                            style.shadow.coerceAtMost(3f),
                            style.shadow.coerceAtMost(3f)
                        ),
                        blurRadius = 1f + style.outline * 2f + style.blur * 2f
                    )
                )
            )
        }
    }
}

@Composable
private fun Timeline(
    positionMs: Long,
    durationMs: Long,
    enabled: Boolean,
    onPositionChange: (Long) -> Unit,
    onRefresh: () -> Unit
) {
    val max = durationMs.coerceAtLeast(1L).toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Slider(
            value = positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L)).toFloat(),
            onValueChange = { onPositionChange(it.toLong()) },
            valueRange = 0f..max,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Neon,
                activeTrackColor = Neon,
                inactiveTrackColor = PanelHigh
            )
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(positionMs.asClock(), color = Neon, style = MaterialTheme.typography.bodyMedium)
            Text(durationMs.asClock(), color = TextDim, style = MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepButton(Icons.Filled.Replay10, "אחורה 10 שניות", enabled) {
                onPositionChange((positionMs - 10_000).coerceAtLeast(0L))
            }
            StepButton(Icons.Filled.Forward10, "קדימה 10 שניות", enabled) {
                onPositionChange((positionMs + 10_000).coerceAtMost(durationMs))
            }
            StepButton(Icons.Filled.Refresh, "רענון התצוגה", enabled, onClick = onRefresh)
            listOf("25%" to 0.25f, "50%" to 0.5f, "75%" to 0.75f).forEach { (label, fraction) ->
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PanelHigh)
                        .clickable(enabled = enabled) {
                            onPositionChange((durationMs * fraction).toLong())
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (enabled) TextHigh else TextDim, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PanelHigh)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (enabled) Neon else TextDim)
    }
}

/* ---------- עיצוב הכתוביות ---------- */

@Composable
private fun ColumnScope.StyleControls(
    settings: BurnSettings,
    locked: Boolean,
    onChange: (BurnSettings) -> Unit
) {
    val style = settings.style

    Tuner(
        title = "גודל הכתוביות",
        hint = "20 הוא ברירת המחדל. במסך טלפון 24–28 נוח יותר לקריאה.",
        value = style.fontSize.toFloat(),
        range = 10f..48f,
        steps = 37,
        display = style.fontSize.toString(),
        enabled = !locked
    ) { onChange(settings.copy(style = style.copy(fontSize = it.roundToInt()))) }

    Text("סוג הרקע", color = TextHigh, style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BackgroundMode.entries.forEach { mode ->
            val selected = style.background == mode
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) Violet.copy(alpha = 0.22f) else PanelHigh)
                    .border(1.dp, if (selected) Violet else Color.Transparent, RoundedCornerShape(14.dp))
                    .clickable(enabled = !locked) {
                        onChange(settings.copy(style = style.copy(background = mode)))
                    }
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(mode.label, color = if (selected) TextHigh else TextDim, fontWeight = FontWeight.Medium)
                Text(mode.hint, color = TextDim, style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    AnimatedVisibility(style.background == BackgroundMode.BOX) {
        Tuner(
            title = "אטימות הרקע השחור",
            hint = "0% = שקוף לגמרי, 100% = שחור אטום.",
            value = style.backgroundOpacity.toFloat(),
            range = 0f..100f,
            steps = 19,
            display = "${style.backgroundOpacity}%",
            enabled = !locked
        ) { onChange(settings.copy(style = style.copy(backgroundOpacity = it.roundToInt()))) }
    }

    Tuner(
        title = "עובי הקו השחור סביב האותיות",
        hint = "זה מה שמפריד את הטקסט מהתמונה. 1.5 מתאים לרוב הסרטים.",
        value = style.outline,
        range = 0f..5f,
        steps = 9,
        display = "%.1f".format(style.outline),
        enabled = !locked
    ) { onChange(settings.copy(style = style.copy(outline = round1(it)))) }

    Tuner(
        title = "צל",
        hint = "צל נופל אלכסונית מתחת לאותיות ומוסיף עומק.",
        value = style.shadow,
        range = 0f..4f,
        steps = 7,
        display = "%.1f".format(style.shadow),
        enabled = !locked
    ) { onChange(settings.copy(style = style.copy(shadow = round1(it)))) }

    Tuner(
        title = "ריכוך הקצוות",
        hint = "מטשטש קלות את השחור סביב האותיות — נראה רך ומקצועי יותר מקצה חד.",
        value = style.blur,
        range = 0f..3f,
        steps = 5,
        display = if (style.blur == 0f) "כבוי" else "%.1f".format(style.blur),
        enabled = !locked
    ) { onChange(settings.copy(style = style.copy(blur = round1(it)))) }

    Tuner(
        title = "מרחק מתחתית המסך",
        hint = "מרים את הכתוביות למעלה כדי שלא יתנגשו בשולי המסך.",
        value = style.marginV.toFloat(),
        range = 0f..120f,
        steps = 23,
        display = "${style.marginV} פיקסלים",
        enabled = !locked
    ) { onChange(settings.copy(style = style.copy(marginV = it.roundToInt()))) }

    ToggleRow(
        title = "הטיה באלכסון",
        hint = "האותיות נשענות קדימה, והשחור שעוטף אותן נראה מקצועי יותר.",
        checked = style.italic,
        enabled = !locked
    ) { onChange(settings.copy(style = style.copy(italic = it))) }

    ToggleRow(
        title = "אותיות מודגשות",
        hint = "עובי גדול יותר — עוזר במסכים קטנים.",
        checked = style.bold,
        enabled = !locked
    ) { onChange(settings.copy(style = style.copy(bold = it))) }

    Field(
        label = "שם הפונט",
        hint = "חייב להיות פונט שמותקן במערכת או שנמצא בתיקיית הפונטים למטה.",
        value = style.fontName,
        placeholder = "Noto Sans Hebrew",
        enabled = !locked
    ) { onChange(settings.copy(style = style.copy(fontName = it))) }

    Field(
        label = "תיקיית פונטים (לא חובה)",
        hint = "נתיב לתיקייה עם קובצי פונט משלך, למשל /storage/emulated/0/fonts",
        value = settings.fontsDir,
        placeholder = "/storage/emulated/0/fonts",
        enabled = !locked
    ) { onChange(settings.copy(fontsDir = it)) }
}

/* ---------- דחיסה ---------- */

@Composable
private fun ColumnScope.QualityControls(
    settings: BurnSettings,
    locked: Boolean,
    onChange: (BurnSettings) -> Unit
) {
    Tuner(
        title = "איכות (CRF)",
        hint = "מספר נמוך = איכות גבוהה וקובץ גדול. 20 שומר על איכות כמעט זהה למקור.",
        value = settings.crf.toFloat(),
        range = 16f..32f,
        steps = 15,
        display = "${settings.crf} · ${crfHint(settings.crf)}",
        enabled = !locked,
        accent = Amber
    ) { onChange(settings.copy(crf = it.roundToInt())) }

    Text("מהירות הקידוד", color = TextHigh, style = MaterialTheme.typography.titleMedium)
    Text(
        "איטי יותר = קובץ קטן יותר באותה איכות. מהיר יותר = מסיים מוקדם, קובץ גדול יותר.",
        color = TextDim,
        style = MaterialTheme.typography.bodyMedium
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        presets.forEach { (value, label) ->
            val selected = settings.preset == value
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Amber.copy(alpha = 0.2f) else PanelHigh)
                    .border(1.dp, if (selected) Amber else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable(enabled = !locked) { onChange(settings.copy(preset = value)) }
                    .padding(vertical = 10.dp, horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) TextHigh else TextDim,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun crfHint(crf: Int) = when {
    crf <= 18 -> "איכות מקסימלית"
    crf <= 22 -> "איכות גבוהה"
    crf <= 26 -> "מאוזן"
    else -> "קובץ קטן"
}

/* ---------- פקודה, התקדמות, לוג ---------- */

@Composable
private fun CommandPanel(command: String) {
    var expanded by remember { mutableStateOf(false) }
    Panel(Neon) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Terminal, contentDescription = null, tint = Neon)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (expanded) "הסתרת הפקודה" else "הצגת הפקודה שתרוץ",
                    color = TextHigh,
                    style = MaterialTheme.typography.titleMedium
                )
                Text("הפקודה המדויקת של ffmpeg, לפי ההגדרות שבחרת", color = TextDim, style = MaterialTheme.typography.labelSmall)
            }
        }
        AnimatedVisibility(expanded) {
            Text(
                command,
                color = Neon,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Void)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun ProgressPanel(state: RenderState.Running, onCancel: () -> Unit) {
    Panel(Neon) {
        Text("בצריבה", color = Neon, style = MaterialTheme.typography.labelSmall)
        Text(state.displayName, color = TextHigh, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        if (state.progress >= 0f) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Neon,
                trackColor = PanelHigh
            )
            Text("${(state.progress * 100).roundToInt()}%", color = Neon, style = MaterialTheme.typography.titleMedium)
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Neon,
                trackColor = PanelHigh
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("מהירות", "%.2fx".format(state.speed))
            Stat("פריימים", "%.0f".format(state.fps))
            Stat("גודל עד כה", state.outputSizeBytes.asReadableSize())
            Stat("זמן שנותר", if (state.etaSeconds < 0) "—" else (state.etaSeconds * 1000).asClock())
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)
        ) { Text("ביטול הצריבה") }
        Text(
            "הצריבה ממשיכה גם כשהמסך כבוי וגם אם יוצאים מהאפליקציה.",
            style = MaterialTheme.typography.labelSmall,
            color = TextDim
        )
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextDim)
        Text(value, color = TextHigh, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatusPanel(title: String, body: String, accent: Color) {
    Panel(accent) {
        Text(title, color = accent, style = MaterialTheme.typography.titleMedium)
        Text(body, color = TextHigh, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LogPanel(lines: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    Panel(TextDim) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (expanded) "הסתרת היומן הטכני" else "הצגת היומן הטכני",
                color = TextHigh,
                style = MaterialTheme.typography.titleMedium
            )
        }
        AnimatedVisibility(expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                lines.takeLast(60).forEach {
                    Text(it, color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun StartButton(enabled: Boolean, running: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (enabled) Brush.horizontalGradient(listOf(Neon, Violet))
                    else Brush.horizontalGradient(listOf(PanelHigh, PanelHigh))
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (running) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Neon, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = if (enabled) Void else TextDim)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    if (running) "צורבת…" else "התחלת הצריבה",
                    color = if (enabled) Void else TextDim,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/* ---------- פקדים בסיסיים ---------- */

@Composable
private fun Tuner(
    title: String,
    hint: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    display: String,
    enabled: Boolean,
    accent: Color = Violet,
    onValue: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = TextHigh, style = MaterialTheme.typography.bodyMedium)
            Text(display, color = accent, style = MaterialTheme.typography.bodyMedium)
        }
        Text(hint, color = TextDim, style = MaterialTheme.typography.labelSmall)
        Slider(
            value = value,
            onValueChange = onValue,
            valueRange = range,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = PanelHigh
            )
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    hint: String,
    checked: Boolean,
    enabled: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextHigh, style = MaterialTheme.typography.bodyMedium)
            Text(hint, color = TextDim, style = MaterialTheme.typography.labelSmall)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Void,
                checkedTrackColor = Violet,
                uncheckedTrackColor = PanelHigh
            )
        )
    }
}

@Composable
private fun Field(
    label: String,
    hint: String,
    value: String,
    placeholder: String,
    enabled: Boolean,
    onValue: (String) -> Unit
) {
    Column {
        Text(label, color = TextHigh, style = MaterialTheme.typography.bodyMedium)
        Text(hint, color = TextDim, style = MaterialTheme.typography.labelSmall)
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            enabled = enabled,
            singleLine = true,
            placeholder = { Text(placeholder, color = TextDim, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Neon,
                unfocusedBorderColor = PanelHigh,
                focusedTextColor = TextHigh,
                unfocusedTextColor = TextHigh,
                cursorColor = Neon
            )
        )
    }
}

private fun round1(v: Float) = (v * 10).roundToInt() / 10f
