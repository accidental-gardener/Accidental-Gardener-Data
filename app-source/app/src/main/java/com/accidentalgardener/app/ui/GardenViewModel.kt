package com.accidentalgardener.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.accidentalgardener.app.data.AppDatabase
import com.accidentalgardener.app.data.Bed
import com.accidentalgardener.app.data.BedKind
import com.accidentalgardener.app.data.BedPlace
import com.accidentalgardener.app.data.covered
import com.accidentalgardener.app.data.Crop
import com.accidentalgardener.app.data.CropCatalog
import com.accidentalgardener.app.data.CustomCrop
import com.accidentalgardener.app.data.EntryType
import com.accidentalgardener.app.data.Garden
import com.accidentalgardener.app.data.JournalEntry
import com.accidentalgardener.app.data.Planting
import com.accidentalgardener.app.data.PlantLookup
import com.accidentalgardener.app.data.SeedChoice
import com.accidentalgardener.app.data.Stage
import com.accidentalgardener.app.data.toCrop
import com.accidentalgardener.app.domain.GardenTask
import com.accidentalgardener.app.ui.components.AvatarStore
import com.accidentalgardener.app.ui.components.AvatarStyle
import com.accidentalgardener.app.domain.Planner
import com.accidentalgardener.app.domain.TaskKind
import com.accidentalgardener.app.domain.Tasks
import com.accidentalgardener.app.weather.Place
import com.accidentalgardener.app.weather.WeatherRepository
import com.accidentalgardener.app.weather.WeatherSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface GardenState {
    data object Loading : GardenState
    data object None : GardenState
    data class Ready(val garden: Garden) : GardenState
}

