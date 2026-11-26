package com.luxcalc.pro.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luxcalc.pro.data.local.*
import com.luxcalc.pro.data.local.seed.FixtureCatalog
import com.luxcalc.pro.data.local.seed.SpaceCatalog
import com.luxcalc.pro.data.preferences.AppSettings
import com.luxcalc.pro.data.preferences.SettingsRepository
import com.luxcalc.pro.domain.engine.GeminiSpaceAdvisor
import com.luxcalc.pro.domain.engine.LightingCalculationEngine
import com.luxcalc.pro.domain.engine.SmartSpaceRecognizer
import com.luxcalc.pro.domain.engine.SpotlightSpacingEngine
import com.luxcalc.pro.domain.export.ArabicReportExporter
import com.luxcalc.pro.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class RoomWorkspaceState(
    val editingRoomId: String? = null,
    val roomNameAr: String = "ريسبشن",
    val selectedCategory: SpaceCategory = SpaceCategory.RESIDENTIAL,
    val recognitionResult: SpaceRecognitionResult? = null,
    val aiLoading: Boolean = false,
    val aiErrorAr: String? = null,
    val geometry: RoomGeometryInput = RoomGeometryInput(lengthM = 6.0, widthM = 4.5, ceilingHeightM = 3.0),
    val surfaces: RoomSurfaceConfig = RoomSurfaceConfig(),
    val targetLux: Double = 200.0,
    val maintenanceFactor: Double = 0.80,
    val primaryFixture: LightingFixture = FixtureCatalog.allFixtures.first { it.id == "spot_anti_glare" },
    val mixedEntries: List<MixedFixtureEntry> = emptyList(),
    val useMixedMode: Boolean = false,
    val layoutPattern: LayoutPattern = LayoutPattern.SYMMETRIC,
    val customWallSetbackM: Double? = null,
    val placements2D: List<PlacedFixture2D> = emptyList(),
    val openings2D: List<WallOpening2D> = listOf(
        WallOpening2D("door_1", OpeningType.DOOR, WallSide.BOTTOM, 0.8, 0.9),
        WallOpening2D("win_1", OpeningType.WINDOW, WallSide.TOP, 1.5, 1.8)
    ),
    val calculationResult: LumenCalculationResult? = null,
    val mixedResult: MixedLightingResult? = null,
    val spacingResult: SpacingLayoutResult? = null
)

