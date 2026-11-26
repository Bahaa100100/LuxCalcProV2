@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.luxcalc.pro.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luxcalc.pro.domain.engine.LightingCalculationEngine
import com.luxcalc.pro.domain.model.*
import com.luxcalc.pro.ui.viewmodel.LuxCalcViewModel
import kotlin.math.hypot

@Composable
fun SpotlightSpacingScreen(
    viewModel: LuxCalcViewModel,
    onNavigate: (String) -> Unit
) {
    val ws by viewModel.workspace.collectAsState()
    var setbackText by remember(ws.customWallSetbackM) { mutableStateOf(ws.customWallSetbackM?.toString() ?: "") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("حاسبة توزيع السبوتات والمسافات الهندسية", style = MaterialTheme.typography.titleLarge)
            Text(
                "تحسب المسافات ديناميكياً بناءً على أبعاد الغرفة وارتفاع التثبيت وزاوية الإشعاع (${ws.primaryFixture.beamAngleDeg}°) دون قواعد ثابتة عشوائية.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Text("نمط التوزيع المعماري", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LayoutPattern.entries) { pat ->
                    FilterChip(
                        selected = ws.layoutPattern == pat,
                        onClick = { viewModel.updateLayoutPattern(pat) },
                        label = { Text(pat.titleAr) }
                    )
                }
            }
            Text(ws.layoutPattern.descriptionAr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }
        item {
            NumberInputField("تحديد مسافة مخصصة من الحائط بالمتر (اتركه فارغاً للحساب التلقائي)", setbackText) {
                setbackText = it
                viewModel.updateLayoutPattern(ws.layoutPattern, it.toDoubleOrNull())
            }
        }
        ws.spacingResult?.let { sp ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ResultRow("عدد السبوتات في الصف (X)", "${sp.countX}")
                        ResultRow("عدد الصفوف (Y)", "${sp.countY}")
                        ResultRow("إجمالي الوحدات بالمخطط", "${sp.totalFixtures} وحدة")
                        ResultRow("المسافة بين السبوتات (X)", "${"%.2f".format(sp.spacingXM)} م")
                        ResultRow("المسافة بين الصفوف (Y)", "${"%.2f".format(sp.spacingYM)} م")
                        ResultRow("المسافة من أول سبوت للحائط (X)", "${"%.2f".format(sp.wallDistanceXM)} م")
                        ResultRow("المسافة من أول سبوت للحائط (Y)", "${"%.2f".format(sp.wallDistanceYM)} م")
                        ResultRow("قطر مخروط الضوء على سطح العمل", "${"%.2f".format(sp.beamSpreadDiameterM)} م")
                        ResultRow("الحد الأقصى الموصى به للمسافة", "${"%.2f".format(sp.maxRecommendedSpacingM)} م")
                        HorizontalDivider()
                        Text(sp.uniformityStatusAr, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            item {
                Button(onClick = { onNavigate("plan_2d") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Architecture, null)
                    Spacer(Modifier.width(8.dp))
                    Text("عرض وتعديل التوزيع على مخطط 2D التفاعلي")
                }
            }
        }
    }
}

