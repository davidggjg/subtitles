package com.subburn.app.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subburn.app.core.BackgroundMode
import com.subburn.app.core.Cue
import com.subburn.app.core.PickedMedia
import com.subburn.app.core.RenderState
import com.subburn.app.core.SrtDocument
import com.subburn.app.core.SubtitleStyle
import com.subburn.app.core.asReadableSize
import kotlin.math.roundToInt

/** ההגדרות שנשלחות לצריבה. */
data class BurnSettings(
    val style: SubtitleStyle = SubtitleStyle(),
    val quality: QualityPreset = QualityPreset.RECOMMENDED,
    val fontsDir: String = ""
) {
    val crf: Int get() = quality.crf
    val preset: String get() = quality.preset
}

/** שלוש בחירות במקום שני סליידרים — כל אחת עם הסבר מה היא עושה לקובץ. */
enum class QualityPreset(
    val label: String,
    val hint: String,
    val crf: Int,
    val preset: String
) {
    BEST("איכות מקסימלית", "כמעט זהה למקור, הקידוד איטי יותר", 18, "slow"),
    RECOMMENDED("מומלץ", "מקטין את הקובץ בכחצי בלי הבדל שנראה לעין", 20, "medium"),
    SMALL("קובץ קטן", "הקטנה חזקה — מתאים לשליחה בוואטסאפ", 25, "fast")
}

/** סגנונות מוכנים; ההגדרות המדויקות נשארות מתחת ל"מתקדם". */
enum class StylePreset(val label: String, val hint: String) {
    CLASSIC("קלאסי", "קו שחור דק סביב האותיות"),
    BOXED("רקע מלא", "מלבן שחור מאחורי הטקסט"),
    CINEMA("קולנועי", "הטיה באלכסון עם שחור רך");

    fun apply(style: SubtitleStyle): SubtitleStyle = when (this) {
        CLASSIC -> style.copy(
            background = BackgroundMode.OUTLINE,
            outline = 1.5f, shadow = 0.5f, blur = 0.4f, italic = false
        )
        BOXED -> style.copy(
            background = BackgroundMode.BOX,
            backgroundOpacity = 65, outline = 1f, shadow = 0f, blur = 0f, italic = false
        )
        CINEMA -> style.copy(
            background = BackgroundMode.OUTLINE,
            outline = 2f, shadow = 1f, blur = 1f, italic = true
        )
    }

