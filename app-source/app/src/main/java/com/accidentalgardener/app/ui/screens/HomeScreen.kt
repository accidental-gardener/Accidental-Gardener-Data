package com.accidentalgardener.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.accidentalgardener.app.data.Area
import com.accidentalgardener.app.data.EntryType
import com.accidentalgardener.app.data.Garden
import com.accidentalgardener.app.data.JournalEntry
import com.accidentalgardener.app.domain.GardenAlert
import com.accidentalgardener.app.domain.GardenAlerts
import com.accidentalgardener.app.domain.GardenTask
import com.accidentalgardener.app.domain.SeasonalTips
import com.accidentalgardener.app.domain.Severity
import com.accidentalgardener.app.domain.Tasks
import com.accidentalgardener.app.domain.WaterAdvice
import com.accidentalgardener.app.domain.WaterAdvisor
import com.accidentalgardener.app.domain.r
import com.accidentalgardener.app.ui.Dest
import com.accidentalgardener.app.ui.GardenViewModel
import com.accidentalgardener.app.ui.WeatherUi
import com.accidentalgardener.app.ui.components.AddEntryDialog
import com.accidentalgardener.app.ui.components.EmojiBubble
import com.accidentalgardener.app.ui.components.EmptyHint
import com.accidentalgardener.app.ui.components.GardenCard
import com.accidentalgardener.app.ui.components.GardenerAvatar
import com.accidentalgardener.app.ui.components.AvatarEditorDialog
import com.accidentalgardener.app.ui.components.AvatarStyle
import com.accidentalgardener.app.ui.components.JobSheet
import com.accidentalgardener.app.ui.components.Pill
import com.accidentalgardener.app.ui.components.SectionHeader
import com.accidentalgardener.app.ui.components.ShortDate
import com.accidentalgardener.app.ui.theme.Butter
import com.accidentalgardener.app.ui.theme.Clay
import com.accidentalgardener.app.ui.theme.Leaf
import com.accidentalgardener.app.ui.theme.Moss
import com.accidentalgardener.app.ui.theme.SkyLight
import com.accidentalgardener.app.ui.theme.Sprout
import com.accidentalgardener.app.weather.WeatherCodes
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun HomeScreen(vm: GardenViewModel, garden: Garden, onOpenSettings: () -> Unit, onGoTo: (String) -> Unit) {
    val weather by vm.weather.collectAsStateWithLifecycle()
    val beds by vm.beds.collectAsStateWithLifecycle()
    val plantings by vm.plantings.collectAsStateWithLifecycle()
    val journal by vm.journal.collectAsStateWithLifecycle()
    val crops by vm.crops.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    val lookup = remember(crops) { crops.associateBy { it.id } }
    val bedNames = remember(beds) { beds.associate { it.id to it.name } }
    val tasks = remember(plantings, lookup) { Tasks.all(plantings) { lookup[it] } }
    val due = tasks.filter { !it.date.isAfter(today.plusDays(14)) }
    val upcoming = tasks.filter { it.date.isAfter(today.plusDays(14)) }.take(3)
    val alerts = remember(weather.snapshot, beds) {
        weather.snapshot?.let { GardenAlerts.evaluate(it, beds.any { b -> b.isTunnel }, today) } ?: emptyList()
    }
    val water = remember(weather.snapshot, beds, plantings, journal) {
        WaterAdvisor.advise(weather.snapshot, beds, plantings, journal, { lookup[it] }, today)
    }
    val plannedCrops = remember(plantings, lookup) { plantings.mapNotNull { lookup[it.cropId] }.distinctBy { it.id } }
    var logType by remember { mutableStateOf<EntryType?>(null) }
    var openTask by remember { mutableStateOf<GardenTask?>(null) }
    var editingName by remember { mutableStateOf(false) }
    var editingAvatar by remember { mutableStateOf(false) }
    val avatar by vm.avatar.collectAsStateWithLifecycle()

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { HomeHeader(garden, weather, avatar, onOpenSettings, vm::refreshWeather, { editingAvatar = true }) { editingName = true } }

        if (alerts.isNotEmpty()) {
            item { SectionHeader("Heads up", "⚠️") }
            items(alerts, key = { it.key }) { AlertCard(it) }
        }

        item { SectionHeader("Watering", "💧") }
        items(water, key = { it.area }) { w ->
            WaterCard(w) {
                vm.addEntry(
                    JournalEntry(
                        timestamp = System.currentTimeMillis(), type = EntryType.WATERED,
                        area = w.area, notes = w.title,
                    )
                )
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GardenerAvatar(avatar, size = 30.dp)
                Spacer(Modifier.width(8.dp))
                Text("Jobs for the next fortnight", style = MaterialTheme.typography.titleLarge)
            }
        }
        when {
            plantings.isEmpty() -> item {
                EmptyHint(
                    "🌰", "Nothing planted yet",
                    "Open the Seed tin, choose what you'd like to grow this year and I'll sort out the beds and dates.",
                    Modifier.padding(horizontal = 16.dp),
                ) { Button(onClick = { onGoTo(Dest.SEEDS.route) }) { Text("Open the Seed tin") } }
            }
            due.isEmpty() -> item {
                GardenCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(18.dp)) {
                        Text("All quiet for now 🌙", style = MaterialTheme.typography.titleMedium)
                        if (upcoming.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Coming up", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            upcoming.forEach { t ->
                                Text(
                                    "${t.crop.emoji}  ${t.title.replaceFirstChar { it.uppercase() }} · ${t.date.format(ShortDate)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
            else -> items(due, key = { "${it.planting.id}-${it.kind}" }) { t ->
                TaskRow(t, bedNames[t.planting.bedId], today, onOpen = { openTask = t }) { vm.completeTask(t) }
            }
        }

        item { SectionHeader("Quick log", "📝") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val quick = listOf(EntryType.WATERED, EntryType.FED, EntryType.HARVESTED, EntryType.WEEDED, EntryType.PEST, EntryType.NOTE)
                items(quick) { t ->
                    AssistChip(
                        onClick = { logType = t },
                        label = { Text("${t.emoji}  ${t.label}") },
                        shape = RoundedCornerShape(50),
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    )
                }
            }
        }
    }

    if (editingAvatar) {
        AvatarEditorDialog(avatar, onDismiss = { editingAvatar = false }) { vm.saveAvatar(it) }
    }

    if (editingName) {
        var newName by remember { mutableStateOf(garden.userName) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editingName = false },
            title = { Text("Change your name") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    newName, { newName = it }, label = { Text("Your name") }, singleLine = true,
                    colors = com.accidentalgardener.app.ui.components.gardenFieldColors(),
                )
            },
            confirmButton = { Button(onClick = { vm.updateName(newName); editingName = false }) { Text("Save") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { editingName = false }) { Text("Cancel") } },
        )
    }

    openTask?.let { t ->
        JobSheet(
            task = t,
            bed = beds.firstOrNull { it.id == t.planting.bedId },
            onDismiss = { openTask = null },
            onDone = { vm.completeTask(t) },
        )
    }

    logType?.let { t ->
        AddEntryDialog(t, beds, plannedCrops, onDismiss = { logType = null }, onSave = { vm.addEntry(it) })
    }
}

