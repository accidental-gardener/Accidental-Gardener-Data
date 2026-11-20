package com.accidentalgardener.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Version 2 adds prep and harden-off tracking to plantings without losing anything. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE plantings ADD COLUMN prepDone INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE plantings ADD COLUMN hardenDone INTEGER NOT NULL DEFAULT 0")
    }
}

/** Version 3 adds pots, bed places, map positions and richer custom plants, keeping everything already saved. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE beds ADD COLUMN kind TEXT NOT NULL DEFAULT 'BED'")
        db.execSQL("ALTER TABLE beds ADD COLUMN place TEXT NOT NULL DEFAULT 'OUTDOOR'")
        db.execSQL("UPDATE beds SET place = 'TUNNEL' WHERE isTunnel = 1")
        db.execSQL("ALTER TABLE beds ADD COLUMN mapX REAL NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE beds ADD COLUMN mapY REAL NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE beds ADD COLUMN mapW REAL NOT NULL DEFAULT 1.2")
        db.execSQL("ALTER TABLE beds ADD COLUMN mapH REAL NOT NULL DEFAULT 2.4")
        db.execSQL("ALTER TABLE garden ADD COLUMN spaceType TEXT NOT NULL DEFAULT 'SMALL'")
        db.execSQL("ALTER TABLE garden ADD COLUMN mapWidthM REAL NOT NULL DEFAULT 10")
        db.execSQL("ALTER TABLE garden ADD COLUMN mapLengthM REAL NOT NULL DEFAULT 14")
        db.execSQL("ALTER TABLE custom_crops ADD COLUMN family TEXT NOT NULL DEFAULT 'OTHER'")
        db.execSQL("ALTER TABLE custom_crops ADD COLUMN perennial INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE custom_crops ADD COLUMN potOk INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE custom_crops ADD COLUMN pollinator INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE custom_crops ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE custom_crops ADD COLUMN source TEXT NOT NULL DEFAULT ''")
    }
}

/** Version 4 adds seed saving to plantings. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE plantings ADD COLUMN saveSeeds INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE plantings ADD COLUMN seedsSaved INTEGER NOT NULL DEFAULT 0")
    }
}

class Converters {
    @TypeConverter
    fun fromDate(d: LocalDate?): Long? = d?.toEpochDay()

    @TypeConverter
    fun toDate(v: Long?): LocalDate? = v?.let { LocalDate.ofEpochDay(it) }
}

@Dao
interface GardenDao {
    // Garden
    @Query("SELECT * FROM garden WHERE id = 1")
    fun gardenFlow(): Flow<Garden?>

    @Query("SELECT * FROM garden WHERE id = 1")
    suspend fun garden(): Garden?

    @Upsert
    suspend fun saveGarden(garden: Garden)

    // Beds
    @Query("SELECT * FROM beds ORDER BY position")
    fun bedsFlow(): Flow<List<Bed>>

    @Query("SELECT * FROM beds ORDER BY position")
    suspend fun beds(): List<Bed>

    @Insert
    suspend fun insertBeds(beds: List<Bed>)

    @Update
    suspend fun updateBed(bed: Bed)

    @Delete
    suspend fun deleteBeds(beds: List<Bed>)

    @Update
    suspend fun updateBeds(beds: List<Bed>)

    @Query("DELETE FROM plantings WHERE bedId = :bedId")
    suspend fun deletePlantingsInBed(bedId: Long)

    @Query("DELETE FROM plantings WHERE year = :year")
    suspend fun clearYear(year: Int)

    @Delete
    suspend fun deletePlanting(p: Planting)

    // Seeds
    @Query("SELECT * FROM seeds")
    fun seedsFlow(): Flow<List<SeedChoice>>

    @Query("SELECT * FROM seeds WHERE year = :year")
    suspend fun seedsFor(year: Int): List<SeedChoice>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSeed(seed: SeedChoice)

    @Delete
    suspend fun removeSeed(seed: SeedChoice)

    // Plantings
    @Query("SELECT * FROM plantings ORDER BY sowDate")
    fun plantingsFlow(): Flow<List<Planting>>

    @Query("SELECT * FROM plantings")
    suspend fun plantingsList(): List<Planting>

    @Query("SELECT * FROM plantings WHERE year = :year")
    suspend fun plantingsFor(year: Int): List<Planting>

    @Query("DELETE FROM plantings WHERE year = :year AND stage = 'PLANNED'")
    suspend fun clearPlanned(year: Int)

    @Insert
    suspend fun insertPlantings(p: List<Planting>)

    @Insert
    suspend fun insertPlanting(p: Planting): Long

    @Update
    suspend fun updatePlanting(p: Planting)

    @Query("UPDATE plantings SET bedId = NULL WHERE bedId IN (:ids)")
    suspend fun unassign(ids: List<Long>)

    // Journal
    @Query("SELECT * FROM journal ORDER BY timestamp DESC")
    fun journalFlow(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal")
    suspend fun journalList(): List<JournalEntry>

    @Insert
    suspend fun addEntry(e: JournalEntry)

    @Delete
    suspend fun deleteEntry(e: JournalEntry)

    // Custom crops
    @Query("SELECT * FROM custom_crops")
    fun customFlow(): Flow<List<CustomCrop>>

    @Query("SELECT * FROM custom_crops")
    suspend fun customList(): List<CustomCrop>

    @Insert
    suspend fun addCustom(c: CustomCrop): Long
}

@Database(
    entities = [Garden::class, Bed::class, SeedChoice::class, Planting::class, JournalEntry::class, CustomCrop::class],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): GardenDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(ctx: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "garden.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
