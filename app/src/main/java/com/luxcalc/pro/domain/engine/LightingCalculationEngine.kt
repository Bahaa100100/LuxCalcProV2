package com.luxcalc.pro.domain.engine

import com.luxcalc.pro.domain.model.*
import kotlin.math.*

object LightingCalculationEngine {

    fun calculateArea(input: RoomGeometryInput): Double {
        input.manualAreaM2?.let { if (it > 0.1) return it }
        return when (input.shape) {
            RoomShape.RECTANGULAR -> (input.lengthM * input.widthM).coerceAtLeast(0.5)
            RoomShape.SQUARE -> (input.lengthM * input.lengthM).coerceAtLeast(0.5)
            RoomShape.CIRCULAR -> (PI * (input.diameterM / 2.0).pow(2)).coerceAtLeast(0.5)
            RoomShape.L_SHAPED -> {
                val base = input.lengthM * input.widthM
                val cut = (input.lCutoutLengthM * input.lCutoutWidthM).coerceAtMost(base * 0.75)
                (base - cut).coerceAtLeast(0.5)
            }
            RoomShape.IRREGULAR_POLYGON -> (input.lengthM * input.widthM).coerceAtLeast(0.5)
        }
    }

    fun effectiveDimensions(input: RoomGeometryInput): Pair<Double, Double> {
        val area = calculateArea(input)
        return when (input.shape) {
            RoomShape.RECTANGULAR -> Pair(input.lengthM.coerceAtLeast(0.5), input.widthM.coerceAtLeast(0.5))
            RoomShape.SQUARE -> Pair(input.lengthM.coerceAtLeast(0.5), input.lengthM.coerceAtLeast(0.5))
            RoomShape.CIRCULAR -> {
                val eqSide = sqrt(area)
                Pair(eqSide, eqSide)
            }
            RoomShape.L_SHAPED, RoomShape.IRREGULAR_POLYGON -> {
                val ratio = if (input.widthM > 0.1) (input.lengthM / input.widthM).coerceIn(0.4, 3.0) else 1.2
                val eqW = sqrt(area / ratio)
                val eqL = area / eqW
                Pair(eqL, eqW)
            }
        }
    }

    fun calculateRoomIndexK(lengthM: Double, widthM: Double, mountingHeightM: Double): Double {
        val hm = mountingHeightM.coerceAtLeast(0.5)
        val denom = hm * (lengthM + widthM)
        return if (denom > 0.0) (lengthM * widthM) / denom else 1.0
    }

    /**
     * Engineering rule: Surface colors/reflectance affect Utilization Factor (UF) ONLY.
     * Target Lux is NEVER altered automatically due to dark walls.
     */
    fun calculateUtilizationFactor(
        roomIndexK: Double,
        surfaces: RoomSurfaceConfig,
        beamAngleDeg: Int = 60,
        fixtureCategory: FixtureCategory = FixtureCategory.SPOTLIGHTS
    ): Double {
        val kClamped = roomIndexK.coerceIn(0.5, 5.0)
        val baseOpticalEfficiency = when (fixtureCategory) {
            FixtureCategory.SPOTLIGHTS -> when {
                beamAngleDeg <= 24 -> 0.80
                beamAngleDeg <= 45 -> 0.78
                beamAngleDeg <= 90 -> 0.75
                else -> 0.72
            }
            FixtureCategory.TRACK -> 0.76
            FixtureCategory.TECHNICAL -> 0.78
            FixtureCategory.LINEAR -> 0.66
            FixtureCategory.DECORATIVE -> 0.56
        }

        val kCurve = (1.0 - exp(-0.85 * kClamped)).coerceIn(0.35, 0.96)
        val wallWeight = (0.42 / (kClamped.pow(0.45))).coerceIn(0.16, 0.46)
        val ceilingWeight = 0.24
        val floorWeight = 0.12

        val weightedReflectance =
            (surfaces.wallReflectance.coerceIn(0.05, 0.90) * wallWeight) +
            (surfaces.ceilingReflectance.coerceIn(0.05, 0.92) * ceilingWeight) +
            (surfaces.floorReflectance.coerceIn(0.05, 0.80) * floorWeight)

        val refReflectance = (0.70 * wallWeight) + (0.80 * ceilingWeight) + (0.30 * floorWeight)
        val reflectanceMultiplier = (1.0 + (weightedReflectance - refReflectance) * 0.95)
            .coerceIn(0.62, 1.15)

        val finishMod = surfaces.finishType.ufModifier
        val uf = baseOpticalEfficiency * (0.45 + 0.55 * kCurve) * reflectanceMultiplier * finishMod
        return uf.coerceIn(0.28, 0.88)
    }

