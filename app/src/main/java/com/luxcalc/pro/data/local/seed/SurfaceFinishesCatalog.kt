package com.luxcalc.pro.data.local.seed

import com.luxcalc.pro.domain.model.ColorPreset

object SurfaceFinishesCatalog {

    val wallColors = listOf(
        ColorPreset("w_white", "أبيض", 0xFFFFFFFF, 0.80),
        ColorPreset("w_offwhite", "أوف وايت", 0xFFF5F3EE, 0.72),
        ColorPreset("w_light_beige", "بيج فاتح", 0xFFE8DCC8, 0.65),
        ColorPreset("w_light_gray", "رمادي فاتح", 0xFFD1D5DB, 0.58),
        ColorPreset("w_med_gray", "رمادي متوسط", 0xFF9CA3AF, 0.42),
        ColorPreset("w_dark_gray", "رمادي غامق", 0xFF4B5563, 0.22),
        ColorPreset("w_black", "أسود", 0xFF18181B, 0.08),
        ColorPreset("w_brown", "بني", 0xFF6B4423, 0.20),
        ColorPreset("w_wood", "خشبي", 0xFFA47551, 0.32),
        ColorPreset("w_dark", "ألوان داكنة", 0xFF283548, 0.16)
    )

    val ceilingColors = listOf(
        ColorPreset("c_white", "أبيض", 0xFFFFFFFF, 0.82),
        ColorPreset("c_offwhite", "أوف وايت", 0xFFF7F5F0, 0.75),
        ColorPreset("c_beige", "بيج", 0xFFE5D8C0, 0.65),
        ColorPreset("c_light_gray", "رمادي فاتح", 0xFFD4D8DE, 0.58),
        ColorPreset("c_dark_gray", "رمادي غامق", 0xFF4B5563, 0.22),
        ColorPreset("c_black", "أسود", 0xFF141416, 0.08),
        ColorPreset("c_wood", "خشب", 0xFF9C6B43, 0.30),
        ColorPreset("c_colored_gypsum", "جبس ملون", 0xFFD6CFC2, 0.52)
    )

    val floorColors = listOf(
        ColorPreset("f_light_ceramic", "سيراميك فاتح", 0xFFE6E0D4, 0.40),
        ColorPreset("f_white_marble", "رخام أبيض", 0xFFF2F2F0, 0.48),
        ColorPreset("f_dark_marble", "رخام غامق", 0xFF33363B, 0.14),
        ColorPreset("f_porcelain", "بورسلين", 0xFFDAD5CB, 0.36),
        ColorPreset("f_light_wood", "خشب فاتح", 0xFFCBA378, 0.30),
        ColorPreset("f_dark_wood", "خشب غامق", 0xFF5C3A21, 0.14),
        ColorPreset("f_concrete", "خرسانة", 0xFF8C8F94, 0.22),
        ColorPreset("f_light_carpet", "سجاد فاتح", 0xFFCFC7B8, 0.28),
        ColorPreset("f_dark_carpet", "سجاد غامق", 0xFF3F3D40, 0.12)
    )

    val ceilingTypesAr = listOf(
        "جبس بورد (Gypsum Board)",
        "سقف خرساني مطلي",
        "بلاطات أرمسترونج (60x60)",
        "سقف خشبي معماري",
        "سقف مكشوف صناعي (Exposed)",
        "سقف مشدود (Stretch Ceiling)"
    )
}
