package com.accidentalgardener.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/** Asks for approximate location permission if needed, then finds the nearest town. */
@Composable
fun UseMyLocationButton(onLocate: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onLocate()
    }
    OutlinedButton(
        modifier = modifier,
        onClick = {
            val has = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            if (has) onLocate() else launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        },
    ) {
        Icon(Icons.Filled.MyLocation, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Use my current location")
    }
}
