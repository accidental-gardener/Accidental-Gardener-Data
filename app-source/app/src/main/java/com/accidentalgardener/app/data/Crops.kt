package com.accidentalgardener.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

enum class Category(val label: String, val emoji: String) {
    VEG("Veg", "🥕"),
    FRUIT("Fruit", "🍓"),
    HERB("Herbs", "🌿"),
    FLOWER("Flowers", "🌸"),
    BULB("Bulbs", "🌷"),
}

/** Flowering seasons, used for the pollinator guide. */
enum class Season(val label: String, val emoji: String) {
    WINTER("Late winter", "❄️"),
    SPRING("Spring", "🌸"),
    SUMMER("Summer", "☀️"),
    AUTUMN("Autumn", "🍂"),
}

/** Plant families matter for crop rotation. */
enum class Family(val label: String) {
    BRASSICA("Cabbage family"),
    ALLIUM("Onion family"),
    LEGUME("Peas and beans"),
    SOLANACEAE("Tomato and potato family"),
    CUCURBIT("Squash family"),
    UMBELLIFER("Carrot family"),
    AMARANTH("Beet and chard family"),
    DAISY("Lettuce and daisy family"),
    OTHER("Other"),
}

enum class Need(val label: String) { LOW("Low"), MEDIUM("Medium"), HIGH("High") }

/** How well a crop suits a polytunnel in an Irish climate. */
enum class TunnelFit(val label: String) {
    NEEDS("Needs a tunnel"),
    LIKES("Likes a tunnel"),
    EITHER("Inside or out"),
    OUTDOOR("Best outdoors"),
}

/** One stage of the growing guide, shown only when that job is due. */
data class GuideSection(
    val title: String,
    val facts: List<Pair<String, String>> = emptyList(),
    val steps: List<String> = emptyList(),
    /** How the bed should look, for sowing direct or planting out. */
    val layout: String? = null,
)

/** Something to do before sowing, like soaking, chilling in the fridge or chitting. */
data class Prep(val kind: String, val leadDays: Int, val section: GuideSection)

/** How to save seed (or grow more plants another way). */
data class SeedSaving(
    /** Easy, Moderate, Hard, Not practical or Not recommended. */
    val difficulty: String,
    /** HARVEST, END, NEXT_YEAR or NONE. */
    val timing: String,
    val storeYears: Int,
    val note: String,
    val section: GuideSection,
) {
    val fromSeed: Boolean get() = timing != "NONE"
}

data class Guide(
    val spacingCm: Int,
    val rowCm: Int,
    val prep: Prep?,
    val sow: GuideSection,
    val harden: GuideSection?,
    val plantOut: GuideSection?,
    val care: List<String>,
    val harvest: List<String>,
    val seedSaving: SeedSaving? = null,
)

data class Crop(
    val id: String,
    val name: String,
    val emoji: String,
    val category: Category,
    val family: Family,
    /** Days from planting out (or direct sowing) to first harvest. */
    val daysToHarvest: Int,
    /** How many weeks you can keep harvesting. */
    val harvestWeeks: Int,
    /** Weeks before last frost to sow in trays indoors. Null = sow or plant straight into the bed. */
    val sowIndoorsWeeksBefore: Int? = null,
    /** Weeks after last frost to plant out (negative = before frost). */
    val plantOutWeeksAfter: Int = 0,
    /** Weeks after last frost to sow or plant direct (negative = before frost). */
    val directWeeksAfter: Int = 0,
    /** Fixed planting date (month, day) for things like garlic, bulbs and fruit bushes. */
    val fixedSow: Pair<Int, Int>? = null,
    val frostHardy: Boolean = false,
    val tunnel: TunnelFit = TunnelFit.EITHER,
    val water: Need = Need.MEDIUM,
    val feed: Need = Need.MEDIUM,
    val perennial: Boolean = false,
    /** Grown from sets, tubers, crowns, bulbs or plants rather than seed. */
    val planted: Boolean = false,
    /** Wants a bed to itself (spreads, or needs special soil). */
    val solo: Boolean = false,
    val companions: Set<String> = emptySet(),
    val avoid: Set<String> = emptySet(),
    val tip: String = "",
    val guide: Guide? = null,
    /** Happy growing in a pot or container. */
    val potOk: Boolean = true,
    val potSize: String = "",
    /** Good for bees, butterflies and hoverflies. */
    val pollinator: Boolean = false,
    val bloom: Set<Season> = emptySet(),
    /** When it flowers, e.g. "Mar to May". */
    val flowers: String = "",
) {
    /** The stored guide, or a sensible general one for plants without one. */
    val howTo: Guide get() = guide ?: GenericGuide.forCrop(this)
}