    fun matches(style: SubtitleStyle): Boolean = apply(style) == style
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurnScreen(
    video: PickedMedia?,
    subtitleName: String?,
    cues: List<Cue>,
    settings: BurnSettings,
    playback: PlaybackState,
    renderState: RenderState,
    logLines: List<String>,
    commandPreview: String,
    onPickVideo: () -> Unit,
    onPickSubtitle: () -> Unit,
    onSettingsChange: (BurnSettings) -> Unit,
    onCueSave: (index: Int?, cue: Cue) -> Unit,
    onCueDelete: (index: Int) -> Unit,
    onReset: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    val running = renderState is RenderState.Running
    val activeCue = SrtDocument.cueAt(cues, playback.positionMs)

    Scaffold(
        containerColor = Void,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("צריבת כתוביות", color = TextHigh, fontWeight = FontWeight.SemiBold)
                        Text(
                            "נגן · עריכה · צריבה עם דחיסה",
                            color = TextDim,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onReset, enabled = !running) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = "התחלה מחדש", tint = TextDim)
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
                .background(Brush.verticalGradient(listOf(Color(0xFF070B16), Void, Color(0xFF0A0717))))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // הנגן ראשון — הוא מה שרואים קודם, כמו בעורכי וידאו.
            VideoStage(
                uri = video?.uri,
                activeCue = activeCue,
                style = settings.style,
                playback = playback
            )
            TransportBar(playback, enabled = video != null && !running)

            FilePickers(
                videoLabel = video?.displayName,
                videoDetail = video?.let { "${it.sizeBytes.asReadableSize()}" },
                subtitleLabel = subtitleName,
                subtitleDetail = if (cues.isEmpty()) null else "${cues.size} כתוביות",
                enabled = !running,
                onPickVideo = onPickVideo,
                onPickSubtitle = onPickSubtitle
            )

            Section("עריכת כתוביות", "לוחצים על כתובית כדי לקפוץ אליה, על העיפרון כדי לתקן מילה, או מוסיפים כתובית חדשה מהמקום שבו הווידאו עומד.", Violet) {
                SubtitleEditor(
                    cues = cues,
                    positionMs = playback.positionMs,
                    enabled = !running,
                    onSeek = {
                        playback.pause()
                        playback.seekTo(it)
                    },
                    onSave = onCueSave,
                    onDelete = onCueDelete
                )
            }

            Section("עיצוב", "בוחרים סגנון, וזה מיד נראה על הווידאו למעלה.", Neon) {
                StyleControls(settings, !running, onSettingsChange)
            }

            Section("דחיסה", "הווידאו מקודד מחדש ב־H.265, מה שמקטין את הקובץ משמעותית. הפסקול מועתק כמו שהוא.", Amber) {
                QualityControls(settings, !running, onSettingsChange)
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
                enabled = video != null && cues.isNotEmpty() && !running,
                running = running,
                onClick = {
                    playback.pause()
                    onStart()
                }
            )

            if (logLines.isNotEmpty()) LogPanel(logLines)
            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ---------- קבצים ---------- */

@Composable
private fun FilePickers(
    videoLabel: String?,
    videoDetail: String?,
    subtitleLabel: String?,
    subtitleDetail: String?,
    enabled: Boolean,
    onPickVideo: () -> Unit,
    onPickSubtitle: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PickerTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Movie,
            accent = Neon,
            title = videoLabel ?: "בחירת וידאו",
            detail = videoDetail ?: "MKV · MP4 · AVI",
            enabled = enabled,
            onClick = onPickVideo
        )
        PickerTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.ClosedCaption,
            accent = Violet,
            title = subtitleLabel ?: "בחירת כתוביות",
            detail = subtitleDetail ?: "קובץ SRT",
            enabled = enabled,
            onClick = onPickSubtitle
        )
    }
}

