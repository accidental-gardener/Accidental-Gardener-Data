package com.accidentalgardener.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.accidentalgardener.app.data.Community
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * The person's own requests: what's waiting, what the admins asked, what was added.
 * Always shown, even when empty, so people know where to look.
 */
@Composable
fun MyRequestsList(userName: String, showHeader: Boolean = true) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val changes by Community.changes.collectAsState()
    var refresh by remember { mutableIntStateOf(0) }
    val mine = remember(changes) { Community.visibleRequests(ctx).reversed().take(15) }
    val statuses = remember { mutableStateMapOf<Int, Community.Status>() }
    var loading by remember { mutableStateOf(false) }
    var asking by remember { mutableStateOf(false) }
    val left = remember(changes, asking) { Community.leftToday(ctx) }

    LaunchedEffect(changes, refresh) {
        if (mine.isEmpty()) return@LaunchedEffect
        loading = true
        mine.forEach { r -> runCatching { Community.status(r.number) }.getOrNull()?.let { statuses[r.number] = it } }
        loading = false
        // Note when requests were approved or declined, so they tidy themselves away a couple of days later.
        val all = Community.myRequests(ctx)
        val updated = all.map { r -> statuses[r.number]?.let { Community.withStatus(r, it.code) } ?: r }
        if (updated != all) Community.saveMyRequests(ctx, updated)
    }

    Column(Modifier.fillMaxWidth()) {
        if (showHeader) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📮 My requests", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else if (mine.isNotEmpty()) IconButton(onClick = { refresh++ }) { Icon(Icons.Filled.Refresh, "Check for updates") }
            }
        }
        val pending = mine.count { (statuses[it.number]?.code ?: it.lastStatus) in setOf("WAITING", "QUESTION", "APPROVED") }
        Text(
            when {
                mine.isEmpty() -> "Nothing here right now. When you ask for a plant or suggest a fix, you can follow it here."
                pending == 0 -> "All your requests have been dealt with. They clear away 2 days after they're approved or declined."
                else -> "$pending still in progress."
            },
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            if (left > 0) "$left of ${Community.DAILY_LIMIT} requests left today." else "No requests left today. You get ${Community.DAILY_LIMIT} more tomorrow.",
            style = MaterialTheme.typography.labelMedium,
            color = if (left > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 2.dp),
        )
        mine.forEach { r ->
            key(r.number) {
                val st = statuses[r.number]
                var answer by remember { mutableStateOf("") }
                var sent by remember { mutableStateOf<String?>(null) }
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                Text(r.plant.ifBlank { "Request #${r.number}" }, style = MaterialTheme.typography.titleSmall)
                Text(
                    "Sent ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(r.sentAt))}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(st?.label ?: "Checking…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                st?.adminNote?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (st?.code == "QUESTION") {
                    st.questions.lastOrNull()?.let { q ->
                        Text("❓ $q", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                    }
                    if (sent == null) {
                        OutlinedTextField(
                            answer, { answer = it.take(3000) }, label = { Text("Your answer") }, minLines = 2,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp), colors = gardenFieldColors(),
                        )
                        Button(enabled = answer.isNotBlank(), onClick = {
                            scope.launch { sent = Community.reply(r, answer).second }
                        }, modifier = Modifier.padding(top = 4.dp)) { Text("Send answer") }
                    } else {
                        Text(sent!!, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(enabled = left > 0, onClick = { asking = true }) { Text("📮 New request") }
    }
    if (asking) RequestDialog(userName = userName) { asking = false }
}

@Composable
fun MyRequestsDialog(userName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📮 My requests") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { MyRequestsList(userName, showHeader = false) } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
