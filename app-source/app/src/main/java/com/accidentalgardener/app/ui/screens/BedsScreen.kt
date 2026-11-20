@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.accidentalgardener.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.accidentalgardener.app.data.Bed
import com.accidentalgardener.app.data.BedKind
import com.accidentalgardener.app.data.BedPlace
import com.accidentalgardener.app.data.Crop
import com.accidentalgardener.app.data.EntryType
import com.accidentalgardener.app.data.Garden
import com.accidentalgardener.app.data.JournalEntry
import com.accidentalgardener.app.data.Planting
import com.accidentalgardener.app.data.Stage
import com.accidentalgardener.app.data.isPot
import com.accidentalgardener.app.ui.Dest
import com.accidentalgardener.app.ui.GardenViewModel
import com.accidentalgardener.app.ui.components.EmojiBubble
import com.accidentalgardener.app.ui.components.EmptyHint
import com.accidentalgardener.app.ui.components.GardenCard
import com.accidentalgardener.app.ui.components.ScreenTitle
import com.accidentalgardener.app.ui.components.ShortDate
import com.accidentalgardener.app.ui.components.Stepper
import com.accidentalgardener.app.ui.components.gardenFieldColors
import com.accidentalgardener.app.ui.components.short
import com.accidentalgardener.app.ui.theme.Bark
import com.accidentalgardener.app.ui.theme.Cream
import com.accidentalgardener.app.ui.theme.Moss
import com.accidentalgardener.app.ui.theme.Soil
import com.accidentalgardener.app.ui.theme.SoilLight
import com.accidentalgardener.app.ui.theme.Terracotta
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

