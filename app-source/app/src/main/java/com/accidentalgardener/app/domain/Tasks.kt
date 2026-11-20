package com.accidentalgardener.app.domain

import com.accidentalgardener.app.data.Crop
import com.accidentalgardener.app.data.GuideSection
import com.accidentalgardener.app.data.Planting
import com.accidentalgardener.app.data.Stage
import java.time.LocalDate

enum class TaskKind(val emoji: String) {
    PREP("🫙"), SOW("🌱"), HARDEN("🌤️"), PLANT_OUT("🪴"), HARVEST("🧺"), SAVE_SEEDS("🌰"), FINISH("🍂")
}

data class GardenTask(val planting: Planting, val crop: Crop, val kind: TaskKind, val date: LocalDate) {
    val title: String
        get() = when (kind) {
            TaskKind.PREP -> crop.howTo.prep?.section?.title ?: "Get ${crop.name.lowercase()} ready"
            TaskKind.SOW -> when {
                planting.sowIndoors -> "Sow ${crop.name.lowercase()} indoors"
                crop.planted || crop.fixedSow != null -> "Plant ${crop.name.lowercase()}"
                else -> "Sow ${crop.name.lowercase()}"
            }
            TaskKind.HARDEN -> "Start hardening off ${crop.name.lowercase()}"
            TaskKind.PLANT_OUT -> "Plant out ${crop.name.lowercase()}"
            TaskKind.HARVEST -> "Start harvesting ${crop.name.lowercase()}"
            TaskKind.SAVE_SEEDS -> if (crop.howTo.seedSaving?.fromSeed != false) "Save seeds from ${crop.name.lowercase()}"
                else "Grow more ${crop.name.lowercase()}"
            TaskKind.FINISH -> "Finish and clear ${crop.name.lowercase()}"
        }

    /** The guide for this job only. Nothing from later stages is shown early. */
    val section: GuideSection
        get() {
            val g = crop.howTo
            return when (kind) {
                TaskKind.PREP -> g.prep?.section ?: GuideSection(title)
                TaskKind.SOW -> g.sow
                TaskKind.HARDEN -> g.harden ?: GuideSection("Harden off")
                TaskKind.PLANT_OUT -> g.plantOut ?: GuideSection("How to plant out")
                TaskKind.HARVEST -> GuideSection("How to harvest", steps = g.harvest)
                TaskKind.SAVE_SEEDS -> g.seedSaving?.let { ss ->
                    ss.section.copy(steps = ss.section.steps + listOfNotNull(ss.note.takeIf { it.isNotBlank() }?.let { "⚠️ $it" }))
                } ?: GuideSection("How to save seeds")
                TaskKind.FINISH -> GuideSection(
                    "Clearing up",
                    steps = listOf(
                        "Pull or cut down the spent plants. Leave the roots of peas and beans in the soil, they add nitrogen.",
                        "Healthy leaves and stems go on the compost heap. Anything diseased goes in the brown bin.",
                        "Spread a layer of compost or cover the bed so it's ready for the next crop.",
                    ),
                )
            }
        }
}

object Tasks {
    private const val HARDEN_DAYS = 7L

    fun next(p: Planting, c: Crop): GardenTask? = when (p.stage) {
        Stage.PLANNED -> {
            val prep = c.howTo.prep
            if (prep != null && !p.prepDone) GardenTask(p, c, TaskKind.PREP, p.sowDate.minusDays(prep.leadDays.toLong()))
            else GardenTask(p, c, TaskKind.SOW, p.sowDate)
        }
        Stage.SOWN -> {
            val out = p.plantOutDate
            when {
                out == null -> GardenTask(p, c, TaskKind.HARVEST, p.harvestStart)
                p.sowIndoors && !p.hardenDone && c.howTo.harden != null ->
                    GardenTask(p, c, TaskKind.HARDEN, out.minusDays(HARDEN_DAYS).let { if (it.isBefore(p.sowDate)) p.sowDate else it })
                else -> GardenTask(p, c, TaskKind.PLANT_OUT, out)
            }
        }
        Stage.PLANTED_OUT -> GardenTask(p, c, TaskKind.HARVEST, p.harvestStart)
        Stage.HARVESTING -> {
            val ss = c.howTo.seedSaving
            if (p.saveSeeds && !p.seedsSaved && ss != null) {
                val date = when (ss.timing) {
                    "HARVEST" -> p.harvestStart.plusDays(14)
                    "NEXT_YEAR" -> p.harvestEnd.plusDays(240)
                    else -> p.harvestEnd.minusDays(7)
                }
                GardenTask(p, c, TaskKind.SAVE_SEEDS, date)
            } else {
                GardenTask(p, c, TaskKind.FINISH, p.harvestEnd)
            }
        }
        Stage.FINISHED -> null
    }

    fun all(plantings: List<Planting>, lookup: (String) -> Crop?): List<GardenTask> =
        plantings.mapNotNull { p -> lookup(p.cropId)?.let { next(p, it) } }.sortedBy { it.date }

    /** The planting after this job is ticked off. */
    fun complete(task: GardenTask): Planting = when (task.kind) {
        TaskKind.PREP -> task.planting.copy(prepDone = true)
        TaskKind.SOW -> task.planting.copy(stage = Stage.SOWN)
        TaskKind.HARDEN -> task.planting.copy(hardenDone = true)
        TaskKind.PLANT_OUT -> task.planting.copy(stage = Stage.PLANTED_OUT, hardenDone = true)
        TaskKind.HARVEST -> task.planting.copy(stage = Stage.HARVESTING)
        TaskKind.SAVE_SEEDS -> task.planting.copy(seedsSaved = true)
        TaskKind.FINISH -> task.planting.copy(stage = Stage.FINISHED)
    }
}

object SeasonalTips {
    private val tips = listOf(
        "Plan the year, order seeds and chit early potatoes on a bright windowsill.",
        "Sow peppers and chillies indoors and give the tunnel a good clean for spring.",
        "Sow tomatoes indoors and plant onion sets once the soil starts to warm.",
        "Plant potatoes and sow carrots, beetroot and peas outside.",
        "Harden off young plants and keep an eye out for late frosts before planting out.",
        "Plant out courgettes and beans and stay on top of the weeds.",
        "Water the tunnel daily in warm spells and feed tomatoes every week.",
        "Harvest little and often and sow winter salads for the tunnel.",
        "Save seeds, lift maincrop potatoes and sow green manure on empty beds.",
        "Plant garlic and spring bulbs and clear away spent crops.",
        "Mulch empty beds, get garlic in if you haven't and storm-proof the tunnel.",
        "Rest up, plan next year's rotation and check on stored crops.",
    )

    fun forMonth(month: Int): String = tips[(month - 1).coerceIn(0, 11)]
}
