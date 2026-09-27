package com.subburn.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import kotlin.math.roundToInt

/** Everything the screen lets the user change before a render starts. */
data class BurnSettings(
    val style: SubtitleStyle = SubtitleStyle(),
    val crf: Int = 20,
    val preset: String = "medium",
    val fontsDir: String = ""
)

private val presets = listOf("ultrafast", "veryfast", "fast", "medium", "slow")

@Composable
fun BurnScreen(
    video: PickedMedia?,
    subtitle: PickedSubtitle?,
    settings: BurnSettings,
    renderState: RenderState,
    logLines: List<String>,
    commandPreview: String,
    onPickVideo: () -> Unit,
    onPickSubtitle: () -> Unit,
    onSettingsChange: (BurnSettings) -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    val running = renderState is RenderState.Running
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF070B16), Void, Color(0xFF0A0717)))
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Header()

            SourceCard(
                label = "VIDEO SOURCE",
                icon = Icons.Filled.Movie,
                accent = Neon,
                primary = video?.displayName ?: "Choose a video file",
                secondary = video?.let {
                    "${it.sizeBytes.asReadableSize()} · ${it.durationMs.asClock()}"
                } ?: "MKV, MP4, AVI, TS…",
                enabled = !running,
                onClick = onPickVideo
            )

            SourceCard(
                label = "SUBTITLE TRACK",
                icon = Icons.Filled.ClosedCaption,
                accent = Violet,
                primary = subtitle?.displayName ?: "Choose an .srt file",
                secondary = subtitle?.let { "${it.cueCount} cues loaded" } ?: "SRT / ASS / SSA",
                enabled = !running,
                onClick = onPickSubtitle
            )

            StylePanel(settings, running) { onSettingsChange(it) }
            QualityPanel(settings, running) { onSettingsChange(it) }
            CommandPanel(commandPreview)

            when (renderState) {
                is RenderState.Running -> ProgressPanel(renderState, onCancel)
                is RenderState.Done -> StatusPanel(
                    "Render complete",
                    listOfNotNull(
                        renderState.savedTo?.let { "Saved to $it" },
                        "Output size ${renderState.sizeBytes.asReadableSize()}"
                    ).joinToString("\n"),
                    Neon
                )
                is RenderState.Failed -> StatusPanel("Render failed", renderState.message, Danger)
                RenderState.Cancelled -> StatusPanel("Cancelled", "No output was written.", Amber)
                RenderState.Idle -> Unit
            }

            StartButton(
                enabled = video != null && subtitle != null && !running,
                running = running,
                onClick = onStart
            )

            if (logLines.isNotEmpty()) LogPanel(logLines)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Header() {
    Column {
        Text(
            "SUBBURN",
            style = MaterialTheme.typography.headlineSmall,
            color = TextHigh
        )
        Box(
            Modifier
                .padding(top = 6.dp)
                .height(3.dp)
                .width(96.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.horizontalGradient(listOf(Neon, Violet)))
        )
        Text(
            "Hardcode subtitles · HEVC re-encode · runs in the background",
            style = MaterialTheme.typography.bodyMedium,
            color = TextDim,
            modifier = Modifier.padding(top = 10.dp)
        )
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
private fun SectionLabel(text: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(accent)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = accent)
    }
}

@Composable
private fun SourceCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    primary: String,
    secondary: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Panel)
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = accent)
                Text(
                    primary,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHigh,
                    maxLines = 2
                )
                Text(secondary, style = MaterialTheme.typography.bodyMedium, color = TextDim)
            }
        }
    }
}

@Composable
private fun StylePanel(
    settings: BurnSettings,
    locked: Boolean,
    onChange: (BurnSettings) -> Unit
) {
    val style = settings.style
    Panel(Violet) {
        SectionLabel("SUBTITLE STYLE", Violet)
        SubtitlePreview(style)

        NeonSlider(
            title = "Font size",
            value = style.fontSize.toFloat(),
            range = 10f..48f,
            steps = 37,
            display = style.fontSize.toString(),
            enabled = !locked
        ) { onChange(settings.copy(style = style.copy(fontSize = it.roundToInt()))) }

        Text("Background", style = MaterialTheme.typography.bodyMedium, color = TextDim)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BackgroundMode.entries.forEach { mode ->
                val selected = style.background == mode
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) Violet.copy(alpha = 0.22f) else PanelHigh)
                        .border(
                            1.dp,
                            if (selected) Violet else Color.Transparent,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable(enabled = !locked) {
                            onChange(settings.copy(style = style.copy(background = mode)))
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        mode.label,
                        color = if (selected) TextHigh else TextDim,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        AnimatedVisibility(style.background == BackgroundMode.BOX) {
            NeonSlider(
                title = "Box opacity",
                value = style.backgroundOpacity.toFloat(),
                range = 0f..100f,
                steps = 19,
                display = "${style.backgroundOpacity}%",
                enabled = !locked
            ) { onChange(settings.copy(style = style.copy(backgroundOpacity = it.roundToInt()))) }
        }

        NeonSlider(
            title = "Black outline",
            value = style.outline,
            range = 0f..5f,
            steps = 9,
            display = "%.1f".format(style.outline),
            enabled = !locked
        ) { onChange(settings.copy(style = style.copy(outline = round1(it)))) }

        NeonSlider(
            title = "Drop shadow",
            value = style.shadow,
            range = 0f..4f,
            steps = 7,
            display = "%.1f".format(style.shadow),
            enabled = !locked
        ) { onChange(settings.copy(style = style.copy(shadow = round1(it)))) }

        NeonSlider(
            title = "Bottom margin",
            value = style.marginV.toFloat(),
            range = 0f..120f,
            steps = 23,
            display = "${style.marginV} px",
            enabled = !locked
        ) { onChange(settings.copy(style = style.copy(marginV = it.roundToInt()))) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Bold text", color = TextHigh, style = MaterialTheme.typography.bodyMedium)
                Text("Heavier glyphs for small screens", color = TextDim, style = MaterialTheme.typography.labelSmall)
            }
            Switch(
                checked = style.bold,
                enabled = !locked,
                onCheckedChange = { onChange(settings.copy(style = style.copy(bold = it))) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Void,
                    checkedTrackColor = Violet,
                    uncheckedTrackColor = PanelHigh
                )
            )
        }

        LabeledField(
            label = "Fonts directory (optional)",
            value = settings.fontsDir,
            placeholder = "/storage/emulated/0/fonts",
            enabled = !locked
        ) { onChange(settings.copy(fontsDir = it)) }

        LabeledField(
            label = "Font name",
            value = style.fontName,
            placeholder = "Noto Sans Hebrew",
            enabled = !locked
        ) { onChange(settings.copy(style = style.copy(fontName = it))) }
    }
}

