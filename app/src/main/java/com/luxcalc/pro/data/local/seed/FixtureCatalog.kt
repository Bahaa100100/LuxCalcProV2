package com.luxcalc.pro.data.local.seed

import com.luxcalc.pro.domain.model.FixtureCategory
import com.luxcalc.pro.domain.model.LightingFixture
import com.luxcalc.pro.domain.model.LightingLayer

object FixtureCatalog {

    private fun f(
        id: String, name: String, cat: FixtureCategory, layer: LightingLayer,
        w: Double, lm: Double, beam: Int, cct: Int, cri: Int,
        ip: String, mount: String, sources: Int = 1,
        mfg: String = "مواصفات هندسية قياسية", model: String = "PRO-LED"
    ) = LightingFixture(id, name, cat, layer, w, lm, beam, cct, cri, ip, mount, sources, mfg, model, "", false)

    val allFixtures: List<LightingFixture> = listOf(
        // ==================== سبوت لايت (12) ====================
        f("spot_single", "سبوت مفرد (7W)", FixtureCategory.SPOTLIGHTS, LightingLayer.GENERAL,
            7.0, 630.0, 38, 3000, 90, "IP20", "غاطس بالسقف (Recessed)", 1, model = "SP-7W-38"),
        f("spot_double", "سبوت مزدوج (2x7W)", FixtureCategory.SPOTLIGHTS, LightingLayer.GENERAL,
            14.0, 1260.0, 38, 3000, 90, "IP20", "غاطس مزدوج (Recessed)", 2, model = "SP-2X7W"),
        f("spot_triple", "سبوت ثلاثي (3x7W)", FixtureCategory.SPOTLIGHTS, LightingLayer.GENERAL,
            21.0, 1890.0, 38, 3000, 90, "IP20", "غاطس ثلاثي (Recessed)", 3, model = "SP-3X7W"),
        f("spot_adjustable", "سبوت متحرك (Adjustable 10W)", FixtureCategory.SPOTLIGHTS, LightingLayer.ACCENT,
            10.0, 900.0, 36, 3000, 90, "IP20", "غاطس متحرك", 1, model = "SP-ADJ-10W"),
        f("spot_fixed", "سبوت ثابت (10W)", FixtureCategory.SPOTLIGHTS, LightingLayer.GENERAL,
            10.0, 950.0, 60, 4000, 85, "IP20", "غاطس ثابت", 1, model = "SP-FIX-10W"),
        f("spot_cob", "سبوت COB عالي الأداء (12W)", FixtureCategory.SPOTLIGHTS, LightingLayer.GENERAL,
            12.0, 1140.0, 36, 3000, 95, "IP20", "غاطس COB", 1, model = "COB-12W-95"),
        f("spot_anti_glare", "سبوت Anti-Glare عميق (10W)", FixtureCategory.SPOTLIGHTS, LightingLayer.GENERAL,
            10.0, 880.0, 38, 3000, 92, "IP20", "غاطس عميق مانع للوهج", 1, model = "AG-10W-UGR16"),
        f("downlight", "داون لايت (Downlight 18W)", FixtureCategory.SPOTLIGHTS, LightingLayer.GENERAL,
            18.0, 1800.0, 100, 4000, 85, "IP20", "غاطس واسع الانتشار", 1, model = "DL-18W-100"),
        f("mini_spot", "ميني سبوت (Mini Spot 3W)", FixtureCategory.SPOTLIGHTS, LightingLayer.ACCENT,
            3.0, 260.0, 24, 3000, 90, "IP20", "غاطس صغير", 1, model = "MINI-3W-24"),
        f("pinhole", "سبوت بين هول (Pinhole 7W)", FixtureCategory.SPOTLIGHTS, LightingLayer.ACCENT,
            7.0, 560.0, 24, 3000, 92, "IP20", "غاطس فتحة دقيقة", 1, model = "PIN-7W-24"),
        f("gimbal", "سبوت جيمبال (Gimbal 12W)", FixtureCategory.SPOTLIGHTS, LightingLayer.ACCENT,
            12.0, 1080.0, 36, 3000, 90, "IP20", "غاطس متعدد المحاور", 1, model = "GMB-12W"),
        f("waterproof_spot", "سبوت مقاوم للماء IP65 (9W)", FixtureCategory.SPOTLIGHTS, LightingLayer.GENERAL,
            9.0, 810.0, 45, 3000, 90, "IP65", "غاطس محكم للرطوبة", 1, model = "WP-SP-9W"),

        // ==================== إضاءة التراك (6) ====================
        f("track_single", "تراك سبوت مفرد (15W)", FixtureCategory.TRACK, LightingLayer.ACCENT,
            15.0, 1350.0, 36, 3000, 92, "IP20", "مثبت على مجرى Track", 1, model = "TRK-15W"),
        f("track_triple", "تراك ثلاثي الرؤوس (3x12W)", FixtureCategory.TRACK, LightingLayer.GENERAL,
            36.0, 3240.0, 36, 3000, 90, "IP20", "مجرى سطحي 3 كشافات", 3, model = "TRK-3X12W"),
        f("magnetic_track", "تراك مغناطيسي وحدة سبوت (12W)", FixtureCategory.TRACK, LightingLayer.GENERAL,
            12.0, 1020.0, 38, 3000, 92, "IP20", "مجرى مغناطيسي 48V", 1, model = "MAG-12W"),
        f("track_spot", "تراك سبوت معارض (25W)", FixtureCategory.TRACK, LightingLayer.ACCENT,
            25.0, 2375.0, 24, 3500, 95, "IP20", "مجرى Track تجاري", 1, model = "TRK-PRO-25W"),
        f("track_linear", "تراك خطي (Magnetic Linear 18W)", FixtureCategory.TRACK, LightingLayer.GENERAL,
            18.0, 1620.0, 110, 4000, 90, "IP20", "مجرى مغناطيسي خطي", 1, model = "MAG-LIN-18W"),
        f("track_wall_washer", "تراك وول واشر (20W)", FixtureCategory.TRACK, LightingLayer.ACCENT,
            20.0, 1750.0, 65, 3000, 95, "IP20", "مجرى Track غير متماثل", 1, model = "TRK-WW-20W"),

        // ==================== إضاءة ديكورية (7) ====================
        f("chandelier", "نجفة معمارية (Chandelier 48W)", FixtureCategory.DECORATIVE, LightingLayer.DECORATIVE,
            48.0, 4320.0, 150, 3000, 90, "IP20", "معلق مركزي بالسقف", 8, model = "CHND-48W"),
        f("pendant", "معلقة مفردة (Pendant 12W)", FixtureCategory.DECORATIVE, LightingLayer.DECORATIVE,
            12.0, 1020.0, 90, 3000, 90, "IP20", "معلق بالسقف", 1, model = "PND-12W"),
        f("linear_pendant", "معلقة خطية فوق طاولة (30W)", FixtureCategory.DECORATIVE, LightingLayer.TASK,
            30.0, 2850.0, 110, 3000, 90, "IP20", "معلق خطي", 1, model = "LPND-30W"),
        f("wall_sconce", "أبليك حائط (Wall Sconce 8W)", FixtureCategory.DECORATIVE, LightingLayer.DECORATIVE,
            8.0, 640.0, 120, 3000, 85, "IP20", "مثبت على الحائط", 1, model = "WSC-8W"),
        f("table_lamp", "أباجورة طاولة (Table Lamp 9W)", FixtureCategory.DECORATIVE, LightingLayer.AMBIENT,
            9.0, 810.0, 140, 2700, 85, "IP20", "متحرك على طاولة", 1, model = "TBL-9W"),
        f("floor_lamp", "إضاءة أرضية (Floor Lamp 15W)", FixtureCategory.DECORATIVE, LightingLayer.AMBIENT,
            15.0, 1350.0, 130, 2700, 90, "IP20", "قائم أرضي", 1, model = "FLR-15W"),
        f("bedside_light", "إضاءة جانبية للسرير (5W)", FixtureCategory.DECORATIVE, LightingLayer.TASK,
            5.0, 420.0, 36, 2700, 90, "IP20", "حائطي بجانب السرير", 1, model = "BSD-5W"),

        // ==================== إضاءة خطية (8) ====================
        f("led_strip", "شريط ليد LED Strip (متر 12W)", FixtureCategory.LINEAR, LightingLayer.AMBIENT,
            12.0, 1080.0, 120, 3000, 90, "IP20", "شريط لاصق / بروفايل (لكل متر)", 1, model = "STRP-12W/M"),
        f("cove_light", "كوف لايت غير مباشر (متر 10W)", FixtureCategory.LINEAR, LightingLayer.AMBIENT,
            10.0, 850.0, 120, 3000, 90, "IP20", "مخفي داخل بيت النور (لكل متر)", 1, model = "COVE-10W/M"),
        f("profile_recessed", "بروفايل ألومنيوم غاطس (متر 18W)", FixtureCategory.LINEAR, LightingLayer.GENERAL,
            18.0, 1620.0, 110, 4000, 90, "IP20", "غاطس بالجبس (لكل متر)", 1, model = "PRF-REC-18W"),
        f("profile_surface", "بروفايل ألومنيوم سطحي (متر 18W)", FixtureCategory.LINEAR, LightingLayer.GENERAL,
            18.0, 1650.0, 110, 4000, 90, "IP20", "مثبت سطحي (لكل متر)", 1, model = "PRF-SRF-18W"),
        f("profile_pendant", "بروفايل معلق مكتبي (1.2م - 36W)", FixtureCategory.LINEAR, LightingLayer.GENERAL,
            36.0, 3600.0, 105, 4000, 90, "IP20", "معلق بواير (120 سم)", 1, model = "PRF-PND-36W"),
        f("under_cabinet", "إضاءة أسفل الدواليب (متر 10W)", FixtureCategory.LINEAR, LightingLayer.TASK,
            10.0, 950.0, 110, 4000, 90, "IP44", "أسفل وحدات المطبخ (لكل متر)", 1, model = "UCB-10W/M"),
        f("wardrobe_light", "إضاءة داخل الدواليب (متر 8W)", FixtureCategory.LINEAR, LightingLayer.TASK,
            8.0, 720.0, 110, 3500, 90, "IP20", "داخل الخزائن مع حساس باب", 1, model = "WRD-8W/M"),
        f("tv_backlight", "إضاءة خلف التلفزيون (متر 7W)", FixtureCategory.LINEAR, LightingLayer.AMBIENT,
            7.0, 560.0, 120, 4000, 90, "IP20", "خلفي للشاشة (Bias Light)", 1, model = "TVB-7W/M"),

        // ==================== إضاءة تقنية وصناعية وخارجية (13) ====================
        f("panel_led", "بانل LED 60x60 (40W)", FixtureCategory.TECHNICAL, LightingLayer.GENERAL,
            40.0, 4400.0, 110, 4000, 85, "IP20", "غاطس سقف معلق 60x60", 1, model = "PNL-6060-40W"),
        f("troffer", "تروفر مكتبي منخفض الوهج (36W)", FixtureCategory.TECHNICAL, LightingLayer.GENERAL,
            36.0, 3960.0, 90, 4000, 85, "IP20", "غاطس 60x60 UGR<19", 1, model = "TRF-36W"),
        f("high_bay", "هاي باي صناعي (High Bay 150W)", FixtureCategory.TECHNICAL, LightingLayer.GENERAL,
            150.0, 21000.0, 90, 5000, 80, "IP65", "معلق للأسقف المرتفعة (>6م)", 1, model = "HB-150W-140LM"),
        f("low_bay", "لو باي صناعي (Low Bay 80W)", FixtureCategory.TECHNICAL, LightingLayer.GENERAL,
            80.0, 10800.0, 110, 5000, 80, "IP65", "معلق أو سطحي (4-6م)", 1, model = "LB-80W"),
        f("floodlight", "كشاف فلود لايت (Floodlight 100W)", FixtureCategory.TECHNICAL, LightingLayer.GENERAL,
            100.0, 12000.0, 110, 5000, 80, "IP66", "خارجي بقاعدة توجيه", 1, model = "FLD-100W-IP66"),
        f("facade_flood", "كشاف واجهات معماري (24W)", FixtureCategory.TECHNICAL, LightingLayer.ACCENT,
            24.0, 2400.0, 30, 3000, 85, "IP66", "خارجي للواجهات", 1, model = "FAC-24W-30"),
        f("wall_washer", "وول واشر (Wall Washer 18W)", FixtureCategory.TECHNICAL, LightingLayer.ACCENT,
            18.0, 1620.0, 45, 3000, 90, "IP65", "غسل حوائط داخلي/خارجي", 1, model = "WW-18W"),
        f("bollard", "عمود حدائق Bollard (10W)", FixtureCategory.TECHNICAL, LightingLayer.GENERAL,
            10.0, 850.0, 120, 3000, 80, "IP65", "مثبت أرضي للممرات", 1, model = "BLD-10W"),
        f("garden_spike", "كشاف حدائق Spike (7W)", FixtureCategory.TECHNICAL, LightingLayer.ACCENT,
            7.0, 630.0, 30, 3000, 85, "IP65", "وتد أرضي للأشجار", 1, model = "SPK-7W"),
        f("step_light", "إضاءة درجات Step Light (3W)", FixtureCategory.TECHNICAL, LightingLayer.ACCENT,
            3.0, 210.0, 60, 3000, 80, "IP65", "غاطس بالحائط/الدرج", 1, model = "STP-3W"),
        f("emergency_light", "كشاف طوارئ Emergency (5W)", FixtureCategory.TECHNICAL, LightingLayer.GENERAL,
            5.0, 450.0, 120, 6000, 75, "IP44", "طوارئ ببطارية 3 ساعات", 1, model = "EMG-5W-3H"),
        f("industrial_luminaire", "كشاف صناعي خطي (60W)", FixtureCategory.TECHNICAL, LightingLayer.GENERAL,
            60.0, 8100.0, 110, 5000, 80, "IP65", "معلق للمصانع والورش", 1, model = "IND-LIN-60W"),
        f("waterproof_linear", "كشاف مقاوم للماء IP65 (36W)", FixtureCategory.TECHNICAL, LightingLayer.GENERAL,
            36.0, 4320.0, 120, 4500, 80, "IP65", "سطحي/معلق مقاوم للرطوبة", 1, model = "WP-LIN-36W")
    )
}
