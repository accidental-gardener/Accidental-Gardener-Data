package com.accidentalgardener.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Admin tools. Admins sign in with their own GitHub account (device sign-in, no password typed
 * into the app). Anyone with write access to the database repo is an admin, everyone else isn't,
 * so GitHub decides who is allowed and the app never contains a key of its own.
 */
object AdminApi {
    /** Client ID of the "Accidental Gardener Admin" OAuth app on GitHub. Not a secret. */
    const val CLIENT_ID = "Ov23li7eYZSU24hQVrVU"
    val configured: Boolean get() = !CLIENT_ID.startsWith("REPLACE")

    private const val PREFS = "admin"
    private const val API = "https://api.github.com/repos/${Community.REPO}"

    data class DeviceCode(val deviceCode: String, val userCode: String, val verifyUrl: String, val intervalSec: Int, val expiresAt: Long)

    data class Request(
        val number: Int, val title: String, val body: String, val labels: List<String>,
        val createdAt: String, val comments: Int, val open: Boolean,
    ) {
        val kindLabel: String get() = when {
            "plant-request" in labels -> "🌱 New plant"
            "guide-fix" in labels -> "✏️ Fix"
            else -> "💬 Feedback"
        }
    }

    data class Comment(val author: String, val body: String, val at: String)

