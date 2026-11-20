@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.accidentalgardener.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accidentalgardener.app.data.AdminApi
import com.accidentalgardener.app.data.AdminSignIn
import com.accidentalgardener.app.ui.components.GardenCard
import com.accidentalgardener.app.ui.components.Pill
import com.accidentalgardener.app.ui.components.gardenFieldColors
import kotlinx.coroutines.launch

private enum class AdminTab(val label: String) { NEW("📥 New"), INFO("❓ Waiting for answer"), APPROVED("✅ Approved"), DONE("📦 Done") }

@Composable
fun AdminScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var login by remember { mutableStateOf(AdminApi.login(ctx)) }
    val signIn by AdminSignIn.state.collectAsState()

    // Every time this screen comes back (for example after approving on GitHub), pick up a waiting sign-in.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) AdminSignIn.resume(ctx) }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(signIn) { if (signIn is AdminSignIn.State.SignedIn) login = AdminApi.login(ctx) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Admin") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        if (login == null) {
            AdminSignInPanel(signIn)
        } else {
            AdminRequests(login!!) { AdminApi.signOut(ctx); AdminSignIn.signedOut(); login = null }
        }
    }
}

@Composable
private fun AdminSignInPanel(state: AdminSignIn.State) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        Text("🔐 Admin sign in", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Admins sign in with their own GitHub account. If the account is allowed to edit the plant database, the admin tools unlock. Nobody else can see them.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        when {
            !AdminApi.configured ->
                Text("Admin sign-in isn't set up in this version of the app yet.", color = MaterialTheme.colorScheme.error)
            state is AdminSignIn.State.Waiting -> {
                val c = state.code
                LaunchedEffect(c.userCode) { clipboard.setText(AnnotatedString(c.userCode)) }
                GardenCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Your code (already copied)", style = MaterialTheme.typography.labelLarge)
                        Text(c.userCode, fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        TextButton(onClick = { clipboard.setText(AnnotatedString(c.userCode)) }) { Text("Copy again") }
                        Text(
                            "1. Tap Open GitHub. It opens in your phone's browser.\n" +
                                "2. Paste the code and tap Authorize.\n" +
                                "3. Come back to this app. It finishes on its own, or tap the button below.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { AdminSignIn.openGitHub(ctx, c) }) { Text("Open GitHub") }
                        Spacer(Modifier.height(6.dp))
                        FilledTonalButton(enabled = !state.checking, onClick = { AdminSignIn.checkNow(ctx) }) {
                            Text("I've approved it, check now")
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(if (state.checking) "  Checking with GitHub…" else "  Waiting for you to approve…", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { AdminSignIn.cancel(ctx) }) { Text("Cancel") }
                    }
                }
            }
            else -> {
                Button(enabled = state !is AdminSignIn.State.Starting, onClick = { AdminSignIn.start(ctx) }) {
                    Text(if (state is AdminSignIn.State.Starting) "Getting a code…" else "Sign in with GitHub")
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Tip: if you have a laptop handy you can also approve there at github.com/login/device while this screen is open.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        (state as? AdminSignIn.State.Error)?.let {
            Text(it.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun AdminRequests(login: String, onSignOut: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(AdminTab.NEW) }
    val open = remember { mutableStateListOf<AdminApi.Request>() }
    val closed = remember { mutableStateListOf<AdminApi.Request>() }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var action by remember { mutableStateOf<Pair<String, AdminApi.Request>?>(null) }

    fun reload() {
        loading = true
        error = null
        scope.launch {
            try {
                val o = AdminApi.list(ctx, true)
                val c = AdminApi.list(ctx, false)
                open.clear(); open.addAll(o)
                closed.clear(); closed.addAll(c)
            } catch (e: Exception) {
                error = e.message ?: "Couldn't load requests."
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    val shown = when (tab) {
        AdminTab.NEW -> open.filter { "approved" !in it.labels && "needs-info" !in it.labels }
        AdminTab.INFO -> open.filter { "needs-info" in it.labels }
        AdminTab.APPROVED -> open.filter { "approved" in it.labels }
        AdminTab.DONE -> closed.toList()
    }

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Plant requests", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Text("Signed in as $login", style = MaterialTheme.typography.bodySmall)
                }
                if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else IconButton(onClick = { reload() }) { Icon(Icons.Filled.Refresh, "Refresh") }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AdminTab.entries) { t ->
                    val n = when (t) {
                        AdminTab.NEW -> open.count { "approved" !in it.labels && "needs-info" !in it.labels }
                        AdminTab.INFO -> open.count { "needs-info" in it.labels }
                        AdminTab.APPROVED -> open.count { "approved" in it.labels }
                        AdminTab.DONE -> closed.size
                    }
                    FilterChip(selected = tab == t, onClick = { tab = t }, label = { Text("${t.label} ($n)") })
                }
            }
            if (tab == AdminTab.APPROVED) {
                Text(
                    "Approved requests are added to the database by Claude every evening. They close by themselves once they're in.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(8.dp))
        }
        if (shown.isEmpty() && !loading) {
            item { Text("Nothing here right now. 🌱", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp)) }
        }
        items(shown, key = { it.number }) { r ->
            RequestCard(r, tab != AdminTab.DONE) { kind -> action = kind to r }
        }
        item {
            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onSignOut) { Text("Sign out of admin") }
        }
    }

    action?.let { (kind, r) ->
        ActionDialog(kind, r, onDismiss = { action = null }) { text ->
            scope.launch {
                try {
                    when (kind) {
                        "approve" -> AdminApi.approve(ctx, r.number, text)
                        "info" -> AdminApi.askForInfo(ctx, r.number, text)
                        else -> AdminApi.decline(ctx, r.number, text)
                    }
                    reload()
                } catch (e: Exception) {
                    error = e.message ?: "That didn't work. Please try again."
                }
            }
        }
    }
}

@Composable
private fun RequestCard(r: AdminApi.Request, actionable: Boolean, onAction: (String) -> Unit) {
    val ctx = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var comments by remember { mutableStateOf<List<AdminApi.Comment>?>(null) }
    LaunchedEffect(expanded) {
        if (expanded && comments == null && r.comments > 0) comments = runCatching { AdminApi.comments(ctx, r.number) }.getOrNull()
    }
    GardenCard(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Column(Modifier.clickable { expanded = !expanded }.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(r.kindLabel, MaterialTheme.colorScheme.surfaceContainer)
                Spacer(Modifier.size(8.dp))
                Text("#${r.number} · ${r.createdAt}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Text(r.title.replace(Regex("^\\[[^\\]]+\\]\\s*"), ""), style = MaterialTheme.typography.titleMedium)
            Text(
                r.body.lines().filterNot { it.startsWith("**Type:**") || it.startsWith("**Plant:**") }.joinToString("\n").replace("**", "").trim(),
                style = MaterialTheme.typography.bodyMedium, maxLines = if (expanded) 50 else 3,
            )
            if (expanded) {
                comments?.forEach { c ->
                    Spacer(Modifier.height(8.dp))
                    GardenCard(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer) {
                        Text(c.body.replace("**", ""), Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (r.comments > 0) {
                Text("💬 ${r.comments} message${if (r.comments == 1) "" else "s"} · tap to read", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (actionable) {
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if ("approved" !in r.labels) Button(onClick = { onAction("approve") }) { Text("✅ Approve") }
                    FilledTonalButton(onClick = { onAction("info") }) { Text("❓ More info") }
                    OutlinedButton(
                        onClick = { onAction("decline") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Decline") }
                }
            }
        }
    }
}

@Composable
private fun ActionDialog(kind: String, r: AdminApi.Request, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val (title, hint, button) = when (kind) {
        "approve" -> Triple("Approve this request?", "Notes for Claude (optional), e.g. which variety or what to fix", "Approve")
        "info" -> Triple("Ask for more info", "Your question for the person who asked", "Send question")
        else -> Triple("Decline this request?", "Reason (they'll see this)", "Decline")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(r.title, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(text, { text = it }, label = { Text(hint) }, minLines = 3, modifier = Modifier.fillMaxWidth(), colors = gardenFieldColors())
            }
        },
        confirmButton = {
            Button(enabled = kind != "info" || text.isNotBlank(), onClick = { onConfirm(text); onDismiss() }) { Text(button) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
