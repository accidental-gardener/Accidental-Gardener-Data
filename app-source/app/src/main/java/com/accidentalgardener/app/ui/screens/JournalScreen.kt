package com.accidentalgardener.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.accidentalgardener.app.data.Area
import com.accidentalgardener.app.data.EntryType
import com.accidentalgardener.app.ui.GardenViewModel
import com.accidentalgardener.app.ui.components.AddEntryDialog
import com.accidentalgardener.app.ui.components.EmojiBubble
import com.accidentalgardener.app.ui.components.EmptyHint
import com.accidentalgardener.app.ui.components.GardenCard
import com.accidentalgardener.app.ui.components.ScreenTitle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DayHeader = DateTimeFormatter.ofPattern("EEEE d MMMM")
private val TimeFmt = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun JournalScreen(vm: GardenViewModel) {
    val journal by vm.journal.collectAsStateWithLifecycle()
    val beds by vm.beds.collectAsStateWithLifecycle()
    val crops by vm.crops.collectAsStateWithLifecycle()
    val plantings by vm.plantings.collectAsStateWithLifecycle()
    val lookup = remember(crops) { crops.associateBy { it.id } }
    val bedNames = remember(beds) { beds.associate { it.id to it.name } }
    val planned = remember(plantings, lookup) { plantings.mapNotNull { lookup[it.cropId] }.distinctBy { it.id } }
    var adding by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()

    val grouped = journal.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp)) {
            item { ScreenTitle("Garden journal", "Watering, feeding, harvests and everything else") }
            if (journal.isEmpty()) {
                item {
                    EmptyHint("📔", "Your journal is empty", "Log watering, feeding, pests or a big harvest. It also helps me give better watering advice.")
                }
            }
            grouped.forEach { (date, entries) ->
                item(key = "h-$date") {
                    Text(
                        when (date) {
                            today -> "Today"
                            today.minusDays(1) -> "Yesterday"
                            else -> date.format(DayHeader)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                    )
                }
                items(entries, key = { it.id }) { e ->
                    val where = e.bedId?.let { bedNames[it] } ?: when (e.area) {
                        Area.OUTDOOR -> "Outdoor beds"
                        Area.TUNNEL -> "Tunnel beds"
                        Area.INDOOR -> "Indoor pots"
                        else -> "Whole garden"
                    }
                    val crop = e.cropId?.let { lookup[it] }
                    GardenCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            EmojiBubble(e.type.emoji, size = 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    e.type.label + (crop?.let { " · ${it.emoji} ${it.name}" } ?: ""),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    "$where · ${Instant.ofEpochMilli(e.timestamp).atZone(zone).format(TimeFmt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (e.amount.isNotBlank()) Text(e.amount, style = MaterialTheme.typography.bodyMedium)
                                if (e.notes.isNotBlank()) Text(e.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { vm.deleteEntry(e) }) {
                                Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { adding = true },
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Add entry") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (adding) {
        AddEntryDialog(EntryType.WATERED, beds, planned, onDismiss = { adding = false }, onSave = { vm.addEntry(it) })
    }
}
