package com.luxcalc.pro.domain.export

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.luxcalc.pro.data.local.DesignJsonSerializer
import com.luxcalc.pro.data.local.ProjectEntity
import com.luxcalc.pro.data.local.RoomDesignEntity
import com.luxcalc.pro.domain.model.BoqLineItem
import com.luxcalc.pro.domain.model.OpeningType
import com.luxcalc.pro.domain.model.PlacedFixture2D
import com.luxcalc.pro.domain.model.WallOpening2D
import com.luxcalc.pro.domain.model.WallSide
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ArabicReportExporter {

    private const val PAGE_W = 595 // A4 @ 72 dpi
    private const val PAGE_H = 842
    private const val MARGIN = 36f

    /**
     * Generates a genuine Arabic RTL engineering PDF report using Android's native HarfBuzz text shaping
     * (StaticLayout + PdfDocument), including project info, room details, surface reflectance,
     * Lumen Method assumptions, embedded 2D layout plan, BOQ table, and engineering notes.
     */
    fun generateProjectPdfReport(
        context: Context,
        project: ProjectEntity,
        rooms: List<RoomDesignEntity>,
        boqItems: List<BoqLineItem>
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeName = project.nameAr.replace(Regex("[^\\p{L}\\p{N}_-]"), "_").ifBlank { "LuxCalc_Report" }
        val pdfFile = File(reportsDir, "${safeName}_${System.currentTimeMillis()}.pdf")

        val doc = PdfDocument()
        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
        var canvas = page.canvas
        var y = MARGIN

        val primaryColor = Color.parseColor("#0F172A")
        val accentColor = Color.parseColor("#D97706")
        val borderColor = Color.parseColor("#CBD5E1")
        val lightBg = Color.parseColor("#F8FAFC")

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FDE68A")
            textSize = 10.5f
        }
        val headingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 9.5f
        }
        val mutedPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 8.5f
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = borderColor
            strokeWidth = 1f
        }

        fun ensureSpace(neededHeight: Float) {
            if (y + neededHeight > PAGE_H - MARGIN) {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
                canvas = page.canvas
                y = MARGIN
            }
        }

        fun drawRtlText(text: String, paint: TextPaint, left: Float, width: Float, topY: Float): Float {
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(20))
                .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
                .setLineSpacing(2f, 1.1f)
                .build()
            canvas.save()
            canvas.translate(left, topY)
            layout.draw(canvas)
            canvas.restore()
            return layout.height.toFloat()
        }

        // Header Banner
        fillPaint.color = primaryColor
        canvas.drawRoundRect(MARGIN, y, PAGE_W - MARGIN, y + 64f, 8f, 8f, fillPaint)
        drawRtlText("تقرير التصميم وحساب الإضاءة الهندسية — LuxCalc Pro", titlePaint, MARGIN + 12f, PAGE_W - 2 * MARGIN - 24f, y + 12f)
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        drawRtlText("المشروع: ${project.nameAr}  |  النوع: ${project.projectTypeAr}  |  التاريخ: $dateStr", subtitlePaint, MARGIN + 12f, PAGE_W - 2 * MARGIN - 24f, y + 40f)
        y += 78f

        // Project Summary Box
        val totalArea = rooms.sumOf { it.areaM2 }
        val totalWatts = rooms.sumOf { it.totalWatts }
        val totalFixtures = rooms.sumOf { it.totalFixturesCount }
        val totalLumens = rooms.sumOf { it.totalLumens }

        fillPaint.color = lightBg
        canvas.drawRoundRect(MARGIN, y, PAGE_W - MARGIN, y + 54f, 6f, 6f, fillPaint)
        canvas.drawRoundRect(MARGIN, y, PAGE_W - MARGIN, y + 54f, 6f, 6f, strokePaint)
        val summaryLine = "عدد الفراغات: ${rooms.size}   |   إجمالي المساحة: ${"%.1f".format(totalArea)} م²   |   إجمالي الوحدات: $totalFixtures   |   إجمالي القدرة: ${"%.1f".format(totalWatts)} Watt   |   إجمالي اللومن: ${totalLumens.toInt()} Lumen"
        drawRtlText("ملخص المشروع الهندسي", headingPaint, MARGIN + 10f, PAGE_W - 2 * MARGIN - 20f, y + 8f)
        drawRtlText(summaryLine, bodyPaint, MARGIN + 10f, PAGE_W - 2 * MARGIN - 20f, y + 28f)
        y += 68f

        // Per-Room Engineering Breakdown + Embedded 2D Layout
        for ((index, room) in rooms.withIndex()) {
            ensureSpace(235f)
            fillPaint.color = accentColor
            canvas.drawRect(PAGE_W - MARGIN - 4f, y, PAGE_W - MARGIN, y + 20f, fillPaint)
            drawRtlText("${index + 1}. فراغ: ${room.roomNameAr} (${room.spaceCategoryName})", headingPaint, MARGIN, PAGE_W - 2 * MARGIN - 10f, y + 2f)
            y += 26f

            val detailsText = buildString {
                appendLine("الأبعاد والشكل: ${room.shapeName} | الطول ${"%.2f".format(room.lengthM)} م × العرض ${"%.2f".format(room.widthM)} م | ارتفاع السقف ${"%.2f".format(room.ceilingHeightM)} م | المساحة ${"%.2f".format(room.areaM2)} م² | السقف: ${room.ceilingTypeAr}")
                appendLine("ألوان وتشطيبات الفراغ: الحوائط ${room.wallColorNameAr} (${(room.wallReflectance * 100).toInt()}%) | السقف ${room.ceilingColorNameAr} (${(room.ceilingReflectance * 100).toInt()}%) | الأرضية ${room.floorColorNameAr} (${(room.floorReflectance * 100).toInt()}%)")
                appendLine("نتائج طريقة اللومن (Lumen Method): المستهدف ${room.targetLux.toInt()} Lux | المصان التقديري ${room.maintainedLux.toInt()} Lux | UF = ${"%.2f".format(room.uf)} | MF = ${"%.2f".format(room.mf)} | القدرة ${"%.1f".format(room.totalWatts)} Watt | عدد الوحدات: ${room.totalFixturesCount}")
                append("التوزيع والمسافات (${room.layoutPatternName}): المسافة بين السبوتات ${"%.2f".format(room.spacingXM)} × ${"%.2f".format(room.spacingYM)} م | المسافة من الحائط ${"%.2f".format(room.wallDistXM)} × ${"%.2f".format(room.wallDistYM)} م")
            }
            val h = drawRtlText(detailsText, bodyPaint, MARGIN, PAGE_W - 2 * MARGIN, y)
            y += h + 8f

            // Draw 2D Lighting Plan Box directly onto PDF Canvas
            val planHeight = 135f
            ensureSpace(planHeight + 14f)
            val placements = DesignJsonSerializer.decodePlacements(room.placementsJson)
            val openings = DesignJsonSerializer.decodeOpenings(room.openingsJson)
            draw2DPlanOnCanvas(
                canvas = canvas,
                bounds = RectF(MARGIN, y, PAGE_W - MARGIN, y + planHeight),
                roomLengthM = room.lengthM,
                roomWidthM = room.widthM,
                placements = placements,
                openings = openings
            )
            y += planHeight + 14f
        }

        // Consolidated BOQ Table
        ensureSpace(120f)
        drawRtlText("جدول الكميات المجمع (BOQ)", headingPaint, MARGIN, PAGE_W - 2 * MARGIN, y)
        y += 22f

        val contentW = PAGE_W - 2 * MARGIN
        // Header row
        fillPaint.color = primaryColor
        canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 22f, fillPaint)
        val headerWhite = TextPaint(bodyPaint).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 9f
        }
        drawRtlText("إجمالي وات", headerWhite, MARGIN + 4f, 65f, y + 5f)
        drawRtlText("الكمية", headerWhite, MARGIN + 72f, 45f, y + 5f)
        drawRtlText("المواصفات الفنية", headerWhite, MARGIN + 120f, 185f, y + 5f)
        drawRtlText("وحدة الإضاءة", headerWhite, MARGIN + 310f, contentW - 314f, y + 5f)
        y += 22f

        for ((idx, item) in boqItems.withIndex()) {
            ensureSpace(26f)
            fillPaint.color = if (idx % 2 == 0) Color.WHITE else lightBg
            canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 24f, fillPaint)
            canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 24f, strokePaint)

            drawRtlText("${"%.1f".format(item.totalWatts)}W", bodyPaint, MARGIN + 4f, 65f, y + 6f)
            drawRtlText("${item.totalQuantity}", bodyPaint, MARGIN + 72f, 45f, y + 6f)
            drawRtlText(item.specsSummaryAr, mutedPaint, MARGIN + 120f, 185f, y + 6f)
            drawRtlText(item.fixtureNameAr, bodyPaint, MARGIN + 310f, contentW - 314f, y + 6f)
            y += 24f
        }

        y += 14f
        ensureSpace(70f)
        val notes = """
            ملاحظات هندسية:
            1. تعتمد الحسابات على طريقة اللومن (Lumen Method) لتقدير متوسط شدة الإضاءة المصانة، ولا تمثل محاكاة نقطية دقيقة (Point-by-Point).
            2. تؤثر ألوان وتشطيبات الحوائط والأسقف والأرضيات على معامل الاستخدام (UF) فقط دون تعديل المعيار القياسي المستهدف (Target Lux).
            3. يُنصح بمراجعة ملفات الفوتومتري (IES/LDT) الفعلية للمورد قبل التوريد النهائي للمشاريع الحرجة.
        """.trimIndent()
        drawRtlText(notes, mutedPaint, MARGIN, contentW, y)

        doc.finishPage(page)
        FileOutputStream(pdfFile).use { doc.writeTo(it) }
        doc.close()
        return pdfFile
    }

    /**
     * Generates Excel-compatible UTF-8 BOM CSV file for BOQ & room summary.
     */
    fun generateBoqCsvForExcel(
        context: Context,
        project: ProjectEntity,
        boqItems: List<BoqLineItem>
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeName = project.nameAr.replace(Regex("[^\\p{L}\\p{N}_-]"), "_").ifBlank { "LuxCalc_BOQ" }
        val csvFile = File(reportsDir, "${safeName}_BOQ_${System.currentTimeMillis()}.csv")

        val sb = StringBuilder()
        sb.append('﻿') // UTF-8 BOM so Microsoft Excel displays Arabic properly
        sb.appendLine("المشروع,${escapeCsv(project.nameAr)}")
        sb.appendLine("وحدة الإضاءة,التصنيف,المواصفات الفنية,الكمية,قدرة الوحدة (Watt),إجمالي القدرة (Watt),إجمالي التدفق (Lumen),الفراغات المستخدمة")
        for (item in boqItems) {
            sb.appendLine(
                listOf(
                    escapeCsv(item.fixtureNameAr),
                    escapeCsv(item.categoryAr),
                    escapeCsv(item.specsSummaryAr),
                    item.totalQuantity.toString(),
                    "%.1f".format(Locale.US, item.unitWatt),
                    "%.1f".format(Locale.US, item.totalWatts),
                    "%.0f".format(Locale.US, item.totalLumens),
                    escapeCsv(item.roomsUsedIn.joinToString(" / "))
                ).joinToString(",")
            )
        }
        csvFile.writeText(sb.toString(), Charsets.UTF_8)
        return csvFile
    }

    /**
     * Exports the 2D lighting plan as a crisp high-resolution PNG image.
     */
    fun export2DPlanAsPng(
        context: Context,
        roomNameAr: String,
        roomLengthM: Double,
        roomWidthM: Double,
        placements: List<PlacedFixture2D>,
        openings: List<WallOpening2D>
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(reportsDir, "Plan2D_${System.currentTimeMillis()}.png")
        val bmp = Bitmap.createBitmap(1400, 1000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("مخطط توزيع الإضاءة ثنائي الأبعاد: $roomNameAr (${"%.2f".format(roomLengthM)}م × ${"%.2f".format(roomWidthM)}م)", 1340f, 64f, headerPaint)

        draw2DPlanOnCanvas(
            canvas = canvas,
            bounds = RectF(60f, 95f, 1340f, 940f),
            roomLengthM = roomLengthM,
            roomWidthM = roomWidthM,
            placements = placements,
            openings = openings
        )
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return file
    }

    fun draw2DPlanOnCanvas(
        canvas: Canvas,
        bounds: RectF,
        roomLengthM: Double,
        roomWidthM: Double,
        placements: List<PlacedFixture2D>,
        openings: List<WallOpening2D>
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F8FAFC") }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(bounds, 8f, 8f, bgPaint)
        canvas.drawRoundRect(bounds, 8f, 8f, borderPaint)

        val pad = minOf(bounds.width(), bounds.height()) * 0.16f
        val availW = (bounds.width() - 2 * pad).coerceAtLeast(20f)
        val availH = (bounds.height() - 2 * pad).coerceAtLeast(20f)
        val scale = minOf(availW / roomLengthM.toFloat().coerceAtLeast(1f), availH / roomWidthM.toFloat().coerceAtLeast(1f))

        val drawW = roomLengthM.toFloat() * scale
        val drawH = roomWidthM.toFloat() * scale
        val originX = bounds.centerX() - drawW / 2f
        val originY = bounds.centerY() - drawH / 2f

        // Room floor fill & thick architectural walls
        val roomFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val wallStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#0F172A")
            strokeWidth = (bounds.height() * 0.014f).coerceIn(2f, 6f)
        }
        val roomRect = RectF(originX, originY, originX + drawW, originY + drawH)
        canvas.drawRect(roomRect, roomFill)
        canvas.drawRect(roomRect, wallStroke)

        // Dimension lines
        val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = (bounds.height() * 0.045f).coerceIn(8f, 22f)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${"%.2f".format(roomLengthM)} م", originX + drawW / 2f, originY - dimPaint.textSize * 0.6f, dimPaint)
        canvas.save()
        canvas.rotate(-90f, originX - dimPaint.textSize * 0.9f, originY + drawH / 2f)
        canvas.drawText("${"%.2f".format(roomWidthM)} م", originX - dimPaint.textSize * 0.9f, originY + drawH / 2f, dimPaint)
        canvas.restore()

        // Doors & Windows
        val doorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B45309")
            strokeWidth = wallStroke.strokeWidth * 1.4f
        }
        val windowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0284C7")
            strokeWidth = wallStroke.strokeWidth * 1.4f
        }
        for (op in openings) {
            val p = if (op.type == OpeningType.DOOR) doorPaint else windowPaint
            val off = op.offsetM.toFloat() * scale
            val w = op.widthM.toFloat() * scale
            when (op.wall) {
                WallSide.TOP -> canvas.drawLine(originX + off, originY, (originX + off + w).coerceAtMost(originX + drawW), originY, p)
                WallSide.BOTTOM -> canvas.drawLine(originX + off, originY + drawH, (originX + off + w).coerceAtMost(originX + drawW), originY + drawH, p)
                WallSide.LEFT -> canvas.drawLine(originX, originY + off, originX, (originY + off + w).coerceAtMost(originY + drawH), p)
                WallSide.RIGHT -> canvas.drawLine(originX + drawW, originY + off, originX + drawW, (originY + off + w).coerceAtMost(originY + drawH), p)
            }
        }

        // Fixtures
        val radius = (minOf(drawW, drawH) * 0.045f).coerceIn(5f, 18f)
        val spotFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F59E0B") }
        val spotRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#78350F")
            strokeWidth = (radius * 0.22f).coerceAtLeast(1.2f)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = (radius * 0.95f).coerceIn(6.5f, 16f)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        placements.forEachIndexed { idx, pf ->
            val cx = originX + (pf.xM.toFloat() * scale).coerceIn(radius, drawW - radius)
            val cy = originY + (pf.yM.toFloat() * scale).coerceIn(radius, drawH - radius)
            canvas.drawCircle(cx, cy, radius, spotFill)
            canvas.drawCircle(cx, cy, radius, spotRing)
            canvas.drawText("${pf.symbolCode}${idx + 1}", cx, cy + radius * 2.1f, labelPaint)
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة التقرير الهندسي").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun escapeCsv(value: String): String {
        val q = '"'
        val escaped = value.replace("$q", "$q$q")
        return "$q$escaped$q"
    }
}
