@file:OptIn(ExperimentalLayoutApi::class)

package com.accidentalgardener.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import android.content.Intent
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.accidentalgardener.app.data.Category
import com.accidentalgardener.app.data.Crop
import com.accidentalgardener.app.data.CustomCrop
import com.accidentalgardener.app.data.Community
import com.accidentalgardener.app.ui.components.RequestDialog
import com.accidentalgardener.app.ui.components.MyRequestsDialog
import com.accidentalgardener.app.ui.components.GardenCard
import com.accidentalgardener.app.data.GuideSection
import androidx.compose.ui.platform.LocalUriHandler
import java.time.LocalDate
import com.accidentalgardener.app.data.PlantLookup
import com.accidentalgardener.app.data.cropId
import com.accidentalgardener.app.data.Garden
import com.accidentalgardener.app.data.Need
import com.accidentalgardener.app.data.TunnelFit
import com.accidentalgardener.app.domain.Planner
import com.accidentalgardener.app.domain.Schedule
import com.accidentalgardener.app.ui.GardenViewModel
import com.accidentalgardener.app.ui.components.AddCropDialog
import com.accidentalgardener.app.ui.components.EmptyHint
import com.accidentalgardener.app.ui.components.LookupDialog
import com.accidentalgardener.app.ui.components.EmojiBubble
import com.accidentalgardener.app.ui.components.Pill
import com.accidentalgardener.app.ui.components.ScreenTitle
import com.accidentalgardener.app.ui.components.short
import com.accidentalgardener.app.ui.theme.Butter
import com.accidentalgardener.app.ui.theme.Clay
import com.accidentalgardener.app.ui.theme.SkyLight
import com.accidentalgardener.app.ui.theme.Sprout
import androidx.compose.ui.graphics.Color

@Composable
fun SeedsScreen(vm: GardenViewModel, garden: Garden, onPlanned: () -> Unit) {
    val crops by vm.crops.collectAsStateWithLifecycle()
    val seeds by vm.seeds.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<Category?>(null) }
    var onlyChosen by rememberSaveable { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var lookingUp by remember { mutableStateOf(false) }
    var requesting by remember { mutableStateOf<Pair<Community.Kind, String>?>(null) }
    var showMine by remember { mutableStateOf(false) }
    var onlyPollinators by rememberSaveable { mutableStateOf(false) }
    var onlyPots by rememberSaveable { mutableStateOf(garden.spaceType == "BALCONY") }
    val customs by vm.customCrops.collectAsStateWithLifecycle()
    val newIds by vm.newPlantIds.collectAsStateWithLifecycle()
    val guideStatus by vm.guideStatus.collectAsStateWithLifecycle()
    val guideBusy by vm.guideBusy.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current

    val filtered = crops
        .filter { category == null || it.category == category }
        .filter { !onlyChosen || it.id in seeds }
        .filter { !onlyPollinators || it.pollinator }
        .filter { !onlyPots || it.potOk }
        .filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
        .sortedWith(compareBy<Crop>({ it.id !in newIds }, { it.name }))

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp)) {
            item {
                ScreenTitle("Seed tin", "Pick everything you'd like to grow in ${garden.planYear}. ${seeds.size} chosen so far.")
            }
            item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("Search seeds, fruit and flowers") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                guideStatus?.let { msg ->
                    GardenCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (msg.startsWith("New")) "🆕 $msg" else msg, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { vm.clearGuideStatus() }) { Text("OK") }
                        }
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = category == null && !onlyChosen, onClick = { category = null; onlyChosen = false }, label = { Text("All") }) }
                    item { FilterChip(selected = onlyChosen, onClick = { onlyChosen = !onlyChosen }, label = { Text("✅ My picks") }) }
                    items(Category.entries) { c ->
                        FilterChip(selected = category == c, onClick = { category = if (category == c) null else c }, label = { Text("${c.emoji} ${c.label}") })
                    }
                    item { FilterChip(selected = onlyPollinators, onClick = { onlyPollinators = !onlyPollinators }, label = { Text("🐝 Pollinators") }) }
                    item { FilterChip(selected = onlyPots, onClick = { onlyPots = !onlyPots }, label = { Text("🪴 Good in pots") }) }
                    item { FilterChip(selected = false, onClick = { lookingUp = true }, label = { Text("🔎 Look up online") }) }
                    item { FilterChip(selected = false, onClick = { showAdd = true }, label = { Text("➕ Add your own") }) }
                    item { FilterChip(selected = false, onClick = { requesting = Community.Kind.NEW_PLANT to query.trim() }, label = { Text("📮 Request a plant") }) }
                    item { FilterChip(selected = false, onClick = { showMine = true }, label = { Text("📋 My requests") }) }
                    item {
                        FilterChip(
                            selected = false, enabled = !guideBusy, onClick = { vm.checkForNewPlants() },
                            label = { Text(if (guideBusy) "Checking…" else "🔄 Check for new plants") },
                        )
                    }
                    item { FilterChip(selected = false, onClick = { uri.openUri(Community.WIKI_URL) }, label = { Text("📖 Full guides (wiki)") }) }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (filtered.isEmpty() && query.isNotBlank()) {
                item {
                    EmptyHint("🔎", "Not in the database yet", "Look \"${query.trim()}\" up online and I'll add it to your plants.") {
                        Button(onClick = { lookingUp = true }) { Text("Look it up online") }
                        TextButton(onClick = { requesting = Community.Kind.NEW_PLANT to query.trim() }) { Text("📮 Ask for it to be added to the database") }
                    }
                }
            }
            items(filtered, key = { it.id }) { c ->
                SeedRow(
                    c, garden, selected = c.id in seeds, custom = customs.firstOrNull { it.cropId == c.id },
                    isNew = c.id in newIds,
                    onOpenWiki = { uri.openUri(Community.WIKI_URL + "#plant=" + c.id) },
                    onSuggestFix = { requesting = Community.Kind.FIX to c.name },
                ) { vm.toggleSeed(c.id) }
            }
        }

        if (seeds.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = { vm.runPlanner(); onPlanned() },
                icon = { Icon(Icons.Filled.AutoAwesome, null) },
                text = { Text("Plan my garden (${seeds.size})") },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            )
        }
    }

    if (showAdd) AddCropDialog(onDismiss = { showAdd = false }, onSave = { vm.addCustomCrop(it) })
    if (lookingUp) LookupDialog(vm, query) { lookingUp = false }
    if (showMine) MyRequestsDialog(garden.userName) { showMine = false }
    requesting?.let { (kind, plant) ->
        RequestDialog(startKind = kind, startPlant = plant, userName = garden.userName) { requesting = null }
    }
}

