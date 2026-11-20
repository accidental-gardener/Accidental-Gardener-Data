@file:OptIn(ExperimentalLayoutApi::class)

package com.accidentalgardener.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.accidentalgardener.app.ui.GardenViewModel
import com.accidentalgardener.app.ui.components.Stepper
import com.accidentalgardener.app.ui.components.GardenerAvatar
import com.accidentalgardener.app.ui.components.AvatarStyle
import com.accidentalgardener.app.ui.components.UseMyLocationButton
import androidx.compose.runtime.LaunchedEffect
import com.accidentalgardener.app.ui.components.gardenFieldColors
import com.accidentalgardener.app.ui.theme.Bark
import com.accidentalgardener.app.ui.theme.Cream
import com.accidentalgardener.app.ui.theme.Parchment
import com.accidentalgardener.app.ui.theme.Soil
import com.accidentalgardener.app.ui.theme.Sprout
import com.accidentalgardener.app.weather.Place

@Composable
fun OnboardingScreen(vm: GardenViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var place by remember { mutableStateOf<Place?>(null) }
    var space by rememberSaveable { mutableStateOf("SMALL") }
    var outdoor by rememberSaveable { mutableIntStateOf(2) }
    var tunnel by rememberSaveable { mutableIntStateOf(0) }
    var potsOut by rememberSaveable { mutableIntStateOf(0) }
    var potsIn by rememberSaveable { mutableIntStateOf(0) }
    fun pickSpace(t: String) {
        space = t
        when (t) {
            "LARGE" -> { outdoor = 6; tunnel = 2; potsOut = 2; potsIn = 0 }
            "SMALL" -> { outdoor = 2; tunnel = 0; potsOut = 2; potsIn = 1 }
            else -> { outdoor = 0; tunnel = 0; potsOut = 1; potsIn = 0 }
        }
    }

    Box(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainer,
                    )
                )
            )
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            // progress dots
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(5) { i ->
                    Box(
                        Modifier.padding(4.dp).size(if (i == step) 26.dp else 10.dp, 10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
                },
                modifier = Modifier.weight(1f),
                label = "onboarding",
            ) { s ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    when (s) {
                        0 -> WelcomeStep(name) { name = it }
                        1 -> LocationStep(vm, place) { place = it }
                        2 -> SpaceStep(space) { pickSpace(it) }
                        3 -> BedsStep(
                            space, outdoor, tunnel, potsOut, potsIn,
                            { outdoor = it }, { tunnel = it }, { potsOut = it }, { potsIn = it },
                        )
                        else -> DoneStep(name, outdoor, tunnel, potsOut + potsIn)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) TextButton(onClick = { step-- }) { Text("Back") }
                if (step == 1 && place == null) TextButton(onClick = { step++ }) { Text("Skip for now") }
                Spacer(Modifier.weight(1f))
                val canGo = when (step) {
                    0 -> name.isNotBlank()
                    1 -> place != null
                    3 -> outdoor + tunnel + potsOut + potsIn > 0
                    else -> true
                }
                Button(
                    onClick = {
                        if (step < 4) step++
                        else vm.completeOnboarding(name, place, space, GardenViewModel.SpaceCounts(outdoor, tunnel, potsOut, potsIn))
                    },
                    enabled = canGo,
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) { Text(if (step < 4) "Next" else "Start gardening 🌱") }
            }
        }
    }
}

