package com.luxcalc.pro.domain.model

enum class SpaceCategory(val titleAr: String, val titleEn: String) {
    RESIDENTIAL("سكني", "Residential"),
    COMMERCIAL("تجاري", "Commercial"),
    INDUSTRIAL("صناعي", "Industrial"),
    MEDICAL_EDUCATIONAL("طبي وتعليمي", "Medical & Educational"),
    OUTDOOR("خارجي", "Outdoor"),
    MODERN_SPECIALIZED("حديث ومتخصص", "Modern Specialized"),
    CUSTOM("فراغ مخصص", "Custom Space")
}

enum class RecommendationSource(val labelAr: String) {
    OFFICIAL_STANDARD("معيار قياسي معتمد (EN 12464-1 / IES)"),
    PROFESSIONAL_RECOMMENDATION("توصية هندسية مهنية"),
    LOCAL_HEURISTIC_ESTIMATE("تقدير ذكي محلي مبني على فئات مشابهة"),
    AI_ESTIMATE("تقدير ذكاء اصطناعي (Gemini AI)")
}

data class SpaceTemplate(
    val id: String,
    val nameAr: String,
    val category: SpaceCategory,
    val minLux: Int,
    val defaultLux: Int,
    val maxLux: Int,
    val recommendedCctAr: String,
    val defaultCctK: Int,
    val minCri: Int,
    val workplaneHeightM: Double = 0.80,
    val lightingRequirementsAr: String,
    val suggestedFixtureIds: List<String> = emptyList(),
    val source: RecommendationSource = RecommendationSource.PROFESSIONAL_RECOMMENDATION,
    val referenceAr: String = "إرشادات التصميم المعماري والهندسي",
    val isCustom: Boolean = false
)

enum class FixtureCategory(val titleAr: String) {
    SPOTLIGHTS("سبوت لايت"),
    TRACK("إضاءة التراك"),
    DECORATIVE("إضاءة ديكورية"),
    LINEAR("إضاءة خطية"),
    TECHNICAL("إضاءة تقنية وصناعية")
}

enum class LightingLayer(val titleAr: String) {
    GENERAL("إضاءة عامة (General)"),
    TASK("إضاءة مهام (Task)"),
    ACCENT("إضاءة توجيهية (Accent)"),
    DECORATIVE("إضاءة ديكورية (Decorative)"),
    AMBIENT("إضاءة محيطية (Ambient)")
}

data class LightingFixture(
    val id: String,
    val nameAr: String,
    val category: FixtureCategory,
    val defaultLayer: LightingLayer,
    val wattage: Double,
    val lumens: Double,
    val beamAngleDeg: Int,
    val cctK: Int,
    val cri: Int,
    val ipRating: String,
    val mountingTypeAr: String,
    val lightSourcesCount: Int = 1,
    val manufacturer: String = "قياسي هندسي",
    val modelCode: String = "STD-01",
    val photometricFileRef: String = "",
    val isCustom: Boolean = false
) {
    val efficacyLmPerW: Double
        get() = if (wattage > 0) lumens / wattage else 0.0
}

enum class RoomShape(val titleAr: String) {
    RECTANGULAR("مستطيل"),
    SQUARE("مربع"),
    CIRCULAR("دائري"),
    L_SHAPED("حرف L"),
    IRREGULAR_POLYGON("مضلع غير منتظم")
}

enum class SurfaceFinishType(val titleAr: String, val ufModifier: Double, val glareNoteAr: String) {
    MATTE("مطفي (Matte)", 1.00, "تشتيت متجانس للضوء مع تقليل الوهج المباشر"),
    SEMI_GLOSS("نصف لامع (Semi-Gloss)", 1.02, "توازن جيد بين الانعكاس وسهولة التنظيف"),
    GLOSSY("لامع (Glossy)", 1.03, "يزيد الانعكاس المرآوي؛ يُراعى تجنب الوهج المزعج"),
    TEXTURED("خشن / محبب (Textured)", 0.94, "يمتص جزءاً من الضوء ويقلل الانعكاس الفعلي"),
    REFLECTIVE("عاكس (Reflective)", 1.05, "انعكاس عالٍ جداً؛ يتطلب زوايا إضاءة مدروسة لمنع الوهج")
}

data class ColorPreset(
    val id: String,
    val nameAr: String,
    val hexColor: Long,
    val reflectance: Double
)

data class RoomSurfaceConfig(
    val wallColorNameAr: String = "أوف وايت",
    val wallHex: Long = 0xFFF5F3EE,
    val wallReflectance: Double = 0.70,
    val ceilingColorNameAr: String = "أبيض",
    val ceilingHex: Long = 0xFFFFFFFF,
    val ceilingReflectance: Double = 0.80,
    val floorColorNameAr: String = "سيراميك فاتح",
    val floorHex: Long = 0xFFE5DFD3,
    val floorReflectance: Double = 0.35,
    val finishType: SurfaceFinishType = SurfaceFinishType.MATTE
)