    fun calculateLumenMethod(
        geometry: RoomGeometryInput,
        surfaces: RoomSurfaceConfig,
        targetLux: Double,
        fixture: LightingFixture,
        maintenanceFactor: Double = 0.80,
        manualUfOverride: Double? = null
    ): LumenCalculationResult {
        val area = calculateArea(geometry)
        val (eqL, eqW) = effectiveDimensions(geometry)
        val hm = (geometry.ceilingHeightM - geometry.workplaneHeightM - geometry.suspensionDropM)
            .coerceAtLeast(0.6)
        val k = calculateRoomIndexK(eqL, eqW, hm)

        val mf = maintenanceFactor.coerceIn(0.50, 0.95)
        val uf = manualUfOverride?.coerceIn(0.25, 0.92)
            ?: calculateUtilizationFactor(k, surfaces, fixture.beamAngleDeg, fixture.category)

        val safeTargetLux = targetLux.coerceAtLeast(10.0)
        val requiredLumens = (safeTargetLux * area) / (uf * mf)
        val safeFixtureLumens = fixture.lumens.coerceAtLeast(50.0)
        val quantity = ceil(requiredLumens / safeFixtureLumens).toInt().coerceAtLeast(1)

        val totalFixtureLumens = quantity * safeFixtureLumens
        val maintainedLux = (totalFixtureLumens * uf * mf) / area
        val totalWatts = quantity * fixture.wattage
        val lpd = totalWatts / area

        val reflectanceNote = when {
            surfaces.wallReflectance < 0.30 || surfaces.ceilingReflectance < 0.40 ->
                "التشطيبات الداكنة تقلل الضوء المنعكس فتخفض معامل الاستخدام (UF = ${"%.2f".format(uf)}) مما يتطلب تدفقاً ضوئياً أعلى للوصول لنفس شدة الإضاءة المستهدفة (${safeTargetLux.toInt()} Lux) دون تغيير المعيار المستهدف."
            surfaces.wallReflectance >= 0.65 && surfaces.ceilingReflectance >= 0.75 ->
                "التشطيبات الفاتحة تعزز الانعكاس الداخلي وترفع معامل الاستخدام (UF = ${"%.2f".format(uf)}) مما يحسن الكفاءة ويقلل عدد الوحدات المطلوبة لتحقيق ${safeTargetLux.toInt()} Lux."
            else ->
                "التشطيبات متوسطة الانعكاس تعطي معامل استخدام متوازن (UF = ${"%.2f".format(uf)}). شدة الإضاءة المستهدفة (${safeTargetLux.toInt()} Lux) تظل ثابتة حسب وظيفة الفراغ."
        }

        val assumptions = listOf(
            "طريقة الحساب: طريقة اللومن (Lumen Method) لحساب متوسط شدة الإضاءة المصانة فقط (لا تمثل حساباً نقطياً Point-by-Point).",
            "معامل الغرفة (Room Index K) = ${"%.2f".format(k)} عند ارتفاع تثبيت فعال Hm = ${"%.2f".format(hm)} م.",
            "انعكاسات الأسطح: السقف ${(surfaces.ceilingReflectance * 100).toInt()}% | الحوائط ${(surfaces.wallReflectance * 100).toInt()}% | الأرضية ${(surfaces.floorReflectance * 100).toInt()}% (${surfaces.finishType.titleAr}).",
            "معامل الاستخدام (UF) = ${"%.2f".format(uf)} | معامل الصيانة (MF) = ${"%.2f".format(mf)}.",
            "تم التقريب للأعلى إلى أقرب عدد صحيح من الوحدات ($quantity وحدة) بقدرة إجمالية ${"%.1f".format(totalWatts)} وات."
        )

        return LumenCalculationResult(
            roomAreaM2 = area,
            mountingHeightM = hm,
            roomIndexK = k,
            targetLux = safeTargetLux,
            utilizationFactor = uf,
            maintenanceFactor = mf,
            requiredLumens = requiredLumens,
            fixtureLumens = safeFixtureLumens,
            requiredQuantity = quantity,
            totalFixtureLumens = totalFixtureLumens,
            estimatedMaintainedLux = maintainedLux,
            totalWatts = totalWatts,
            powerDensityWPerM2 = lpd,
            assumptionsAr = assumptions,
            reflectanceImpactNoteAr = reflectanceNote
        )
    }

