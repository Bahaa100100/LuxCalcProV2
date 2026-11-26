package com.luxcalc.pro.domain.engine

import com.luxcalc.pro.domain.model.*

object SmartSpaceRecognizer {

    private data class DomainRule(
        val keywords: List<String>,
        val category: SpaceCategory,
        val minLux: Int,
        val defaultLux: Int,
        val maxLux: Int,
        val cctAr: String,
        val defaultCctK: Int,
        val minCri: Int,
        val workplaneM: Double,
        val tasksAr: String,
        val reqAr: String,
        val fixtureIds: List<String>,
        val referenceCategoryAr: String
    )

    private val semanticRules = listOf(
        DomainRule(
            keywords = listOf("gaming", "جيمنج", "العاب", "esports", "vr", "افتراضي", "بلايستيشن"),
            category = SpaceCategory.MODERN_SPECIALIZED,
            minLux = 150, defaultLux = 250, maxLux = 350,
            cctAr = "4000K محايد مع إضاءة محيطية خلف الشاشات",
            defaultCctK = 4000, minCri = 85, workplaneM = 0.75,
            tasksAr = "مشاهدة شاشات عالية التباين لفترات طويلة مع حساسية شديدة للوهج والانعكاس",
            reqAr = "إضاءة عامة غير مباشرة أو Anti-Glare خافتة (UGR < 19) + إضاءة خلف الشاشة (Bias Lighting) لتقليل إجهاد العين.",
            fixtureIds = listOf("spot_anti_glare", "led_strip", "tv_backlight", "cove_light"),
            referenceCategoryAr = "مكاتب الشاشات وغرف الوسائط الحديثة"
        ),
        DomainRule(
            keywords = listOf("streaming", "بث", "يوتيوب", "youtube", "بودكاست", "podcast", "محتوي", "صناعه محتوي", "تصوير", "كاميرا", "استوديو"),
            category = SpaceCategory.MODERN_SPECIALIZED,
            minLux = 300, defaultLux = 500, maxLux = 1000,
            cctAr = "5000K - 5600K (ضوء نهاري متوازن مع الكاميرات)",
            defaultCctK = 5000, minCri = 95, workplaneM = 0.85,
            tasksAr = "التصوير الفوتوغرافي أو الفيديو وإظهار ألوان البشرة والمنتجات بدقة عالية بدون وميض",
            reqAr = "وحدات عالية تجسيد الألوان CRI >= 95 وخالية من الوميض (Flicker-Free) مع إضاءة رئيسية ومكملة وخلفية منفصلة الدوائر.",
            fixtureIds = listOf("track_spot", "panel_led", "profile_pendant", "spot_cob"),
            referenceCategoryAr = "استوديوهات التصوير والإنتاج المرئي"
        ),
        DomainRule(
            keywords = listOf("مونتاج", "تعديل الوان", "color grading", "جرافيك", "تصميم", "معماري", "رسم", "هندسي", "cad"),
            category = SpaceCategory.MODERN_SPECIALIZED,
            minLux = 300, defaultLux = 500, maxLux = 750,
            cctAr = "4000K - 5000K (محايد دقيق الألوان)",
            defaultCctK = 4500, minCri = 90, workplaneM = 0.75,
            tasksAr = "تدقيق تفاصيل دقيقة ومخططات وألوان على الشاشات أو طاولات الرسم",
            reqAr = "إضاءة متجانسة منخفضة الوهج (UGR <= 19) مع إمكانية التعتيم (Dimming) أثناء العمل على الشاشات.",
            fixtureIds = listOf("profile_pendant", "spot_anti_glare", "panel_led", "track_linear"),
            referenceCategoryAr = "مكاتب التصميم المعماري والعمل البصري الدقيق"
        ),
        DomainRule(
            keywords = listOf("مجوهرات", "ذهب", "الماس", "ساعات", "عرض", "جاليري", "لوحات", "تحف", "متحف"),
            category = SpaceCategory.COMMERCIAL,
            minLux = 500, defaultLux = 750, maxLux = 1500,
            cctAr = "3000K للذهب / 4000K-5000K للألماس والفضة",
            defaultCctK = 4000, minCri = 95, workplaneM = 0.90,
            tasksAr = "إبراز البريق والتفاصيل الدقيقة والألوان الحقيقية للمعروضات",
            reqAr = "سبوتات ضيقة الزاوية (15°-24°) عالية CRI >= 95 فوق فاترينات العرض مع إضاءة عامة متوازنة.",
            fixtureIds = listOf("spot_cob", "mini_spot", "track_spot", "magnetic_track"),
            referenceCategoryAr = "معارض المجوهرات والمتاجر التخصصية"
        ),
        DomainRule(
            keywords = listOf("سيرفر", "خوادم", "شبكات", "تحكم", "مراقبه", "امن", "كهرباء", "ميكانيكا", "طلمبات"),
            category = SpaceCategory.INDUSTRIAL,
            minLux = 200, defaultLux = 300, maxLux = 500,
            cctAr = "4000K - 5000K (أبيض محايد للوضوح)",
            defaultCctK = 4000, minCri = 80, workplaneM = 0.85,
            tasksAr = "قراءة الكابلات واللوحات والأجهزة وإجراء الصيانة الدورية بأمان",
            reqAr = "إضاءة عمودية جيدة على واجهات الراكات واللوحات مع إضاءة طوارئ (Emergency Light) ومقاومة غبار IP54 عند الحاجة.",
            fixtureIds = listOf("panel_led", "waterproof_linear", "emergency_light", "troffer"),
            referenceCategoryAr = "الغرف الفنية وغرف التحكم والمعدات"
        ),
        DomainRule(
            keywords = listOf("طباعه", "ثلاثيه الابعاد", "3d", "دهان", "دهانات", "طلاء", "ورشه", "نجاره", "حداده", "صيانه", "فحص", "تجارب", "معمل", "مختبر", "كيمياء", "علميه"),
            category = SpaceCategory.INDUSTRIAL,
            minLux = 500, defaultLux = 750, maxLux = 1000,
            cctAr = "4500K - 5500K (محايد إلى نهاري لفحص الألوان والعيوب)",
            defaultCctK = 5000, minCri = 90, workplaneM = 0.85,
            tasksAr = "مهام تقنية وفحص سطوح وألوان ومكونات دقيقة تتطلب وضوحاً عالياً",
            reqAr = "إضاءة مهام عالية الشدة خالية من الظلال القاسية مع معامل تجسيد ألوان CRI >= 90 وحماية IP54/IP65 في بيئات الدهانات والمعامل.",
            fixtureIds = listOf("waterproof_linear", "high_bay", "panel_led", "profile_surface"),
            referenceCategoryAr = "المعامل والورش الفنية وغرف فحص الجودة"
        ),
        DomainRule(
            keywords = listOf("اطفال", "حضانه", "لعب", "العاب اطفال", "مذاكره", "تعليم"),
            category = SpaceCategory.RESIDENTIAL,
            minLux = 250, defaultLux = 300, maxLux = 450,
            cctAr = "3500K - 4000K (مريح ومحفز للانتباه دون قسوة)",
            defaultCctK = 4000, minCri = 85, workplaneM = 0.60,
            tasksAr = "اللعب والحركة والقراءة والأنشطة اليومية مع أمان بصري عالٍ",
            reqAr = "إضاءة منتشرة خالية من الوهج والوميض مع إمكانية التعتيم لوقت الراحة.",
            fixtureIds = listOf("downlight", "spot_anti_glare", "cove_light", "pendant"),
            referenceCategoryAr = "غرف الأطفال والفراغات التعليمية"
        ),
        DomainRule(
            keywords = listOf("تامل", "يوجا", "سبا", "مساج", "استرخاء", "wellness", "ساونا", "جاكوزي"),
            category = SpaceCategory.MODERN_SPECIALIZED,
            minLux = 75, defaultLux = 120, maxLux = 200,
            cctAr = "2700K - 3000K (دافئ مريح للأعصاب)",
            defaultCctK = 2700, minCri = 85, workplaneM = 0.0,
            tasksAr = "الاسترخاء والراحة النفسية مع تجنب الإضاءة السقفية المباشرة في خط النظر",
            reqAr = "إضاءة غير مباشرة (كوف لايت / وول واشر) قابلة للتعتيم، وحماية IP65 في المناطق الرطبة.",
            fixtureIds = listOf("cove_light", "wall_washer", "step_light", "spot_anti_glare"),
            referenceCategoryAr = "فراغات الاسترخاء والضيافة والسبا"
        ),
        DomainRule(
            keywords = listOf("مطبخ", "كوفي", "قهوه", "بار", "طعام", "اكل", "مخبز"),
            category = SpaceCategory.RESIDENTIAL,
            minLux = 250, defaultLux = 350, maxLux = 500,
            cctAr = "3000K - 4000K (إظهار ألوان الطعام بشكل شهي وطبيعي)",
            defaultCctK = 3500, minCri = 90, workplaneM = 0.90,
            tasksAr = "تحضير الطعام والتعامل مع الأدوات الحادة وتنظيف الأسطح",
            reqAr = "إضاءة عامة متجانسة + إضاءة مهام أسفل الدواليب (Under-Cabinet) لمنع ظل الجسم على الكاونتر.",
            fixtureIds = listOf("downlight", "under_cabinet", "spot_fixed", "linear_pendant"),
            referenceCategoryAr = "المطابخ ومناطق تحضير الطعام"
        )
    )

