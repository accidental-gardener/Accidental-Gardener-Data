package com.accidentalgardener.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.accidentalgardener.app.data.Garden
import com.accidentalgardener.app.ui.screens.AboutScreen
import com.accidentalgardener.app.ui.screens.AdminScreen
import com.accidentalgardener.app.ui.screens.BedsScreen
import com.accidentalgardener.app.ui.screens.CalendarScreen
import com.accidentalgardener.app.ui.screens.HomeScreen
import com.accidentalgardener.app.ui.screens.JournalScreen
import com.accidentalgardener.app.ui.screens.OnboardingScreen
import com.accidentalgardener.app.ui.screens.SeedsScreen
import com.accidentalgardener.app.ui.screens.SettingsScreen

enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Garden", Icons.Filled.Yard),
    BEDS("beds", "Beds", Icons.Filled.Grass),
    SEEDS("seeds", "Seed tin", Icons.Filled.Spa),
    CALENDAR("calendar", "Calendar", Icons.Filled.CalendarMonth),
    JOURNAL("journal", "Journal", Icons.Filled.EditNote),
    ABOUT("about", "About", Icons.Filled.Info),
}

@Composable
fun AppRoot(vm: GardenViewModel = viewModel()) {
    val state by vm.gardenState.collectAsStateWithLifecycle()
    val notifyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    when (val s = state) {
        GardenState.Loading -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        GardenState.None -> OnboardingScreen(vm)
        is GardenState.Ready -> {
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33) notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            MainScaffold(vm, s.garden)
        }
    }
}

private fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun MainScaffold(vm: GardenViewModel, garden: Garden) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (route != "settings" && route != "admin") {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Dest.entries.forEach { d ->
                        NavigationBarItem(
                            selected = route == d.route,
                            onClick = { nav.goTab(d.route) },
                            icon = { Icon(d.icon, contentDescription = null) },
                            label = { Text(d.label, fontSize = 10.sp, maxLines = 1) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = Dest.HOME.route, modifier = Modifier.padding(pad)) {
            composable(Dest.HOME.route) {
                HomeScreen(vm, garden, onOpenSettings = { nav.navigate("settings") }, onGoTo = { nav.goTab(it) })
            }
            composable(Dest.BEDS.route) { BedsScreen(vm, garden, onGoTo = { nav.goTab(it) }) }
            composable(Dest.SEEDS.route) { SeedsScreen(vm, garden, onPlanned = { nav.goTab(Dest.BEDS.route) }) }
            composable(Dest.CALENDAR.route) { CalendarScreen(vm, garden) }
            composable(Dest.JOURNAL.route) { JournalScreen(vm) }
            composable(Dest.ABOUT.route) {
                AboutScreen(vm, garden, onOpenSettings = { nav.navigate("settings") }, onOpenAdmin = { nav.navigate("admin") })
            }
            composable("admin") { AdminScreen(onBack = { nav.popBackStack() }) }
            composable("settings") { SettingsScreen(vm, garden, onBack = { nav.popBackStack() }) }
        }
    }
}
