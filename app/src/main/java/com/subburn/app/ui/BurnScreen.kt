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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
            outline = 1.5f, shadow = 0.5f, blur = 0f, italic = false
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

/** מה פתוח כרגע בחלונית התחתונה. */
private enum class Sheet(val title: String) {
    EDIT("עריכת כתוביות"),
    STYLE("עיצוב הכתוביות"),
    QUALITY("איכות ודחיסה"),
    LOG("יומן טכני")
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
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var pendingCueIndex by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = Void,
        topBar = {
            TopBar(
                canBurn = video != null && cues.isNotEmpty() && !running,
                running = running,
                onReset = onReset,
                onBurn = {
                    playback.pause()
                    onStart()
                }
            )
        }
    ) { insets ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(insets)
                .background(Brush.verticalGradient(listOf(Color(0xFF070B16), Void)))
        ) {
            VideoStage(
                uri = video?.uri,
                activeCue = activeCue,
                style = settings.style,
                playback = playback,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )

            Column(Modifier.padding(horizontal = 14.dp)) {
                TransportBar(playback, enabled = video != null && !running)
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                contentAlignment = Alignment.TopStart
            ) {
                TimelineTrack(
                    cues = cues,
                    playback = playback,
                    enabled = video != null && !running,
                    onCueClick = { index ->
                        playback.pause()
                        playback.seekTo(cues[index].startMs)
                        pendingCueIndex = index
                        sheet = Sheet.EDIT
                    },
                    onAddCue = { sheet = Sheet.EDIT }
                )
            }

            StatusStrip(renderState, onCancel)

            BottomToolbar(
                hasVideo = video != null,
                subtitleName = subtitleName,
                cueCount = cues.size,
                enabled = !running,
                hasLog = logLines.isNotEmpty(),
                onPickVideo = onPickVideo,
                onPickSubtitle = onPickSubtitle,
                onOpen = { sheet = it }
            )
        }
    }

    val current = sheet
    if (current != null) {
        ModalBottomSheet(
            onDismissRequest = {
                sheet = null
                pendingCueIndex = null
            },
            sheetState = sheetState,
            containerColor = Panel,
            contentColor = TextHigh
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(current.title, color = TextHigh, style = MaterialTheme.typography.titleMedium)
                when (current) {
                    Sheet.EDIT -> SubtitleEditor(
                        cues = cues,
                        positionMs = playback.positionMs,
                        enabled = !running,
                        openIndex = pendingCueIndex,
                        onOpened = { pendingCueIndex = null },
                        onSeek = {
                            playback.pause()
                            playback.seekTo(it)
                        },
                        onSave = onCueSave,
                        onDelete = onCueDelete
                    )
                    Sheet.STYLE -> StyleControls(settings, !running, onSettingsChange)
                    Sheet.QUALITY -> {
                        QualityControls(settings, !running, onSettingsChange)
                        CommandPanel(commandPreview)
                    }
                    Sheet.LOG -> LogLines(logLines)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    canBurn: Boolean,
    running: Boolean,
    onReset: () -> Unit,
    onBurn: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text("צריבת כתוביות", color = TextHigh, fontWeight = FontWeight.SemiBold)
        },
        navigationIcon = {
            IconButton(onClick = onReset, enabled = !running) {
                Icon(Icons.Filled.RestartAlt, contentDescription = "התחלה מחדש", tint = TextDim)
            }
        },
        actions = {
            Row(
                Modifier
                    .padding(end = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (canBurn) Brush.horizontalGradient(listOf(Neon, Violet))
                        else Brush.horizontalGradient(listOf(PanelHigh, PanelHigh))
                    )
                    .clickable(enabled = canBurn, onClick = onBurn)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Neon,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = if (canBurn) Void else TextDim,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    if (running) "צורבת" else "צריבה",
                    color = if (canBurn) Void else TextDim,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Panel)
    )
}

/** סרגל הכלים התחתון — כל כפתור פותח חלונית, כמו בעורכי וידאו. */
@Composable
private fun BottomToolbar(
    hasVideo: Boolean,
    subtitleName: String?,
    cueCount: Int,
    enabled: Boolean,
    hasLog: Boolean,
    onPickVideo: () -> Unit,
    onPickSubtitle: () -> Unit,
    onOpen: (Sheet) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Panel)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolItem(Icons.Filled.Movie, "וידאו", hasVideo, enabled, onPickVideo)
        ToolItem(Icons.Filled.ClosedCaption, "קובץ כתוביות", subtitleName != null, enabled, onPickSubtitle)
        ToolItem(Icons.Filled.Edit, if (cueCount > 0) "עריכה ($cueCount)" else "עריכה", cueCount > 0, enabled) {
            onOpen(Sheet.EDIT)
        }
        ToolItem(Icons.Filled.Palette, "עיצוב", false, enabled) { onOpen(Sheet.STYLE) }
        ToolItem(Icons.Filled.Tune, "איכות", false, enabled) { onOpen(Sheet.QUALITY) }
        if (hasLog) ToolItem(Icons.Filled.Terminal, "יומן", false, true) { onOpen(Sheet.LOG) }
    }
}

@Composable
private fun ToolItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    done: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = when {
                !enabled -> TextDim.copy(alpha = 0.4f)
                done -> Neon
                else -> TextHigh
            },
            modifier = Modifier.size(22.dp)
        )
        Text(
            label,
            color = if (enabled) TextDim else TextDim.copy(alpha = 0.4f),
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

/** שורת מצב דקה מעל סרגל הכלים: התקדמות, סיום או שגיאה. */
@Composable
private fun StatusStrip(state: RenderState, onCancel: () -> Unit) {
    when (state) {
        is RenderState.Running -> Column(
            Modifier
                .fillMaxWidth()
                .background(PanelHigh)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (state.progress >= 0f) "בצריבה · ${(state.progress * 100).roundToInt()}%" else "בצריבה",
                    color = Neon,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "%.2fx · נותרו %s · %s".format(
                        state.speed,
                        if (state.etaSeconds < 0) "—" else formatEta(state.etaSeconds),
                        state.outputSizeBytes.asReadableSize()
                    ),
                    color = TextDim,
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "ביטול",
                    color = Danger,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onCancel)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            LinearProgressIndicator(
                progress = { state.progress.coerceAtLeast(0f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Neon,
                trackColor = Panel
            )
        }

        is RenderState.Done -> StatusLine(
            "הצריבה הושלמה · ${state.sizeBytes.asReadableSize()}" +
                (state.savedTo?.let { " · נשמר ב־$it" } ?: ""),
            Neon
        )
        is RenderState.Failed -> StatusLine("הצריבה נכשלה: ${state.message}", Danger)
        RenderState.Cancelled -> StatusLine("הצריבה בוטלה — לא נשמר קובץ", Amber)
        RenderState.Idle -> Unit
    }
}

@Composable
private fun StatusLine(text: String, accent: Color) {
    Text(
        text,
        color = accent,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 2,
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelHigh)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun LogLines(lines: List<String>) {
    Column(Modifier.fillMaxWidth()) {
        lines.takeLast(80).forEach {
            Text(it, color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
    }
}

/* ---------- מסגרת ---------- */

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

private fun formatEta(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
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
