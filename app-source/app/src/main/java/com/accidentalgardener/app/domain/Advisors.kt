package com.accidentalgardener.app.domain

import com.accidentalgardener.app.data.Area
import com.accidentalgardener.app.data.Bed
import com.accidentalgardener.app.data.BedPlace
import com.accidentalgardener.app.data.isPot
import com.accidentalgardener.app.data.Crop
import com.accidentalgardener.app.data.EntryType
import com.accidentalgardener.app.data.JournalEntry
import com.accidentalgardener.app.data.Need
import com.accidentalgardener.app.data.Planting
import com.accidentalgardener.app.data.Stage
import com.accidentalgardener.app.weather.WeatherSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

fun Double.r(): String = roundToInt().toString()

enum class Severity { INFO, WATCH, WARNING }

data class GardenAlert(
    val key: String,
    val severity: Severity,
    val emoji: String,
    val title: String,
    val message: String,
)

/** Turns the forecast into plain-English garden alerts. */
object GardenAlerts {

    fun dayName(d: LocalDate, today: LocalDate): String = when (d) {
        today -> "today"
        today.plusDays(1) -> "tomorrow"
        else -> "on " + d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    }

    fun evaluate(s: WeatherSnapshot, hasTunnel: Boolean, today: LocalDate = LocalDate.now()): List<GardenAlert> {
        val out = mutableListOf<GardenAlert>()

        s.warnings.forEach { w ->
            val sev = when (w.level.lowercase()) {
                "red", "orange" -> Severity.WARNING
                else -> Severity.WATCH
            }
            out += GardenAlert(
                key = "met-${w.id}",
                severity = sev,
                emoji = "⚠️",
                title = "Met Éireann ${w.level} ${w.type} warning".replace("  ", " ").trim(),
                message = w.headline.ifBlank { w.description },
            )
        }

        val next = s.days.filter { !it.date.isBefore(today) }.take(3)
        next.forEachIndexed { i, d ->
            val day = dayName(d.date, today)
            if (d.tMin <= 1.0) {
                out += GardenAlert(
                    "frost-${d.date}", if (d.tMin <= -2) Severity.WARNING else Severity.WATCH, "❄️",
                    "Frost risk $day",
                    "Lows of ${d.tMin.r()}°C. Cover tender plants with fleece" +
                        (if (hasTunnel) ", close the tunnel doors" else "") +
                        " and hold off on planting out.",
                )
            }
            if (d.gustKmh >= 60) {
                out += GardenAlert(
                    "wind-${d.date}", if (d.gustKmh >= 80) Severity.WARNING else Severity.WATCH, "💨",
                    "Strong winds $day",
                    "Gusts up to ${d.gustKmh.r()} km/h. " +
                        (if (hasTunnel) "Close the tunnel and check the cover is tight. " else "") +
                        "Stake tall plants and tie in climbers.",
                )
            }
            if (d.rainMm >= 15) {
                out += GardenAlert(
                    "rain-${d.date}", Severity.WATCH, "🌧️",
                    "Heavy rain $day",
                    "Around ${d.rainMm.r()} mm expected. Skip watering outside and check the beds drain well.",
                )
            }
            if (d.tMax >= 24) {
                out += GardenAlert(
                    "heat-${d.date}", Severity.WATCH, "🌡️",
                    "Hot day $day",
                    "Up to ${d.tMax.r()}°C. " +
                        (if (hasTunnel) "Open the tunnel early so it doesn't overheat. " else "") +
                        "Water in the morning or evening rather than midday.",
                )
            }
            if (i + 1 < next.size) {
                val n = next[i + 1]
                if (d.tMax - n.tMax >= 7) {
                    out += GardenAlert(
                        "drop-${n.date}", Severity.WATCH, "📉",
                        "Sharp drop in temperature",
                        "Highs fall from ${d.tMax.r()}°C to ${n.tMax.r()}°C ${dayName(n.date, today)}. " +
                            "Young plants may need extra cover.",
                    )
                }
            }
        }
        return out.sortedByDescending { it.severity.ordinal }
    }
}

data class WaterAdvice(
    val area: String,
    val title: String,
    val emoji: String,
    val needed: Boolean,
    val message: String,
)

/** Decides whether each group of beds needs watering today. */
object WaterAdvisor {

    private val growing = setOf(Stage.SOWN, Stage.PLANTED_OUT, Stage.HARVESTING)

