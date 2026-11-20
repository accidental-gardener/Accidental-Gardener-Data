package com.accidentalgardener.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accidentalgardener.app.data.Bed
import com.accidentalgardener.app.data.BedPlace
import com.accidentalgardener.app.data.Crop
import com.accidentalgardener.app.data.Garden
import com.accidentalgardener.app.data.isPot
import com.accidentalgardener.app.ui.components.GardenCard
import com.accidentalgardener.app.ui.components.gardenFieldColors
import com.accidentalgardener.app.ui.theme.Bark
import com.accidentalgardener.app.ui.theme.Cream
import com.accidentalgardener.app.ui.theme.Soil
import com.accidentalgardener.app.ui.theme.SoilLight
import com.accidentalgardener.app.ui.theme.Terracotta
import kotlin.math.roundToInt

private val Grass = listOf(Color(0xFF9CC27A), Color(0xFF86B262))

/** Gives every bed or pot that hasn't been placed yet a tidy spot on the map. */
private fun autoPlace(beds: List<Bed>, widthM: Float): List<Bed> {
    var x = 0.5f
    var y = 0.5f
    var rowH = 0f
    return beds.map { b ->
        if (b.mapX >= 0f) return@map b
        if (x + b.mapW > widthM - 0.3f) {
            x = 0.5f
            y += rowH + 0.6f
            rowH = 0f
        }
        val placed = b.copy(mapX = x, mapY = y)
        x += b.mapW + 0.6f
        rowH = maxOf(rowH, b.mapH)
        placed
    }
}

/**
 * A top-down plan of the garden. Drag beds and pots to where they really are.
 * Tap one to open it. Indoor pots sit in a strip underneath as they're not in the garden.
 */
@Composable
fun GardenMap(
    garden: Garden,
    beds: List<Bed>,
    cropsIn: (Bed) -> List<Crop>,
    onSave: (List<Bed>) -> Unit,
    onOpen: (Bed) -> Unit,
    onResize: (Float, Float) -> Unit,
) {
    val outside = beds.filter { it.place != BedPlace.INDOOR }
    val indoor = beds.filter { it.place == BedPlace.INDOOR }
    var resizing by remember { mutableStateOf(false) }

    // Save positions for anything that hasn't been placed yet, once.
    LaunchedEffect(outside.map { it.id }) {
        if (outside.any { it.mapX < 0f }) onSave(autoPlace(outside, garden.mapWidthM).filter { b -> outside.first { it.id == b.id }.mapX < 0f })
    }

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
                "Drag to move · tap to open",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            AssistChip(
                onClick = { resizing = true },
                label = { Text("📐 ${fmt(garden.mapWidthM)} × ${fmt(garden.mapLengthM)} m") },
            )
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val metre = maxWidth / garden.mapWidthM
            val metrePx = with(LocalDensity.current) { metre.toPx() }
            val positions = remember(outside) {
                mutableStateMapOf<Long, Offset>().apply {
                    autoPlace(outside, garden.mapWidthM).forEach { put(it.id, Offset(it.mapX, it.mapY)) }
                }
            }

            Box(
                Modifier.fillMaxWidth().height(metre * garden.mapLengthM)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(Grass))
                    .drawBehind {
                        // a faint one-metre grid
                        var gx = metrePx
                        while (gx < size.width) {
                            drawLine(Color.White.copy(alpha = 0.18f), Offset(gx, 0f), Offset(gx, size.height), 1f)
                            gx += metrePx
                        }
                        var gy = metrePx
                        while (gy < size.height) {
                            drawLine(Color.White.copy(alpha = 0.18f), Offset(0f, gy), Offset(size.width, gy), 1f)
                            gy += metrePx
                        }
                    }
            ) {
                outside.forEach { b ->
                    val pos = positions[b.id] ?: Offset(0f, 0f)
                    MapItem(
                        bed = b,
                        crops = cropsIn(b),
                        modifier = Modifier
                            .offset { IntOffset((pos.x * metrePx).roundToInt(), (pos.y * metrePx).roundToInt()) }
                            .size(metre * b.mapW, metre * b.mapH)
                            .pointerInput(b, metrePx) {
                                detectDragGestures(
                                    onDragEnd = {
                                        positions[b.id]?.let { p -> onSave(listOf(b.copy(mapX = p.x, mapY = p.y))) }
                                    },
                                ) { change, drag ->
                                    change.consume()
                                    val cur = positions[b.id] ?: Offset(0f, 0f)
                                    positions[b.id] = Offset(
                                        (cur.x + drag.x / metrePx).coerceIn(0f, (garden.mapWidthM - b.mapW).coerceAtLeast(0f)),
                                        (cur.y + drag.y / metrePx).coerceIn(0f, (garden.mapLengthM - b.mapH).coerceAtLeast(0f)),
                                    )
                                }
                            }
                            .clickable { onOpen(b) },
                    )
                }
                if (outside.isEmpty()) {
                    Text(
                        "Add a bed or pot and it will appear here",
                        color = Color.White, modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (indoor.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            GardenCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.padding(12.dp)) {
                    Text("🏠 Indoors", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        indoor.take(8).forEach { b ->
                            MapItem(b, cropsIn(b), Modifier.size(56.dp).clickable { onOpen(b) })
                        }
                    }
                }
            }
        }
    }

    if (resizing) {
        var w by remember { mutableStateOf(fmt(garden.mapWidthM)) }
        var l by remember { mutableStateOf(fmt(garden.mapLengthM)) }
        AlertDialog(
            onDismissRequest = { resizing = false },
            title = { Text("Garden size") },
            text = {
                Column {
                    Text("Roughly how big is the space? A pace is about a metre.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(w, { w = it }, label = { Text("Width in metres") }, singleLine = true, colors = gardenFieldColors())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(l, { l = it }, label = { Text("Length in metres") }, singleLine = true, colors = gardenFieldColors())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val nw = w.toFloatOrNull()?.coerceIn(1f, 200f)
                    val nl = l.toFloatOrNull()?.coerceIn(1f, 200f)
                    if (nw != null && nl != null) onResize(nw, nl)
                    resizing = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { resizing = false }) { Text("Cancel") } },
        )
    }
}

private fun fmt(f: Float): String = if (f % 1f == 0f) f.toInt().toString() else "%.1f".format(f)

@Composable
private fun MapItem(bed: Bed, crops: List<Crop>, modifier: Modifier) {
    val shape = if (bed.isPot) CircleShape else RoundedCornerShape(6.dp)
    val bg = when {
        bed.isPot -> Brush.radialGradient(listOf(Soil, Terracotta))
        bed.place == BedPlace.TUNNEL -> Brush.verticalGradient(listOf(Color(0xFFEFF5EA), Color(0xFFCFE0C3)))
        else -> Brush.verticalGradient(listOf(SoilLight, Soil))
    }
    val border = when {
        bed.isPot -> Color(0xFF9E4B2B)
        bed.place == BedPlace.TUNNEL -> Color.White
        else -> Bark
    }
    Box(
        modifier.shadow(3.dp, shape).clip(shape).background(bg).border(3.dp, border, shape).padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                crops.take(3).joinToString("") { it.emoji }.ifBlank { if (bed.isPot) "🪴" else "" },
                fontSize = 14.sp,
            )
            if (!bed.isPot) {
                Text(
                    bed.name,
                    color = if (bed.place == BedPlace.TUNNEL) Color(0xFF2B4A1E) else Cream,
                    fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                )
            }
        }
    }
}
