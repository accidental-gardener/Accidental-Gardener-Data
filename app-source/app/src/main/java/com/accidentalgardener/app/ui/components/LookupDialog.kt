package com.accidentalgardener.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.accidentalgardener.app.ui.GardenViewModel

/**
 * Find a plant that isn't in the database. Searches online, then opens the
 * add-plant form filled in with what was found so it can be checked and saved.
 */
@Composable
fun LookupDialog(vm: GardenViewModel, startText: String, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf(startText) }
    val matches by vm.lookupMatches.collectAsStateWithLifecycle()
    val busy by vm.lookupBusy.collectAsStateWithLifecycle()
    val error by vm.lookupError.collectAsStateWithLifecycle()
    val draft by vm.lookupDraft.collectAsStateWithLifecycle()

    val d = draft
    if (d != null) {
        AddCropDialog(
            onDismiss = { vm.clearLookup(); onDismiss() },
            initial = d,
            onSave = { vm.addCustomCrop(it) },
        )
        return
    }

    AlertDialog(
        onDismissRequest = { vm.clearLookup(); onDismiss() },
        title = { Text("🔎 Look up a plant") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Can't find it in the Seed tin? Type its name and I'll look it up online, then add it to your plants.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    query, { query = it }, label = { Text("Plant name") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.lookupSearch(query) }),
                    trailingIcon = {
                        if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else IconButton(onClick = { vm.lookupSearch(query) }) { Icon(Icons.Filled.Search, "Search") }
                    },
                    modifier = Modifier.fillMaxWidth(), colors = gardenFieldColors(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp)) }
                Spacer(Modifier.height(8.dp))
                if (matches.isNotEmpty()) Text("Which one do you mean?", style = MaterialTheme.typography.titleSmall)
                matches.forEach { m ->
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !busy) { vm.lookupPick(m, query) }
                            .padding(10.dp)
                    ) {
                        Text(m.title, style = MaterialTheme.typography.titleMedium)
                        Text(m.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { vm.clearLookup(); onDismiss() }) { Text("Close") } },
    )
}
