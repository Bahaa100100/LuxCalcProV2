package com.luxcalc.pro.domain.engine

import com.luxcalc.pro.domain.model.*
import java.util.UUID
import kotlin.math.*

object SpotlightSpacingEngine {

    fun calculateLayout(
        roomLengthM: Double,
        roomWidthM: Double,
        ceilingHeightM: Double,
        workplaneHeightM: Double = 0.80,
        fixture: LightingFixture,
        desiredQuantity: Int,
        pattern: LayoutPattern = LayoutPattern.SYMMETRIC,
        customWallSetbackM: Double? = null
    ): SpacingLayoutResult {
        val length = roomLengthM.coerceAtLeast(1.0)
        val width = roomWidthM.coerceAtLeast(1.0)
        val hm = (ceilingHeightM - workplaneHeightM).coerceAtLeast(0.8)

        val halfAngleRad = Math.toRadians((fixture.beamAngleDeg.coerceIn(10, 150)) / 2.0)
        val beamSpreadD = 2.0 * hm * tan(halfAngleRad)

        val shr = when {
            fixture.beamAngleDeg <= 24 -> 0.55
            fixture.beamAngleDeg <= 38 -> 0.75
            fixture.beamAngleDeg <= 60 -> 1.00
            fixture.beamAngleDeg <= 90 -> 1.20
            else -> 1.35
        }
        val maxRecommendedSpacing = (hm * shr).coerceIn(0.6, 4.5)
        val targetCount = desiredQuantity.coerceAtLeast(1)

        val placements = mutableListOf<PlacedFixture2D>()
        val symbol = symbolForCategory(fixture.category)

        return when (pattern) {
            LayoutPattern.GRID, LayoutPattern.SYMMETRIC -> {
                val aspect = length / width
                var cols = max(1, round(sqrt(targetCount * aspect)).toInt())
                var rows = max(1, ceil(targetCount.toDouble() / cols).toInt())

                while (cols * rows < targetCount) {
                    if (length / cols >= width / rows) cols++ else rows++
                }

                val wallX = customWallSetbackM?.coerceIn(0.25, length / 3.0) ?: (length / (2.0 * cols))
                val wallY = customWallSetbackM?.coerceIn(0.25, width / 3.0) ?: (width / (2.0 * rows))

                val spacingX = if (cols > 1) (length - 2.0 * wallX) / (cols - 1) else 0.0
                val spacingY = if (rows > 1) (width - 2.0 * wallY) / (rows - 1) else 0.0

                for (r in 0 until rows) {
                    val y = if (rows == 1) width / 2.0 else wallY + r * spacingY
                    for (c in 0 until cols) {
                        val x = if (cols == 1) length / 2.0 else wallX + c * spacingX
                        placements.add(newPlacement(x, y, fixture, symbol))
                    }
                }

                val actualMaxS = max(spacingX, spacingY)
                SpacingLayoutResult(
                    pattern = pattern,
                    countX = cols,
                    countY = rows,
                    totalFixtures = placements.size,
                    spacingXM = spacingX,
                    spacingYM = spacingY,
                    wallDistanceXM = wallX,
                    wallDistanceYM = wallY,
                    maxRecommendedSpacingM = maxRecommendedSpacing,
                    beamSpreadDiameterM = beamSpreadD,
                    uniformityStatusAr = evaluateUniformity(actualMaxS, maxRecommendedSpacing, beamSpreadD),
                    placements = placements
                )
            }

            LayoutPattern.PERIMETER -> {
                val offset = customWallSetbackM?.coerceIn(0.35, min(length, width) * 0.25)
                    ?: (hm * 0.30).coerceIn(0.45, 0.80)
                val innerL = (length - 2 * offset).coerceAtLeast(0.4)
                val innerW = (width - 2 * offset).coerceAtLeast(0.4)
                val perim = 2 * (innerL + innerW)
                val count = max(4, targetCount)
                val step = perim / count

                val colsTop = max(2, round(innerL / step).toInt() + 1)
                val rowsSide = max(0, round(innerW / step).toInt() - 1)

                for (i in 0 until colsTop) {
                    val x = offset + (innerL * i / (colsTop - 1))
                    placements.add(newPlacement(x, offset, fixture, symbol))
                    placements.add(newPlacement(x, width - offset, fixture, symbol))
                }
                for (j in 1..rowsSide) {
                    val y = offset + (innerW * j / (rowsSide + 1))
                    placements.add(newPlacement(offset, y, fixture, symbol))
                    placements.add(newPlacement(length - offset, y, fixture, symbol))
                }

                val sx = innerL / (colsTop - 1)
                val sy = innerW / (rowsSide + 1)
                SpacingLayoutResult(
                    pattern = pattern,
                    countX = colsTop,
                    countY = rowsSide + 2,
                    totalFixtures = placements.size,
                    spacingXM = sx,
                    spacingYM = sy,
                    wallDistanceXM = offset,
                    wallDistanceYM = offset,
                    maxRecommendedSpacingM = maxRecommendedSpacing,
                    beamSpreadDiameterM = beamSpreadD,
                    uniformityStatusAr = "توزيع محيطي بمسافة ${"%.2f".format(offset)} م من الحوائط؛ مناسب لترك الوسط لنجفة أو إضاءة مركزية.",
                    placements = placements
                )
            }

            LayoutPattern.AXIAL -> {
                val alongX = length >= width
                val n = targetCount
                val span = if (alongX) length else width
                val wallDist = customWallSetbackM?.coerceIn(0.25, span / 3.0) ?: (span / (2.0 * n))
                val spacing = if (n > 1) (span - 2.0 * wallDist) / (n - 1) else 0.0

                for (i in 0 until n) {
                    val pos = if (n == 1) span / 2.0 else wallDist + i * spacing
                    val x = if (alongX) pos else length / 2.0
                    val y = if (alongX) width / 2.0 else pos
                    placements.add(newPlacement(x, y, fixture, symbol))
                }

                SpacingLayoutResult(
                    pattern = pattern,
                    countX = if (alongX) n else 1,
                    countY = if (alongX) 1 else n,
                    totalFixtures = n,
                    spacingXM = if (alongX) spacing else 0.0,
                    spacingYM = if (alongX) 0.0 else spacing,
                    wallDistanceXM = if (alongX) wallDist else length / 2.0,
                    wallDistanceYM = if (alongX) width / 2.0 else wallDist,
                    maxRecommendedSpacingM = maxRecommendedSpacing,
                    beamSpreadDiameterM = beamSpreadD,
                    uniformityStatusAr = evaluateUniformity(spacing, maxRecommendedSpacing, beamSpreadD),
                    placements = placements
                )
            }

            LayoutPattern.WALL_WASHER -> {
                val wallOffset = customWallSetbackM?.coerceIn(0.25, 1.2)
                    ?: (ceilingHeightM / 3.2).coerceIn(0.40, 0.90)
                val n = max(2, targetCount)
                val endMargin = wallOffset.coerceAtMost(length / 4.0)
                val spacing = if (n > 1) (length - 2.0 * endMargin) / (n - 1) else 0.0

                for (i in 0 until n) {
                    val x = endMargin + i * spacing
                    placements.add(newPlacement(x, wallOffset, fixture, "WW"))
                }

                SpacingLayoutResult(
                    pattern = pattern,
                    countX = n,
                    countY = 1,
                    totalFixtures = n,
                    spacingXM = spacing,
                    spacingYM = 0.0,
                    wallDistanceXM = endMargin,
                    wallDistanceYM = wallOffset,
                    maxRecommendedSpacingM = wallOffset * 1.25,
                    beamSpreadDiameterM = beamSpreadD,
                    uniformityStatusAr = "غسل حائط على بُعد ${"%.2f".format(wallOffset)} م من الجدار لتوزيع ناعم ومتجانس على الحائط.",
                    placements = placements
                )
            }

            LayoutPattern.ACCENT -> {
                val n = targetCount
                val wallOffset = customWallSetbackM?.coerceIn(0.30, 1.2) ?: (hm * 0.28).coerceIn(0.40, 0.85)
                val marginX = (length / (n + 1)).coerceAtLeast(0.4)
                for (i in 0 until n) {
                    val x = marginX * (i + 1)
                    val y = if (i % 2 == 0) wallOffset else (width - wallOffset)
                    placements.add(newPlacement(x.coerceIn(0.3, length - 0.3), y.coerceIn(0.3, width - 0.3), fixture, "AC"))
                }
                val sx = if (n > 1) length / (n + 1) else 0.0
                SpacingLayoutResult(
                    pattern = pattern,
                    countX = n,
                    countY = 1,
                    totalFixtures = n,
                    spacingXM = sx,
                    spacingYM = width - 2 * wallOffset,
                    wallDistanceXM = marginX,
                    wallDistanceYM = wallOffset,
                    maxRecommendedSpacingM = maxRecommendedSpacing,
                    beamSpreadDiameterM = beamSpreadD,
                    uniformityStatusAr = "إضاءة توجيهية لإبراز العناصر المعمارية والديكورية؛ يُنصح بزاوية توجيه 30° نحو العنصر.",
                    placements = placements
                )
            }
        }
    }