    fun advise(
        s: WeatherSnapshot?,
        beds: List<Bed>,
        plantings: List<Planting>,
        journal: List<JournalEntry>,
        lookup: (String) -> Crop?,
        today: LocalDate = LocalDate.now(),
    ): List<WaterAdvice> {
        val out = mutableListOf<WaterAdvice>()
        val outdoor = beds.filter { it.place == BedPlace.OUTDOOR }
        val tunnel = beds.filter { it.place == BedPlace.TUNNEL }
        val indoor = beds.filter { it.place == BedPlace.INDOOR }
        val outdoorPots = outdoor.any { it.isPot }
        val todayF = s?.days?.firstOrNull { it.date == today }

        fun daysSince(group: List<Bed>, area: String): Long? {
            val ids = group.map { it.id }.toSet()
            val last = journal
                .filter { e ->
                    e.type == EntryType.WATERED &&
                        (e.bedId in ids || (e.bedId == null && (e.area == Area.ALL || e.area == area)))
                }
                .maxOfOrNull { it.timestamp } ?: return null
            val d = Instant.ofEpochMilli(last).atZone(ZoneId.systemDefault()).toLocalDate()
            return ChronoUnit.DAYS.between(d, today)
        }

        fun thirsty(group: List<Bed>): String {
            val ids = group.map { it.id }.toSet()
            val names = plantings
                .filter { it.bedId in ids && it.stage in growing }
                .mapNotNull { lookup(it.cropId) }
                .filter { it.water == Need.HIGH }
                .map { it.name.lowercase() }
                .distinct()
            return if (names.isEmpty()) "" else " Start with the thirsty ones (${names.joinToString(" and ")})."
        }

        if (outdoor.isNotEmpty()) {
            val since = daysSince(outdoor, Area.OUTDOOR)
            val rainSoon = s?.days?.filter { !it.date.isBefore(today) }?.take(2)?.sumOf { it.rainMm } ?: 0.0
            val (need, msg) = when {
                s == null -> false to "Connect to the internet to get watering advice based on the weather."
                s.pastRain3d >= 10 -> false to "${s.pastRain3d.r()} mm of rain fell in the last three days so the soil should still be moist."
                rainSoon >= 5 -> false to "About ${rainSoon.r()} mm of rain is on the way so let nature do the watering."
                todayF != null && todayF.tMax < 8 -> false to "It's cold so plants are barely growing. No watering needed."
                todayF != null && todayF.tMax >= 20 && (since == null || since >= 1) ->
                    true to "Warm and dry. Give the beds a good soak this evening."
                (since == null || since >= 4) && s.pastRain3d < 3 ->
                    true to "It's been dry for a while. Water at the base of the plants rather than over the leaves."
                else -> false to "Should be fine today. Push a finger into the soil and water if it's dry 3 cm down."
            }
            val potNote = if (outdoorPots && (todayF?.tMax ?: 0.0) >= 16) " Pots dry out much faster than beds, so check them every day." else ""
            out += WaterAdvice(
                Area.OUTDOOR, if (outdoor.all { it.isPot }) "Outdoor pots" else "Outdoor beds", "☀️",
                need, msg + (if (need) thirsty(outdoor) else "") + potNote,
            )
        }

        if (tunnel.isNotEmpty()) {
            val t = todayF?.tMax ?: 14.0
            val interval = when {
                t >= 22 -> 1L
                t >= 15 -> 2L
                t >= 8 -> 3L
                else -> 6L
            }
            val since = daysSince(tunnel, Area.TUNNEL)
            val (need, msg) = when {
                since == null -> true to "No watering logged yet. Tunnel beds get no rain so check them today."
                since >= interval -> true to "Last watered $since days ago. Tunnel beds get no rain so give them a drink today."
                since == 0L -> false to "Watered today. Next drink due in about $interval days."
                else -> false to "Watered $since day${if (since == 1L) "" else "s"} ago. Next drink due in about ${interval - since} day${if (interval - since == 1L) "" else "s"}."
            }
            out += WaterAdvice(Area.TUNNEL, "Tunnel beds", "⛺", need, msg + if (need) thirsty(tunnel) else "")
        }
        if (indoor.isNotEmpty()) {
            val since = daysSince(indoor, Area.INDOOR)
            val (need, msg) = when {
                since == null -> true to "Indoor pots get no rain. Push a finger into the compost and water if the top is dry."
                since >= 3 -> true to "Last watered $since days ago. Check the compost and water if it feels dry."
                else -> false to "Watered recently. Most indoor pots need a drink every three or four days."
            }
            out += WaterAdvice(Area.INDOOR, "Indoor pots", "🏠", need, msg)
        }
        return out
    }
}