@Composable
private fun SeedRow(
    c: Crop, garden: Garden, selected: Boolean, custom: CustomCrop?, isNew: Boolean,
    onOpenWiki: () -> Unit, onSuggestFix: () -> Unit, onToggle: () -> Unit,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.clickable { expanded = !expanded }.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBubble(c.emoji, size = 52.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (custom != null) Pill("📱 Only on my phone", MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.onSurface)
                        if (isNew) Pill("🆕 New", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                        when (c.tunnel) {
                            TunnelFit.NEEDS -> Pill("⛺ Tunnel", SkyLight, Color(0xFF0E3242))
                            TunnelFit.LIKES -> Pill("⛺ Likes cover", SkyLight, Color(0xFF0E3242))
                            TunnelFit.OUTDOOR -> Pill("☀️ Outdoors", Butter, Color(0xFF4A3A0C))
                            TunnelFit.EITHER -> {}
                        }
                        if (c.frostHardy) Pill("❄️ Hardy", Sprout.copy(alpha = 0.6f), Color(0xFF1C3010))
                        if (c.water == Need.HIGH) Pill("💧 Thirsty", SkyLight, Color(0xFF0E3242))
                        if (c.perennial) Pill("♻️ Perennial", Clay, Color(0xFF4A1E0C))
                        if (c.pollinator) Pill("🐝 Bees love it", Butter, Color(0xFF4A3A0C))
                        if (c.potOk) Pill("🪴 Pots", MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.onSurface)
                    }
                }
                IconButton(onClick = onToggle) {
                    if (selected) Icon(Icons.Filled.CheckCircle, "Remove", tint = MaterialTheme.colorScheme.primary)
                    else Icon(Icons.Filled.AddCircleOutline, "Add", tint = MaterialTheme.colorScheme.outline)
                }
            }
            AnimatedVisibility(expanded) {
                val inTunnel = c.tunnel == TunnelFit.NEEDS
                val s = Planner.schedule(c, garden.planYear, garden.lastFrostDoy, inTunnel)
                Column(Modifier.padding(top = 10.dp)) {
                    Text(
                        buildString {
                            append(if (s.sowIndoors) "🌱 Sow indoors from ${s.sow.short()}" else if (c.planted || c.fixedSow != null) "🪴 Plant from ${s.sow.short()}" else "🌱 Sow from ${s.sow.short()}")
                            s.plantOut?.let { append("\n🪴 Plant out around ${it.short()}") }
                            append("\n🧺 Harvest ${s.harvestStart.short()} to ${s.harvestEnd.short()}")
                            if (inTunnel) append(" (in the tunnel)")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    RightNow(c, s, garden)
                    Text(c.family.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
                    if (c.tip.isNotBlank()) Text("💡 ${c.tip}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    if (c.flowers.isNotBlank()) Text("🌼 Flowers ${c.flowers}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    if (c.potOk && c.potSize.isNotBlank()) Text("🪴 In a pot, use a ${c.potSize}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                    if (custom == null) {
                        FlowRow {
                            TextButton(onClick = onOpenWiki) { Text("📖 Full guide on the wiki") }
                            TextButton(onClick = onSuggestFix) { Text("✏️ Suggest a fix") }
                        }
                    }
                    if (custom != null) {
                        TextButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "New plant for the Accidental Gardener database: ${custom.name}")
                                putExtra(Intent.EXTRA_TEXT, PlantLookup.toJson(custom))
                            }
                            context.startActivity(Intent.createChooser(send, "Share plant"))
                        }) { Text("📤 Share the details") }
                    }
                }
            }
        }
    }
}


