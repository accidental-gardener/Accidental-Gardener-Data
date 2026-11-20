package com.accidentalgardener.app.domain

import com.accidentalgardener.app.data.Bed
import com.accidentalgardener.app.data.BedPlace
import com.accidentalgardener.app.data.covered
import com.accidentalgardener.app.data.isPot
import com.accidentalgardener.app.data.Crop
import com.accidentalgardener.app.data.Family
import com.accidentalgardener.app.data.Garden
import com.accidentalgardener.app.data.Planting
import com.accidentalgardener.app.data.TunnelFit
import java.time.LocalDate
import kotlin.math.roundToInt

data class Schedule(
    val sow: LocalDate,
    val sowIndoors: Boolean,
    val plantOut: LocalDate?,
    val harvestStart: LocalDate,
    val harvestEnd: LocalDate,
)

/**
 * Decides which bed each crop goes into and when to sow it.
 *
 * Scoring per bed
 *  - tunnel lovers go into the tunnel, outdoor crops stay outside
 *  - avoid the same plant family as last year (crop rotation)
 *  - reward good companions, heavily penalise bad neighbours
 *  - spread crops so no bed gets overloaded
 */
object Planner {

    /** Tunnel beds warm up about three weeks earlier than outside. */
    private const val TUNNEL_HEAD_START_DAYS = 21L

    fun schedule(crop: Crop, year: Int, lastFrostDoy: Int, inTunnel: Boolean): Schedule {
        val frost = LocalDate.ofYearDay(year, lastFrostDoy.coerceIn(1, 365))
            .minusDays(if (inTunnel) TUNNEL_HEAD_START_DAYS else 0)

        crop.fixedSow?.let { (month, day) ->
            // Autumn plantings (garlic, bulbs) go in the autumn before the harvest year.
            val sowYear = if (month >= 8) year - 1 else year
            val sow = LocalDate.of(sowYear, month, day)
            val hs = sow.plusDays(crop.daysToHarvest.toLong())
            return Schedule(sow, false, null, hs, hs.plusWeeks(crop.harvestWeeks.toLong()))
        }

        val indoorWeeks = crop.sowIndoorsWeeksBefore
        return if (indoorWeeks != null) {
            val sow = frost.minusWeeks(indoorWeeks.toLong())
            val out = frost.plusWeeks(crop.plantOutWeeksAfter.toLong())
            val hs = out.plusDays(crop.daysToHarvest.toLong())
            Schedule(sow, true, out, hs, hs.plusWeeks(crop.harvestWeeks.toLong()))
        } else {
            val sow = frost.plusWeeks(crop.directWeeksAfter.toLong())
            val hs = sow.plusDays(crop.daysToHarvest.toLong())
            Schedule(sow, false, null, hs, hs.plusWeeks(crop.harvestWeeks.toLong()))
        }
    }

    private fun capacity(bed: Bed, garden: Garden): Int = when {
        bed.isPot -> 1
        else -> bed.sizeM2?.let { (it / 1.2).roundToInt().coerceIn(1, 8) } ?: garden.cropsPerBed.coerceIn(1, 8)
    }

    private fun friends(a: Crop, b: Crop) = b.id in a.companions || a.id in b.companions
    private fun enemies(a: Crop, b: Crop) = b.id in a.avoid || a.id in b.avoid

    /**
     * @param chosen crops to place
     * @param locked plantings already under way this year (they keep their beds)
     * @param lastYear last year's plantings, used for rotation
     */
    fun plan(
        garden: Garden,
        beds: List<Bed>,
        chosen: List<Crop>,
        locked: List<Planting>,
        lastYear: List<Planting>,
        lookup: (String) -> Crop?,
    ): List<Planting> {
        val occupants: Map<Long, MutableList<Crop>> = beds.associate { bed ->
            bed.id to locked.filter { it.bedId == bed.id }.mapNotNull { lookup(it.cropId) }.toMutableList()
        }
        val lastFamilies: Map<Long, Set<Family>> = lastYear
            .filter { it.bedId != null }
            .groupBy { it.bedId!! }
            .mapValues { (_, ps) -> ps.mapNotNull { lookup(it.cropId)?.family }.toSet() }

        // Fussy crops first so they get the best spots.
        val ordered = chosen.sortedWith(
            compareBy<Crop>({ it.tunnel.ordinal }, { if (it.solo) 0 else 1 }, { -it.feed.ordinal }, { it.name })
        )

        val result = mutableListOf<Planting>()
        for (crop in ordered) {
            var bestBed: Bed? = null
            var bestScore = Int.MIN_VALUE
            var bestWhy: List<String> = emptyList()

            for (bed in beds) {
                val here = occupants.getValue(bed.id)
                val cap = capacity(bed, garden)
                if (here.any { it.solo }) continue
                if (crop.solo && here.isNotEmpty()) continue
                if (here.size >= cap) continue
                if (bed.isPot && !crop.potOk) continue
                // Indoors only suits pot-friendly herbs, salads and tender crops, not big outdoor crops.
                if (bed.place == BedPlace.INDOOR && crop.tunnel == TunnelFit.OUTDOOR) continue

                var score = 0
                val why = mutableListOf<String>()

                val warm = if (bed.place == BedPlace.INDOOR) "loves a warm spot indoors" else "loves the warmth of the tunnel"
                when (crop.tunnel) {
                    TunnelFit.NEEDS -> if (bed.covered) { score += 60; why += warm } else score -= 40
                    TunnelFit.LIKES -> if (bed.covered) { score += 20; why += "does better under cover" }
                    TunnelFit.EITHER -> if (bed.covered) score -= 8
                    TunnelFit.OUTDOOR -> if (bed.covered) score -= 50 else { score += 10; why += "happiest outdoors" }
                }
                if (bed.isPot) { score += 4; why += "grows well in a ${crop.potSize.ifBlank { "pot" }}" }

                val prev = lastFamilies[bed.id].orEmpty()
                if (!crop.perennial && crop.family != Family.OTHER) {
                    if (crop.family in prev) {
                        score -= 35
                    } else if (prev.isNotEmpty()) {
                        score += 8; why += "good rotation after last year"
                    }
                }

                for (other in here) {
                    if (enemies(crop, other)) score -= 60
                    else if (friends(crop, other)) { score += 15; why += "good companion for ${other.name.lowercase()}" }
                    if (other.family == crop.family && crop.family != Family.OTHER) {
                        score += 6; why += "keeps the ${crop.family.label.lowercase()} together for rotation"
                    }
                }
                score -= here.size * 4

                if (score > bestScore) {
                    bestScore = score; bestBed = bed; bestWhy = why
                }
            }

            val bed = bestBed
            val reason = when {
                bed == null -> "No free space that suits it. Add a bed or pot, or allow more crops per bed in Settings."
                crop.tunnel == TunnelFit.NEEDS && !bed.covered ->
                    "No tunnel space left so it's going outside. Pick the sunniest sheltered spot."
                bestWhy.isEmpty() -> "Plenty of room here."
                else -> bestWhy.distinct().joinToString(" · ").replaceFirstChar { it.uppercase() }
            }
            if (bed != null) occupants.getValue(bed.id).add(crop)

            val s = schedule(crop, garden.planYear, garden.lastFrostDoy, bed?.covered == true)
            result += Planting(
                year = garden.planYear,
                cropId = crop.id,
                bedId = bed?.id,
                sowDate = s.sow,
                sowIndoors = s.sowIndoors,
                plantOutDate = s.plantOut,
                harvestStart = s.harvestStart,
                harvestEnd = s.harvestEnd,
                reason = reason,
            )
        }
        return result
    }
}