@Composable
private fun WelcomeStep(name: String, onName: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.size(140.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.Center,
        ) { Text("🌱", fontSize = 72.sp) }
        Spacer(Modifier.height(24.dp))
        Text("The Accidental Gardener", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Grow more and worry less. Tell me about your patch and I'll work out what goes where and when.",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = name, onValueChange = onName,
            label = { Text("What should I call you?") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = gardenFieldColors(),
            textStyle = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun LocationStep(vm: GardenViewModel, selected: Place?, onSelect: (Place) -> Unit) {
    val places by vm.places.collectAsStateWithLifecycle()
    val searching by vm.searching.collectAsStateWithLifecycle()
    val error by vm.searchError.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    Text("📍", fontSize = 48.sp)
    Text("Where's your garden?", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
    Text(
        "Your nearest town is enough. I use it for the local forecast, frost alerts and Met Éireann warnings.",
        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    val located by vm.located.collectAsStateWithLifecycle()
    LaunchedEffect(located) { located?.let { onSelect(it); vm.clearLocated() } }
    UseMyLocationButton(onLocate = { vm.useCurrentLocation() })
    Text(
        "or type it in",
        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = TextAlign.Center,
    )
    OutlinedTextField(
        value = query, onValueChange = { query = it },
        label = { Text("Town or village") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, capitalization = KeyboardCapitalization.Words),
        keyboardActions = KeyboardActions(onSearch = { vm.searchPlaces(query) }),
        trailingIcon = {
            if (searching) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else IconButton(onClick = { vm.searchPlaces(query) }) { Icon(Icons.Filled.Search, "Search") }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = gardenFieldColors(),
        textStyle = MaterialTheme.typography.titleMedium,
    )
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
    Spacer(Modifier.height(12.dp))
    places.forEach { p ->
        val isSel = selected == p
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest)
                .clickable { onSelect(p) }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium)
                Text(listOf(p.region, p.country).filter { it.isNotBlank() }.joinToString(", "), style = MaterialTheme.typography.bodySmall)
            }
            if (isSel) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SpaceStep(space: String, onPick: (String) -> Unit) {
    Text("🏡", fontSize = 48.sp)
    Text("What kind of space do you have?", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
    Text(
        "Every bit of space counts, from a big garden to one pot on a windowsill. You can change all of this later.",
        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    listOf(
        Triple("LARGE", "🚜", "Lots of space" to "Several beds, maybe a polytunnel or an allotment"),
        Triple("SMALL", "🏡", "A small garden" to "A couple of beds or borders and some pots"),
        Triple("BALCONY", "🪴", "Balcony, patio or windowsill" to "Pots and containers only, even just one"),
    ).forEach { (key, emoji, text) ->
        val sel = space == key
        Row(
            Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(18.dp))
                .background(if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(2.dp, if (sel) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(18.dp))
                .clickable { onPick(key) }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(emoji, fontSize = 34.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(text.first, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(text.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (sel) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BedsStep(
    space: String, outdoor: Int, tunnel: Int, potsOut: Int, potsIn: Int,
    onOutdoor: (Int) -> Unit, onTunnel: (Int) -> Unit, onPotsOut: (Int) -> Unit, onPotsIn: (Int) -> Unit,
) {
    Text("🪴", fontSize = 48.sp)
    Text("How many growing spaces?", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
    Text(
        "Count every bed, border or patch, and every pot or container. Size doesn't matter, you can add sizes later.",
        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    if (space != "BALCONY") {
        Stepper("Outdoor beds", "Raised beds, borders or patches", outdoor, 0..60, onOutdoor, "☀️")
        Stepper("Tunnel beds", "Inside a polytunnel or greenhouse", tunnel, 0..40, onTunnel, "⛺")
    }
    Stepper("Pots outside", "On a balcony, patio or in the garden", potsOut, 0..60, onPotsOut, "🪴")
    Stepper("Pots indoors", "On a windowsill or in a conservatory", potsIn, 0..30, onPotsIn, "🏠")
    Spacer(Modifier.height(16.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(tunnel.coerceAtMost(12)) {
            Box(
                Modifier.padding(bottom = 8.dp).size(44.dp, 34.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(Color(0xFFDCE9D2))
                    .border(2.dp, Color.White, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
            )
        }
        repeat(outdoor.coerceAtMost(20)) {
            Box(
                Modifier.padding(bottom = 8.dp).size(44.dp, 34.dp)
                    .clip(RoundedCornerShape(6.dp)).background(Bark).padding(4.dp)
                    .clip(RoundedCornerShape(3.dp)).background(Soil)
            )
        }
        repeat((potsOut + potsIn).coerceAtMost(20)) {
            Box(
                Modifier.padding(bottom = 8.dp).size(30.dp, 34.dp)
                    .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp, topStart = 3.dp, topEnd = 3.dp))
                    .background(Color(0xFFC0623C))
            )
        }
    }
}

@Composable
private fun DoneStep(name: String, outdoor: Int, tunnel: Int, pots: Int) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        GardenerAvatar(AvatarStyle(), size = 120.dp)
        Spacer(Modifier.height(16.dp))
        Text("You're all set, ${name.trim()}!", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        val what = buildList {
            if (outdoor > 0) add("$outdoor outdoor bed${if (outdoor == 1) "" else "s"}")
            if (tunnel > 0) add("$tunnel in the tunnel")
            if (pots > 0) add("$pots pot${if (pots == 1) "" else "s"}")
        }
        val joined = if (what.size > 1) what.dropLast(1).joinToString(", ") + " and " + what.last() else what.firstOrNull() ?: ""
        Text(
            "I've set up $joined. Next open the Seed tin and pick what you'd like to grow. If things are already growing, tap a bed or pot and add them.",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