@Composable
fun BedsScreen(vm: GardenViewModel, garden: Garden, onGoTo: (String) -> Unit) {
    val beds by vm.beds.collectAsStateWithLifecycle()
    val plantings by vm.plantings.collectAsStateWithLifecycle()
    val crops by vm.crops.collectAsStateWithLifecycle()
    val journal by vm.journal.collectAsStateWithLifecycle()
    val lookup = remember(crops) { crops.associateBy { it.id } }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var showMap by rememberSaveable { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }

    fun cropsIn(bed: Bed): List<Crop> = plantings.filter { it.bedId == bed.id }.mapNotNull { lookup[it.cropId] }
    val unplaced = plantings.filter { it.bedId == null }

    val groups = listOf(
        "⛺  Polytunnel" to beds.filter { it.place == BedPlace.TUNNEL },
        "☀️  Outdoor beds" to beds.filter { it.place == BedPlace.OUTDOOR && !it.isPot },
        "🪴  Outdoor pots" to beds.filter { it.place == BedPlace.OUTDOOR && it.isPot },
        "🏠  Indoors" to beds.filter { it.place == BedPlace.INDOOR },
    ).filter { it.second.isNotEmpty() }

    val summary = buildList {
        val b = beds.count { !it.isPot }
        val p = beds.count { it.isPot }
        if (b > 0) add("$b bed${if (b == 1) "" else "s"}")
        if (p > 0) add("$p pot${if (p == 1) "" else "s"}")
    }.joinToString(" and ").ifBlank { "No growing spaces yet" }

    @Composable
    fun Header() {
        ScreenTitle("Your growing spaces", "$summary · ${garden.planYear} plan")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            FilterChip(selected = !showMap, onClick = { showMap = false }, label = { Text("☰  List") })
            FilterChip(selected = showMap, onClick = { showMap = true }, label = { Text("🗺️  Garden map") })
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (showMap) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 80.dp)) {
                Header()
                GardenMap(
                    garden = garden, beds = beds, cropsIn = ::cropsIn,
                    onSave = { vm.updateBeds(it) }, onOpen = { selectedId = it.id },
                    onResize = { w, l -> vm.setMapSize(w, l) },
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) { Column { Header() } }
                if (beds.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyHint("🪴", "No beds or pots yet", "Tap Add below to add a bed, a tunnel bed or even a single pot on a balcony.")
                    }
                } else if (plantings.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyHint("🌰", "Nothing planned yet", "Pick your seeds and tap Plan my garden. Already have things growing? Tap a bed and choose Add what's already growing.") {
                            Button(onClick = { onGoTo(Dest.SEEDS.route) }) { Text("Open the Seed tin") }
                        }
                    }
                }
                groups.forEach { (title, list) ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 6.dp))
                    }
                    items(list, key = { it.id }) { b ->
                        when {
                            b.isPot -> PotTile(b, cropsIn(b)) { selectedId = b.id }
                            b.place == BedPlace.TUNNEL -> TunnelTile(b, cropsIn(b)) { selectedId = b.id }
                            else -> RaisedBedTile(b, cropsIn(b)) { selectedId = b.id }
                        }
                    }
                }
                if (unplaced.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        GardenCard(color = MaterialTheme.colorScheme.secondaryContainer) {
                            Column(Modifier.padding(16.dp)) {
                                Text("🤔 Waiting for a spot", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    unplaced.mapNotNull { lookup[it.cropId]?.let { c -> "${c.emoji} ${c.name}" } }.joinToString("   "),
                                    style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 6.dp),
                                )
                                Text(
                                    "There isn't a suitable free space for these. Add a bed or pot, or allow more crops per bed in Settings, then plan again.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { adding = true },
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Add bed or pot") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (adding) {
        AddSpaceDialog(onDismiss = { adding = false }) { kind, place, name, size, count ->
            vm.addBeds(kind, place, name, size, count)
        }
    }

    val selected = beds.firstOrNull { it.id == selectedId }
    if (selected != null) {
        BedSheet(
            bed = selected,
            beds = beds,
            crops = crops,
            plantings = plantings.filter { it.bedId == selected.id },
            lookup = { lookup[it] },
            recent = journal.filter { it.bedId == selected.id }.take(5),
            onDismiss = { selectedId = null },
            onSaveBed = { vm.updateBed(it) },
            onDelete = { vm.deleteBed(selected); selectedId = null },
            onMove = { p, b -> vm.movePlanting(p, b) },
            onRemovePlanting = { vm.removePlanting(it) },
            onSaveSeeds = { p, on -> vm.setSaveSeeds(p, on) },
            onAddExisting = { cropId, stage, date -> vm.addExisting(cropId, selected, stage, date) },
            onLog = { type -> vm.addEntry(JournalEntry(timestamp = System.currentTimeMillis(), type = type, bedId = selected.id)) },
        )
    }
}

// ---------------------------------------------------------------------------
// Tiles
// ---------------------------------------------------------------------------

@Composable
private fun CropEmojis(crops: List<Crop>, modifier: Modifier = Modifier, emptyColor: Color, empty: String = "Empty · ready for planting") {
    if (crops.isEmpty()) {
        Text(empty, color = emptyColor, style = MaterialTheme.typography.labelMedium, modifier = modifier)
    } else {
        FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
            crops.forEach { Text(it.emoji, fontSize = 28.sp) }
        }
    }
}

/** Wooden raised bed full of soil. */
@Composable
private fun RaisedBedTile(bed: Bed, crops: List<Crop>, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
            .background(Brush.verticalGradient(listOf(Color(0xFFA0744C), Bark)))
            .padding(7.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(9.dp))
                .background(Brush.verticalGradient(listOf(SoilLight, Soil)))
                .drawBehind {
                    val rnd = Random(bed.id.toInt())
                    repeat(45) {
                        drawCircle(
                            Color.Black.copy(alpha = 0.13f),
                            radius = rnd.nextFloat() * 4f + 1f,
                            center = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height),
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) { CropEmojis(crops, Modifier.padding(8.dp), Cream.copy(alpha = 0.7f)) }
        BedLabel(bed, crops, Cream)
    }
}