data class Catalog(val version: Int, val updated: String, val source: String, val plants: List<Crop>)

/**
 * The plant database. A copy ships inside the app (assets/plants.json) so it
 * works offline. A newer copy can be downloaded from the internet and is kept
 * on the phone, so updates don't need a new app release.
 */
object CropCatalog {
    private const val CACHE_FILE = "plants.json"
    private const val PREFS = "guides"

    /** The shared plant database on GitHub. Users can override it in Settings. */
    const val DEFAULT_URL = "https://raw.githubusercontent.com/accidental-gardener/Accidental-Gardener-Data/main/plants.json"

    private val _state = MutableStateFlow(Catalog(0, "", "none", emptyList()))
    val state: StateFlow<Catalog> = _state.asStateFlow()
    val all: List<Crop> get() = _state.value.plants

    @Volatile private var byId: Map<String, Crop> = emptyMap()
    @Volatile private var loaded = false

    fun init(ctx: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val bundled = try {
                CatalogParser.parse(ctx.assets.open(CACHE_FILE).bufferedReader().use { it.readText() }, "Built in")
            } catch (e: Exception) {
                null
            }
            val cached = try {
                File(ctx.filesDir, CACHE_FILE).takeIf { it.exists() }?.readText()?.let { CatalogParser.parse(it, "Downloaded") }
            } catch (e: Exception) {
                null
            }
            val best = listOfNotNull(bundled, cached).filter { it.plants.isNotEmpty() }.maxByOrNull { it.version }
            if (best != null) publish(best)
            loaded = true
        }
    }

    private fun publish(c: Catalog) {
        byId = c.plants.associateBy { it.id }
        _state.value = c
    }

    fun get(id: String): Crop? = byId[id]

    fun resolve(id: String, customs: List<CustomCrop>): Crop? =
        byId[id] ?: customs.firstOrNull { it.cropId == id }?.toCrop()

    // ----- where updates come from -----

    fun sourceUrl(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("url", DEFAULT_URL) ?: DEFAULT_URL

    fun setSourceUrl(ctx: Context, url: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("url", url.trim()).apply()
    }

    /** Plants added by a recent update. Shown with a "New" badge for a week. */
    fun newIds(ctx: Context): Set<String> = newStamps(ctx).keys

    /** Each new plant with the time it arrived, dropping any that arrived more than a week ago. */
    private fun newStamps(ctx: Context): Map<String, Long> {
        val week = 7L * 24 * 3600 * 1000
        val now = System.currentTimeMillis()
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("new_ids", "") ?: ""
        return raw.split(",").mapNotNull { e ->
            val id = e.substringBefore(":").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val at = e.substringAfter(":", "").toLongOrNull() ?: return@mapNotNull null
            if (now - at < week) id to at else null
        }.toMap()
    }

    fun lastChecked(ctx: Context): Long =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("checked", 0)

    /** Downloads the guide file. Returns a short message for the user. */
    suspend fun refresh(ctx: Context): String = withContext(Dispatchers.IO) {
        init(ctx)
        val url = sourceUrl(ctx)
        if (url.isBlank()) return@withContext "Add a guide link first."
        // Check the tiny version file first so the full database is only downloaded when it has changed.
        val remoteVersion = try {
            JSONObject(download(url.substringBeforeLast('/') + "/version.json")).optInt("version", -1)
        } catch (e: Exception) {
            -1
        }
        if (remoteVersion in 0.._state.value.version) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("checked", System.currentTimeMillis()).apply()
            return@withContext "Already up to date (version ${_state.value.version})."
        }
        val text = try {
            download(url)
        } catch (e: Exception) {
            return@withContext "Couldn't download the guides. Check the link and your connection."
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("checked", System.currentTimeMillis()).apply()
        val c = try {
            CatalogParser.parse(text, "Downloaded")
        } catch (e: Exception) {
            return@withContext "That file isn't a valid plant guide file."
        }
        if (c.plants.isEmpty()) return@withContext "The downloaded file has no plants in it."
        if (c.version <= _state.value.version) return@withContext "Already up to date (version ${_state.value.version})."
        File(ctx.filesDir, CACHE_FILE).writeText(text)
        // Remember which plants are new so the Seed tin can point them out for a week.
        val before = byId.keys
        val added = c.plants.map { it.id }.filter { it !in before }
        if (added.isNotEmpty() && before.isNotEmpty()) {
            val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val keep = newStamps(ctx) + added.associateWith { now }
            p.edit().putString("new_ids", keep.entries.joinToString(",") { "${it.key}:${it.value}" }).apply()
        }
        publish(c)
        if (added.isNotEmpty() && before.isNotEmpty())
            "New in the Seed tin: " + added.mapNotNull { id -> c.plants.firstOrNull { it.id == id }?.name }.joinToString(", ") + "."
        else "Updated to version ${c.version} with ${c.plants.size} plants."
    }

    private fun download(url: String): String {
        // GitHub keeps files cached for about 5 minutes. A changing query string asks for a fresh copy,
        // so a plant added a minute ago shows up straight away.
        val fresh = url + (if ('?' in url) "&" else "?") + "t=" + System.currentTimeMillis() / 1000
        val conn = URL(fresh).openConnection() as HttpURLConnection
        conn.useCaches = false
        conn.setRequestProperty("Cache-Control", "no-cache")
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.setRequestProperty("User-Agent", "AccidentalGardener/1.0 (Android)")
        try {
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}

/** Reads plants.json. Unknown or broken entries are skipped rather than crashing. */
object CatalogParser {

    fun parse(text: String, source: String): Catalog {
        val root = JSONObject(text)
        val arr = root.getJSONArray("plants")
        val plants = (0 until arr.length()).mapNotNull { i ->
            try { crop(arr.getJSONObject(i)) } catch (e: Exception) { null }
        }
        return Catalog(root.optInt("version", 0), root.optString("updated"), source, plants)
    }

    private inline fun <reified T : Enum<T>> enumOr(s: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == s } ?: default

    private fun strings(a: JSONArray?): List<String> =
        if (a == null) emptyList() else (0 until a.length()).map { a.getString(it) }

    private fun intOrNull(o: JSONObject, key: String): Int? =
        if (!o.has(key) || o.isNull(key)) null else o.getInt(key)

    private fun crop(o: JSONObject): Crop = Crop(
        id = o.getString("id"),
        name = o.getString("name"),
        emoji = o.optString("emoji", "🌱"),
        category = enumOr(o.optString("category"), Category.VEG),
        family = enumOr(o.optString("family"), Family.OTHER),
        daysToHarvest = o.optInt("daysToHarvest", 60),
        harvestWeeks = o.optInt("harvestWeeks", 4),
        sowIndoorsWeeksBefore = intOrNull(o, "sowIndoorsWeeksBefore"),
        plantOutWeeksAfter = o.optInt("plantOutWeeksAfter", 0),
        directWeeksAfter = o.optInt("directWeeksAfter", 0),
        fixedSow = o.optJSONArray("fixedSow")?.let { it.getInt(0) to it.getInt(1) },
        frostHardy = o.optBoolean("frostHardy"),
        tunnel = enumOr(o.optString("tunnel"), TunnelFit.EITHER),
        water = enumOr(o.optString("water"), Need.MEDIUM),
        feed = enumOr(o.optString("feed"), Need.MEDIUM),
        perennial = o.optBoolean("perennial"),
        planted = o.optBoolean("planted"),
        solo = o.optBoolean("solo"),
        companions = strings(o.optJSONArray("companions")).toSet(),
        avoid = strings(o.optJSONArray("avoid")).toSet(),
        tip = o.optString("tip"),
        guide = o.optJSONObject("guide")?.let { guide(it) },
        potOk = o.optBoolean("potOk", true),
        potSize = o.optString("potSize"),
        pollinator = o.optBoolean("pollinator"),
        bloom = strings(o.optJSONArray("bloom")).mapNotNull { n -> Season.entries.firstOrNull { it.name == n } }.toSet(),
        flowers = o.optString("flowers"),
    )

    private fun section(o: JSONObject?): GuideSection? {
        if (o == null) return null
        val facts = o.optJSONArray("facts")?.let { a ->
            (0 until a.length()).mapNotNull { i -> a.optJSONArray(i)?.let { it.getString(0) to it.getString(1) } }
        } ?: emptyList()
        return GuideSection(
            title = o.optString("title"),
            facts = facts,
            steps = strings(o.optJSONArray("steps")),
            layout = o.optString("layout").takeIf { it.isNotBlank() },
        )
    }

    private fun guide(o: JSONObject): Guide = Guide(
        spacingCm = o.optInt("spacingCm", 30),
        rowCm = o.optInt("rowCm", 30),
        prep = o.optJSONObject("prep")?.let { p ->
            section(p.optJSONObject("section"))?.let { Prep(p.optString("kind"), p.optInt("leadDays", 1), it) }
        },
        sow = section(o.optJSONObject("sow")) ?: GuideSection("How to sow"),
        harden = section(o.optJSONObject("harden")),
        plantOut = section(o.optJSONObject("plantOut")),
        care = strings(o.optJSONArray("care")),
        harvest = strings(o.optJSONArray("harvest")),
        seedSaving = o.optJSONObject("seedSaving")?.let { ss ->
            section(ss.optJSONObject("section"))?.let { sec ->
                SeedSaving(ss.optString("difficulty"), ss.optString("timing", "END"), ss.optInt("storeYears"), ss.optString("note"), sec)
            }
        },
    )
}

/** A general guide for plants that don't have their own, such as ones the user adds. */
object GenericGuide {
    private val harden = GuideSection(
        "Harden off",
        steps = listOf(
            "About a week before planting out, start getting the plants used to life outside.",
            "Put them out in a sheltered spot for a few hours on the first days and bring them in at night.",
            "Leave them out a little longer each day, then overnight for the last couple of nights unless frost is forecast.",
        ),
    )

    fun forCrop(c: Crop): Guide {
        val indoor = c.sowIndoorsWeeksBefore != null
        val sow = when {
            c.planted -> GuideSection(
                "How to plant",
                steps = listOf(
                    "Fork over the bed and work in some compost.",
                    "Plant at the same depth it was growing, or as the packet or label says.",
                    "Water in well.",
                ),
            )
            indoor -> GuideSection(
                "How to sow indoors",
                facts = listOf("Depth" to "about twice the seed's width"),
                steps = listOf(
                    "Fill small pots or modules with seed compost and water them first.",
                    "Sow two seeds per pot at about twice the width of the seed deep. Tiny seeds go on the surface.",
                    "Keep them somewhere bright and warm and keep the compost damp.",
                    "When they come up, keep the strongest seedling in each pot.",
                    "Check the seed packet for anything special like soaking or chilling.",
                ),
            )
            else -> GuideSection(
                "How to sow",
                facts = listOf("Depth" to "about twice the seed's width"),
                steps = listOf(
                    "Rake the soil fine and remove weeds and stones.",
                    "Sow thinly in a drill about twice the seed's width deep.",
                    "Cover, firm gently and water.",
                    "Thin seedlings to the spacing on the packet.",
                ),
            )
        }
        val plantOut = if (indoor) GuideSection(
            "How to plant out",
            steps = listOf(
                "Water the plants an hour before moving them.",
                "Plant at the same depth they were growing at the spacing on the packet.",
                "Firm in gently and water well.",
            ),
        ) else null
        return Guide(
            spacingCm = 30, rowCm = 30, prep = null, sow = sow,
            harden = if (indoor) harden else null, plantOut = plantOut,
            care = listOf("Keep weeded and water in dry spells."),
            harvest = listOf("Harvest when it looks ready. The seed packet will give you a guide."),
            seedSaving = SeedSaving(
                difficulty = "Varies", timing = "END", storeYears = 3,
                note = "Only save seed from open-pollinated or heritage varieties. F1 hybrids don't come true from seed.",
                section = GuideSection(
                    "How to save seeds",
                    steps = listOf(
                        "Leave a few of the best plants or fruits to fully ripen and dry on the plant.",
                        "Collect the seed heads or pods on a dry day into a paper bag.",
                        "Dry them somewhere airy for a week, then clean the seed and store in a labelled envelope somewhere cool and dry.",
                    ),
                ),
            ),
        )
    }
}

val CustomCrop.cropId: String get() = "custom_$id"

fun CustomCrop.toCrop(): Crop = Crop(
    id = cropId,
    name = name,
    emoji = emoji,
    category = category,
    family = Family.entries.firstOrNull { it.name == family } ?: Family.OTHER,
    perennial = perennial,
    potOk = potOk,
    pollinator = pollinator,
    planted = category == Category.BULB,
    daysToHarvest = daysToHarvest,
    harvestWeeks = harvestWeeks,
    sowIndoorsWeeksBefore = if (sowIndoors) -weeksFromFrost else null,
    plantOutWeeksAfter = if (sowIndoors) maxOf(2, weeksFromFrost + 4) else 0,
    directWeeksAfter = if (sowIndoors) 0 else weeksFromFrost,
    frostHardy = frostHardy,
    tunnel = tunnel,
    water = water,
    tip = notes.ifBlank { "Your own plant." },
)