    fun calculateMixedLighting(
        geometry: RoomGeometryInput,
        surfaces: RoomSurfaceConfig,
        targetLux: Double,
        entries: List<MixedFixtureEntry>,
        maintenanceFactor: Double = 0.80
    ): MixedLightingResult {
        val area = calculateArea(geometry)
        val (eqL, eqW) = effectiveDimensions(geometry)
        val hm = (geometry.ceilingHeightM - geometry.workplaneHeightM - geometry.suspensionDropM)
            .coerceAtLeast(0.6)
        val k = calculateRoomIndexK(eqL, eqW, hm)
        val mf = maintenanceFactor.coerceIn(0.50, 0.95)

        if (entries.isEmpty()) {
            val defaultUf = calculateUtilizationFactor(k, surfaces)
            return MixedLightingResult(
                totalAreaM2 = area,
                targetLux = targetLux,
                uf = defaultUf,
                mf = mf,
                requiredTotalLumens = (targetLux * area) / (defaultUf * mf),
                deliveredTotalLumens = 0.0,
                estimatedMaintainedLux = 0.0,
                totalWatts = 0.0,
                powerDensityWPerM2 = 0.0,
                layerBreakdown = emptyMap(),
                groupBreakdown = emptyMap(),
                adequacyNoteAr = "لم تتم إضافة وحدات إضاءة بعد."
            )
        }

        var deliveredEffectiveLumens = 0.0
        var deliveredRawLumens = 0.0
        var totalWatts = 0.0
        val layerMap = mutableMapOf<LightingLayer, MutableList<Pair<MixedFixtureEntry, Double>>>()

        for (entry in entries) {
            if (entry.quantity <= 0) continue
            val entryUf = calculateUtilizationFactor(k, surfaces, entry.fixture.beamAngleDeg, entry.fixture.category)
            val effectiveFlux = entry.totalLumens * entryUf * mf
            val luxContrib = effectiveFlux / area
            deliveredEffectiveLumens += effectiveFlux
            deliveredRawLumens += entry.totalLumens
            totalWatts += entry.totalWatts
            layerMap.getOrPut(entry.layer) { mutableListOf() }.add(Pair(entry, luxContrib))
        }

        val weightedUf = if (deliveredRawLumens > 0.0) {
            (deliveredEffectiveLumens / (deliveredRawLumens * mf)).coerceIn(0.25, 0.90)
        } else {
            calculateUtilizationFactor(k, surfaces)
        }

        val requiredLumens = (targetLux * area) / (weightedUf * mf)
        val maintainedLux = deliveredEffectiveLumens / area
        val ratio = if (targetLux > 0) maintainedLux / targetLux else 0.0

        val layerSummaries = layerMap.mapValues { (_, list) ->
            LayerSummary(
                fixtureCount = list.sumOf { it.first.quantity },
                totalLumens = list.sumOf { it.first.totalLumens },
                totalWatts = list.sumOf { it.first.totalWatts },
                estimatedLuxContribution = list.sumOf { it.second }
            )
        }

        val groupSummaries = entries.filter { it.quantity > 0 }
            .groupBy { it.switchingGroup }
            .mapValues { (group, list) ->
                GroupSummary(
                    groupName = group,
                    fixtureCount = list.sumOf { it.quantity },
                    totalWatts = list.sumOf { it.totalWatts },
                    hasDimming = list.any { it.dimmable }
                )
            }

        val adequacyNote = when {
            ratio < 0.85 -> "الإضاءة الإجمالية أقل من المستهدف (${maintainedLux.toInt()} مقابل ${targetLux.toInt()} Lux) — يُنصح بزيادة الوحدات أو اختيار تدفق لومن أعلى."
            ratio > 1.35 -> "الإضاءة الإجمالية أعلى بكثير من المستهدف (${maintainedLux.toInt()} مقابل ${targetLux.toInt()} Lux) — يُنصح باستخدام Dimming أو تقسيم دوائر التشغيل."
            else -> "توزيع الإضاءة المختلطة متوازن ويحقق شدة إضاءة مصانة مناسبة (${maintainedLux.toInt()} Lux) ضمن النطاق المستهدف."
        }

        return MixedLightingResult(
            totalAreaM2 = area,
            targetLux = targetLux,
            uf = weightedUf,
            mf = mf,
            requiredTotalLumens = requiredLumens,
            deliveredTotalLumens = deliveredRawLumens,
            estimatedMaintainedLux = maintainedLux,
            totalWatts = totalWatts,
            powerDensityWPerM2 = totalWatts / area,
            layerBreakdown = layerSummaries,
            groupBreakdown = groupSummaries,
            adequacyNoteAr = adequacyNote
        )
    }
}
