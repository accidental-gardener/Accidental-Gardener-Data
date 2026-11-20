package com.accidentalgardener.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends plant requests and guide fixes to the admins.
 * The request service (a Cloudflare Worker) turns each one into a GitHub Issue,
 * so users never need an account and the app never holds a key to change the database.
 */
object Community {
    /** The request service. Filled in once the Cloudflare Worker is live. */
    const val REQUEST_URL = "https://gardener-requests.accidental-gardener.workers.dev"

    /** The public plant wiki on GitHub Pages. */
    const val WIKI_URL = "https://accidental-gardener.github.io/Accidental-Gardener-Data/"

    enum class Kind(val key: String, val label: String) {
        NEW_PLANT("new-plant", "Please add a new plant"),
        FIX("fix", "Something needs fixing in a guide"),
        OTHER("other", "Other feedback"),
    }

    const val REPO = "accidental-gardener/Accidental-Gardener-Data"
    const val ADMIN_QUESTION = "**Question from the admins:**"
    const val REQUESTER_REPLY = "**Reply from the person who asked:**"

    /** A request this phone has sent, so the person can follow it and answer questions. */
    data class MyRequest(
        val number: Int, val key: String, val kind: String, val plant: String, val sentAt: Long,
        /** Last status we told the user about: WAITING, QUESTION, APPROVED, ADDED, DECLINED. */
        val lastStatus: String = "WAITING",
        /** When it was approved, added or declined. It drops off the list a couple of days later. */
        val settledAt: Long = 0,
    )

    data class Status(val code: String, val label: String, val questions: List<String>, val adminNote: String?)

    private const val PREFS = "community"

    /** Each phone (and each internet address on the server) can send this many requests a day. */
    const val DAILY_LIMIT = 3

    /** Goes up whenever the list of my requests changes, so screens showing it can refresh. */
    val changes = kotlinx.coroutines.flow.MutableStateFlow(0)

    private fun today(): String = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString()