    fun token(ctx: Context): String? = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("token", null)
    fun login(ctx: Context): String? = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("login", null)
    fun signOut(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()

    // ---------- Sign in ----------

    suspend fun startSignIn(): DeviceCode = withContext(Dispatchers.IO) {
        val o = JSONObject(form("https://github.com/login/device/code", "client_id=$CLIENT_ID&scope=public_repo"))
        DeviceCode(
            o.getString("device_code"), o.getString("user_code"), o.optString("verification_uri", "https://github.com/login/device"),
            o.optInt("interval", 5), System.currentTimeMillis() + o.optInt("expires_in", 900) * 1000L,
        )
    }

    /** Result of asking GitHub once whether the code has been approved yet. */
    sealed interface Check {
        data object Pending : Check
        data object SlowDown : Check
        data object Done : Check
        data class Failed(val message: String) : Check
    }

    /** Asks GitHub once. Saves the sign-in and returns Done when the admin has approved. */
    suspend fun checkOnce(ctx: Context, code: DeviceCode): Check = withContext(Dispatchers.IO) {
        if (System.currentTimeMillis() >= code.expiresAt) return@withContext Check.Failed("The code ran out. Please start again.")
        val o = JSONObject(
            form(
                "https://github.com/login/oauth/access_token",
                "client_id=$CLIENT_ID&device_code=${URLEncoder.encode(code.deviceCode, "UTF-8")}" +
                    "&grant_type=urn:ietf:params:oauth:grant-type:device_code",
            )
        )
        val token = o.optString("access_token")
        if (token.isNotBlank()) {
            val repo = JSONObject(api(token, "", "GET"))
            val canEdit = repo.optJSONObject("permissions")?.optBoolean("push") == true
            if (!canEdit) return@withContext Check.Failed("That GitHub account isn't an admin of the plant database.")
            val user = JSONObject(api(token, "https://api.github.com/user", "GET", absolute = true)).optString("login")
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("token", token).putString("login", user).apply()
            clearPending(ctx)
            return@withContext Check.Done
        }
        when (o.optString("error")) {
            "authorization_pending" -> Check.Pending
            "slow_down" -> Check.SlowDown
            "expired_token" -> Check.Failed("The code ran out. Please start again.")
            "access_denied" -> Check.Failed("Sign-in was cancelled.")
            else -> Check.Failed("Sign-in failed (${o.optString("error")}).")
        }
    }

    // A sign-in waiting for approval is saved, so it carries on even if the app is closed while GitHub is open.
    fun savePending(ctx: Context, c: DeviceCode) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString("p_device", c.deviceCode).putString("p_user", c.userCode).putString("p_url", c.verifyUrl)
        .putInt("p_interval", c.intervalSec).putLong("p_expires", c.expiresAt).apply()

    fun pending(ctx: Context): DeviceCode? {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val device = p.getString("p_device", null) ?: return null
        val c = DeviceCode(device, p.getString("p_user", "") ?: "", p.getString("p_url", "https://github.com/login/device") ?: "",
            p.getInt("p_interval", 5), p.getLong("p_expires", 0))
        return if (System.currentTimeMillis() < c.expiresAt) c else { clearPending(ctx); null }
    }

    fun clearPending(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .remove("p_device").remove("p_user").remove("p_url").remove("p_interval").remove("p_expires").apply()

    // ---------- Requests ----------

    suspend fun list(ctx: Context, open: Boolean): List<Request> = withContext(Dispatchers.IO) {
        val t = token(ctx) ?: throw IOException("Not signed in")
        val a = JSONArray(api(t, "/issues?state=${if (open) "open" else "closed"}&per_page=50&sort=created&direction=desc", "GET"))
        (0 until a.length()).map { a.getJSONObject(it) }
            .filter { !it.has("pull_request") }
            .map { o ->
                Request(
                    o.getInt("number"), o.optString("title"), o.optString("body"),
                    o.optJSONArray("labels")?.let { l -> (0 until l.length()).map { l.getJSONObject(it).optString("name") } } ?: emptyList(),
                    o.optString("created_at").take(10), o.optInt("comments"), o.optString("state") == "open",
                )
            }
    }

    suspend fun comments(ctx: Context, number: Int): List<Comment> = withContext(Dispatchers.IO) {
        val t = token(ctx) ?: return@withContext emptyList()
        val a = JSONArray(api(t, "/issues/$number/comments", "GET"))
        (0 until a.length()).map { a.getJSONObject(it) }.map {
            Comment(it.optJSONObject("user")?.optString("login") ?: "", it.optString("body"), it.optString("created_at").take(10))
        }
    }

    /** Marks a request approved. Claude's evening run picks these up and adds them properly. */
    suspend fun approve(ctx: Context, number: Int, note: String) = withContext(Dispatchers.IO) {
        val t = token(ctx) ?: throw IOException("Not signed in")
        api(t, "/issues/$number/labels", "POST", JSONObject().put("labels", JSONArray().put("approved")).toString())
        runCatching { api(t, "/issues/$number/labels/needs-info", "DELETE") }
        val body = "✅ **Approved** by an admin. It'll be added in the next evening update." +
            if (note.isNotBlank()) "\n\n**Notes for Claude:** ${note.trim()}" else ""
        api(t, "/issues/$number/comments", "POST", JSONObject().put("body", body).toString())
    }

    /** Asks the person who sent it for more details. They see it in the app and can reply. */
    suspend fun askForInfo(ctx: Context, number: Int, question: String) = withContext(Dispatchers.IO) {
        val t = token(ctx) ?: throw IOException("Not signed in")
        api(t, "/issues/$number/comments", "POST", JSONObject().put("body", "${Community.ADMIN_QUESTION}\n\n${question.trim()}").toString())
        api(t, "/issues/$number/labels", "POST", JSONObject().put("labels", JSONArray().put("needs-info")).toString())
    }

    suspend fun decline(ctx: Context, number: Int, reason: String) = withContext(Dispatchers.IO) {
        val t = token(ctx) ?: throw IOException("Not signed in")
        api(t, "/issues/$number/comments", "POST", JSONObject().put("body", "**Not added:** ${reason.trim().ifBlank { "Not suitable for the database." }}").toString())
        api(t, "/issues/$number", "PATCH", JSONObject().put("state", "closed").put("state_reason", "not_planned").toString())
    }

    // ---------- HTTP ----------

    private fun form(url: String, body: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.doOutput = true
        c.setRequestProperty("Accept", "application/json")
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        c.outputStream.use { it.write(body.toByteArray()) }
        try {
            return (if (c.responseCode in 200..299) c.inputStream else c.errorStream).bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    private fun api(token: String, path: String, method: String, body: String? = null, absolute: Boolean = false): String {
        val c = URL(if (absolute) path else API + path).openConnection() as HttpURLConnection
        // Android's built-in HTTP client supports PATCH. Plain Java's doesn't, so fall back to an override header there.
        try {
            c.requestMethod = method
        } catch (e: java.net.ProtocolException) {
            c.requestMethod = "POST"
            c.setRequestProperty("X-HTTP-Method-Override", method)
        }
        c.connectTimeout = 15_000
        c.readTimeout = 20_000
        c.setRequestProperty("Authorization", "Bearer $token")
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        c.setRequestProperty("User-Agent", "AccidentalGardener-Admin")
        if (body != null) {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toByteArray()) }
        }
        try {
            val code = c.responseCode
            if (code == 401) throw IOException("Your admin sign-in has expired. Please sign in again.")
            if (code !in 200..299) throw IOException("GitHub said ${c.responseCode}")
            return c.inputStream?.bufferedReader()?.use { it.readText() } ?: ""
        } finally {
            c.disconnect()
        }
    }
}
