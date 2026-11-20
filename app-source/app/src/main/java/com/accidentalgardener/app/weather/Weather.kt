package com.accidentalgardener.app.weather

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import android.location.Geocoder
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Locale

data class DayForecast(
    val date: LocalDate,
    val tMax: Double,
    val tMin: Double,
    val rainMm: Double,
    val gustKmh: Double,
    val code: Int,
)

data class MetWarning(
    val id: String,
    val level: String,
    val type: String,
    val headline: String,
    val description: String,
    val expiry: String,
)

data class WeatherSnapshot(
    val fetchedAt: Long,
    val currentTemp: Double,
    val currentCode: Int,
    val windKmh: Double,
    /** Today onwards. */
    val days: List<DayForecast>,
    /** Rain over the previous three days. */
    val pastRain3d: Double,
    val warnings: List<MetWarning>,
)

data class Place(val name: String, val region: String, val country: String, val lat: Double, val lon: Double) {
    val label: String get() = listOf(name, region).filter { it.isNotBlank() }.distinct().joinToString(", ")
}

/**
 * Forecasts and warnings both come from Met Éireann open data (free, no API key).
 * Official warnings come from Met Éireann open data.
 */
class WeatherRepository(ctx: Context) {
    private val appCtx = ctx.applicationContext
    private val prefs = appCtx.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)

    suspend fun fetch(lat: Double, lon: Double): WeatherSnapshot = withContext(Dispatchers.IO) {
        val fc = getForecast(lat, lon)
        val warn = try { get(WARNINGS_URL) } catch (e: Exception) { "[]" }
        val now = System.currentTimeMillis()
        prefs.edit().putString("fc_met", fc).putString("warn", warn).putLong("at", now).apply()
        parse(fc, warn, now, remember = true)
    }

    fun cached(): WeatherSnapshot? {
        val fc = prefs.getString("fc_met", null) ?: return null
        return try {
            parse(fc, prefs.getString("warn", "[]") ?: "[]", prefs.getLong("at", 0), remember = false)
        } catch (e: Exception) {
            null
        }
    }

    /** Town search using Android's built-in geocoder. Free and needs no key. */
    @Suppress("DEPRECATION")
    suspend fun searchPlaces(query: String): List<Place> = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) throw IOException("No geocoder on this phone")
        val results = Geocoder(appCtx, Locale.getDefault()).getFromLocationName(query.trim(), 8) ?: emptyList()
        results.map { a ->
            Place(
                name = a.locality ?: a.subAdminArea ?: a.featureName ?: query.trim(),
                region = a.adminArea ?: a.subAdminArea ?: "",
                country = a.countryName ?: "",
                lat = a.latitude,
                lon = a.longitude,
            )
        }.distinctBy { "${it.name}|${it.region}|${it.country}" }
    }

    /**
     * Finds the phone's approximate location and turns it into a town name.
     * Uses coarse (network) location only and rounds to about 1 km, which is plenty for a forecast.
     */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    suspend fun currentPlace(): Place = withContext(Dispatchers.IO) {
        val lm = appCtx.getSystemService(LocationManager::class.java)
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        var loc: Location? = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        val stale = loc == null || System.currentTimeMillis() - loc.time > 60 * 60 * 1000
        if (stale && Build.VERSION.SDK_INT >= 30 && providers.isNotEmpty()) {
            val fresh = withTimeoutOrNull(15_000) {
                suspendCancellableCoroutine<Location?> { cont ->
                    lm.getCurrentLocation(providers.first(), null, ContextCompat.getMainExecutor(appCtx)) { cont.resume(it) }
                }
            }
            if (fresh != null) loc = fresh
        }
        val found = loc ?: throw IOException("Location not available. Is location turned on?")
        val lat = Math.round(found.latitude * 100) / 100.0
        val lon = Math.round(found.longitude * 100) / 100.0
        val a = runCatching { Geocoder(appCtx, Locale.getDefault()).getFromLocation(lat, lon, 1)?.firstOrNull() }.getOrNull()
        Place(
            name = a?.locality ?: a?.subAdminArea ?: "My location",
            region = a?.adminArea ?: "",
            country = a?.countryName ?: "",
            lat = lat,
            lon = lon,
        )
    }

    /**
     * Met Éireann point forecast (XML). Uses their high-resolution HARMONIE model for the first two
     * days and ECMWF after that. Free under Met Éireann's open data licence with attribution.
     */
    private fun getForecast(lat: Double, lon: Double): String {
        val q = String.format(Locale.US, "lat=%.4f;long=%.4f", lat, lon)
        return try {
            get("https://openaccess.pf.api.met.ie/metno-wdb2ts/locationforecast?$q")
        } catch (e: Exception) {
            // The service has historically been plain http only. Allowed for this one host in network_security_config.
            get("http://openaccess.pf.api.met.ie/metno-wdb2ts/locationforecast?$q")
        }
    }

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 20_000
        conn.setRequestProperty("User-Agent", "AccidentalGardener/1.0 (Android)")
        try {
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private class Instant(val time: ZonedDateTime, val temp: Double?, val windMps: Double?, val gustMps: Double?)
    private class Interval(val from: ZonedDateTime, val to: ZonedDateTime, val rain: Double?, val symbol: Int?)

    private fun parse(xml: String, warn: String, at: Long, remember: Boolean): WeatherSnapshot {
        val zone = ZoneId.systemDefault()
        val instants = mutableListOf<Instant>()
        val intervals = mutableListOf<Interval>()

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))
        var from: ZonedDateTime? = null
        var to: ZonedDateTime? = null
        var temp: Double? = null
        var wind: Double? = null
        var gust: Double? = null
        var rain: Double? = null
        var symbol: Int? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "time" -> {
                        from = parser.getAttributeValue(null, "from")?.let { OffsetDateTime.parse(it).atZoneSameInstant(zone) }
                        to = parser.getAttributeValue(null, "to")?.let { OffsetDateTime.parse(it).atZoneSameInstant(zone) }
                        temp = null; wind = null; gust = null; rain = null; symbol = null
                    }
                    "temperature" -> temp = parser.getAttributeValue(null, "value")?.toDoubleOrNull()
                    "windSpeed" -> wind = parser.getAttributeValue(null, "mps")?.toDoubleOrNull()
                    "windGust" -> gust = parser.getAttributeValue(null, "mps")?.toDoubleOrNull()
                    "precipitation" -> rain = parser.getAttributeValue(null, "value")?.toDoubleOrNull()
                    "symbol" -> symbol = parser.getAttributeValue(null, "number")?.toIntOrNull()
                }
            } else if (event == XmlPullParser.END_TAG && parser.name == "time") {
                val f = from
                val t = to
                if (f != null && t != null) {
                    if (f == t) instants += Instant(f, temp, wind, gust)
                    else intervals += Interval(f, t, rain, symbol)
                }
            }
            event = parser.next()
        }

        // Rain: chain the shortest non-overlapping periods so nothing is counted twice.
        val rainByDay = mutableMapOf<LocalDate, Double>()
        var coveredUntil: ZonedDateTime? = null
        intervals.filter { it.rain != null }
            .groupBy { it.from }
            .entries.sortedBy { it.key.toInstant() }
            .forEach { (start, list) ->
                val shortest = list.minByOrNull { java.time.Duration.between(it.from, it.to) } ?: return@forEach
                val covered = coveredUntil
                if (covered == null || !start.isBefore(covered)) {
                    val d = start.toLocalDate()
                    rainByDay[d] = (rainByDay[d] ?: 0.0) + (shortest.rain ?: 0.0)
                    coveredUntil = shortest.to
                }
            }

        val today = LocalDate.now()
        val days = instants.groupBy { it.time.toLocalDate() }
            .filterKeys { !it.isBefore(today) }
            .entries.sortedBy { it.key.toEpochDay() }
            .take(7)
            .map { (date, list) ->
                val temps = list.mapNotNull { it.temp }
                val gusts = list.mapNotNull { it.gustMps ?: it.windMps?.times(1.5) }
                // Use the weather symbol nearest midday for the day's icon.
                val sym = intervals.filter { it.symbol != null && it.from.toLocalDate() == date }
                    .minByOrNull { kotlin.math.abs(it.from.hour - 12) }?.symbol
                DayForecast(
                    date = date,
                    tMax = temps.maxOrNull() ?: 0.0,
                    tMin = temps.minOrNull() ?: 0.0,
                    rainMm = rainByDay[date] ?: 0.0,
                    gustKmh = (gusts.maxOrNull() ?: 0.0) * 3.6,
                    code = metSymbolToWmo(sym ?: 3),
                )
            }

        // Met Éireann's forecast doesn't include past rain, so keep our own record of each day's total.
        val log = prefs.getStringSet("rainlog", emptySet())!!.mapNotNull {
            val parts = it.split("=")
            if (parts.size == 2) LocalDate.parse(parts[0]) to (parts[1].toDoubleOrNull() ?: 0.0) else null
        }.toMap().toMutableMap()
        if (remember) {
            days.forEach { log[it.date] = it.rainMm }
            val keep = log.filterKeys { !it.isBefore(today.minusDays(10)) }
            prefs.edit().putStringSet("rainlog", keep.map { "${it.key}=${it.value}" }.toSet()).apply()
        }
        val pastRain = (1..3).sumOf { log[today.minusDays(it.toLong())] ?: 0.0 }

        val now = ZonedDateTime.now(zone)
        val current = instants.firstOrNull { !it.time.isBefore(now.minusMinutes(30)) } ?: instants.lastOrNull()
        val currentSym = intervals.firstOrNull { it.symbol != null && !it.to.isBefore(now) }?.symbol

        return WeatherSnapshot(
            fetchedAt = at,
            currentTemp = current?.temp ?: days.firstOrNull()?.tMax ?: 0.0,
            currentCode = metSymbolToWmo(currentSym ?: 3),
            windKmh = (current?.windMps ?: 0.0) * 3.6,
            days = days,
            pastRain3d = pastRain,
            warnings = parseWarnings(warn),
        )
    }

    /** Converts Met Éireann (met.no style) weather symbols to the codes the app's icons use. */
    private fun metSymbolToWmo(n: Int): Int {
        val base = if (n > 100) n - 100 else n // night versions are numbered +100
        return when (base) {
            1 -> 0
            2 -> 1
            3 -> 2
            4 -> 3
            15 -> 45
            40, 46 -> 51
            5, 9, 41 -> 61
            10 -> 63
            6, 11, 14, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34 -> 95
            7, 12, 42, 43, 47, 48 -> 66
            8, 13, 44, 45, 49, 50 -> 71
            else -> 3
        }
    }

    private fun parseWarnings(s: String): List<MetWarning> {
        val t = s.trim()
        if (!t.startsWith("[")) return emptyList()
        val arr = JSONArray(t)
        val now = OffsetDateTime.now()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val expiry = o.optString("expiry")
            val expired = try { OffsetDateTime.parse(expiry).isBefore(now) } catch (e: Exception) { false }
            if (expired) return@mapNotNull null
            MetWarning(
                id = o.optString("id").ifBlank { o.optString("headline") + o.optString("onset") },
                level = o.optString("level").ifBlank { "Yellow" },
                type = o.optString("type"),
                headline = o.optString("headline"),
                description = o.optString("description"),
                expiry = expiry,
            )
        }
    }

    companion object {
        const val WARNINGS_URL = "https://www.met.ie/Open_Data/json/warning_IRELAND.json"
    }
}

object WeatherCodes {
    fun emoji(code: Int): String = when (code) {
        0 -> "☀️"
        1 -> "🌤️"
        2 -> "⛅"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..57 -> "🌦️"
        in 61..67, in 80..82 -> "🌧️"
        in 71..77, 85, 86 -> "🌨️"
        in 95..99 -> "⛈️"
        else -> "🌡️"
    }

    fun describe(code: Int): String = when (code) {
        0 -> "Clear skies"
        1 -> "Mostly sunny"
        2 -> "Sunny spells"
        3 -> "Cloudy"
        45, 48 -> "Foggy"
        in 51..57 -> "Drizzle"
        in 61..65 -> "Rain"
        66, 67 -> "Freezing rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Showers"
        85, 86 -> "Snow showers"
        in 95..99 -> "Thunderstorms"
        else -> "Mixed"
    }
}