    /** How many requests are left today. The server keeps its own count too, so this is a friendly early warning. */
    fun leftToday(ctx: Context): Int {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p.getString("full_day", null) == today()) return 0
        val used = if (p.getString("count_day", null) == today()) p.getInt("count", 0) else 0
        return (DAILY_LIMIT - used).coerceAtLeast(0)
    }

    private fun countOne(ctx: Context) {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val used = if (p.getString("count_day", null) == today()) p.getInt("count", 0) else 0
        p.edit().putString("count_day", today()).putInt("count", used + 1).apply()
    }

    private fun markFull(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("full_day", today()).apply()

    fun myRequests(ctx: Context): List<MyRequest> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("mine", "[]") ?: "[]"
        return runCatching {
            val a = JSONArray(raw)
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                MyRequest(o.getInt("n"), o.optString("k"), o.optString("kind"), o.optString("plant"), o.optLong("at"), o.optString("st", "WAITING"), o.optLong("sa", 0))
            }
        }.getOrDefault(emptyList())
    }

    /** How long a finished request (approved, added or declined) stays on the person's list. */
    const val KEEP_FINISHED_MS = 2L * 24 * 3600 * 1000
    val FINISHED = setOf("APPROVED", "ADDED", "DECLINED")

    /** Records the latest status, noting when a request was finished, and returns the updated request. */
    fun withStatus(r: MyRequest, code: String): MyRequest = r.copy(
        lastStatus = code,
        settledAt = if (code in FINISHED) r.settledAt.takeIf { it > 0 } ?: System.currentTimeMillis() else 0,
    )

    /** Requests still worth showing: anything in progress, and finished ones for a couple of days. */
    fun visibleRequests(ctx: Context): List<MyRequest> {
        val now = System.currentTimeMillis()
        val all = myRequests(ctx)
        val keep = all.filter { it.settledAt == 0L || now - it.settledAt < KEEP_FINISHED_MS }
        if (keep.size != all.size) saveMyRequests(ctx, keep)
        return keep
    }

    fun saveMyRequests(ctx: Context, list: List<MyRequest>) {
        val a = JSONArray()
        list.takeLast(50).forEach {
            a.put(JSONObject().put("n", it.number).put("k", it.key).put("kind", it.kind).put("plant", it.plant).put("at", it.sentAt).put("st", it.lastStatus).put("sa", it.settledAt))
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("mine", a.toString()).apply()
        changes.value++
    }

    /** Reads a request's progress from GitHub (public, no sign-in needed). */
    suspend fun status(number: Int): Status? = withContext(Dispatchers.IO) {
        runCatching {
            val issue = JSONObject(httpGet("https://api.github.com/repos/$REPO/issues/$number"))
            val labels = issue.optJSONArray("labels")?.let { a -> (0 until a.length()).map { a.getJSONObject(it).optString("name") } } ?: emptyList()
            val comments = if (issue.optInt("comments") > 0) JSONArray(httpGet("https://api.github.com/repos/$REPO/issues/$number/comments")) else JSONArray()
            val bodies = (0 until comments.length()).map { comments.getJSONObject(it).optString("body") }
            val questions = bodies.filter { it.startsWith(ADMIN_QUESTION) }.map { it.removePrefix(ADMIN_QUESTION).trim() }
            val note = bodies.lastOrNull { it.startsWith("**Not added:**") }?.removePrefix("**Not added:**")?.trim()
            val closed = issue.optString("state") == "closed"
            val code = when {
                closed && issue.optString("state_reason") == "not_planned" -> "DECLINED"
                closed -> "ADDED"
                "needs-info" in labels -> "QUESTION"
                "approved" in labels -> "APPROVED"
                else -> "WAITING"
            }
            val label = when (code) {
                "DECLINED" -> "Not added"
                "ADDED" -> "Added to the database 🎉"
                "QUESTION" -> "The admins have a question"
                "APPROVED" -> "Approved, coming in the next update"
                else -> "Waiting for the admins"
            }
            Status(code, label, questions, note)
        }.getOrNull()
    }

    /** Answers the admins' question on a request this phone sent. */
    suspend fun reply(r: MyRequest, message: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$REQUEST_URL/reply").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use {
                it.write(JSONObject().put("number", r.number).put("key", r.key).put("message", message.trim()).toString().toByteArray())
            }
            val ok = conn.responseCode in 200..299
            conn.disconnect()
            if (ok) true to "Thanks, your answer has been sent." else false to "Couldn't send your answer. Please try again later."
        } catch (e: Exception) {
            false to "Couldn't send that. Please check your internet connection."
        }
    }

    private fun httpGet(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 20_000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("User-Agent", "AccidentalGardener/1.2 (Android)")
        try {
            if (c.responseCode !in 200..299) throw java.io.IOException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    /** Returns a friendly message either way. Saves the request on this phone so it can be followed. */
    suspend fun send(ctx: Context, kind: Kind, plant: String, details: String, from: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (leftToday(ctx) <= 0) return@withContext false to "You've used your $DAILY_LIMIT requests for today. Please try again tomorrow."
        try {
            val conn = URL(REQUEST_URL).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "AccidentalGardener/1.2 (Android)")
            val body = JSONObject()
                .put("kind", kind.key).put("plant", plant.trim()).put("details", details.trim())
                .put("from", from.trim()).put("source", "app").toString()
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            conn.disconnect()
            if (code == 429) markFull(ctx)
            if (code in 200..299) {
                countOne(ctx)
                runCatching {
                    val o = JSONObject(text)
                    val n = o.optInt("number")
                    if (n > 0) saveMyRequests(ctx, myRequests(ctx) + MyRequest(n, o.optString("key"), kind.key, plant.trim(), System.currentTimeMillis()))
                }
                true to "Thanks! Your request has been sent to the admins. You can follow it in About, under My requests."
            }
            else false to (runCatching { JSONObject(text).optString("error") }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: "Couldn't send that just now. Please try again later.")
        } catch (e: Exception) {
            false to "Couldn't send that. Please check your internet connection."
        }
    }
}