@Composable
private fun HomeHeader(
    garden: Garden, weather: WeatherUi, avatar: AvatarStyle,
    onSettings: () -> Unit, onRefresh: () -> Unit, onEditAvatar: () -> Unit, onEditName: () -> Unit,
) {
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
    val s = weather.snapshot
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(Brush.linearGradient(listOf(Moss, Leaf)))
    ) {
        // decorative leaves
        Text("🌿", fontSize = 90.sp, modifier = Modifier.align(Alignment.TopEnd).padding(top = 30.dp, end = 4.dp), color = Color.White.copy(alpha = 0.18f))
        Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GardenerAvatar(avatar, size = 60.dp, background = Color.White.copy(alpha = 0.9f), modifier = Modifier.clickable(onClick = onEditAvatar))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("$greeting,", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${garden.userName} ✏️", color = Color.White, style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.clickable(onClick = onEditName),
                    )
                }
                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Settings", tint = Color.White) }
            }
            Text("📍 ${garden.town}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (s != null) {
                    Text(WeatherCodes.emoji(s.currentCode), fontSize = 46.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${s.currentTemp.r()}°C", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        val todayF = s.days.firstOrNull()
                        Text(
                            WeatherCodes.describe(s.currentCode) +
                                (todayF?.let { " · ${it.tMin.r()}° to ${it.tMax.r()}° · ${it.rainMm.r()} mm rain" } ?: ""),
                            color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    Text(
                        if (weather.loading) "Checking the sky…" else (weather.error ?: "No forecast yet"),
                        color = Color.White, modifier = Modifier.weight(1f),
                    )
                }
                if (weather.loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, "Refresh weather", tint = Color.White) }
            }

            if (s != null && s.days.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 12.dp)) {
                    items(s.days) { d ->
                        Column(
                            Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.16f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                if (d.date == LocalDate.now()) "Today" else d.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                color = Color.White, style = MaterialTheme.typography.labelMedium,
                            )
                            Text(WeatherCodes.emoji(d.code), fontSize = 22.sp)
                            Text("${d.tMax.r()}°", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${d.tMin.r()}°", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            if (s != null) {
                Text("Forecast by Met Éireann", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            }
            weather.error?.takeIf { s != null }?.let {
                Text("$it. Showing the last forecast.", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
            }

            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.92f)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🗓️", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "This month",
                        style = MaterialTheme.typography.labelMedium, color = Moss,
                    )
                    Text(SeasonalTips.forMonth(LocalDate.now().monthValue), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2B2A22))
                }
            }
        }
    }
}