/** Polytunnel bed with an arched cover and ribs. */
@Composable
private fun TunnelTile(bed: Bed, crops: List<Crop>, onClick: () -> Unit) {
    val arch = RoundedCornerShape(topStart = 70.dp, topEnd = 70.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    Column(
        Modifier.clip(arch).clickable(onClick = onClick)
            .background(Brush.verticalGradient(listOf(Color(0xFFF1F6EC), Color(0xFFD3E4C6))))
            .border(2.dp, Color.White, arch)
            .drawBehind {
                for (i in 1..4) {
                    val x = size.width * i / 5f
                    drawLine(Color.White.copy(alpha = 0.8f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 3f)
                }
            }
            .padding(start = 8.dp, end = 8.dp, top = 30.dp, bottom = 8.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(if (bed.isPot) 70.dp else 90.dp).clip(RoundedCornerShape(if (bed.isPot) 40.dp else 9.dp))
                .background(Brush.verticalGradient(if (bed.isPot) listOf(Terracotta, Color(0xFF9E4B2B)) else listOf(SoilLight, Soil))),
            contentAlignment = Alignment.Center,
        ) { CropEmojis(crops, Modifier.padding(8.dp), Cream.copy(alpha = 0.75f), if (bed.isPot) "Empty pot" else "Empty · ready for planting") }
        BedLabel(bed, crops, Moss)
    }
}

/** A terracotta pot. */
@Composable
private fun PotTile(bed: Bed, crops: List<Crop>, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // rim
        Box(
            Modifier.fillMaxWidth(0.95f).height(16.dp).clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFB0552F))
        )
        // body, slightly narrower at the bottom
        Box(
            Modifier.fillMaxWidth(0.82f).height(84.dp)
                .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                .background(Brush.verticalGradient(listOf(Terracotta, Color(0xFF9E4B2B)))),
            contentAlignment = Alignment.Center,
        ) { CropEmojis(crops, Modifier.padding(6.dp), Cream.copy(alpha = 0.85f), "Empty pot") }
        Spacer(Modifier.height(4.dp))
        BedLabel(bed, crops, MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun BedLabel(bed: Bed, crops: List<Crop>, color: Color) {
    Column(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(bed.name, color = color, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!bed.isPot) bed.sizeM2?.let { Text("${trimNum(it)} m²", color = color.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall) }
        }
        Text(
            if (crops.isEmpty()) " " else crops.joinToString { it.name },
            color = color.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun trimNum(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else "%.1f".format(d)
private fun trimNum(f: Float): String = trimNum(f.toDouble())

// ---------------------------------------------------------------------------
// Adding a bed or pot
// ---------------------------------------------------------------------------

@Composable
private fun AddSpaceDialog(onDismiss: () -> Unit, onAdd: (BedKind, BedPlace, String, Double?, Int) -> Unit) {
    var kind by remember { mutableStateOf(BedKind.BED) }
    var place by remember { mutableStateOf(BedPlace.OUTDOOR) }
    var name by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var count by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a growing space") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("What is it?", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = kind == BedKind.BED, onClick = { kind = BedKind.BED }, label = { Text("🟫 Bed or border") })
                    FilterChip(selected = kind == BedKind.POT, onClick = { kind = BedKind.POT }, label = { Text("🪴 Pot or container") })
                }
                Spacer(Modifier.height(10.dp))
                Text("Where is it?", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BedPlace.entries.forEach { p ->
                        FilterChip(selected = place == p, onClick = { place = p }, label = { Text("${p.emoji} ${p.label}") })
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    name, { name = it }, label = { Text("Name (optional)") }, singleLine = true,
                    placeholder = { Text(if (kind == BedKind.POT) "e.g. Balcony pot" else "e.g. Bed by the shed") },
                    modifier = Modifier.fillMaxWidth(), colors = gardenFieldColors(),
                )
                if (kind == BedKind.BED) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        size, { size = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Size in m² (optional)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(), colors = gardenFieldColors(),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Stepper("How many?", null, count, 1..20, { count = it }, if (kind == BedKind.POT) "🪴" else "🟫")
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(kind, place, name, size.toDoubleOrNull(), count); onDismiss() }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---------------------------------------------------------------------------
// Bed details
// ---------------------------------------------------------------------------

@Composable
private fun BedSheet(
    bed: Bed,
    beds: List<Bed>,
    crops: List<Crop>,
    plantings: List<Planting>,
    lookup: (String) -> Crop?,
    recent: List<JournalEntry>,
    onDismiss: () -> Unit,
    onSaveBed: (Bed) -> Unit,
    onDelete: () -> Unit,
    onMove: (Planting, Bed?) -> Unit,
    onRemovePlanting: (Planting) -> Unit,
    onSaveSeeds: (Planting, Boolean) -> Unit,
    onAddExisting: (String, Stage, LocalDate) -> Unit,
    onLog: (EntryType) -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(bed.id) { mutableStateOf(bed.name) }
    var place by remember(bed.id) { mutableStateOf(bed.place) }
    var widthM by remember(bed.id) { mutableStateOf(trimNum(bed.mapW)) }
    var lengthM by remember(bed.id) { mutableStateOf(trimNum(bed.mapH)) }
    var moving by remember { mutableStateOf<Planting?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var addingExisting by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBubble(if (bed.isPot) "🪴" else bed.place.emoji)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(bed.name, style = MaterialTheme.typography.headlineSmall)
                    Text("${bed.kind.label} · ${bed.place.label}", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Quick log", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(EntryType.WATERED, EntryType.FED, EntryType.WEEDED, EntryType.MULCHED, EntryType.COVERED).forEach { t ->
                    AssistChip(onClick = { onLog(t) }, label = { Text("${t.emoji} ${t.label}") })
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Growing here", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                TextButton(onClick = { addingExisting = true }) { Text("➕ Add what's already growing") }
            }
            if (plantings.isEmpty()) {
                Text("Nothing planned here yet.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
            }
            plantings.mapNotNull { p -> lookup(p.cropId)?.let { p to it } }.forEach { (p, c) ->
                GardenCard(Modifier.fillMaxWidth().padding(vertical = 5.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.emoji, fontSize = 30.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(c.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { moving = if (moving == p) null else p }) { Text("Move") }
                            IconButton(onClick = { onRemovePlanting(p) }) {
                                Icon(Icons.Outlined.Delete, "Remove", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Text(
                            buildString {
                                append(if (p.sowIndoors) "Sow indoors ${p.sowDate.short()}" else if (c.planted || c.fixedSow != null) "Plant ${p.sowDate.short()}" else "Sow ${p.sowDate.short()}")
                                p.plantOutDate?.let { append(" · Plant out ${it.short()}") }
                                append(" · Harvest ${p.harvestStart.short()} to ${p.harvestEnd.short()}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text("💡 ${p.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        c.howTo.seedSaving?.let { ss ->
                            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (ss.fromSeed) "🌰 Save seeds from this" else "🌱 Grow more plants from this",
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    Text(
                                        if (p.saveSeeds) (if (p.seedsSaved) "Done for this year." else "I'll show you how when the time comes.")
                                        else "${ss.difficulty}${if (ss.timing == "NEXT_YEAR") " · seeds come the year after" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(checked = p.saveSeeds, onCheckedChange = { onSaveSeeds(p, it) })
                            }
                        }
                        val inGround = p.stage == Stage.PLANTED_OUT || p.stage == Stage.HARVESTING ||
                            (p.stage == Stage.SOWN && p.plantOutDate == null)
                        if (inGround && c.howTo.care.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Looking after it", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            c.howTo.care.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp)) }
                        }
                        if (moving == p) {
                            Spacer(Modifier.height(6.dp))
                            Text("Move to", style = MaterialTheme.typography.labelLarge)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                beds.filter { it.id != bed.id }.forEach { other ->
                                    FilterChip(selected = false, onClick = { onMove(p, other); moving = null }, label = { Text(other.name) })
                                }
                            }
                        }
                    }
                }
            }

            if (recent.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Recent activity", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                recent.forEach { e ->
                    val d = Instant.ofEpochMilli(e.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                    Text("${e.type.emoji} ${e.type.label} · ${d.format(ShortDate)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("Details", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = gardenFieldColors())
            Spacer(Modifier.height(8.dp))
            Text("Where is it?", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BedPlace.entries.forEach { p ->
                    FilterChip(selected = place == p, onClick = { place = p }, label = { Text("${p.emoji} ${p.label}") })
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    widthM, { widthM = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(if (bed.isPot) "Width (m)" else "Width (m)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f), colors = gardenFieldColors(),
                )
                OutlinedTextField(
                    lengthM, { lengthM = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Length (m)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f), colors = gardenFieldColors(),
                )
            }
            Text(
                "Roughly is fine. It sets how big it looks on the garden map, and bigger beds fit more crops.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = {
                    val w = widthM.toFloatOrNull()?.coerceIn(0.2f, 50f) ?: bed.mapW
                    val l = lengthM.toFloatOrNull()?.coerceIn(0.2f, 50f) ?: bed.mapH
                    onSaveBed(
                        bed.copy(
                            name = name.ifBlank { bed.name }, place = place, isTunnel = place == BedPlace.TUNNEL,
                            mapW = w, mapH = l, sizeM2 = if (bed.isPot) null else (w * l).toDouble(),
                        )
                    )
                }) { Text("Save") }
                OutlinedButton(onClick = {
                    onSaveBed(bed.copy(mapW = bed.mapH, mapH = bed.mapW))
                    widthM = trimNum(bed.mapH); lengthM = trimNum(bed.mapW)
                }) { Text("↻ Turn on map") }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${bed.name}?") },
            text = { Text("Anything planned here will move to Waiting for a spot so you can replan it. Your journal stays as it is.") },
            confirmButton = {
                Button(
                    onClick = { confirmDelete = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep it") } },
        )
    }

    if (addingExisting) {
        ExistingPlantDialog(crops, bed.isPot, onDismiss = { addingExisting = false }) { id, stage, date ->
            onAddExisting(id, stage, date)
        }
    }
}

// ---------------------------------------------------------------------------
// What's already there
// ---------------------------------------------------------------------------

@Composable
private fun ExistingPlantDialog(
    crops: List<Crop>,
    isPot: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Stage, LocalDate) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<Crop?>(null) }
    var stage by remember { mutableStateOf(Stage.PLANTED_OUT) }
    var ago by remember { mutableIntStateOf(30) }
    val matches = crops.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
        .filter { !isPot || it.potOk }.sortedBy { it.name }.take(30)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What's already growing?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    query, { query = it }, label = { Text("Search plants") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), colors = gardenFieldColors(),
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    matches.forEach { c ->
                        FilterChip(selected = picked?.id == c.id, onClick = { picked = c }, label = { Text("${c.emoji} ${c.name}") })
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("How far along is it?", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = stage == Stage.SOWN, onClick = { stage = Stage.SOWN }, label = { Text("🌱 Seedlings, not planted out") })
                    FilterChip(selected = stage == Stage.PLANTED_OUT, onClick = { stage = Stage.PLANTED_OUT }, label = { Text("🪴 Growing in place") })
                    FilterChip(selected = stage == Stage.HARVESTING, onClick = { stage = Stage.HARVESTING }, label = { Text("🧺 Already picking") })
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    when (stage) {
                        Stage.SOWN -> "When did you sow it?"
                        Stage.HARVESTING -> "When did you start picking?"
                        else -> "When did it go in?"
                    },
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "This week", 14 to "Two weeks ago", 30 to "About a month ago", 75 to "Two or three months ago", 150 to "Earlier this year")
                        .forEach { (d, label) -> FilterChip(selected = ago == d, onClick = { ago = d }, label = { Text(label) }) }
                }
            }
        },
        confirmButton = {
            Button(enabled = picked != null, onClick = {
                picked?.let { onSave(it.id, stage, LocalDate.now().minusDays(ago.toLong())) }
                onDismiss()
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
