@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.accidentalgardener.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import com.accidentalgardener.app.data.Area
import com.accidentalgardener.app.data.Bed
import com.accidentalgardener.app.data.Category
import com.accidentalgardener.app.data.Crop
import com.accidentalgardener.app.data.CustomCrop
import com.accidentalgardener.app.data.EntryType
import com.accidentalgardener.app.data.JournalEntry
import com.accidentalgardener.app.data.Need
import com.accidentalgardener.app.data.TunnelFit
import kotlin.math.roundToInt

@Composable
private fun Label(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(4.dp))
}

/** Log watering, feeding or anything else. */
@Composable
fun AddEntryDialog(
    initialType: EntryType,
    beds: List<Bed>,
    crops: List<Crop>,
    initialBedId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (JournalEntry) -> Unit,
) {
    var type by remember { mutableStateOf(initialType) }
    var where by remember { mutableStateOf(initialBedId?.let { "bed:$it" } ?: "all") }
    var cropId by remember { mutableStateOf<String?>(null) }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val hasTunnel = beds.any { it.isTunnel }
    val hasOutdoor = beds.any { !it.isTunnel }

    val amountLabel = when (type) {
        EntryType.WATERED -> "How much (e.g. 2 cans)"
        EntryType.FED -> "Which feed (e.g. tomato feed, seaweed)"
        EntryType.HARVESTED -> "How much (e.g. 1 kg)"
        EntryType.PEST -> "What did you see"
        else -> "Details (optional)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${type.emoji} Add to journal") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Label("What did you do?")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EntryType.entries.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text("${t.emoji} ${t.label}") })
                    }
                }
                Label("Where?")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = where == "all", onClick = { where = "all" }, label = { Text("Whole garden") })
                    if (hasOutdoor && hasTunnel) {
                        FilterChip(selected = where == Area.OUTDOOR, onClick = { where = Area.OUTDOOR }, label = { Text("All outdoor") })
                        FilterChip(selected = where == Area.TUNNEL, onClick = { where = Area.TUNNEL }, label = { Text("All tunnel") })
                    }
                    beds.forEach { b ->
                        FilterChip(selected = where == "bed:${b.id}", onClick = { where = "bed:${b.id}" }, label = { Text(b.name) })
                    }
                }
                if (crops.isNotEmpty()) {
                    Label("Which plant? (optional)")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        crops.forEach { c ->
                            FilterChip(
                                selected = cropId == c.id,
                                onClick = { cropId = if (cropId == c.id) null else c.id },
                                label = { Text("${c.emoji} ${c.name}") },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(amount, { amount = it }, label = { Text(amountLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = gardenFieldColors())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, minLines = 2, modifier = Modifier.fillMaxWidth(), colors = gardenFieldColors())
            }
        },
        confirmButton = {
            Button(onClick = {
                val bedId = where.removePrefix("bed:").toLongOrNull().takeIf { where.startsWith("bed:") }
                val area = when (where) {
                    Area.OUTDOOR -> Area.OUTDOOR
                    Area.TUNNEL -> Area.TUNNEL
                    else -> Area.ALL
                }
                onSave(
                    JournalEntry(
                        timestamp = System.currentTimeMillis(), type = type, bedId = bedId,
                        area = if (bedId == null) area else Area.ALL,
                        cropId = cropId, amount = amount.trim(), notes = notes.trim(),
                    )
                )
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val emojiChoices = listOf("🌱", "🌿", "🥬", "🥕", "🫘", "🍅", "🍓", "🍎", "🍐", "🍒", "🌸", "🌼", "🌻", "🌷", "🪴", "🍄")

/** Add a plant that isn't in the seed tin. */
@Composable
fun AddCropDialog(onDismiss: () -> Unit, initial: CustomCrop? = null, onSave: (CustomCrop) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "🌱") }
    var category by remember { mutableStateOf(initial?.category ?: Category.VEG) }
    var tunnel by remember { mutableStateOf(initial?.tunnel ?: TunnelFit.EITHER) }
    var indoors by remember { mutableStateOf(initial?.sowIndoors ?: false) }
    var hardy by remember { mutableStateOf(initial?.frostHardy ?: false) }
    var thirsty by remember { mutableStateOf(initial?.water == Need.HIGH) }
    var perennial by remember { mutableStateOf(initial?.perennial ?: false) }
    var potOk by remember { mutableStateOf(initial?.potOk ?: true) }
    var pollinator by remember { mutableStateOf(initial?.pollinator ?: false) }
    var weeks by remember { mutableFloatStateOf((initial?.weeksFromFrost ?: 0).toFloat()) }
    var days by remember { mutableFloatStateOf((initial?.daysToHarvest ?: 70).toFloat()) }
    var harvestWeeks by remember { mutableFloatStateOf((initial?.harvestWeeks ?: 6).toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) "Check the details" else "Add your own plant") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "📱 This is saved on your phone only. It won't change the plant database everyone uses. " +
                        "To get it added for everyone, use 📮 Request a plant.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (initial != null && initial.notes.isNotBlank()) {
                    Text(initial.notes, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "From ${initial.source}. I've filled in my best guess below, so change anything that looks wrong.",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    )
                }
                OutlinedTextField(name, { name = it }, label = { Text("Plant name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = gardenFieldColors())
                Label("Pick an icon")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    emojiChoices.forEach { e -> FilterChip(selected = emoji == e, onClick = { emoji = e }, label = { Text(e) }) }
                }
                Label("Type")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Category.entries.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text("${c.emoji} ${c.label}") })
                    }
                }
                Label("Where does it grow best?")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TunnelFit.entries.forEach { t ->
                        FilterChip(selected = tunnel == t, onClick = { tunnel = t }, label = { Text(t.label) })
                    }
                }
                SwitchRow("Start in trays indoors first", indoors) { indoors = it }
                SwitchRow("Copes with frost", hardy) { hardy = it }
                SwitchRow("Thirsty plant", thirsty) { thirsty = it }
                SwitchRow("Comes back every year", perennial) { perennial = it }
                SwitchRow("Fine in a pot", potOk) { potOk = it }
                SwitchRow("Good for bees and butterflies", pollinator) { pollinator = it }

                val w = weeks.roundToInt()
                Label(
                    when {
                        w < 0 -> "Sow ${-w} weeks before the last frost"
                        w > 0 -> "Sow $w weeks after the last frost"
                        else -> "Sow around the last frost"
                    }
                )
                Slider(weeks, { weeks = it }, valueRange = -12f..12f, steps = 23)
                Label("About ${days.roundToInt()} days until harvest")
                Slider(days, { days = it }, valueRange = 20f..240f)
                Label("Harvest for about ${harvestWeeks.roundToInt()} weeks")
                Slider(harvestWeeks, { harvestWeeks = it }, valueRange = 1f..24f, steps = 22)
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = {
                val base = initial ?: CustomCrop(
                    name = "", emoji = emoji, category = category, tunnel = tunnel, sowIndoors = indoors,
                    weeksFromFrost = 0, daysToHarvest = 70, harvestWeeks = 6, frostHardy = hardy,
                )
                onSave(
                    base.copy(
                        id = 0, name = name.trim(), emoji = emoji, category = category, tunnel = tunnel,
                        sowIndoors = indoors, weeksFromFrost = weeks.roundToInt(), daysToHarvest = days.roundToInt(),
                        harvestWeeks = harvestWeeks.roundToInt(), frostHardy = hardy,
                        water = if (thirsty) Need.HIGH else if (base.water == Need.LOW) Need.LOW else Need.MEDIUM,
                        perennial = perennial, potOk = potOk, pollinator = pollinator,
                    )
                )
                onDismiss()
            }) { Text("Add to seed tin") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked, onChange)
    }
}
