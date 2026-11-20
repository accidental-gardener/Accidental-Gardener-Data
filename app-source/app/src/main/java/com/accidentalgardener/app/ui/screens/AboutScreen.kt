@file:OptIn(ExperimentalLayoutApi::class)

package com.accidentalgardener.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.accidentalgardener.app.data.Category
import com.accidentalgardener.app.data.Community
import com.accidentalgardener.app.data.AdminApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.accidentalgardener.app.ui.components.RequestDialog
import com.accidentalgardener.app.ui.components.MyRequestsList
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalUriHandler
import com.accidentalgardener.app.data.Crop
import com.accidentalgardener.app.data.Garden
import com.accidentalgardener.app.data.Season
import com.accidentalgardener.app.domain.Planner
import com.accidentalgardener.app.ui.GardenViewModel
import com.accidentalgardener.app.ui.components.EmojiBubble
import com.accidentalgardener.app.ui.components.GardenerAvatar
import com.accidentalgardener.app.ui.components.AvatarEditorDialog
import com.accidentalgardener.app.ui.components.GardenCard
import com.accidentalgardener.app.ui.components.Pill
import com.accidentalgardener.app.ui.components.ScreenTitle
import com.accidentalgardener.app.ui.components.gardenFieldColors
import com.accidentalgardener.app.ui.components.short
import com.accidentalgardener.app.ui.theme.Butter
import com.accidentalgardener.app.ui.theme.Sprout
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AboutScreen(vm: GardenViewModel, garden: Garden, onOpenSettings: () -> Unit, onOpenAdmin: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val crops by vm.crops.collectAsStateWithLifecycle()
    val seeds by vm.seeds.collectAsStateWithLifecycle()
    val catalog by vm.catalog.collectAsStateWithLifecycle()

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                item { FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("ℹ️ About") }) }
                item { FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("🌷 Bulb planting") }) }
                item { FilterChip(selected = tab == 2, onClick = { tab = 2 }, label = { Text("🐝 Pollinators") }) }
            }
        }
        when (tab) {
            0 -> item { AboutTab(vm, garden, catalog.plants.size, catalog.version, onOpenSettings, onOpenAdmin) }
            1 -> {
                val bulbs = crops.filter { it.category == Category.BULB }
                    .map { it to Planner.schedule(it, garden.planYear, garden.lastFrostDoy, false).sow }
                    .sortedWith(compareBy<Pair<Crop, java.time.LocalDate>>({ (it.second.monthValue + 4) % 12 }, { it.second.dayOfMonth })) // start in August
                item {
                    ScreenTitle("Bulb planting", "When to plant each bulb for flowers through the year")
                    GardenCard(Modifier.fillMaxWidth(), color = Butter) {
                        Text(
                            "Plant spring bulbs (daffodils, crocus, tulips) in autumn and summer bulbs (lilies, dahlias, gladioli) in spring. " +
                                "A good rule is to plant at two to three times the height of the bulb, pointy end up.",
                            Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.ui.graphics.Color(0xFF3A372D),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                var lastMonth = -1
                bulbs.forEach { (c, d) ->
                    if (d.monthValue != lastMonth) {
                        lastMonth = d.monthValue
                        val m = d.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        item(key = "m$m") {
                            Text("Plant in $m", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        }
                    }
                    item(key = c.id) {
                        GuideRow(c, "Plant around ${d.short()}", c.id in seeds) { vm.toggleSeed(c.id) }
                    }
                }
            }
            else -> {
                item {
                    ScreenTitle("Planting for pollinators", "Bees, butterflies and hoverflies need food from February to October")
                    GardenCard(Modifier.fillMaxWidth(), color = Sprout.copy(alpha = 0.45f)) {
                        Column(Modifier.padding(14.dp)) {
                            listOf(
                                "🗓️ Pick at least one plant from each season below so there's always something in flower.",
                                "🌼 Choose single flowers. Double, frilly flowers often have no pollen or nectar.",
                                "🌿 Let some herbs like chives, thyme and oregano flower. Bees love them.",
                                "🌾 Leave a patch of dandelions and clover in the lawn and mow it less often.",
                                "🚫 Avoid pesticides and weedkillers.",
                            ).forEach {
                                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp),
                                    color = androidx.compose.ui.graphics.Color(0xFF2B2A22))
                            }
                            Text(
                                "These tips follow the approach of the All-Ireland Pollinator Plan.",
                                style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp),
                                color = androidx.compose.ui.graphics.Color(0xFF3A372D),
                            )
                        }
                    }
                }
                Season.entries.forEach { season ->
                    val list = crops.filter { it.pollinator && season in it.bloom }.sortedBy { it.name }
                    if (list.isNotEmpty()) {
                        item(key = "s${season.name}") {
                            Text(
                                "${season.emoji} Flowering in ${season.label.lowercase()}",
                                style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                            )
                        }
                        items(list, key = { "${season.name}-${it.id}" }) { c ->
                            val s = Planner.schedule(c, garden.planYear, garden.lastFrostDoy, false)
                            val verb = if (c.planted || c.fixedSow != null) "Plant" else if (s.sowIndoors) "Sow indoors" else "Sow"
                            GuideRow(c, "$verb around ${s.sow.short()}", c.id in seeds) { vm.toggleSeed(c.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideRow(c: Crop, whenText: String, chosen: Boolean, onToggle: () -> Unit) {
    GardenCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            EmojiBubble(c.emoji, size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.titleMedium)
                Text(whenText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                if (c.flowers.isNotBlank()) Text("Flowers ${c.flowers}", style = MaterialTheme.typography.bodySmall)
                c.howTo.sow.facts.firstOrNull { it.first == "Depth" }?.let {
                    Text("Depth ${it.second}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (c.potOk) Row(Modifier.padding(top = 4.dp)) { Pill("🪴 Good in pots", MaterialTheme.colorScheme.surfaceContainer) }
            }
            IconButton(onClick = onToggle) {
                if (chosen) Icon(Icons.Filled.CheckCircle, "In your seed tin", tint = MaterialTheme.colorScheme.primary)
                else Icon(Icons.Filled.AddCircleOutline, "Add to seed tin", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

private const val ADMIN_TAPS = 20

@Composable
private fun AboutTab(vm: GardenViewModel, garden: Garden, plantCount: Int, version: Int, onOpenSettings: () -> Unit, onOpenAdmin: () -> Unit) {
    var editingName by remember { mutableStateOf(false) }
    var editingAvatar by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val adminPrefs = remember { ctx.getSharedPreferences("admin", android.content.Context.MODE_PRIVATE) }
    // The admin area is hidden. Tap the seedling or the version 20 times to show it.
    var adminVisible by remember {
        mutableStateOf(AdminApi.login(ctx) != null || AdminApi.pending(ctx) != null || adminPrefs.getBoolean("unlocked", false))
    }
    var taps by remember { mutableStateOf(0) }
    var tapHint by remember { mutableStateOf<String?>(null) }
    val tap: () -> Unit = {
        if (!adminVisible) {
            taps++
            val left = ADMIN_TAPS - taps
            when {
                left <= 0 -> { adminVisible = true; adminPrefs.edit().putBoolean("unlocked", true).apply(); tapHint = "🔐 Admin area unlocked" }
                left <= 5 -> tapHint = "$left more…"
            }
        }
    }
    val tapSource = remember { MutableInteractionSource() }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.clickable(interactionSource = tapSource, indication = null) { tap() },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🌱", fontSize = 64.sp)
            Text("The Accidental Gardener", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Text("Version 1.2", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            tapHint?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Built by an accidental gardener who got tired of overcomplicated gardening apps, this is a straightforward, " +
                "no-nonsense companion for tracking your seeds, harvests and beds. No fuss, no clutter, just good growing.",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        GardenCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                val avatar by vm.avatar.collectAsStateWithLifecycle()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GardenerAvatar(avatar, size = 64.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("You", style = MaterialTheme.typography.titleMedium)
                        Text("Gardening as ${garden.userName} in ${garden.town}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = { editingName = true }) { Text("Change my name") }
                    OutlinedButton(onClick = { editingAvatar = true }) { Text("Change my gardener") }
                }
                TextButton(onClick = onOpenSettings) { Text("All settings") }
            }
        }
        Spacer(Modifier.height(10.dp))
        GardenCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("📚 Plant database", style = MaterialTheme.typography.titleMedium)
                Text("$plantCount plants with growing guides, version $version.", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "The database can update itself from the internet. Plants that aren't in it can be looked up online from the Seed tin.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val uri = LocalUriHandler.current
                var asking by remember { mutableStateOf(false) }
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = { asking = true }) { Text("📮 Request a plant") }
                    OutlinedButton(onClick = { uri.openUri(Community.WIKI_URL) }) { Text("📖 Open the plant wiki") }
                }
                if (asking) RequestDialog(userName = garden.userName) { asking = false }
            }
        }
        Spacer(Modifier.height(10.dp))
        GardenCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) { MyRequestsList(garden.userName) }
        }
        if (adminVisible) {
            Spacer(Modifier.height(10.dp))
            GardenCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val admin = remember { AdminApi.login(ctx) }
                    Text("🔐 Admins", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (admin != null) "Signed in as $admin." else "For the people who look after the plant database.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(onClick = onOpenAdmin) { Text(if (admin != null) "Open the admin area" else "Admin sign in") }
                        if (admin == null) TextButton(onClick = {
                            adminVisible = false; taps = 0; tapHint = null
                            adminPrefs.edit().putBoolean("unlocked", false).apply()
                        }) { Text("Hide") }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        GardenCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("🙏 Thanks to", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Weather forecasts and warnings copyright Met Éireann, source www.met.ie, used under their open data licence (CC BY 4.0). Met Éireann accepts no liability for errors or omissions in the data. " +
                        "Plant descriptions for looked-up plants from Wikipedia.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Planting dates and advice are a guide. Always check the soil, the sky and the seed packet.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (editingAvatar) {
        val current by vm.avatar.collectAsStateWithLifecycle()
        AvatarEditorDialog(current, onDismiss = { editingAvatar = false }) { vm.saveAvatar(it) }
    }

    if (editingName) {
        var name by remember { mutableStateOf(garden.userName) }
        AlertDialog(
            onDismissRequest = { editingName = false },
            title = { Text("Change your name") },
            text = {
                OutlinedTextField(name, { name = it }, label = { Text("Your name") }, singleLine = true, colors = gardenFieldColors())
            },
            confirmButton = { Button(onClick = { vm.updateName(name); editingName = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editingName = false }) { Text("Cancel") } },
        )
    }
}