@Composable
fun Interactive2DPlanScreen(viewModel: LuxCalcViewModel) {
    val ws by viewModel.workspace.collectAsState()
    val context = LocalContext.current
    val (roomL, roomW) = remember(ws.geometry) { LightingCalculationEngine.effectiveDimensions(ws.geometry) }

    var selectedFixtureId by remember { mutableStateOf<String?>(null) }
    var tapToAddMode by remember { mutableStateOf(false) }
    var showDimensions by remember { mutableStateOf(true) }
    var showOpeningDialog by remember { mutableStateOf(false) }

    // Canvas coordinate transform state for accurate touch hit-testing & dragging
    var originPx by remember { mutableStateOf(Offset.Zero) }
    var scalePxPerM by remember { mutableFloatStateOf(1f) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("مخطط الإضاءة 2D — ${ws.roomNameAr}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "اسحب أي وحدة لتحريكها | ${ws.placements2D.size} وحدة | ${"%.1f".format(roomL)}م × ${"%.1f".format(roomW)}م",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = { viewModel.exportCurrent2DPlanPng(context) }) {
                    Icon(Icons.Default.Image, "تصدير PNG")
                }
                IconButton(onClick = { viewModel.saveCurrentRoomToProject() }) {
                    Icon(Icons.Default.Save, "حفظ المخطط")
                }
            }
        }

        // Mode & Action Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = tapToAddMode,
                    onClick = { tapToAddMode = !tapToAddMode },
                    label = { Text(if (tapToAddMode) "وضع الإضافة باللمس: مفعل" else "إضافة وحدة باللمس") },
                    leadingIcon = { Icon(Icons.Default.AddLocationAlt, null, Modifier.size(18.dp)) }
                )
            }
            item {
                AssistChip(
                    onClick = { viewModel.recalculateWorkspace(preserveManualPlacements = false) },
                    label = { Text("توزيع تلقائي") },
                    leadingIcon = { Icon(Icons.Default.AutoFixHigh, null, Modifier.size(18.dp)) }
                )
            }
            item {
                AssistChip(
                    onClick = { showOpeningDialog = true },
                    label = { Text("إضافة باب/شباك") },
                    leadingIcon = { Icon(Icons.Default.DoorFront, null, Modifier.size(18.dp)) }
                )
            }
            item {
                FilterChip(
                    selected = showDimensions,
                    onClick = { showDimensions = !showDimensions },
                    label = { Text("إظهار الأبعاد") }
                )
            }
            if (selectedFixtureId != null) {
                item {
                    AssistChip(
                        onClick = {
                            selectedFixtureId?.let { viewModel.removePlacedFixture(it) }
                            selectedFixtureId = null
                        },
                        label = { Text("حذف الوحدة المحددة") },
                        leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(18.dp)) }
                    )
                }
            }
        }

        // Interactive 2D Blueprint Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                .pointerInput(ws.placements2D, tapToAddMode, roomL, roomW) {
                    detectTapGestures { offset ->
                        if (scalePxPerM <= 0f) return@detectTapGestures
                        val tapXM = ((offset.x - originPx.x) / scalePxPerM).toDouble()
                        val tapYM = ((offset.y - originPx.y) / scalePxPerM).toDouble()
                        if (tapToAddMode && tapXM in 0.1..(roomL - 0.1) && tapYM in 0.1..(roomW - 0.1)) {
                            viewModel.addManualFixtureAt(tapXM, tapYM)
                        } else {
                            val hit = ws.placements2D.minByOrNull { p ->
                                val px = originPx.x + (p.xM.toFloat() * scalePxPerM)
                                val py = originPx.y + (p.yM.toFloat() * scalePxPerM)
                                hypot((offset.x - px).toDouble(), (offset.y - py).toDouble())
                            }
                            if (hit != null) {
                                val px = originPx.x + (hit.xM.toFloat() * scalePxPerM)
                                val py = originPx.y + (hit.yM.toFloat() * scalePxPerM)
                                selectedFixtureId = if (hypot((offset.x - px).toDouble(), (offset.y - py).toDouble()) < 55.0) hit.id else null
                            }
                        }
                    }
                }
                .pointerInput(ws.placements2D, roomL, roomW) {
                    var draggingId: String? = null
                    detectDragGestures(
                        onDragStart = { start ->
                            val hit = ws.placements2D.minByOrNull { p ->
                                val px = originPx.x + (p.xM.toFloat() * scalePxPerM)
                                val py = originPx.y + (p.yM.toFloat() * scalePxPerM)
                                hypot((start.x - px).toDouble(), (start.y - py).toDouble())
                            }
                            if (hit != null) {
                                val px = originPx.x + (hit.xM.toFloat() * scalePxPerM)
                                val py = originPx.y + (hit.yM.toFloat() * scalePxPerM)
                                if (hypot((start.x - px).toDouble(), (start.y - py).toDouble()) < 65.0) {
                                    draggingId = hit.id
                                    selectedFixtureId = hit.id
                                }
                            }
                        },
                        onDragEnd = { draggingId = null },
                        onDragCancel = { draggingId = null },
                        onDrag = { change, _ ->
                            val id = draggingId
                            if (id != null) {
                                change.consume()
                                if (scalePxPerM > 0f) {
                                    val newXM = ((change.position.x - originPx.x) / scalePxPerM).toDouble()
                                    val newYM = ((change.position.y - originPx.y) / scalePxPerM).toDouble()
                                    viewModel.movePlacedFixture(id, newXM, newYM)
                                }
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pad = 80f
                val availW = (size.width - 2 * pad).coerceAtLeast(40f)
                val availH = (size.height - 2 * pad).coerceAtLeast(40f)
                val scale = minOf(availW / roomL.toFloat(), availH / roomW.toFloat())
                val drawW = roomL.toFloat() * scale
                val drawH = roomW.toFloat() * scale
                val ox = (size.width - drawW) / 2f
                val oy = (size.height - drawH) / 2f
                originPx = Offset(ox, oy)
                scalePxPerM = scale

                // Engineering Grid (every 0.5m)
                val gridPaint = Color(0xFFE2E8F0)
                var gx = 0.5
                while (gx < roomL) {
                    val x = ox + (gx.toFloat() * scale)
                    drawLine(gridPaint, Offset(x, oy), Offset(x, oy + drawH), strokeWidth = 1f)
                    gx += 0.5
                }
                var gy = 0.5
                while (gy < roomW) {
                    val y = oy + (gy.toFloat() * scale)
                    drawLine(gridPaint, Offset(ox, y), Offset(ox + drawW, y), strokeWidth = 1f)
                    gy += 0.5
                }

                // Room Floor & Architectural Walls
                drawRect(Color.White, Offset(ox, oy), Size(drawW, drawH))
                drawRect(Color(0xFF0F172A), Offset(ox, oy), Size(drawW, drawH), style = Stroke(width = 7f))

                // Doors & Windows
                ws.openings2D.forEach { op ->
                    val col = if (op.type == OpeningType.DOOR) Color(0xFFB45309) else Color(0xFF0284C7)
                    val off = op.offsetM.toFloat() * scale
                    val w = op.widthM.toFloat() * scale
                    when (op.wall) {
                        WallSide.TOP -> drawLine(col, Offset(ox + off, oy), Offset((ox + off + w).coerceAtMost(ox + drawW), oy), 12f)
                        WallSide.BOTTOM -> drawLine(col, Offset(ox + off, oy + drawH), Offset((ox + off + w).coerceAtMost(ox + drawW), oy + drawH), 12f)
                        WallSide.LEFT -> drawLine(col, Offset(ox, oy + off), Offset(ox, (oy + off + w).coerceAtMost(oy + drawH)), 12f)
                        WallSide.RIGHT -> drawLine(col, Offset(ox + drawW, oy + off), Offset(ox + drawW, (oy + off + w).coerceAtMost(oy + drawH)), 12f)
                    }
                }

                // Lighting Rows & Wall Distance Guides
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                if (showDimensions && ws.placements2D.isNotEmpty()) {
                    val first = ws.placements2D.first()
                    val fx = ox + first.xM.toFloat() * scale
                    val fy = oy + first.yM.toFloat() * scale
                    drawLine(Color(0xFF0284C7), Offset(ox, fy), Offset(fx, fy), 2f, pathEffect = dashEffect)
                    drawLine(Color(0xFF0284C7), Offset(fx, oy), Offset(fx, fy), 2f, pathEffect = dashEffect)
                }

                val textPaint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.parseColor("#0F172A")
                    textSize = 26f
                    textAlign = Paint.Align.CENTER
                }

                // Outer room dimensions
                if (showDimensions) {
                    drawContext.canvas.nativeCanvas.drawText("${"%.2f".format(roomL)} م", ox + drawW / 2f, oy - 24f, textPaint)
                    drawContext.canvas.nativeCanvas.drawText("${"%.2f".format(roomW)} م", ox - 38f, oy + drawH / 2f, textPaint)
                }

                // Fixtures
                ws.placements2D.forEachIndexed { idx, p ->
                    val cx = ox + p.xM.toFloat() * scale
                    val cy = oy + p.yM.toFloat() * scale
                    val isSel = p.id == selectedFixtureId
                    drawCircle(
                        color = if (isSel) Color(0xFF0284C7) else Color(0xFFF59E0B),
                        radius = if (isSel) 20f else 15f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = if (isSel) 20f else 15f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 3f)
                    )
                    val labelPaint = Paint(textPaint).apply { textSize = 22f }
                    drawContext.canvas.nativeCanvas.drawText("${p.symbolCode}${idx + 1}", cx, cy + 38f, labelPaint)
                }
            }
        }

        // Selected Fixture Inspector Bar
        val selected = ws.placements2D.firstOrNull { it.id == selectedFixtureId }
        if (selected != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(
                    Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selected.fixtureNameAr} | X = ${"%.2f".format(selected.xM)} م | Y = ${"%.2f".format(selected.yM)} م",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { viewModel.removePlacedFixture(selected.id); selectedFixtureId = null }) {
                        Text("إزالة")
                    }
                }
            }
        }
    }

    if (showOpeningDialog) {
        var opType by remember { mutableStateOf(OpeningType.DOOR) }
        var wallSide by remember { mutableStateOf(WallSide.BOTTOM) }
        var offsetText by remember { mutableStateOf("1.0") }
        var widthText by remember { mutableStateOf("1.0") }

        AlertDialog(
            onDismissRequest = { showOpeningDialog = false },
            title = { Text("إضافة باب أو شباك على المخطط") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OpeningType.entries.forEach { t ->
                            FilterChip(selected = opType == t, onClick = { opType = t }, label = { Text(t.titleAr) })
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(WallSide.entries) { w ->
                            FilterChip(selected = wallSide == w, onClick = { wallSide = w }, label = { Text(w.titleAr) })
                        }
                    }
                    NumberInputField("المسافة من الزاوية (م)", offsetText) { offsetText = it }
                    NumberInputField("عرض الفتحة (م)", widthText) { widthText = it }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addOpening(opType, wallSide, offsetText.toDoubleOrNull() ?: 1.0, widthText.toDoubleOrNull() ?: 1.0)
                    showOpeningDialog = false
                }) { Text("إضافة") }
            },
            dismissButton = { TextButton(onClick = { showOpeningDialog = false }) { Text("إلغاء") } }
        )
    }
}