@Composable
private fun PickerTile(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    title: String,
    detail: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accent)
        Text(title, color = TextHigh, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        Text(detail, color = TextDim, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/* ---------- מסגרת ---------- */

@Composable
private fun Section(
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, color = TextHigh, style = MaterialTheme.typography.titleMedium)
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

/* ---------- עיצוב ---------- */

@Composable
private fun ColumnScope.StyleControls(
    settings: BurnSettings,
    enabled: Boolean,
    onChange: (BurnSettings) -> Unit
) {
    val style = settings.style
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StylePreset.entries.forEach { preset ->
            val selected = preset.matches(style)
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) Neon.copy(alpha = 0.18f) else PanelHigh)
                    .border(1.dp, if (selected) Neon else Color.Transparent, RoundedCornerShape(14.dp))
                    .clickable(enabled = enabled) {
                        onChange(settings.copy(style = preset.apply(style)))
                    }
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(preset.label, color = if (selected) TextHigh else TextDim, fontWeight = FontWeight.Medium)
                Text(preset.hint, color = TextDim, style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    Tuner(
        title = "גודל הכתוביות",
        hint = "בטלפון 22–28 נוח לקריאה.",
        value = style.fontSize.toFloat(),
        range = 10f..48f,
        steps = 37,
        display = style.fontSize.toString(),
        enabled = enabled,
        accent = Neon
    ) { onChange(settings.copy(style = style.copy(fontSize = it.roundToInt()))) }

    var advanced by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { advanced = !advanced },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Tune, contentDescription = null, tint = TextDim)
        Spacer(Modifier.width(8.dp))
        Text(
            if (advanced) "סגירת ההגדרות המדויקות" else "הגדרות מדויקות (לא חובה)",
            color = TextDim,
            style = MaterialTheme.typography.bodyMedium
        )
    }

    AnimatedVisibility(advanced) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AnimatedVisibility(style.background == BackgroundMode.BOX) {
                Tuner(
                    title = "אטימות הרקע השחור",
                    hint = "0% שקוף, 100% שחור מלא.",
                    value = style.backgroundOpacity.toFloat(),
                    range = 0f..100f,
                    steps = 19,
                    display = "${style.backgroundOpacity}%",
                    enabled = enabled
                ) { onChange(settings.copy(style = style.copy(backgroundOpacity = it.roundToInt()))) }
            }
            Tuner(
                title = "עובי הקו השחור",
                hint = "מפריד את האותיות מהתמונה.",
                value = style.outline,
                range = 0f..5f,
                steps = 9,
                display = "%.1f".format(style.outline),
                enabled = enabled
            ) { onChange(settings.copy(style = style.copy(outline = round1(it)))) }
            Tuner(
                title = "צל",
                hint = "צל אלכסוני מתחת לאותיות.",
                value = style.shadow,
                range = 0f..4f,
                steps = 7,
                display = "%.1f".format(style.shadow),
                enabled = enabled
            ) { onChange(settings.copy(style = style.copy(shadow = round1(it)))) }
            Tuner(
                title = "ריכוך הקצוות",
                hint = "מטשטש את השחור סביב האותיות.",
                value = style.blur,
                range = 0f..3f,
                steps = 5,
                display = if (style.blur == 0f) "כבוי" else "%.1f".format(style.blur),
                enabled = enabled
            ) { onChange(settings.copy(style = style.copy(blur = round1(it)))) }
            Tuner(
                title = "מרחק מתחתית המסך",
                hint = "מרים את הכתוביות מעל שולי המסך.",
                value = style.marginV.toFloat(),
                range = 0f..120f,
                steps = 23,
                display = "${style.marginV}",
                enabled = enabled
            ) { onChange(settings.copy(style = style.copy(marginV = it.roundToInt()))) }
            ToggleRow("הטיה באלכסון", "האותיות נשענות קדימה.", style.italic, enabled) {
                onChange(settings.copy(style = style.copy(italic = it)))
            }
            ToggleRow("אותיות מודגשות", "עובי גדול יותר.", style.bold, enabled) {
                onChange(settings.copy(style = style.copy(bold = it)))
            }
            Field(
                label = "שם הפונט",
                hint = "פונט שמותקן במערכת, או שנמצא בתיקייה שלמטה.",
                value = style.fontName,
                placeholder = "Noto Sans Hebrew",
                enabled = enabled
            ) { onChange(settings.copy(style = style.copy(fontName = it))) }
            Field(
                label = "תיקיית פונטים",
                hint = "לא חובה — נתיב לתיקייה עם קובצי פונט משלך.",
                value = settings.fontsDir,
                placeholder = "/storage/emulated/0/fonts",
                enabled = enabled
            ) { onChange(settings.copy(fontsDir = it)) }
        }
    }
}

/* ---------- דחיסה ---------- */

@Composable
private fun ColumnScope.QualityControls(
    settings: BurnSettings,
    enabled: Boolean,
    onChange: (BurnSettings) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QualityPreset.entries.forEach { preset ->
            val selected = settings.quality == preset
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) Amber.copy(alpha = 0.2f) else PanelHigh)
                    .border(1.dp, if (selected) Amber else Color.Transparent, RoundedCornerShape(14.dp))
                    .clickable(enabled = enabled) { onChange(settings.copy(quality = preset)) }
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(preset.label, color = if (selected) TextHigh else TextDim, fontWeight = FontWeight.Medium)
                Text(preset.hint, color = TextDim, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/* ---------- פקודה, התקדמות, יומן ---------- */

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
            Text(
                if (expanded) "הסתרת הפקודה" else "הצגת הפקודה שתרוץ",
                color = TextHigh,
                style = MaterialTheme.typography.bodyMedium
            )
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
            Stat("זמן שנותר", if (state.etaSeconds < 0) "—" else formatEta(state.etaSeconds))
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

private fun formatEta(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
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
                style = MaterialTheme.typography.bodyMedium
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
                    if (running) "צורבת…" else "צריבה ושמירה",
                    color = if (enabled) Void else TextDim,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/* ---------- פקדים ---------- */

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
