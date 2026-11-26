package com.luxcalc.pro

import com.luxcalc.pro.data.local.seed.FixtureCatalog
import com.luxcalc.pro.data.local.seed.SpaceCatalog
import com.luxcalc.pro.domain.engine.LightingCalculationEngine
import com.luxcalc.pro.domain.engine.SmartSpaceRecognizer
import com.luxcalc.pro.domain.engine.SpotlightSpacingEngine
import com.luxcalc.pro.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class LightingCalculationEngineTest {

    @Test
    fun lumenMethod_roundsUpFixturesAndCalculatesMaintainedLux() {
        val geom = RoomGeometryInput(RoomShape.RECTANGULAR, lengthM = 5.0, widthM = 4.0, ceilingHeightM = 3.0)
        val surfaces = RoomSurfaceConfig()
        val spot = FixtureCatalog.allFixtures.first { it.id == "spot_anti_glare" } // 880 lm, 10W

        val result = LightingCalculationEngine.calculateLumenMethod(
            geometry = geom,
            surfaces = surfaces,
            targetLux = 200.0,
            fixture = spot,
            maintenanceFactor = 0.80,
            manualUfOverride = 0.65
        )

        assertEquals(20.0, result.roomAreaM2, 0.01)
        // Required Lumens = (200 * 20) / (0.65 * 0.80) = 7692.3 lm
        assertEquals(7692.3, result.requiredLumens, 1.0)
        // 7692.3 / 880 = 8.74 -> ceil = 9
        assertEquals(9, result.requiredQuantity)
        assertTrue(result.estimatedMaintainedLux >= 200.0)
        assertEquals(90.0, result.totalWatts, 0.01)
    }

    @Test
    fun darkWalls_reduceUfAndIncreaseFixtures_withoutAlteringTargetLux() {
        val geom = RoomGeometryInput(RoomShape.RECTANGULAR, lengthM = 5.0, widthM = 4.0, ceilingHeightM = 3.0)
        val spot = FixtureCatalog.allFixtures.first { it.id == "spot_anti_glare" }

        val lightRoom = RoomSurfaceConfig(wallReflectance = 0.80, ceilingReflectance = 0.82, floorReflectance = 0.40)
        val darkRoom = RoomSurfaceConfig(wallReflectance = 0.10, ceilingReflectance = 0.15, floorReflectance = 0.12)

        val resLight = LightingCalculationEngine.calculateLumenMethod(geom, lightRoom, 250.0, spot, 0.80)
        val resDark = LightingCalculationEngine.calculateLumenMethod(geom, darkRoom, 250.0, spot, 0.80)

        assertEquals(250.0, resLight.targetLux, 0.001)
        assertEquals(250.0, resDark.targetLux, 0.001)
        assertTrue("Dark room UF must be lower than light room UF", resDark.utilizationFactor < resLight.utilizationFactor)
        assertTrue("Dark room must need more lumens for same Target Lux", resDark.requiredLumens > resLight.requiredLumens)
        assertTrue("Dark room must need >= fixtures", resDark.requiredQuantity >= resLight.requiredQuantity)
    }

    @Test
    fun spotlightSpacing_adaptsToBeamAngleAndGeneratesPlacements() {
        val narrow = FixtureCatalog.allFixtures.first { it.id == "mini_spot" } // 24 deg
        val wide = FixtureCatalog.allFixtures.first { it.id == "downlight" } // 100 deg

        val layNarrow = SpotlightSpacingEngine.calculateLayout(6.0, 4.0, 3.0, 0.8, narrow, 6, LayoutPattern.SYMMETRIC)
        val layWide = SpotlightSpacingEngine.calculateLayout(6.0, 4.0, 3.0, 0.8, wide, 6, LayoutPattern.SYMMETRIC)

        assertEquals(6, layNarrow.placements.size)
        assertTrue(layWide.maxRecommendedSpacingM > layNarrow.maxRecommendedSpacingM)
        assertTrue(layWide.beamSpreadDiameterM > layNarrow.beamSpreadDiameterM)
    }

    @Test
    fun smartSpaceRecognizer_handlesCustomAndAmbiguousRoomsOffline() {
        val paintRoom = SmartSpaceRecognizer.analyzeSpace("غرفة دهانات سيارات", SpaceCatalog.allSpaces)
        assertFalse(paintRoom.needsClarification)
        assertTrue(paintRoom.matchedTemplate.defaultLux >= 500)
        assertEquals(RecommendationSource.LOCAL_HEURISTIC_ESTIMATE, paintRoom.matchedTemplate.source)

        val ambiguous = SmartSpaceRecognizer.analyzeSpace("غرفة جديدة", SpaceCatalog.allSpaces)
        assertTrue(ambiguous.needsClarification)
        assertNotNull(ambiguous.clarificationQuestionAr)
    }
}