@Composable
private fun SubtitlePreview(style: SubtitleStyle) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF16203A), Color(0xFF241634)))
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        val boxed = style.background == BackgroundMode.BOX
        Box(
            Modifier
                .padding(bottom = (style.marginV / 6).coerceIn(4, 40).dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (boxed) {
                        Color.Black.copy(alpha = style.backgroundOpacity / 100f)
                    } else {
                        Color.Transparent
                    }
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                "שלום עולם · Sample subtitle",
                color = Color.White,
                fontSize = (style.fontSize * 0.7f).coerceIn(10f, 28f).sp,
                fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black,
                        offset = androidx.compose.ui.geometry.Offset(
                            style.shadow.coerceAtMost(3f),
                            style.shadow.coerceAtMost(3f)
                        ),
                        blurRadius = 1f + style.outline * 2f
                    )
                )
            )
        }
    }
}

@Composable
private fun QualityPanel(
    settings: BurnSettings,
    locked: Boolean,
    onChange: (BurnSettings) -> Unit
) {
    Panel(Amber) {
        SectionLabel("COMPRESSION · H.265", Amber)
        NeonSlider(
            title = "Quality (CRF)",
            value = settings.crf.toFloat(),
            range = 16f..32f,
            steps = 15,
            display = "${settings.crf} · ${crfHint(settings.crf)}",
            enabled = !locked,
            accent = Amber
        ) { onChange(settings.copy(crf = it.roundToInt())) }

        Text("Encoder preset", style = MaterialTheme.typography.bodyMedium, color = TextDim)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            presets.forEach { preset ->
                val selected = settings.preset == preset
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Amber.copy(alpha = 0.2f) else PanelHigh)
                        .border(1.dp, if (selected) Amber else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable(enabled = !locked) { onChange(settings.copy(preset = preset)) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        preset.take(5),
                        color = if (selected) TextHigh else TextDim,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        Text(
            "Audio is stream-copied, so only the video is re-encoded.",
            style = MaterialTheme.typography.labelSmall,
            color = TextDim
        )
    }
}

private fun crfHint(crf: Int) = when {
    crf <= 18 -> "near-lossless"
    crf <= 22 -> "high quality"
    crf <= 26 -> "balanced"
    else -> "small file"
}

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
                if (expanded) "Hide ffmpeg command" else "Show ffmpeg command",
                color = TextHigh,
                style = MaterialTheme.typography.titleMedium
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
        SectionLabel("RENDERING", Neon)
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
            Stat("SPEED", "%.2fx".format(state.speed))
            Stat("FPS", "%.0f".format(state.fps))
            Stat("OUTPUT", state.outputSizeBytes.asReadableSize())
            Stat("ETA", if (state.etaSeconds < 0) "—" else (state.etaSeconds * 1000).asClock())
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)
        ) { Text("Cancel render") }
        Text(
            "Keeps running with the screen off — a wake lock holds the encode alive.",
            style = MaterialTheme.typography.labelSmall,
            color = TextDim
        )
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextDim)
        Text(value, color = TextHigh, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatusPanel(title: String, body: String, accent: Color) {
    Panel(accent) {
        SectionLabel(title.uppercase(), accent)
        Text(body, color = TextHigh, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LogPanel(lines: List<String>) {
    Panel(TextDim) {
        SectionLabel("FFMPEG LOG", TextDim)
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 180.dp)
                .verticalScroll(rememberScrollState())
        ) {
            lines.takeLast(40).forEach {
                Text(it, color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (enabled) {
                        Brush.horizontalGradient(listOf(Neon, Violet))
                    } else {
                        Brush.horizontalGradient(listOf(PanelHigh, PanelHigh))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Neon,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = if (enabled) Void else TextDim
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    if (running) "Rendering…" else "Burn subtitles",
                    color = if (enabled) Void else TextDim,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun NeonSlider(
    title: String,
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
            Text(title, color = TextDim, style = MaterialTheme.typography.bodyMedium)
            Text(display, color = accent, style = MaterialTheme.typography.bodyMedium)
        }
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
private fun LabeledField(
    label: String,
    value: String,
    placeholder: String,
    enabled: Boolean,
    onValue: (String) -> Unit
) {
    Column {
        Text(label, color = TextDim, style = MaterialTheme.typography.labelSmall)
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onValue,
            enabled = enabled,
            singleLine = true,
            placeholder = { Text(placeholder, color = TextDim, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
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
