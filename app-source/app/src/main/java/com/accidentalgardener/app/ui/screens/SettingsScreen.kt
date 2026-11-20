@file:OptIn(ExperimentalMaterial3Api::class)

package com.accidentalgardener.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.accidentalgardener.app.data.Garden
import com.accidentalgardener.app.ui.GardenViewModel
import com.accidentalgardener.app.ui.components.GardenCard
import com.accidentalgardener.app.ui.components.gardenFieldColors
import com.accidentalgardener.app.ui.components.Stepper
import com.accidentalgardener.app.ui.components.UseMyLocationButton
import androidx.compose.runtime.LaunchedEffect
import com.accidentalgardener.app.weather.Place
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: GardenViewModel, garden: Garden, onBack: () -> Unit) {
    val beds by vm.beds.collectAsStateWithLifecycle()
    val places by vm.places.collectAsStateWithLifecycle()
    val searching by vm.searching.collectAsStateWithLifecycle()
    val searchError by vm.searchError.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf(garden.userName) }
    var query by remember { mutableStateOf("") }
    var place by remember { mutableStateOf<Place?>(null) }
    var perenual by remember { mutableStateOf(vm.perenualKey()) }
    var confirmReset by remember { mutableStateOf<String?>(null) }
    var perBed by remember { mutableIntStateOf(garden.cropsPerBed) }
    var year by remember { mutableIntStateOf(garden.planYear) }
    var frost by remember { mutableFloatStateOf(garden.lastFrostDoy.toFloat()) }
    var notify by remember { mutableStateOf(garden.notificationsOn) }
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val guideStatus by vm.guideStatus.collectAsStateWithLifecycle()
    val guideBusy by vm.guideBusy.collectAsStateWithLifecycle()
    var guideUrl by remember { mutableStateOf(vm.guideUrl()) }

    val frostDate = LocalDate.ofYearDay(2027, frost.roundToInt().coerceIn(1, 365))

    fun build() = garden.copy(
        userName = name.ifBlank { garden.userName },
        town = place?.label ?: garden.town,
        latitude = place?.lat ?: garden.latitude,
        longitude = place?.lon ?: garden.longitude,
        cropsPerBed = perBed,
        planYear = year,
        lastFrostDoy = frost.roundToInt(),
        notificationsOn = notify,
    )

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Section("👋 You") {
                OutlinedTextField(name, { name = it }, label = { Text("Your name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = gardenFieldColors())
            }

            Section("📍 Garden location") {
                Text("Currently ${place?.label ?: garden.town}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                val located by vm.located.collectAsStateWithLifecycle()
                LaunchedEffect(located) { located?.let { place = it; vm.clearLocated() } }
                UseMyLocationButton(onLocate = { vm.useCurrentLocation() })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    query, { query = it }, label = { Text("Search for a new town") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.searchPlaces(query) }),
                    trailingIcon = {
                        if (searching) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else IconButton(onClick = { vm.searchPlaces(query) }) { Icon(Icons.Filled.Search, "Search") }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = gardenFieldColors(),
                )
                searchError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (query.isNotBlank()) {
                    places.forEach { p ->
                        Row(
                            Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (place == p) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                                .clickable { place = p }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${p.label}, ${p.country}", modifier = Modifier.weight(1f))
                            if (place == p) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Section("🪴 Beds and pots") {
                Text(
                    "You have ${beds.count { !it.isTunnel && it.kind.name == "BED" }} outdoor beds, ${beds.count { it.isTunnel }} in the tunnel and ${beds.count { it.kind.name == "POT" }} pots. Add, delete or move them on the Beds screen.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Stepper("Crops per bed", "Used when a bed has no size set. Pots always hold one crop.", perBed, 1..6, { perBed = it }, "🌱")
            }

            Section("❄️ Frost and season") {
                Text("Last spring frost around ${frostDate.format(DateTimeFormatter.ofPattern("d MMMM"))}", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Most of Ireland is safe from late April to mid May. Coastal gardens are earlier and high or inland gardens are later.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(frost, { frost = it }, valueRange = 75f..160f)
                Stepper("Planning year", null, year, 2024..2040, { year = it }, "📅")
            }

            Section("📚 Growing guides") {
                Text(
                    "${catalog.plants.size} plants · version ${catalog.version} · ${catalog.source.lowercase()}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "The guides live in a plants.json file online, for example on GitHub. The app keeps a copy on the phone so it works with no signal and checks for a newer version once a day.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    guideUrl, { guideUrl = it }, label = { Text("Guide file link") }, singleLine = true,
                    placeholder = { Text("https://raw.githubusercontent.com/…/plants.json") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = gardenFieldColors(),
                )
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { vm.refreshGuides(guideUrl) }, enabled = !guideBusy && guideUrl.isNotBlank()) {
                        Text("Check for updates")
                    }
                    if (guideBusy) {
                        Spacer(Modifier.size(12.dp))
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    }
                }
                guideStatus?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)) }
            }

            Section("🔎 Looking up new plants") {
                Text(
                    "Plants that aren't in the database are looked up on Wikipedia for free. For extra care details like watering, add a free Perenual key from perenual.com (optional).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    perenual, { perenual = it }, label = { Text("Perenual API key (optional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), colors = gardenFieldColors(),
                )
            }

            Section("🔔 Alerts") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Weather and watering alerts", style = MaterialTheme.typography.titleSmall)
                        Text("Frost, storms, heavy rain, heat and Met Éireann warnings", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(notify, { notify = it })
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.setPerenualKey(perenual); vm.saveSettings(build(), replan = false); onBack() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
            OutlinedButton(
                onClick = { vm.setPerenualKey(perenual); vm.saveSettings(build(), replan = true); onBack() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Save and re-plan my garden") }

            Spacer(Modifier.height(12.dp))
            Section("🔄 Start over") {
                Text("Pick how much you'd like to clear. These can't be undone.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { confirmReset = "plan" }, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear this year's plan")
                }
                OutlinedButton(onClick = { confirmReset = "spaces" }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text("Remove all beds and pots and start the layout again")
                }
                OutlinedButton(
                    onClick = { confirmReset = "all" },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Reset everything and run setup again") }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Weather forecasts and warnings copyright Met Éireann, source www.met.ie, used under their open data licence (CC BY 4.0). Met Éireann accepts no liability for errors or omissions in the data. " +
                    "Planting dates are a guide, so always check the soil and the sky.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    confirmReset?.let { kind ->
        ConfirmReset(kind, onDismiss = { confirmReset = null }) {
            when (kind) {
                "plan" -> vm.clearPlan()
                "spaces" -> vm.clearSpaces()
                else -> vm.resetEverything()
            }
            if (kind == "all") onBack()
        }
    }
}

@Composable
private fun ConfirmReset(kind: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val (title, body) = when (kind) {
        "plan" -> "Clear this year's plan?" to "Every planned and growing crop for this year is removed. Your beds, pots, seed picks and journal stay."
        "spaces" -> "Remove all beds and pots?" to "All beds, pots and this year's plan are removed so you can lay out your space again. Your seed picks and journal stay."
        else -> "Reset everything?" to "This deletes all your beds, pots, plans, seeds, your own plants and the journal, then takes you back to setup."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            Button(onClick = { onConfirm(); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Yes, clear it")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    GardenCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
