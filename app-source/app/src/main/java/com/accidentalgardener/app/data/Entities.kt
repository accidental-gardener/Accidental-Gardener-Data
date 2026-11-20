package com.accidentalgardener.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/** The single garden profile (always id = 1). */
@Entity(tableName = "garden")
data class Garden(
    @PrimaryKey val id: Int = 1,
    val userName: String,
    val town: String,
    val latitude: Double,
    val longitude: Double,
    /** Day of the year of the average last frost. 121 is about 1 May. */
    val lastFrostDoy: Int = 121,
    /** How many different crops share one bed when the bed size isn't known. */
    val cropsPerBed: Int = 2,
    val planYear: Int,
    val notificationsOn: Boolean = true,
    /** LARGE, SMALL or BALCONY. Used to tailor setup and advice. */
    val spaceType: String = "SMALL",
    /** Size of the garden map in metres. */
    val mapWidthM: Float = 10f,
    val mapLengthM: Float = 14f,
)

enum class BedKind(val label: String) { BED("Bed"), POT("Pot") }

enum class BedPlace(val label: String, val emoji: String) {
    OUTDOOR("Outside", "☀️"),
    TUNNEL("Tunnel", "⛺"),
    INDOOR("Indoors", "🏠"),
}

/** One planting area. Size is optional. */
@Entity(tableName = "beds")
data class Bed(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** True when it's in the tunnel. Kept in step with place. */
    val isTunnel: Boolean,
    val sizeM2: Double? = null,
    val position: Int,
    val kind: BedKind = BedKind.BED,
    val place: BedPlace = if (isTunnel) BedPlace.TUNNEL else BedPlace.OUTDOOR,
    /** Where it sits on the garden map, in metres. Negative means not placed yet. */
    val mapX: Float = -1f,
    val mapY: Float = -1f,
    val mapW: Float = if (kind == BedKind.POT) 0.5f else 1.2f,
    val mapH: Float = if (kind == BedKind.POT) 0.5f else 2.4f,
)

/** Under cover (tunnel or indoors) so tender crops are happy. */
val Bed.covered: Boolean get() = place != BedPlace.OUTDOOR
val Bed.isPot: Boolean get() = kind == BedKind.POT
val Bed.emoji: String get() = when {
    isPot -> "🪴"
    place == BedPlace.TUNNEL -> "⛺"
    place == BedPlace.INDOOR -> "🏠"
    else -> "🟫"
}

/** A seed the user wants to grow in a given year. */
@Entity(tableName = "seeds", primaryKeys = ["cropId", "year"])
data class SeedChoice(
    val cropId: String,
    val year: Int,
)

enum class Stage { PLANNED, SOWN, PLANTED_OUT, HARVESTING, FINISHED }

/** A crop placed in a bed with its dates. Created by the planner. */
@Entity(tableName = "plantings")
data class Planting(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val year: Int,
    val cropId: String,
    val bedId: Long?,
    val sowDate: LocalDate,
    val sowIndoors: Boolean,
    val plantOutDate: LocalDate?,
    val harvestStart: LocalDate,
    val harvestEnd: LocalDate,
    val stage: Stage = Stage.PLANNED,
    /** Plain-English reasons the planner chose this bed. */
    val reason: String = "",
    /** Soaking, chilling or chitting done. */
    @ColumnInfo(defaultValue = "0") val prepDone: Boolean = false,
    /** Hardening off done. */
    @ColumnInfo(defaultValue = "0") val hardenDone: Boolean = false,
    /** The user wants to save seed from this crop. */
    @ColumnInfo(defaultValue = "0") val saveSeeds: Boolean = false,
    @ColumnInfo(defaultValue = "0") val seedsSaved: Boolean = false,
)

enum class EntryType(val label: String, val emoji: String) {
    WATERED("Watered", "💧"),
    FED("Fed", "🧪"),
    SOWN("Sowed", "🌱"),
    PLANTED("Planted out", "🪴"),
    HARVESTED("Harvested", "🧺"),
    WEEDED("Weeded", "🌾"),
    PRUNED("Pruned or pinched", "✂️"),
    MULCHED("Mulched or composted", "🍂"),
    COVERED("Covered or protected", "🛡️"),
    PEST("Pests or disease", "🐛"),
    SEEDS("Saved seeds", "🌰"),
    NOTE("Note", "📝"),
}

/** Area values for journal entries that cover a group of beds. */
object Area {
    const val ALL = ""
    const val OUTDOOR = "OUTDOOR"
    const val TUNNEL = "TUNNEL"
    const val INDOOR = "INDOOR"
}

@Entity(tableName = "journal")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: EntryType,
    val bedId: Long? = null,
    /** Used when bedId is null. "" = whole garden, or Area.OUTDOOR / Area.TUNNEL. */
    val area: String = Area.ALL,
    val cropId: String? = null,
    val amount: String = "",
    val notes: String = "",
)

/** A plant the user added themselves ("anything else"). */
@Entity(tableName = "custom_crops")
data class CustomCrop(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val category: Category,
    val tunnel: TunnelFit,
    val sowIndoors: Boolean,
    /** Weeks relative to last frost. Negative = before. */
    val weeksFromFrost: Int,
    val daysToHarvest: Int,
    val harvestWeeks: Int,
    val frostHardy: Boolean,
    val water: Need = Need.MEDIUM,
    val family: String = "OTHER",
    val perennial: Boolean = false,
    val potOk: Boolean = true,
    val pollinator: Boolean = false,
    /** Description, e.g. from an online lookup. */
    val notes: String = "",
    /** Where the details came from, e.g. Wikipedia. */
    val source: String = "",
)
