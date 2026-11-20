@file:OptIn(ExperimentalLayoutApi::class)

package com.accidentalgardener.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.accidentalgardener.app.data.Community
import kotlinx.coroutines.launch

/** Ask the admins to add a plant or fix a guide. No account needed. */
@Composable
fun RequestDialog(
    startKind: Community.Kind = Community.Kind.NEW_PLANT,
    startPlant: String = "",
    startDetails: String = "",
    userName: String = "",
    onDismiss: () -> Unit,
) {
    var kind by remember { mutableStateOf(startKind) }
    var plant by remember { mutableStateOf(startPlant) }
    var details by remember { mutableStateOf(startDetails) }
    var from by remember { mutableStateOf(userName) }
    var sending by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val done = result?.first == true
    val left = remember(result) { Community.leftToday(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (kind == Community.Kind.FIX) "✏️ Suggest a fix" else "📮 Request a plant") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Requests go to the app's admins, who check them and add them properly to the plant database for everyone.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    if (left > 0) "You can send $left more ${if (left == 1) "request" else "requests"} today (${Community.DAILY_LIMIT} a day)."
                    else "You've used your ${Community.DAILY_LIMIT} requests for today. Come back tomorrow.",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (left > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Community.Kind.entries.forEach { k ->
                        FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.label) }, enabled = !done)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    plant, { plant = it.take(80) }, label = { Text("Plant name") }, singleLine = true, enabled = !done,
                    modifier = Modifier.fillMaxWidth(), colors = gardenFieldColors(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    details, { details = it.take(3000) }, enabled = !done,
                    label = { Text(if (kind == Community.Kind.FIX) "What needs fixing?" else "Anything useful (variety, how you grow it)") },
                    minLines = 3, modifier = Modifier.fillMaxWidth(), colors = gardenFieldColors(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    from, { from = it.take(60) }, label = { Text("Your first name (optional)") }, singleLine = true, enabled = !done,
                    modifier = Modifier.fillMaxWidth(), colors = gardenFieldColors(),
                )
                Text(
                    "Requests are posted publicly on the app's GitHub page, so please don't include personal details.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                result?.let { (ok, msg) ->
                    Spacer(Modifier.height(8.dp))
                    Text(msg, color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            if (done) {
                Button(onClick = onDismiss) { Text("Done") }
            } else {
                Button(
                    enabled = !sending && left > 0 && (plant.isNotBlank() || details.isNotBlank()),
                    onClick = {
                        sending = true
                        scope.launch {
                            result = Community.send(context, kind, plant, details, from)
                            sending = false
                        }
                    },
                ) { Text(if (sending) "Sending…" else "Send") }
            }
        },
        dismissButton = { if (!done) TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
