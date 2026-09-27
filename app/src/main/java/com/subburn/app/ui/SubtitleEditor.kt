package com.subburn.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subburn.app.core.Cue
import com.subburn.app.core.asClock

/** מה נערך כרגע: כתובית קיימת (עם המקום שלה ברשימה) או חדשה. */
private data class Draft(
    val index: Int?,
    val startMs: Long,
    val endMs: Long,
    val text: String
)

/**
 * רשימת הכתוביות עם עריכה, הוספה ומחיקה. הזמנים נלקחים ממקום הנגינה, כמו
 * בעורכי וידאו בטלפון: עוצרים במקום הנכון ולוחצים "התחלה מכאן".
 */
@Composable
fun SubtitleEditor(
    cues: List<Cue>,
    positionMs: Long,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
    onSave: (index: Int?, cue: Cue) -> Unit,
    onDelete: (index: Int) -> Unit
) {
    var draft by remember { mutableStateOf<Draft?>(null) }
    val activeIndex = cues.indexOfLast { positionMs >= it.startMs && positionMs <= it.endMs }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Neon.copy(alpha = 0.14f))
                .border(1.dp, Neon.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .clickable(enabled = enabled) {
                    draft = Draft(null, positionMs, positionMs + 2_500, "")
                }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Neon)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("הוספת כתובית מכאן", color = TextHigh, fontWeight = FontWeight.Medium)
                Text(
                    "תיפתח כתובית חדשה מ־${positionMs.asClock()} לשתי שניות וחצי, ואפשר לשנות",
                    color = TextDim,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        if (cues.isEmpty()) {
            Text(
                "אין עדיין כתוביות. בחרי קובץ SRT, או הוסיפי כתוביות בעצמך.",
                color = TextDim,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cues.forEachIndexed { index, cue ->
                    CueRow(
                        cue = cue,
                        number = index + 1,
                        active = index == activeIndex,
                        enabled = enabled,
                        onSeek = { onSeek(cue.startMs) },
                        onEdit = { draft = Draft(index, cue.startMs, cue.endMs, cue.text) }
                    )
                }
            }
        }
    }

    draft?.let { current ->
        EditDialog(
            draft = current,
            positionMs = positionMs,
            onChange = { draft = it },
            onDismiss = { draft = null },
            onDelete = current.index?.let {
                {
                    onDelete(it)
                    draft = null
                }
            },
            onConfirm = {
                val safeEnd = if (current.endMs <= current.startMs) current.startMs + 1_000 else current.endMs
                onSave(current.index, Cue(current.startMs, safeEnd, current.text.trim()))
                draft = null
            }
        )
    }
}

@Composable
private fun CueRow(
    cue: Cue,
    number: Int,
    active: Boolean,
    enabled: Boolean,
    onSeek: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) Violet.copy(alpha = 0.18f) else PanelHigh)
            .border(
                1.dp,
                if (active) Violet else Color.Transparent,
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled, onClick = onSeek)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "$number · ${cue.startMs.asClock()} → ${cue.endMs.asClock()}",
                color = if (active) Violet else TextDim,
                style = MaterialTheme.typography.labelSmall
            )
            Text(cue.text, color = TextHigh, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
        }
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Panel)
                .clickable(enabled = enabled, onClick = onEdit),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Edit, contentDescription = "עריכה", tint = Neon)
        }
    }
}

@Composable
private fun EditDialog(
    draft: Draft,
    positionMs: Long,
    onChange: (Draft) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = {
            Text(
                if (draft.index == null) "כתובית חדשה" else "עריכת כתובית",
                color = TextHigh
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.text,
                    onValueChange = { onChange(draft.copy(text = it)) },
                    label = { Text("הטקסט של הכתובית", color = TextDim) },
                    minLines = 2,
                    maxLines = 4,
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

                TimeRow(
                    label = "התחלה",
                    valueMs = draft.startMs,
                    onFromPlayhead = { onChange(draft.copy(startMs = positionMs)) },
                    onNudge = { delta ->
                        onChange(draft.copy(startMs = (draft.startMs + delta).coerceAtLeast(0L)))
                    }
                )
                TimeRow(
                    label = "סוף",
                    valueMs = draft.endMs,
                    onFromPlayhead = { onChange(draft.copy(endMs = positionMs)) },
                    onNudge = { delta ->
                        onChange(draft.copy(endMs = (draft.endMs + delta).coerceAtLeast(0L)))
                    }
                )
                Text(
                    "אפשר לעצור את הווידאו בנקודה המדויקת וללחוץ \"מכאן\".",
                    color = TextDim,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("שמירה", color = Neon, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = Danger)
                        Spacer(Modifier.width(4.dp))
                        Text("מחיקה", color = Danger)
                    }
                }
                TextButton(onClick = onDismiss) { Text("ביטול", color = TextDim) }
            }
        }
    )
}

@Composable
private fun TimeRow(
    label: String,
    valueMs: Long,
    onFromPlayhead: () -> Unit,
    onNudge: (Long) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextDim, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text(
            "%s.%03d".format(valueMs.asClock(), valueMs % 1000),
            color = TextHigh,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.weight(1f))
        MiniButton("−") { onNudge(-200) }
        Spacer(Modifier.width(6.dp))
        MiniButton("+") { onNudge(200) }
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Neon.copy(alpha = 0.16f))
                .clickable(onClick = onFromPlayhead)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("מכאן", color = Neon, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MiniButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(PanelHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = TextHigh)
    }
}