class LuxCalcViewModel(
    private val db: LuxCalcDatabase,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val projectDao = db.projectDao()
    private val catalogDao = db.catalogDao()

    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val projects: StateFlow<List<ProjectEntity>> = projectDao.observeAllProjects()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allRooms: StateFlow<List<RoomDesignEntity>> = projectDao.observeAllRooms()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _activeProjectId = MutableStateFlow<String?>(null)
    val activeProjectId: StateFlow<String?> = _activeProjectId.asStateFlow()

    val activeProjectRooms: StateFlow<List<RoomDesignEntity>> = combine(allRooms, _activeProjectId) { rooms, pid ->
        if (pid == null) rooms else rooms.filter { it.projectId == pid }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allSpaces: StateFlow<List<SpaceTemplate>> = catalogDao.observeCustomSpaces().map { customList ->
        val mapped = customList.map { c ->
            SpaceTemplate(
                id = c.id,
                nameAr = c.nameAr,
                category = runCatching { SpaceCategory.valueOf(c.categoryName) }.getOrDefault(SpaceCategory.CUSTOM),
                minLux = c.minLux,
                defaultLux = c.defaultLux,
                maxLux = c.maxLux,
                recommendedCctAr = c.cctAr,
                defaultCctK = c.defaultCctK,
                minCri = c.minCri,
                workplaneHeightM = c.workplaneHeightM,
                lightingRequirementsAr = c.requirementsAr,
                source = runCatching { RecommendationSource.valueOf(c.sourceName) }.getOrDefault(RecommendationSource.LOCAL_HEURISTIC_ESTIMATE),
                referenceAr = c.referenceAr,
                isCustom = true
            )
        }
        mapped + SpaceCatalog.allSpaces
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SpaceCatalog.allSpaces)

    val allFixtures: StateFlow<List<LightingFixture>> = catalogDao.observeCustomFixtures().map { customList ->
        val customMap = customList.associateBy { it.id }
        val base = FixtureCatalog.allFixtures.map { f ->
            customMap[f.id]?.toDomain() ?: f
        }
        val extra = customList.filter { c -> FixtureCatalog.allFixtures.none { it.id == c.id } }.map { it.toDomain() }
        extra + base
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FixtureCatalog.allFixtures)

    private val _workspace = MutableStateFlow(RoomWorkspaceState())
    val workspace: StateFlow<RoomWorkspaceState> = _workspace.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        recalculateWorkspace()
        viewModelScope.launch {
            projects.collect { list ->
                if (list.isEmpty()) {
                    val defaultProject = ProjectEntity(
                        id = "proj_default_villa",
                        nameAr = "فيلا سكنية — مشروع افتراضي",
                        clientNameAr = "عميل سكني",
                        projectTypeAr = "سكني",
                        notesAr = "مشروع متعدد الفراغات جاهز للتصميم وإضافة الغرف"
                    )
                    projectDao.upsertProject(defaultProject)
                    _activeProjectId.value = defaultProject.id
                } else if (_activeProjectId.value == null || list.none { it.id == _activeProjectId.value }) {
                    _activeProjectId.value = list.first().id
                }
            }
        }
    }

    fun clearMessage() { _statusMessage.value = null }
    fun showMessage(msg: String) { _statusMessage.value = msg }

    fun selectProject(projectId: String) {
        _activeProjectId.value = projectId
    }

    fun createProject(nameAr: String, clientNameAr: String, typeAr: String) {
        if (nameAr.isBlank()) return
        viewModelScope.launch {
            val entity = ProjectEntity(
                id = UUID.randomUUID().toString(),
                nameAr = nameAr.trim(),
                clientNameAr = clientNameAr.trim(),
                projectTypeAr = typeAr
            )
            projectDao.upsertProject(entity)
            _activeProjectId.value = entity.id
            showMessage("تم إنشاء المشروع: ${entity.nameAr}")
        }
    }

    fun duplicateProject(project: ProjectEntity) {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val copyProj = project.copy(
                id = newId,
                nameAr = "${project.nameAr} (نسخة)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            projectDao.upsertProject(copyProj)
            val rooms = projectDao.getRoomsForProjectOnce(project.id)
            for (r in rooms) {
                projectDao.upsertRoom(r.copy(id = UUID.randomUUID().toString(), projectId = newId))
            }
            _activeProjectId.value = newId
            showMessage("تم نسخ المشروع بنجاح")
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            projectDao.deleteProject(project)
            showMessage("تم حذف المشروع: ${project.nameAr}")
        }
    }

    fun deleteRoom(room: RoomDesignEntity) {
        viewModelScope.launch {
            projectDao.deleteRoom(room)
            showMessage("تم حذف الفراغ: ${room.roomNameAr}")
        }
    }

    fun loadRoomIntoWorkspace(room: RoomDesignEntity) {
        val entries = DesignJsonSerializer.decodeMixedEntries(room.mixedFixturesJson)
        val placements = DesignJsonSerializer.decodePlacements(room.placementsJson)
        val openings = DesignJsonSerializer.decodeOpenings(room.openingsJson)
        val shape = runCatching { RoomShape.valueOf(room.shapeName) }.getOrDefault(RoomShape.RECTANGULAR)
        val cat = runCatching { SpaceCategory.valueOf(room.spaceCategoryName) }.getOrDefault(SpaceCategory.RESIDENTIAL)
        val finish = runCatching { SurfaceFinishType.valueOf(room.finishTypeName) }.getOrDefault(SurfaceFinishType.MATTE)
        val pattern = runCatching { LayoutPattern.valueOf(room.layoutPatternName) }.getOrDefault(LayoutPattern.SYMMETRIC)

        _workspace.update { cur ->
            cur.copy(
                editingRoomId = room.id,
                roomNameAr = room.roomNameAr,
                selectedCategory = cat,
                geometry = RoomGeometryInput(
                    shape = shape,
                    lengthM = room.lengthM,
                    widthM = room.widthM,
                    ceilingHeightM = room.ceilingHeightM,
                    ceilingTypeAr = room.ceilingTypeAr
                ),
                surfaces = RoomSurfaceConfig(
                    wallColorNameAr = room.wallColorNameAr,
                    wallHex = room.wallHex,
                    wallReflectance = room.wallReflectance,
                    ceilingColorNameAr = room.ceilingColorNameAr,
                    ceilingHex = room.ceilingHex,
                    ceilingReflectance = room.ceilingReflectance,
                    floorColorNameAr = room.floorColorNameAr,
                    floorHex = room.floorHex,
                    floorReflectance = room.floorReflectance,
                    finishType = finish
                ),
                targetLux = room.targetLux,
                maintenanceFactor = room.mf,
                mixedEntries = entries,
                useMixedMode = entries.size > 1,
                primaryFixture = entries.firstOrNull()?.fixture ?: cur.primaryFixture,
                layoutPattern = pattern,
                placements2D = placements,
                openings2D = openings
            )
        }
        recalculateWorkspace(preserveManualPlacements = placements.isNotEmpty())
        showMessage("تم تحميل فراغ: ${room.roomNameAr}")
    }

    fun updateRoomNameAndAnalyze(name: String) {
        val rec = SmartSpaceRecognizer.analyzeSpace(name, allSpaces.value)
        val suggestedFix = allFixtures.value.firstOrNull { it.id in rec.matchedTemplate.suggestedFixtureIds }
            ?: _workspace.value.primaryFixture
        _workspace.update {
            it.copy(
                roomNameAr = name,
                selectedCategory = rec.matchedTemplate.category,
                recognitionResult = rec,
                targetLux = rec.matchedTemplate.defaultLux.toDouble(),
                geometry = it.geometry.copy(workplaneHeightM = rec.matchedTemplate.workplaneHeightM),
                primaryFixture = suggestedFix,
                aiErrorAr = null
            )
        }
        recalculateWorkspace()
    }

    fun applySpaceTemplate(template: SpaceTemplate) {
        val suggestedFix = allFixtures.value.firstOrNull { it.id in template.suggestedFixtureIds }
            ?: _workspace.value.primaryFixture
        _workspace.update {
            it.copy(
                roomNameAr = template.nameAr,
                selectedCategory = template.category,
                recognitionResult = SpaceRecognitionResult(
                    inputName = template.nameAr,
                    matchedTemplate = template,
                    explanationAr = "${template.source.labelAr} — ${template.referenceAr}",
                    visualTasksAr = template.lightingRequirementsAr
                ),
                targetLux = template.defaultLux.toDouble(),
                geometry = it.geometry.copy(workplaneHeightM = template.workplaneHeightM),
                primaryFixture = suggestedFix
            )
        }
        recalculateWorkspace()
    }

    fun runGeminiSpaceAnalysis(contextNote: String = "") {
        val apiKey = settings.value.geminiApiKey
        val roomName = _workspace.value.roomNameAr
        if (apiKey.isBlank()) {
            _workspace.update { it.copy(aiErrorAr = "أدخل مفتاح Gemini API في صفحة الإعدادات أولاً (الميزة اختيارية بالكامل).") }
            return
        }
        viewModelScope.launch {
            _workspace.update { it.copy(aiLoading = true, aiErrorAr = null) }
            val result = GeminiSpaceAdvisor.analyzeCustomSpaceWithGemini(apiKey, roomName, contextNote)
            result.onSuccess { rec ->
                _workspace.update {
                    it.copy(
                        aiLoading = false,
                        recognitionResult = rec,
                        targetLux = rec.matchedTemplate.defaultLux.toDouble(),
                        geometry = it.geometry.copy(workplaneHeightM = rec.matchedTemplate.workplaneHeightM)
                    )
                }
                recalculateWorkspace()
                showMessage("تم تحديث التوصية بواسطة Gemini AI")
            }.onFailure { err ->
                _workspace.update { it.copy(aiLoading = false, aiErrorAr = err.message ?: "تعذر الاتصال بـ Gemini AI") }
            }
        }
    }

    fun updateGeometry(transform: (RoomGeometryInput) -> RoomGeometryInput) {
        _workspace.update { it.copy(geometry = transform(it.geometry)) }
        recalculateWorkspace()
    }

    fun updateSurfaces(transform: (RoomSurfaceConfig) -> RoomSurfaceConfig) {
        _workspace.update { it.copy(surfaces = transform(it.surfaces)) }
        recalculateWorkspace()
    }

    fun updateTargetLux(lux: Double) {
        _workspace.update { it.copy(targetLux = lux.coerceIn(10.0, 5000.0)) }
        recalculateWorkspace()
    }

    fun updateMaintenanceFactor(mf: Double) {
        _workspace.update { it.copy(maintenanceFactor = mf.coerceIn(0.50, 0.95)) }
        recalculateWorkspace()
    }

    fun selectPrimaryFixture(fixture: LightingFixture) {
        _workspace.update { it.copy(primaryFixture = fixture) }
        recalculateWorkspace()
    }

    fun setMixedMode(enabled: Boolean) {
        _workspace.update { cur ->
            val seeded = if (enabled && cur.mixedEntries.isEmpty()) {
                val qty = cur.calculationResult?.requiredQuantity ?: 4
                listOf(MixedFixtureEntry(UUID.randomUUID().toString(), cur.primaryFixture, qty))
            } else cur.mixedEntries
            cur.copy(useMixedMode = enabled, mixedEntries = seeded)
        }
        recalculateWorkspace()
    }

    fun addMixedFixture(fixture: LightingFixture, qty: Int, layer: LightingLayer, group: String, dimmable: Boolean) {
        val entry = MixedFixtureEntry(UUID.randomUUID().toString(), fixture, qty.coerceAtLeast(1), layer, group, dimmable)
        _workspace.update { it.copy(useMixedMode = true, mixedEntries = it.mixedEntries + entry) }
        recalculateWorkspace()
    }

    fun updateMixedEntryQty(entryId: String, newQty: Int) {
        _workspace.update { cur ->
            cur.copy(mixedEntries = cur.mixedEntries.mapNotNull {
                if (it.id == entryId) {
                    if (newQty <= 0) null else it.copy(quantity = newQty)
                } else it
            })
        }
        recalculateWorkspace()
    }

    fun updateLayoutPattern(pattern: LayoutPattern, customSetback: Double? = _workspace.value.customWallSetbackM) {
        _workspace.update { it.copy(layoutPattern = pattern, customWallSetbackM = customSetback) }
        recalculateWorkspace(preserveManualPlacements = false)
    }

    fun movePlacedFixture(id: String, newXM: Double, newYM: Double) {
        val g = _workspace.value.geometry
        val (eqL, eqW) = LightingCalculationEngine.effectiveDimensions(g)
        _workspace.update { cur ->
            cur.copy(
                placements2D = cur.placements2D.map { p ->
                    if (p.id == id) p.copy(
                        xM = newXM.coerceIn(0.15, (eqL - 0.15).coerceAtLeast(0.2)),
                        yM = newYM.coerceIn(0.15, (eqW - 0.15).coerceAtLeast(0.2))
                    ) else p
                }
            )
        }
    }

    fun addManualFixtureAt(xM: Double, yM: Double, fixture: LightingFixture = _workspace.value.primaryFixture) {
        val placed = PlacedFixture2D(
            id = UUID.randomUUID().toString(),
            xM = xM,
            yM = yM,
            fixtureNameAr = fixture.nameAr,
            fixtureId = fixture.id,
            layer = fixture.defaultLayer,
            symbolCode = "SP",
            wattage = fixture.wattage,
            lumens = fixture.lumens
        )
        _workspace.update { it.copy(placements2D = it.placements2D + placed) }
    }

    fun removePlacedFixture(id: String) {
        _workspace.update { it.copy(placements2D = it.placements2D.filterNot { p -> p.id == id }) }
    }

    fun addOpening(type: OpeningType, wall: WallSide, offsetM: Double, widthM: Double) {
        val op = WallOpening2D(UUID.randomUUID().toString(), type, wall, offsetM.coerceAtLeast(0.1), widthM.coerceIn(0.5, 4.0))
        _workspace.update { it.copy(openings2D = it.openings2D + op) }
    }

    fun removeOpening(id: String) {
        _workspace.update { it.copy(openings2D = it.openings2D.filterNot { it.id == id }) }
    }

    fun recalculateWorkspace(preserveManualPlacements: Boolean = false) {
        val s = _workspace.value
        val lumenRes = LightingCalculationEngine.calculateLumenMethod(
            geometry = s.geometry,
            surfaces = s.surfaces,
            targetLux = s.targetLux,
            fixture = s.primaryFixture,
            maintenanceFactor = s.maintenanceFactor
        )
        val mixedRes = if (s.useMixedMode && s.mixedEntries.isNotEmpty()) {
            LightingCalculationEngine.calculateMixedLighting(
                geometry = s.geometry,
                surfaces = s.surfaces,
                targetLux = s.targetLux,
                entries = s.mixedEntries,
                maintenanceFactor = s.maintenanceFactor
            )
        } else null

        val (eqL, eqW) = LightingCalculationEngine.effectiveDimensions(s.geometry)
        val qtyForSpacing = if (s.useMixedMode && s.mixedEntries.isNotEmpty()) {
            s.mixedEntries.first().quantity
        } else {
            lumenRes.requiredQuantity
        }
        val fixForSpacing = if (s.useMixedMode && s.mixedEntries.isNotEmpty()) {
            s.mixedEntries.first().fixture
        } else {
            s.primaryFixture
        }

        val spacing = SpotlightSpacingEngine.calculateLayout(
            roomLengthM = eqL,
            roomWidthM = eqW,
            ceilingHeightM = s.geometry.ceilingHeightM,
            workplaneHeightM = s.geometry.workplaneHeightM,
            fixture = fixForSpacing,
            desiredQuantity = qtyForSpacing,
            pattern = s.layoutPattern,
            customWallSetbackM = s.customWallSetbackM
        )

        _workspace.update {
            it.copy(
                calculationResult = lumenRes,
                mixedResult = mixedRes,
                spacingResult = spacing,
                placements2D = if (preserveManualPlacements && it.placements2D.isNotEmpty()) it.placements2D else spacing.placements
            )
        }
    }

    fun saveCurrentRoomToProject() {
        val pid = _activeProjectId.value ?: return
        val s = _workspace.value
        val calc = s.calculationResult ?: return
        val spacing = s.spacingResult ?: return
        val entriesToSave = if (s.useMixedMode && s.mixedEntries.isNotEmpty()) {
            s.mixedEntries
        } else {
            listOf(MixedFixtureEntry(UUID.randomUUID().toString(), s.primaryFixture, s.placements2D.size.coerceAtLeast(calc.requiredQuantity)))
        }

        val totalWatts = s.mixedResult?.totalWatts ?: (entriesToSave.sumOf { it.totalWatts })
        val totalLumens = s.mixedResult?.deliveredTotalLumens ?: (entriesToSave.sumOf { it.totalLumens })
        val totalCount = entriesToSave.sumOf { it.quantity }
        val maintainedLux = s.mixedResult?.estimatedMaintainedLux ?: calc.estimatedMaintainedLux
        val uf = s.mixedResult?.uf ?: calc.utilizationFactor

        val entity = RoomDesignEntity(
            id = s.editingRoomId ?: UUID.randomUUID().toString(),
            projectId = pid,
            roomNameAr = s.roomNameAr.ifBlank { "فراغ بدون اسم" },
            spaceCategoryName = s.selectedCategory.name,
            shapeName = s.geometry.shape.name,
            lengthM = s.geometry.lengthM,
            widthM = s.geometry.widthM,
            ceilingHeightM = s.geometry.ceilingHeightM,
            areaM2 = calc.roomAreaM2,
            ceilingTypeAr = s.geometry.ceilingTypeAr,
            wallColorNameAr = s.surfaces.wallColorNameAr,
            wallHex = s.surfaces.wallHex,
            wallReflectance = s.surfaces.wallReflectance,
            ceilingColorNameAr = s.surfaces.ceilingColorNameAr,
            ceilingHex = s.surfaces.ceilingHex,
            ceilingReflectance = s.surfaces.ceilingReflectance,
            floorColorNameAr = s.surfaces.floorColorNameAr,
            floorHex = s.surfaces.floorHex,
            floorReflectance = s.surfaces.floorReflectance,
            finishTypeName = s.surfaces.finishType.name,
            targetLux = s.targetLux,
            uf = uf,
            mf = s.maintenanceFactor,
            maintainedLux = maintainedLux,
            totalWatts = totalWatts,
            totalLumens = totalLumens,
            totalFixturesCount = totalCount,
            layoutPatternName = s.layoutPattern.titleAr,
            spacingXM = spacing.spacingXM,
            spacingYM = spacing.spacingYM,
            wallDistXM = spacing.wallDistanceXM,
            wallDistYM = spacing.wallDistanceYM,
            mixedFixturesJson = DesignJsonSerializer.encodeMixedEntries(entriesToSave),
            placementsJson = DesignJsonSerializer.encodePlacements(s.placements2D),
            openingsJson = DesignJsonSerializer.encodeOpenings(s.openings2D)
        )
        viewModelScope.launch {
            projectDao.upsertRoom(entity)
            _workspace.update { it.copy(editingRoomId = entity.id) }
            showMessage("تم حفظ الفراغ (${entity.roomNameAr}) في المشروع")
        }
    }

    fun startNewRoom() {
        _workspace.update { it.copy(editingRoomId = null, roomNameAr = "فراغ جديد", useMixedMode = false, mixedEntries = emptyList()) }
        recalculateWorkspace(preserveManualPlacements = false)
    }

    fun saveCustomSpace(template: SpaceTemplate) {
        viewModelScope.launch {
            catalogDao.upsertCustomSpace(
                CustomSpaceEntity(
                    id = template.id,
                    nameAr = template.nameAr,
                    categoryName = template.category.name,
                    minLux = template.minLux,
                    defaultLux = template.defaultLux,
                    maxLux = template.maxLux,
                    cctAr = template.recommendedCctAr,
                    defaultCctK = template.defaultCctK,
                    minCri = template.minCri,
                    workplaneHeightM = template.workplaneHeightM,
                    requirementsAr = template.lightingRequirementsAr,
                    sourceName = template.source.name,
                    referenceAr = template.referenceAr
                )
            )
            showMessage("تم حفظ الفراغ المخصص: ${template.nameAr}")
        }
    }

    fun saveCustomFixture(fixture: LightingFixture) {
        viewModelScope.launch {
            catalogDao.upsertCustomFixture(
                CustomFixtureEntity(
                    id = fixture.id,
                    nameAr = fixture.nameAr,
                    categoryName = fixture.category.name,
                    layerName = fixture.defaultLayer.name,
                    wattage = fixture.wattage,
                    lumens = fixture.lumens,
                    beamAngleDeg = fixture.beamAngleDeg,
                    cctK = fixture.cctK,
                    cri = fixture.cri,
                    ipRating = fixture.ipRating,
                    mountingTypeAr = fixture.mountingTypeAr,
                    lightSourcesCount = fixture.lightSourcesCount,
                    manufacturer = fixture.manufacturer,
                    modelCode = fixture.modelCode,
                    photometricFileRef = fixture.photometricFileRef
                )
            )
            if (_workspace.value.primaryFixture.id == fixture.id) {
                selectPrimaryFixture(fixture)
            }
            showMessage("تم حفظ مواصفات وحدة الإضاءة: ${fixture.nameAr}")
        }
    }

    fun computeProjectBoq(rooms: List<RoomDesignEntity>): List<BoqLineItem> {
        val acc = linkedMapOf<String, MutableList<Pair<String, MixedFixtureEntry>>>()
        for (room in rooms) {
            val entries = DesignJsonSerializer.decodeMixedEntries(room.mixedFixturesJson)
            for (e in entries) {
                val key = "${e.fixture.id}_${e.fixture.wattage}_${e.fixture.lumens}"
                acc.getOrPut(key) { mutableListOf() }.add(Pair(room.roomNameAr, e))
            }
        }
        return acc.values.map { list ->
            val first = list.first().second.fixture
            val totalQty = list.sumOf { it.second.quantity }
            val totalW = list.sumOf { it.second.totalWatts }
            val totalLm = list.sumOf { it.second.totalLumens }
            BoqLineItem(
                fixtureId = first.id,
                fixtureNameAr = first.nameAr,
                categoryAr = first.category.titleAr,
                specsSummaryAr = "${first.wattage.toInt()}W | ${first.lumens.toInt()} lm | ${first.beamAngleDeg}° | ${first.cctK}K | CRI ${first.cri} | ${first.ipRating}",
                totalQuantity = totalQty,
                unitWatt = first.wattage,
                totalWatts = totalW,
                totalLumens = totalLm,
                roomsUsedIn = list.map { it.first }.distinct()
            )
        }
    }

    fun computeProjectTotals(rooms: List<RoomDesignEntity>): ProjectTotals {
        val totalArea = rooms.sumOf { it.areaM2 }
        val totalWatts = rooms.sumOf { it.totalWatts }
        val totalLumens = rooms.sumOf { it.totalLumens }
        val totalFixtures = rooms.sumOf { it.totalFixturesCount }
        val avgLpd = if (totalArea > 0) totalWatts / totalArea else 0.0
        val appSet = settings.value
        val monthlyKwh = (totalWatts / 1000.0) * appSet.dailyOperatingHours * 30.0
        val monthlyCost = monthlyKwh * appSet.electricityTariffPerKwh
        return ProjectTotals(rooms.size, totalArea, totalFixtures, totalWatts, totalLumens, avgLpd, monthlyKwh, monthlyCost)
    }

    fun exportPdfReport(context: Context): File? {
        val proj = projects.value.firstOrNull { it.id == _activeProjectId.value } ?: return null
        val rooms = activeProjectRooms.value
        if (rooms.isEmpty()) {
            showMessage("أضف فراغاً واحداً على الأقل للمشروع قبل تصدير التقرير.")
            return null
        }
        val boq = computeProjectBoq(rooms)
        val file = ArabicReportExporter.generateProjectPdfReport(context, proj, rooms, boq)
        ArabicReportExporter.shareFile(context, file, "application/pdf")
        showMessage("تم إنشاء تقرير PDF العربي بنجاح")
        return file
    }

    fun exportExcelCsvBoq(context: Context): File? {
        val proj = projects.value.firstOrNull { it.id == _activeProjectId.value } ?: return null
        val rooms = activeProjectRooms.value
        if (rooms.isEmpty()) {
            showMessage("لا توجد فراغات لتصدير جدول الكميات.")
            return null
        }
        val boq = computeProjectBoq(rooms)
        val file = ArabicReportExporter.generateBoqCsvForExcel(context, proj, boq)
        ArabicReportExporter.shareFile(context, file, "text/csv")
        showMessage("تم تصدير جدول الكميات بصيغة CSV متوافقة مع Excel")
        return file
    }

    fun exportCurrent2DPlanPng(context: Context): File {
        val s = _workspace.value
        val (eqL, eqW) = LightingCalculationEngine.effectiveDimensions(s.geometry)
        val file = ArabicReportExporter.export2DPlanAsPng(context, s.roomNameAr, eqL, eqW, s.placements2D, s.openings2D)
        ArabicReportExporter.shareFile(context, file, "image/png")
        showMessage("تم تصدير مخطط الإضاءة كصورة PNG")
        return file
    }

    // Settings mutations
    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch { settingsRepo.setDarkTheme(enabled) }
    fun setArabicDefault(arabic: Boolean) = viewModelScope.launch { settingsRepo.setArabicDefault(arabic) }
    fun setDefaultMf(mf: Double) = viewModelScope.launch {
        settingsRepo.setDefaultMf(mf)
        updateMaintenanceFactor(mf)
    }
    fun setEnergyParams(hours: Double, tariff: Double) = viewModelScope.launch { settingsRepo.setEnergyParams(hours, tariff) }
    fun setGeminiApiKey(key: String) = viewModelScope.launch {
        settingsRepo.setGeminiApiKey(key)
        showMessage("تم حفظ مفتاح Gemini AI محلياً")
    }

    private fun CustomFixtureEntity.toDomain() = LightingFixture(
        id = id,
        nameAr = nameAr,
        category = runCatching { FixtureCategory.valueOf(categoryName) }.getOrDefault(FixtureCategory.SPOTLIGHTS),
        defaultLayer = runCatching { LightingLayer.valueOf(layerName) }.getOrDefault(LightingLayer.GENERAL),
        wattage = wattage,
        lumens = lumens,
        beamAngleDeg = beamAngleDeg,
        cctK = cctK,
        cri = cri,
        ipRating = ipRating,
        mountingTypeAr = mountingTypeAr,
        lightSourcesCount = lightSourcesCount,
        manufacturer = manufacturer,
        modelCode = modelCode,
        photometricFileRef = photometricFileRef,
        isCustom = true
    )
}

class LuxCalcViewModelFactory(
    private val db: LuxCalcDatabase,
    private val settingsRepo: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return LuxCalcViewModel(db, settingsRepo) as T
    }
}