@Composable
private fun AlertCard(a: GardenAlert) {
    val bg = when (a.severity) {
        Severity.WARNING -> Clay
        Severity.WATCH -> Butter
        Severity.INFO -> SkyLight
    }
    GardenCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), color = bg) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Text(a.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(a.title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF2B2A22))
                Text(a.message, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF3A372D))
            }
        }
    }
}

@Composable
private fun WaterCard(w: WaterAdvice, onWatered: () -> Unit) {
    GardenCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBubble(w.emoji, bg = if (w.needed) SkyLight else Sprout.copy(alpha = 0.5f), size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Text(w.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (w.needed) Pill("Water today", SkyLight, Color(0xFF0E3242))
                else Pill("No need", Sprout.copy(alpha = 0.6f), Color(0xFF1C3010))
            }
            Spacer(Modifier.height(8.dp))
            Text(w.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onWatered) { Text("💧  I've watered these") }
        }
    }
}

@Composable
private fun TaskRow(t: GardenTask, bedName: String?, today: LocalDate, onOpen: () -> Unit, onDone: () -> Unit) {
    val daysAway = ChronoUnit.DAYS.between(today, t.date)
    val whenText = when {
        daysAway < -7 -> "Running late · was due ${t.date.format(ShortDate)}"
        daysAway < 0 -> "Due ${-daysAway} day${if (daysAway == -1L) "" else "s"} ago"
        daysAway == 0L -> "Today"
        daysAway == 1L -> "Tomorrow"
        else -> t.date.format(ShortDate)
    }
    GardenCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.clickable(onClick = onOpen).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            EmojiBubble(t.crop.emoji)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${t.kind.emoji} ${t.title.replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.titleSmall)
                Text(
                    listOfNotNull(bedName, whenText).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (daysAway < 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Tap for step-by-step help",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            FilledTonalIconButton(onClick = onDone) { Icon(Icons.Filled.Check, "Done") }
        }
    }
}
