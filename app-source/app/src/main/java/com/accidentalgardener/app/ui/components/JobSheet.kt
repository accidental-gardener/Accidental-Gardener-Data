@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.accidentalgardener.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accidentalgardener.app.data.Bed
import com.accidentalgardener.app.data.GuideSection
import com.accidentalgardener.app.data.isPot
import com.accidentalgardener.app.domain.GardenTask
import com.accidentalgardener.app.ui.theme.Bark
import com.accidentalgardener.app.ui.theme.Cream
import com.accidentalgardener.app.ui.theme.Soil
import com.accidentalgardener.app.ui.theme.SoilLight
import kotlin.math.roundToInt

/** Step-by-step help for one job, shown only when that job is due. */
@Composable
fun JobSheet(task: GardenTask, bed: Bed?, onDismiss: () -> Unit, onDone: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val section = task.section
    val ticked = remember(task.planting.id, task.kind) { mutableStateListOf<Int>() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBubble(task.crop.emoji, size = 56.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(task.title.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        listOfNotNull(bed?.name, task.date.format(ShortDate)).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (section.facts.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                FactGrid(section)
            }

            if (section.steps.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(section.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                section.steps.forEachIndexed { i, step ->
                    val done = i in ticked
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable { if (done) ticked.remove(i) else ticked.add(i) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Checkbox(checked = done, onCheckedChange = { if (it) ticked.add(i) else ticked.remove(i) })
                        Text(
                            step,
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (done) TextDecoration.LineThrough else null,
                            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }

            section.layout?.let { layout ->
                Spacer(Modifier.height(16.dp))
                Text("How the bed should look", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(layout, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(10.dp))
                if (bed?.isPot == true) {
                    GardenCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer) {
                        Text(
                            "🪴 In a pot, grow one plant per ${task.crop.potSize.ifBlank { "pot" }}. Use multipurpose compost, " +
                                "make sure there are drainage holes and stand the pot somewhere sunny and sheltered.",
                            Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    BedLayoutDiagram(task.crop.emoji, task.crop.howTo.spacingCm, task.crop.howTo.rowCm, bed?.sizeM2)
                }
            }

            if (task.crop.tip.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                GardenCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("💡 ${task.crop.tip}", Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Not yet") }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onDone(); onDismiss() }) { Text("✅  Done") }
            }
        }
    }
}

@Composable
private fun FactGrid(section: GuideSection) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        section.facts.forEach { (label, value) ->
            Column(
                Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(value, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/**
 * A little picture of the bed with plants drawn at the right spacing.
 * Uses the bed's size if known, otherwise a typical 1.2 m by 2.4 m raised bed.
 */
@Composable
fun BedLayoutDiagram(emoji: String, spacingCm: Int, rowCm: Int, sizeM2: Double?) {
    val widthCm = 120
    val lengthCm = sizeM2?.let { (it * 10_000 / widthCm).roundToInt().coerceIn(60, 1000) } ?: 240
    val realPerRow = (lengthCm / spacingCm.coerceAtLeast(1)).coerceAtLeast(1)
    val realRows = (widthCm / rowCm.coerceAtLeast(1)).coerceAtLeast(1)
    val perRow = realPerRow.coerceAtMost(12)
    val rows = realRows.coerceAtMost(6)
    val emojiSize = when {
        perRow > 9 -> 14.sp
        perRow > 6 -> 18.sp
        else -> 24.sp
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color(0xFFA0744C), Bark)))
            .padding(7.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(Brush.verticalGradient(listOf(SoilLight, Soil)))
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(rows) { r ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    // stagger every second row slightly by dropping one plant
                    val count = if (rows > 1 && r % 2 == 1 && perRow > 2) perRow - 1 else perRow
                    repeat(count) { Text(emoji, fontSize = emojiSize) }
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp, end = 4.dp)) {
            Text(
                buildString {
                    append(if (sizeM2 != null) "Your bed" else "A 1.2 m by 2.4 m bed")
                    append(" fits about $realRows row${if (realRows == 1) "" else "s"} of $realPerRow")
                    if (realPerRow > perRow || realRows > rows) append(" (picture simplified)")
                },
                color = Cream, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