enum class LayoutPattern(val titleAr: String, val descriptionAr: String) {
    GRID("توزيع شبكي منتظم", "صفوف وأعمدة متساوية لتغطية عامة متجانسة"),
    SYMMETRIC("توزيع متماثل معماري", "توزيع متزن مع مسافة حائط تساوي نصف المسافة البينية"),
    PERIMETER("توزيع محيطي", "تركيز الوحدات حول محيط الفراغ لإبراز الجدران وترك الوسط للنجفة"),
    AXIAL("توزيع محوري", "ترتيب خطي على محور الممر أو الطاولة الرئيسية"),
    WALL_WASHER("غسل حوائط (Wall Washer)", "تثبيت قريب من الحائط بمسافة مدروسة لإضاءة الجدار بالكامل"),
    ACCENT("إضاءة توجيهية (Accent)", "توزيع موجه نحو نقاط الاهتمام واللوحات والقطع الديكورية")
}

data class MixedFixtureEntry(
    val id: String,
    val fixture: LightingFixture,
    val quantity: Int,
    val layer: LightingLayer = fixture.defaultLayer,
    val switchingGroup: String = "مجموعة 1",
    val dimmable: Boolean = false
) {
    val totalLumens: Double get() = fixture.lumens * quantity
    val totalWatts: Double get() = fixture.wattage * quantity
}

data class RoomGeometryInput(
    val shape: RoomShape = RoomShape.RECTANGULAR,
    val lengthM: Double = 5.0,
    val widthM: Double = 4.0,
    val ceilingHeightM: Double = 3.0,
    val suspensionDropM: Double = 0.0,
    val workplaneHeightM: Double = 0.80,
    val diameterM: Double = 4.0,
    val lCutoutLengthM: Double = 2.0,
    val lCutoutWidthM: Double = 1.5,
    val manualAreaM2: Double? = null,
    val ceilingTypeAr: String = "جبس بورد (Gypsum Board)"
)

data class LumenCalculationResult(
    val roomAreaM2: Double,
    val mountingHeightM: Double,
    val roomIndexK: Double,
    val targetLux: Double,
    val utilizationFactor: Double,
    val maintenanceFactor: Double,
    val requiredLumens: Double,
    val fixtureLumens: Double,
    val requiredQuantity: Int,
    val totalFixtureLumens: Double,
    val estimatedMaintainedLux: Double,
    val totalWatts: Double,
    val powerDensityWPerM2: Double,
    val assumptionsAr: List<String>,
    val reflectanceImpactNoteAr: String
)

data class MixedLightingResult(
    val totalAreaM2: Double,
    val targetLux: Double,
    val uf: Double,
    val mf: Double,
    val requiredTotalLumens: Double,
    val deliveredTotalLumens: Double,
    val estimatedMaintainedLux: Double,
    val totalWatts: Double,
    val powerDensityWPerM2: Double,
    val layerBreakdown: Map<LightingLayer, LayerSummary>,
    val groupBreakdown: Map<String, GroupSummary>,
    val adequacyNoteAr: String
)

data class LayerSummary(
    val fixtureCount: Int,
    val totalLumens: Double,
    val totalWatts: Double,
    val estimatedLuxContribution: Double
)

data class GroupSummary(
    val groupName: String,
    val fixtureCount: Int,
    val totalWatts: Double,
    val hasDimming: Boolean
)

data class PlacedFixture2D(
    val id: String,
    val xM: Double,
    val yM: Double,
    val fixtureNameAr: String,
    val fixtureId: String,
    val layer: LightingLayer,
    val symbolCode: String,
    val wattage: Double,
    val lumens: Double
)

enum class OpeningType(val titleAr: String) {
    DOOR("باب"),
    WINDOW("شباك")
}

enum class WallSide(val titleAr: String) {
    TOP("الشمال (أعلى)"),
    BOTTOM("الجنوب (أسفل)"),
    LEFT("اليسار"),
    RIGHT("اليمين")
}

data class WallOpening2D(
    val id: String,
    val type: OpeningType,
    val wall: WallSide,
    val offsetM: Double,
    val widthM: Double
)

data class SpacingLayoutResult(
    val pattern: LayoutPattern,
    val countX: Int,
    val countY: Int,
    val totalFixtures: Int,
    val spacingXM: Double,
    val spacingYM: Double,
    val wallDistanceXM: Double,
    val wallDistanceYM: Double,
    val maxRecommendedSpacingM: Double,
    val beamSpreadDiameterM: Double,
    val uniformityStatusAr: String,
    val placements: List<PlacedFixture2D>
)

data class SpaceRecognitionResult(
    val inputName: String,
    val matchedTemplate: SpaceTemplate,
    val explanationAr: String,
    val visualTasksAr: String,
    val needsClarification: Boolean = false,
    val clarificationQuestionAr: String? = null
)

data class BoqLineItem(
    val fixtureId: String,
    val fixtureNameAr: String,
    val categoryAr: String,
    val specsSummaryAr: String,
    val totalQuantity: Int,
    val unitWatt: Double,
    val totalWatts: Double,
    val totalLumens: Double,
    val roomsUsedIn: List<String>
)

data class ProjectTotals(
    val roomCount: Int,
    val totalAreaM2: Double,
    val totalFixtures: Int,
    val totalWatts: Double,
    val totalLumens: Double,
    val avgPowerDensityWPerM2: Double,
    val estimatedMonthlyKwh: Double,
    val estimatedMonthlyCost: Double
)
