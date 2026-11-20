package com.accidentalgardener.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.accidentalgardener.app.data.Crop
import com.accidentalgardener.app.data.Garden
import com.accidentalgardener.app.data.Planting
import com.accidentalgardener.app.ui.GardenViewModel
import com.accidentalgardener.app.ui.components.EmptyHint
import com.accidentalgardener.app.ui.components.GardenCard
import com.accidentalgardener.app.ui.components.ScreenTitle
import com.accidentalgardener.app.ui.components.short
import com.accidentalgardener.app.ui.theme.Leaf
import com.accidentalgardener.app.ui.theme.Sprout
import com.accidentalgardener.app.ui.theme.Terracotta
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

private enum class Phase { NONE, INDOOR, GROWING, HARVEST }

private fun phaseFor(p: Planting, year: Int, month: Int): Phase {
    val start = LocalDate.of(year, month, 1)
    val end = start.plusMonths(1).minusDays(1)
    fun overlaps(a: LocalDate, b: LocalDate) = !a.isAfter(end) && !b.isBefore(start)
    val out = p.plantOutDate
    return when {
        overlaps(p.harvestStart, p.harvestEnd) -> Phase.HARVEST
        overlaps(out ?: p.sowDate, p.harvestStart.minusDays(1)) -> Phase.GROWING
        p.sowIndoors && out != null && overlaps(p.sowDate, out.minusDays(1)) -> Phase.INDOOR
        else -> Phase.NONE
    }
}

@Composable
private fun Phase.color(): Color = when (this) {
    Phase.INDOOR -> Sprout
    Phase.GROWING -> Leaf
    Phase.HARVEST -> Terracotta
    Phase.NONE -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
fun CalendarScreen(vm: GardenViewModel, garden: Garden) {
    val plantings by vm.plantings.collectAsStateWithLifecycle()
    val crops by vm.crops.collectAsStateWithLifecycle()
    val lookup = remember(crops) { crops.associateBy { it.id } }
    val now = LocalDate.now()
    var month by rememberSaveable { mutableIntStateOf(if (now.year == garden.planYear) now.monthValue else 1) }
    val rows = plantings.sortedBy { it.sowDate }.mapNotNull { p -> lookup[p.cropId]?.let { p to it } }
    val monthNames = Month.entries.map { it.getDisplayName(TextStyle.NARROW, Locale.getDefault()) }

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item { ScreenTitle("Growing calendar", "Your ${garden.planYear} season at a glance") }
        if (rows.isEmpty()) {
            item { EmptyHint("📅", "Your calendar is empty", "Once you plan your garden every sowing, planting and harvest shows up here.") }
            return@LazyColumn
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                Legend("Sow indoors", Sprout); Legend("Growing", Leaf); Legend("Harvest", Terracotta)
            }
        }
        item {
            GardenCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row {
                        Spacer(Modifier.width(112.dp))
                        monthNames.forEachIndexed { i, m ->
                            val current = now.year == garden.planYear && now.monthValue == i + 1
                            Text(
                                m, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                                color = if (current) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    rows.forEach { (p, c) -> TimelineRow(p, c, garden.planYear) }
                }
            }
        }
        item {
            Spacer(Modifier.height(20.dp))
            Text("Month by month", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(Month.entries) { m ->
                    FilterChip(
                        selected = month == m.value, onClick = { month = m.value },
                        label = { Text(m.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        val events = monthEvents(rows, garden.planYear, month)
        if (events.isEmpty()) {
            item { Text("Nothing to sow, plant or pick this month. A good time to weed and tidy.", style = MaterialTheme.typography.bodyMedium) }
        }
        items(events) { (date, text) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(date.short(), modifier = Modifier.width(64.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private fun monthEvents(rows: List<Pair<Planting, Crop>>, year: Int, month: Int): List<Pair<LocalDate, String>> {
    val out = mutableListOf<Pair<LocalDate, String>>()
    fun inMonth(d: LocalDate) = d.year == year && d.monthValue == month
    rows.forEach { (p, c) ->
        val n = c.name.lowercase()
        if (inMonth(p.sowDate)) out += p.sowDate to when {
            p.sowIndoors -> "🌱 ${c.emoji} Sow $n indoors"
            c.planted || c.fixedSow != null -> "🪴 ${c.emoji} Plant $n"
            else -> "🌱 ${c.emoji} Sow $n"
        }
        p.plantOutDate?.let { if (inMonth(it)) out += it to "🪴 ${c.emoji} Plant out $n" }
        if (inMonth(p.harvestStart)) out += p.harvestStart to "🧺 ${c.emoji} Start picking $n"
    }
    return out.sortedBy { it.first }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun TimelineRow(p: Planting, c: Crop, year: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.width(112.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(c.emoji, fontSize = 18.sp)
            Spacer(Modifier.width(4.dp))
            Text(c.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        for (m in 1..12) {
            Box(
                Modifier.weight(1f).height(18.dp).padding(horizontal = 1.dp)
                    .clip(RoundedCornerShape(4.dp)).background(phaseFor(p, year, m).color())
            )
        }
    }
}