    private fun newPlacement(x: Double, y: Double, fixture: LightingFixture, symbol: String) =
        PlacedFixture2D(
            id = UUID.randomUUID().toString(),
            xM = x,
            yM = y,
            fixtureNameAr = fixture.nameAr,
            fixtureId = fixture.id,
            layer = fixture.defaultLayer,
            symbolCode = symbol,
            wattage = fixture.wattage,
            lumens = fixture.lumens
        )

    private fun symbolForCategory(category: FixtureCategory): String = when (category) {
        FixtureCategory.SPOTLIGHTS -> "SP"
        FixtureCategory.TRACK -> "TR"
        FixtureCategory.DECORATIVE -> "DC"
        FixtureCategory.LINEAR -> "LN"
        FixtureCategory.TECHNICAL -> "TC"
    }

    private fun evaluateUniformity(actualSpacing: Double, maxRecommended: Double, beamSpread: Double): String {
        if (actualSpacing <= 0.01) return "وحدة واحدة مركزية — تأكد من كفاية زاوية الانتشار (${"%.2f".format(beamSpread)} م)."
        return when {
            actualSpacing <= maxRecommended * 0.95 ->
                "تجانس ممتاز: المسافة (${"%.2f".format(actualSpacing)} م) أقل من الحد الأقصى الموصى به (${"%.2f".format(maxRecommended)} م) مما يضمن تداخل مخاريط الضوء."
            actualSpacing <= maxRecommended * 1.15 ->
                "تجانس جيد ومقبول معمارياً (المسافة ${"%.2f".format(actualSpacing)} م قريبة من الحد الموصى به ${"%.2f".format(maxRecommended)} م)."
            else ->
                "تحذير تجانس: المسافة (${"%.2f".format(actualSpacing)} م) تتجاوز الحد الموصى به (${"%.2f".format(maxRecommended)} م) لزاوية الإشعاع الحالية؛ يُنصح بزيادة العدد أو اختيار زاوية انتشار أوسع."
        }
    }
}