data class WeatherUi(
    val snapshot: WeatherSnapshot? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

class GardenViewModel(app: Application) : AndroidViewModel(app) {
    init {
        CropCatalog.init(app)
        AvatarStore.load(app)
    }


    val avatar = AvatarStore.style

    fun saveAvatar(style: AvatarStyle) = AvatarStore.save(getApplication(), style)

    private val dao = AppDatabase.get(app).dao()
    private val weatherRepo = WeatherRepository(app)

    val gardenState: StateFlow<GardenState> = dao.gardenFlow()
        .map { if (it == null) GardenState.None else GardenState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, GardenState.Loading)

    private val year = gardenState.map { (it as? GardenState.Ready)?.garden?.planYear }

    val beds: StateFlow<List<Bed>> = dao.bedsFlow().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customCrops: StateFlow<List<CustomCrop>> =
        dao.customFlow().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val crops: StateFlow<List<Crop>> = combine(CropCatalog.state, customCrops) { cat, list ->
        cat.plants + list.map { it.toCrop() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CropCatalog.all)

    val catalog = CropCatalog.state

    private val _guideStatus = MutableStateFlow<String?>(null)
    val guideStatus: StateFlow<String?> = _guideStatus.asStateFlow()
    private val _guideBusy = MutableStateFlow(false)
    val guideBusy: StateFlow<Boolean> = _guideBusy.asStateFlow()

    fun guideUrl(): String = CropCatalog.sourceUrl(getApplication())

    fun refreshGuides(url: String) {
        CropCatalog.setSourceUrl(getApplication(), url)
        viewModelScope.launch {
            _guideBusy.value = true
            _guideStatus.value = CropCatalog.refresh(getApplication())
            _guideBusy.value = false
        }
    }

    /** Plants added by a recent update, for the "New" badge in the Seed tin. */
    val newPlantIds: StateFlow<Set<String>> = CropCatalog.state
        .map { CropCatalog.newIds(getApplication()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CropCatalog.newIds(getApplication()))

    init {
        // Look for new plants every time the app opens (at most every 15 minutes). Only a tiny
        // version file is downloaded unless something has actually changed.
        viewModelScope.launch {
            if (System.currentTimeMillis() - CropCatalog.lastChecked(getApplication()) > 15 * 60 * 1000L) {
                val msg = runCatching { CropCatalog.refresh(getApplication()) }.getOrNull()
                if (msg != null && msg.startsWith("New in the Seed tin")) _guideStatus.value = msg
            }
        }
    }

    /** The Seed tin's "Check for new plants" button. */
    fun checkForNewPlants() {
        viewModelScope.launch {
            _guideBusy.value = true
            _guideStatus.value = runCatching { CropCatalog.refresh(getApplication()) }.getOrElse { "Couldn't check just now." }
            _guideBusy.value = false
        }
    }

    fun clearGuideStatus() { _guideStatus.value = null }

    val seeds: StateFlow<Set<String>> = combine(dao.seedsFlow(), year) { s, y ->
        s.filter { it.year == y }.map { it.cropId }.toSet()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val plantings: StateFlow<List<Planting>> = combine(dao.plantingsFlow(), year) { p, y ->
        p.filter { it.year == y }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val journal: StateFlow<List<JournalEntry>> =
        dao.journalFlow().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---------- Looking up plants that aren't in the database ----------

    private val _lookupMatches = MutableStateFlow<List<PlantLookup.Match>>(emptyList())
    val lookupMatches: StateFlow<List<PlantLookup.Match>> = _lookupMatches.asStateFlow()
    private val _lookupBusy = MutableStateFlow(false)
    val lookupBusy: StateFlow<Boolean> = _lookupBusy.asStateFlow()
    private val _lookupError = MutableStateFlow<String?>(null)
    val lookupError: StateFlow<String?> = _lookupError.asStateFlow()
    private val _lookupDraft = MutableStateFlow<CustomCrop?>(null)
    val lookupDraft: StateFlow<CustomCrop?> = _lookupDraft.asStateFlow()

    fun lookupSearch(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _lookupBusy.value = true
            _lookupError.value = null
            try {
                _lookupMatches.value = PlantLookup.search(query)
                if (_lookupMatches.value.isEmpty()) _lookupError.value = "Nothing found. Try another name, or add it by hand."
            } catch (e: Exception) {
                _lookupError.value = "Couldn't search online. Check your connection, or add it by hand."
            }
            _lookupBusy.value = false
        }
    }

    fun lookupPick(match: PlantLookup.Match, typedName: String) {
        viewModelScope.launch {
            _lookupBusy.value = true
            _lookupDraft.value = PlantLookup.draft(getApplication(), match.title, typedName.ifBlank { match.title })
            _lookupBusy.value = false
        }
    }

    fun clearLookup() {
        _lookupMatches.value = emptyList()
        _lookupDraft.value = null
        _lookupError.value = null
    }

    fun perenualKey(): String = PlantLookup.perenualKey(getApplication())
    fun setPerenualKey(key: String) = PlantLookup.setPerenualKey(getApplication(), key)

    private val _weather = MutableStateFlow(WeatherUi(snapshot = weatherRepo.cached()))
    val weather: StateFlow<WeatherUi> = _weather.asStateFlow()

    private val _places = MutableStateFlow<List<Place>>(emptyList())
    val places: StateFlow<List<Place>> = _places.asStateFlow()
    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()
    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    init {
        viewModelScope.launch {
            gardenState.filterIsInstance<GardenState.Ready>().first()
            refreshWeather()
        }
    }

    // ---------- Weather ----------

    fun refreshWeather() {
        val g = (gardenState.value as? GardenState.Ready)?.garden ?: return
        fetchWeather(g.latitude, g.longitude)
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            _weather.update { it.copy(loading = true, error = null) }
            try {
                _weather.value = WeatherUi(weatherRepo.fetch(lat, lon))
            } catch (e: Exception) {
                _weather.update { it.copy(loading = false, error = "Couldn't reach the weather service") }
            }
        }
    }

    private val _located = MutableStateFlow<Place?>(null)
    /** The place found from the phone's location, ready to be selected. */
    val located: StateFlow<Place?> = _located.asStateFlow()

    /** Call once the located place has been picked up by the screen. */
    fun clearLocated() {
        _located.value = null
    }

    fun useCurrentLocation() {
        viewModelScope.launch {
            _searching.value = true
            _searchError.value = null
            try {
                val p = weatherRepo.currentPlace()
                _places.value = listOf(p)
                _located.value = p
            } catch (e: Exception) {
                _searchError.value = "Couldn't find your location. Check location is turned on, or type your town instead."
            }
            _searching.value = false
        }
    }

    fun searchPlaces(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _searching.value = true
            _searchError.value = null
            try {
                _places.value = weatherRepo.searchPlaces(query)
                if (_places.value.isEmpty()) _searchError.value = "No places found. Try a nearby town."
            } catch (e: Exception) {
                _searchError.value = "No internet connection. You can skip this and set it later."
            }
            _searching.value = false
        }
    }

    // ---------- Setup and settings ----------

    /** Counts of each kind of growing space chosen during setup. */
    data class SpaceCounts(val outdoorBeds: Int, val tunnelBeds: Int, val outdoorPots: Int, val indoorPots: Int, val tunnelPots: Int = 0)

    fun completeOnboarding(name: String, place: Place?, spaceType: String, counts: SpaceCounts) {
        viewModelScope.launch {
            val now = LocalDate.now()
            val planYear = if (now.monthValue >= 9) now.year + 1 else now.year
            val p = place ?: Place("Ireland", "", "Ireland", 53.4, -7.9)
            val beds = buildList {
                repeat(counts.tunnelBeds) { add(newBed(BedKind.BED, BedPlace.TUNNEL, "Tunnel bed ${it + 1}")) }
                repeat(counts.outdoorBeds) { add(newBed(BedKind.BED, BedPlace.OUTDOOR, "Bed ${it + 1}")) }
                repeat(counts.tunnelPots) { add(newBed(BedKind.POT, BedPlace.TUNNEL, "Tunnel pot ${it + 1}")) }
                repeat(counts.outdoorPots) { add(newBed(BedKind.POT, BedPlace.OUTDOOR, "Pot ${it + 1}")) }
                repeat(counts.indoorPots) { add(newBed(BedKind.POT, BedPlace.INDOOR, "Indoor pot ${it + 1}")) }
            }.mapIndexed { i, b -> b.copy(position = i) }
            dao.insertBeds(beds)
            dao.saveGarden(
                Garden(
                    userName = name.trim(), town = p.label, latitude = p.lat, longitude = p.lon,
                    planYear = planYear, spaceType = spaceType,
                    mapWidthM = if (spaceType == "BALCONY") 3f else if (spaceType == "LARGE") 20f else 10f,
                    mapLengthM = if (spaceType == "BALCONY") 2f else if (spaceType == "LARGE") 30f else 14f,
                )
            )
            fetchWeather(p.lat, p.lon)
        }
    }

    private fun newBed(kind: BedKind, place: BedPlace, name: String, sizeM2: Double? = null) = Bed(
        name = name, isTunnel = place == BedPlace.TUNNEL, kind = kind, place = place, sizeM2 = sizeM2, position = 0,
    )

    fun saveSettings(garden: Garden, replan: Boolean) {
        viewModelScope.launch {
            val old = dao.garden()
            dao.saveGarden(garden)
            if (old == null || old.latitude != garden.latitude || old.longitude != garden.longitude) {
                fetchWeather(garden.latitude, garden.longitude)
            }
            if (replan) planNow()
        }
    }

    fun updateName(name: String) {
        val g = (gardenState.value as? GardenState.Ready)?.garden ?: return
        if (name.isBlank()) return
        viewModelScope.launch { dao.saveGarden(g.copy(userName = name.trim())) }
    }

    fun setMapSize(widthM: Float, lengthM: Float) {
        val g = (gardenState.value as? GardenState.Ready)?.garden ?: return
        viewModelScope.launch { dao.saveGarden(g.copy(mapWidthM = widthM, mapLengthM = lengthM)) }
    }

    // ---------- Beds and pots ----------

    fun addBeds(kind: BedKind, place: BedPlace, name: String, sizeM2: Double?, howMany: Int) = viewModelScope.launch {
        repeat(howMany.coerceIn(1, 50)) { i ->
            val all = dao.beds()
            val count = all.count { it.kind == kind && it.place == place } + 1
            val label = if (name.isNotBlank()) (if (howMany > 1) "${name.trim()} ${i + 1}" else name.trim()) else when {
                kind == BedKind.POT && place == BedPlace.INDOOR -> "Indoor pot $count"
                kind == BedKind.POT && place == BedPlace.TUNNEL -> "Tunnel pot $count"
                kind == BedKind.POT -> "Pot $count"
                place == BedPlace.TUNNEL -> "Tunnel bed $count"
                place == BedPlace.INDOOR -> "Indoor planter $count"
                else -> "Bed $count"
            }
            dao.insertBeds(listOf(newBed(kind, place, label, sizeM2).copy(position = (all.maxOfOrNull { it.position } ?: -1) + 1)))
        }
    }

    /** Removes a bed or pot. Anything planned in it goes back to "waiting for a spot". */
    fun deleteBed(bed: Bed) = viewModelScope.launch {
        dao.unassign(listOf(bed.id))
        dao.deleteBeds(listOf(bed))
    }

    fun updateBed(bed: Bed) = viewModelScope.launch {
        dao.updateBed(bed.copy(isTunnel = bed.place == BedPlace.TUNNEL))
    }

    fun updateBeds(beds: List<Bed>) = viewModelScope.launch { dao.updateBeds(beds) }

    fun movePlanting(p: Planting, bed: Bed?) = viewModelScope.launch {
        val g = dao.garden() ?: return@launch
        val crop = crop(p.cropId) ?: return@launch
        val s = Planner.schedule(crop, p.year, g.lastFrostDoy, bed?.covered == true)
        // Only re-date things that haven't started yet.
        val updated = if (p.stage == Stage.PLANNED) {
            p.copy(bedId = bed?.id, sowDate = s.sow, sowIndoors = s.sowIndoors, plantOutDate = s.plantOut,
                harvestStart = s.harvestStart, harvestEnd = s.harvestEnd, reason = "You moved it here.")
        } else {
            p.copy(bedId = bed?.id, reason = "You moved it here.")
        }
        dao.updatePlanting(updated)
    }

    fun setSaveSeeds(p: Planting, on: Boolean) = viewModelScope.launch {
        dao.updatePlanting(p.copy(saveSeeds = on, seedsSaved = if (on) p.seedsSaved else false))
    }

    fun removePlanting(p: Planting) = viewModelScope.launch { dao.deletePlanting(p) }

    /** Something already growing when you started using the app. */
    fun addExisting(cropId: String, bed: Bed, stage: Stage, since: LocalDate) = viewModelScope.launch {
        val g = dao.garden() ?: return@launch
        val crop = crop(cropId) ?: return@launch
        val s = Planner.schedule(crop, g.planYear, g.lastFrostDoy, bed.covered)
        val anchor = when (stage) {
            Stage.HARVESTING -> s.harvestStart
            Stage.PLANTED_OUT -> s.plantOut ?: s.sow
            else -> s.sow
        }
        val shift = java.time.temporal.ChronoUnit.DAYS.between(anchor, since)
        dao.insertPlanting(
            Planting(
                year = g.planYear, cropId = cropId, bedId = bed.id,
                sowDate = s.sow.plusDays(shift), sowIndoors = s.sowIndoors && stage == Stage.SOWN,
                plantOutDate = s.plantOut?.plusDays(shift),
                harvestStart = s.harvestStart.plusDays(shift), harvestEnd = s.harvestEnd.plusDays(shift),
                stage = if (stage == Stage.SOWN && s.plantOut == null) Stage.PLANTED_OUT else stage,
                reason = "Already growing here when you started.",
                prepDone = true, hardenDone = stage != Stage.SOWN,
            )
        )
        dao.addSeed(SeedChoice(cropId, g.planYear))
    }

    // ---------- Starting over ----------

    /** Clears this year's plan but keeps beds, pots, seeds and the journal. */
    fun clearPlan() = viewModelScope.launch {
        val g = dao.garden() ?: return@launch
        dao.clearYear(g.planYear)
    }

    /** Removes every bed, pot and plan so you can lay out your space again. Seeds and journal stay. */
    fun clearSpaces() = viewModelScope.launch {
        val g = dao.garden() ?: return@launch
        dao.clearYear(g.planYear)
        dao.deleteBeds(dao.beds())
    }

    /** Wipes everything and goes back to the setup screens. */
    fun resetEverything() = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        AppDatabase.get(getApplication()).clearAllTables()
    }

    // ---------- Seeds and planning ----------

    fun crop(id: String): Crop? = crops.value.firstOrNull { it.id == id }

    fun toggleSeed(cropId: String) {
        val g = (gardenState.value as? GardenState.Ready)?.garden ?: return
        viewModelScope.launch {
            val choice = SeedChoice(cropId, g.planYear)
            if (cropId in seeds.value) dao.removeSeed(choice) else dao.addSeed(choice)
        }
    }

    fun addCustomCrop(c: CustomCrop) {
        val g = (gardenState.value as? GardenState.Ready)?.garden ?: return
        viewModelScope.launch {
            val id = dao.addCustom(c)
            dao.addSeed(SeedChoice("custom_$id", g.planYear))
        }
    }

    fun runPlanner() {
        viewModelScope.launch { planNow() }
    }

    private suspend fun planNow() {
        val g = dao.garden() ?: return
        val customs = dao.customList()
        val lookup = { id: String -> CropCatalog.resolve(id, customs) }
        val existing = dao.plantingsFor(g.planYear)
        val locked = existing.filter { it.stage != Stage.PLANNED }
        val chosen = dao.seedsFor(g.planYear)
            .map { it.cropId }
            .filter { id -> locked.none { it.cropId == id } }
            .mapNotNull(lookup)
        // Keep any soaking or chitting already done for crops that haven't been sown yet.
        val prepped = existing.filter { it.stage == Stage.PLANNED && it.prepDone }.map { it.cropId }.toSet()
        val savingSeed = existing.filter { it.stage == Stage.PLANNED && it.saveSeeds }.map { it.cropId }.toSet()
        val result = Planner.plan(g, dao.beds(), chosen, locked, dao.plantingsFor(g.planYear - 1), lookup)
            .map { it.copy(prepDone = it.cropId in prepped, saveSeeds = it.cropId in savingSeed) }
        dao.clearPlanned(g.planYear)
        dao.insertPlantings(result)
    }

    // ---------- Tasks and journal ----------

    fun completeTask(t: GardenTask) = viewModelScope.launch {
        dao.updatePlanting(Tasks.complete(t))
        val type = when (t.kind) {
            TaskKind.PREP, TaskKind.HARDEN -> EntryType.NOTE
            TaskKind.SAVE_SEEDS -> EntryType.SEEDS
            TaskKind.SOW -> EntryType.SOWN
            TaskKind.PLANT_OUT -> EntryType.PLANTED
            TaskKind.HARVEST -> EntryType.HARVESTED
            TaskKind.FINISH -> EntryType.NOTE
        }
        dao.addEntry(
            JournalEntry(
                timestamp = System.currentTimeMillis(), type = type, bedId = t.planting.bedId,
                cropId = t.crop.id, notes = t.title.replaceFirstChar { it.uppercase() },
            )
        )
    }

    fun addEntry(e: JournalEntry) = viewModelScope.launch { dao.addEntry(e) }

    fun deleteEntry(e: JournalEntry) = viewModelScope.launch { dao.deleteEntry(e) }
}
