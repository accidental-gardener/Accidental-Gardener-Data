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
 * Looks up plants that aren't in the plant database.
 *
 * Wikipedia (free, no key) identifies the plant and gives a description, which is
 * used to guess its type, family and whether it's hardy or perennial.
 * If you add a free Perenual API key in Settings, watering and life cycle come from there too.
 * The result fills in the "Add your own plant" form so you can check it before saving.
 */
object PlantLookup {

    data class Match(val title: String, val description: String)

    private const val PREFS = "lookup"

    fun perenualKey(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("perenual", "") ?: ""

    fun setPerenualKey(ctx: Context, key: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("perenual", key.trim()).apply()
    }

    suspend fun search(query: String): List<Match> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode("$query plant", "UTF-8")
        val body = get("https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$q&srlimit=6&format=json")
        val arr = JSONObject(body).optJSONObject("query")?.optJSONArray("search") ?: return@withContext emptyList()
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Match(o.getString("title"), o.optString("snippet").replace(Regex("<[^>]+>"), "").replace("&quot;", "\""))
        }
    }

    /** Builds a draft plant from what's known online. The user checks it before saving. */
    suspend fun draft(ctx: Context, title: String, typedName: String): CustomCrop = withContext(Dispatchers.IO) {
        val path = URLEncoder.encode(title.replace(' ', '_'), "UTF-8")
        val summary = try {
            JSONObject(get("https://en.wikipedia.org/api/rest_v1/page/summary/$path")).optString("extract")
        } catch (e: Exception) {
            ""
        }
        val text = summary.lowercase()
        fun has(vararg words: String) = words.any { it in text }

        val category = when {
            has("bulb", "corm", "rhizome") && has("ornamental", "flower", "garden") -> Category.BULB
            has("herb", "culinary") -> Category.HERB
            has("fruit", "berry", "berries") && !has("vegetable") -> Category.FRUIT
            has("vegetable", "edible", "leaf vegetable", "root vegetable") -> Category.VEG
            else -> Category.FLOWER
        }
        val family = when {
            has("brassicaceae", "cabbage family", "mustard family") -> Family.BRASSICA
            has("solanaceae", "nightshade") -> Family.SOLANACEAE
            has("fabaceae", "legume") -> Family.LEGUME
            has("amaryllidaceae", "allium") -> Family.ALLIUM
            has("cucurbitaceae", "gourd") -> Family.CUCURBIT
            has("apiaceae", "umbellifer") -> Family.UMBELLIFER
            has("amaranthaceae") -> Family.AMARANTH
            has("asteraceae", "daisy family") -> Family.DAISY
            else -> Family.OTHER
        }
        val tender = has("tropical", "subtropical", "frost-tender", "frost tender", "not frost hardy")
        var water = if (has("drought", "dry conditions", "succulent")) Need.LOW else Need.MEDIUM
        var perennial = has("perennial", "shrub", "tree")

        // Optional extra detail from Perenual if the user has a key.
        val key = perenualKey(ctx)
        if (key.isNotBlank()) {
            try {
                val q = URLEncoder.encode(typedName, "UTF-8")
                val list = JSONObject(get("https://perenual.com/api/v2/species-list?key=$key&q=$q")).optJSONArray("data")
                val first = list?.optJSONObject(0)
                if (first != null) {
                    when (first.optString("watering").lowercase()) {
                        "frequent" -> water = Need.HIGH
                        "minimum", "none" -> water = Need.LOW
                    }
                    val cycle = first.optString("cycle").lowercase()
                    if (cycle.isNotBlank()) perennial = "perennial" in cycle
                }
            } catch (e: Exception) {
                // Perenual is optional. Carry on with what we have.
            }
        }

        val shortSummary = summary.split(". ").take(2).joinToString(". ").let { if (it.isNotBlank() && !it.endsWith(".")) "$it." else it }
        CustomCrop(
            name = typedName.trim().replaceFirstChar { it.uppercase() },
            emoji = when (category) {
                Category.VEG -> "🥬"; Category.FRUIT -> "🍓"; Category.HERB -> "🌿"
                Category.FLOWER -> "🌸"; Category.BULB -> "🌷"
            },
            category = category,
            tunnel = if (tender) TunnelFit.LIKES else TunnelFit.EITHER,
            sowIndoors = tender || category == Category.FLOWER,
            weeksFromFrost = if (tender) -6 else -2,
            daysToHarvest = if (perennial) 120 else 70,
            harvestWeeks = if (perennial) 12 else 6,
            frostHardy = !tender && has("hardy"),
            water = water,
            family = family.name,
            perennial = perennial,
            potOk = !has("tree", "large shrub"),
            pollinator = has("bee", "pollinator", "butterfl", "nectar"),
            notes = shortSummary,
            source = "Wikipedia" + if (key.isNotBlank()) " and Perenual" else "",
        )
    }

    /** The plant in the same format as plants.json, ready to share and add to the shared database. */
    fun toJson(c: CustomCrop): String = JSONObject().apply {
        put("id", c.name.lowercase().replace(Regex("[^a-z0-9]"), ""))
        put("name", c.name)
        put("emoji", c.emoji)
        put("category", c.category.name)
        put("family", c.family)
        put("daysToHarvest", c.daysToHarvest)
        put("harvestWeeks", c.harvestWeeks)
        if (c.sowIndoors) {
            put("sowIndoorsWeeksBefore", -c.weeksFromFrost)
            put("plantOutWeeksAfter", maxOf(2, c.weeksFromFrost + 4))
        } else {
            put("directWeeksAfter", c.weeksFromFrost)
        }
        put("frostHardy", c.frostHardy)
        put("tunnel", c.tunnel.name)
        put("water", c.water.name)
        put("perennial", c.perennial)
        put("potOk", c.potOk)
        put("pollinator", c.pollinator)
        put("tip", c.notes)
        put("companions", JSONArray())
        put("avoid", JSONArray())
    }.toString(1)

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", "AccidentalGardener/1.0 (Android gardening app)")
        try {
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