    private val ambiguousTokens = setOf(
        "غرفه", "غرفة", "مساحه", "مساحة", "مكان", "فراغ", "جديد", "جديده", "مشروع", "room", "space", "area", "test", "1", "2"
    )

    fun normalizeArabic(text: String): String {
        return text.trim().lowercase()
            .replace(Regex("[\u064B-\u065F\u0670]"), "")
            .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
            .replace("ة", "ه").replace("ى", "ي")
            .replace(Regex("\\s+"), " ")
    }

    fun analyzeSpace(
        rawName: String,
        catalog: List<SpaceTemplate>
    ): SpaceRecognitionResult {
        val cleaned = rawName.trim()
        val norm = normalizeArabic(cleaned)

        val exact = catalog.firstOrNull { normalizeArabic(it.nameAr) == norm }
        if (exact != null) {
            return SpaceRecognitionResult(
                inputName = cleaned,
                matchedTemplate = exact,
                explanationAr = "تم التعرف على الفراغ من قاعدة البيانات المحلية (${exact.source.labelAr}). المرجع: ${exact.referenceAr}",
                visualTasksAr = exact.lightingRequirementsAr,
                needsClarification = false
            )
        }

        val partial = catalog.firstOrNull {
            val catNorm = normalizeArabic(it.nameAr)
            norm.length >= 4 && (catNorm.contains(norm) || norm.contains(catNorm))
        }
        if (partial != null) {
            val adapted = partial.copy(
                id = "custom_${norm.hashCode()}",
                nameAr = cleaned,
                source = RecommendationSource.PROFESSIONAL_RECOMMENDATION,
                referenceAr = "بناءً على فئة مشابهة: ${partial.nameAr} (${partial.referenceAr})",
                isCustom = true
            )
            return SpaceRecognitionResult(
                inputName = cleaned,
                matchedTemplate = adapted,
                explanationAr = "تم ربط ($cleaned) بفئة مشابهة (${partial.nameAr}). يمكنك تعديل شدة الإضاءة وحفظ الفراغ في قاعدتك الخاصة.",
                visualTasksAr = partial.lightingRequirementsAr,
                needsClarification = false
            )
        }

        var bestRule: DomainRule? = null
        var bestScore = 0
        for (rule in semanticRules) {
            val score = rule.keywords.count { kw -> norm.contains(normalizeArabic(kw)) }
            if (score > bestScore) {
                bestScore = score
                bestRule = rule
            }
        }

        if (bestRule != null && bestScore > 0) {
            val template = SpaceTemplate(
                id = "custom_${System.currentTimeMillis()}",
                nameAr = cleaned,
                category = bestRule.category,
                minLux = bestRule.minLux,
                defaultLux = bestRule.defaultLux,
                maxLux = bestRule.maxLux,
                recommendedCctAr = bestRule.cctAr,
                defaultCctK = bestRule.defaultCctK,
                minCri = bestRule.minCri,
                workplaneHeightM = bestRule.workplaneM,
                lightingRequirementsAr = bestRule.reqAr,
                suggestedFixtureIds = bestRule.fixtureIds,
                source = RecommendationSource.LOCAL_HEURISTIC_ESTIMATE,
                referenceAr = "تقدير مهني مقارن بفئة: ${bestRule.referenceCategoryAr} (ليس معياراً قياسياً منفصلاً)",
                isCustom = true
            )
            return SpaceRecognitionResult(
                inputName = cleaned,
                matchedTemplate = template,
                explanationAr = "تحليل ذكي محلي: تم تصنيف ($cleaned) ضمن (${bestRule.referenceCategoryAr}). شدة الإضاءة الموصى بها ${bestRule.defaultLux} Lux (النطاق ${bestRule.minLux}-${bestRule.maxLux} Lux) مع CRI >= ${bestRule.minCri}.",
                visualTasksAr = bestRule.tasksAr,
                needsClarification = false
            )
        }

        val tokens = norm.split(" ").filter { it.isNotBlank() }
        val isAmbiguous = tokens.isEmpty() || tokens.all { it in ambiguousTokens } || norm.length < 3
        val fallback = SpaceTemplate(
            id = "custom_${System.currentTimeMillis()}",
            nameAr = cleaned.ifBlank { "فراغ مخصص" },
            category = SpaceCategory.CUSTOM,
            minLux = 200,
            defaultLux = 300,
            maxLux = 400,
            recommendedCctAr = "3500K - 4000K (محايد متوازن)",
            defaultCctK = 4000,
            minCri = 85,
            workplaneHeightM = 0.80,
            lightingRequirementsAr = "توزيع عام متجانس قابل للتعديل حسب طبيعة النشاط البصري الفعلي في الفراغ.",
            suggestedFixtureIds = listOf("spot_anti_glare", "downlight", "cove_light"),
            source = RecommendationSource.LOCAL_HEURISTIC_ESTIMATE,
            referenceAr = "تقدير مبدئي عام (لا يوجد معيار قياسي محدد لهذا الاسم)",
            isCustom = true
        )

        val question = if (isAmbiguous) {
            "اسم الفراغ عام أو غير محدد. ما النشاط الرئيسي داخل هذا الفراغ؟ (مثلاً: عمل مكتبي دقيق، استرخاء، عرض منتجات، أو ورشة فنية؟)"
        } else {
            "فراغ ($cleaned) غير مسجل كمعيار مباشر. هل يتطلب مهام بصرية دقيقة (500 Lux) أم إضاءة معيشية عامة (200-300 Lux)؟"
        }

        return SpaceRecognitionResult(
            inputName = cleaned,
            matchedTemplate = fallback,
            explanationAr = "تم تقديم تقدير هندسي مبدئي (300 Lux) حتى يتم تحديد طبيعة المهمة البصرية، ويمكنك تعديل جميع القيم أو استخدام تحليل Gemini AI الاختياري.",
            visualTasksAr = "مهام بصرية عامة متوسطة الدقة (تقدير مبدئي).",
            needsClarification = true,
            clarificationQuestionAr = question
        )
    }
}