/**
 * Only the part of the guide that matters today. The full guide (every stage, seed saving and so on)
 * lives on the wiki, and the step-by-step jobs pop up on the Home screen when they're due.
 */
@Composable
private fun RightNow(c: Crop, s: Schedule, garden: Garden) {
    val today = LocalDate.now()
    val g = c.howTo
    val prepStart = g.prep?.let { s.sow.minusDays(it.leadDays.toLong()) }
    val firstJob = prepStart ?: s.sow
    val sowEnd = s.sow.plusWeeks(6)
    val verb = if (c.planted || c.fixedSow != null) "Plant" else "Sow"
    val seeds = g.seedSaving?.takeIf { it.fromSeed && it.timing != "NONE" }

    val (headline, section) = when {
        today.isBefore(firstJob.minusWeeks(3)) ->
            "Nothing to do yet. ${prepStart?.let { "Start getting it ready around ${it.short()}" } ?: "$verb from ${s.sow.short()}"}." to null
        prepStart != null && today.isBefore(s.sow) ->
            "Time to get it ready, $verb from ${s.sow.short()}." to g.prep!!.section
        today.isBefore(s.sow) ->
            "Coming up soon. $verb from ${s.sow.short()}." to g.sow
        !today.isAfter(sowEnd) ->
            "Good time to ${verb.lowercase()} now." to g.sow
        s.plantOut != null && today.isBefore(s.plantOut.plusWeeks(3)) ->
            (if (today.isBefore(s.plantOut.minusDays(10))) "Getting close to planting out on ${s.plantOut.short()}." else "Time to plant out.") to
                (if (today.isBefore(s.plantOut.minusDays(10))) g.harden else g.plantOut)
        today.isBefore(s.harvestStart) ->
            "Growing on. Harvest from ${s.harvestStart.short()}." to GuideSection("Looking after it", steps = g.care)
        !today.isAfter(s.harvestEnd) ->
            "Harvest time." to GuideSection("How to harvest", steps = g.harvest)
        seeds != null && !today.isAfter(s.harvestEnd.plusWeeks(6)) ->
            "Seed saving time, if you'd like to." to seeds.section
        else ->
            "This year's ${verb.lowercase()}ing time has passed. Add it to your plan for next year." to null
    }

    GardenCard(Modifier.fillMaxWidth().padding(top = 8.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(12.dp)) {
            Text("📍 Right now", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(headline, style = MaterialTheme.typography.bodyMedium)
            section?.let { sec ->
                if (sec.facts.isNotEmpty()) Text(
                    sec.facts.joinToString("  ·  ") { "${it.first} ${it.second}" },
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp),
                )
                sec.steps.take(4).forEachIndexed { i, step ->
                    Text("${i + 1}. $step", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 3.dp))
                }
                if (sec.steps.size > 4) Text(
                    "and ${sec.steps.size - 4} more steps on the wiki.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
