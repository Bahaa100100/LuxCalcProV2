@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.luxcalc.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.luxcalc.pro.data.local.seed.SurfaceFinishesCatalog
import com.luxcalc.pro.domain.engine.LightingCalculationEngine
import com.luxcalc.pro.domain.model.*
import com.luxcalc.pro.ui.viewmodel.LuxCalcViewModel
import kotlin.math.ceil

@Composable
fun DashboardScreen(
    viewModel: LuxCalcViewModel,
    onNavigate: (String) -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    val rooms by viewModel.activeProjectRooms.collectAsState()
    val activePid by viewModel.activeProjectId.collectAsState()
    val activeProject = projects.firstOrNull { it.id == activePid }
    val totals = remember(rooms) { viewModel.computeProjectTotals(rooms) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("LuxCalc Pro", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondaryContainer)
                    Text(
                        "مصمم وحاسبة الإضاءة الهندسية",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "المشروع النشط: ${activeProject?.nameAr ?: "بدون مشروع"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.startNewRoom(); onNavigate("new_calc") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.AddCircle, null)
                            Spacer(Modifier.width(6.dp))
                            Text("حساب إضاءة جديد")
                        }
                        OutlinedButton(
                            onClick = { onNavigate("plan_2d") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Text("مخطط 2D")
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricStatCard("الفراغات", "${totals.roomCount}", "غرفة", Modifier.weight(1f))
                MetricStatCard("إجمالي الوحدات", "${totals.totalFixtures}", "وحدة", Modifier.weight(1f))
                MetricStatCard("القدرة الكلية", "${"%.0f".format(totals.totalWatts)}", "Watt", Modifier.weight(1f))
            }
        }

        item {
            Text("الوصول السريع للأدوات الهندسية", style = MaterialTheme.typography.titleMedium)
        }

        item {
            val shortcuts = listOf(
                Triple("حساب إضاءة جديد", Icons.Default.Calculate, "new_calc"),
                Triple("توزيع السبوتات", Icons.Default.GridOn, "spacing"),
                Triple("مخطط الإضاءة 2D", Icons.Default.Architecture, "plan_2d"),
                Triple("قاعدة الفراغات (110+)", Icons.Default.MeetingRoom, "spaces"),
                Triple("مكتبة الوحدات (50+)", Icons.Default.Lightbulb, "fixtures"),
                Triple("حاسبة Lux سريعة", Icons.Default.FlashOn, "quick_lux"),
                Triple("إدارة المشاريع", Icons.Default.FolderCopy, "projects"),
                Triple("جدول الكميات BOQ", Icons.Default.TableChart, "boq"),
                Triple("تقارير PDF / Excel", Icons.Default.PictureAsPdf, "reports")
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shortcuts.chunked(3).forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { (title, icon, route) ->
                            ElevatedCard(
                                modifier = Modifier.weight(1f).clickable { onNavigate(route) },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(Modifier.height(6.dp))
                                    Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (rooms.isNotEmpty()) {
            item { Text("فراغات المشروع النشط", style = MaterialTheme.typography.titleMedium) }
            items(rooms.take(5)) { room ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.loadRoomIntoWorkspace(room)
                        onNavigate("new_calc")
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(room.roomNameAr, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${"%.1f".format(room.areaM2)} م² | المستهدف ${room.targetLux.toInt()} Lux | المصان ${room.maintainedLux.toInt()} Lux",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AssistChip(onClick = { viewModel.loadRoomIntoWorkspace(room); onNavigate("plan_2d") }, label = {
                            Text("${room.totalFixturesCount} وحدة")
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun MetricStatCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(unit, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCalculationScreen(
    viewModel: LuxCalcViewModel,
    onNavigate: (String) -> Unit
) {
    val ws by viewModel.workspace.collectAsState()
    val fixtures by viewModel.allFixtures.collectAsState()
    var activeTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("1. الفراغ", "2. الأبعاد", "3. الألوان", "4. الوحدات", "5. النتائج")

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = activeTab, edgePadding = 8.dp) {
            tabs.forEachIndexed { idx, title ->
                Tab(selected = activeTab == idx, onClick = { activeTab = idx }, text = { Text(title) })
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (activeTab) {
                // TAB 0: Smart Space Recognition & Custom Room Name
                0 -> {
                    item {
                        Text("التعرف الذكي على الفراغ (يعمل بدون إنترنت)", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = ws.roomNameAr,
                            onValueChange = { viewModel.updateRoomNameAndAnalyze(it) },
                            label = { Text("اسم الفراغ (اكتب أي اسم مثل: غرفة تصوير منتجات، غرفة Gaming)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { viewModel.updateRoomNameAndAnalyze(ws.roomNameAr) }) {
                                    Icon(Icons.Default.AutoAwesome, "تحليل")
                                }
                            }
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onNavigate("spaces") }) {
                                Icon(Icons.Default.Search, null)
                                Spacer(Modifier.width(4.dp))
                                Text("اختيار من القاعدة")
                            }
                            FilledTonalButton(
                                onClick = { viewModel.runGeminiSpaceAnalysis() },
                                enabled = !ws.aiLoading
                            ) {
                                Icon(Icons.Default.Psychology, null)
                                Spacer(Modifier.width(4.dp))
                                Text(if (ws.aiLoading) "جارٍ التحليل..." else "تحليل متقدم Gemini AI")
                            }
                        }
                        ws.aiErrorAr?.let { err ->
                            Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    ws.recognitionResult?.let { rec ->
                        item {
                            ElevatedCard(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("تحليل الفراغ: ${rec.matchedTemplate.nameAr}", style = MaterialTheme.typography.titleMedium)
                                    AssistChip(onClick = {}, label = { Text(rec.matchedTemplate.source.labelAr) })
                                    Text("المهام البصرية: ${rec.visualTasksAr}", style = MaterialTheme.typography.bodyMedium)
                                    Text("التفسير الهندسي: ${rec.explanationAr}", style = MaterialTheme.typography.bodyMedium)
                                    HorizontalDivider()
                                    Text(
                                        "نطاق Lux الموصى به: ${rec.matchedTemplate.minLux} – ${rec.matchedTemplate.maxLux} Lux (الافتراضي ${rec.matchedTemplate.defaultLux} Lux)",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("درجة اللون المقترحة (CCT): ${rec.matchedTemplate.recommendedCctAr}")
                                    Text("معامل تجسيد الألوان (CRI): >= ${rec.matchedTemplate.minCri}")
                                    if (rec.needsClarification && rec.clarificationQuestionAr != null) {
                                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                            Text(
                                                "سؤال توضيحي: ${rec.clarificationQuestionAr}",
                                                modifier = Modifier.padding(10.dp),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    if (rec.matchedTemplate.isCustom) {
                                        TextButton(onClick = {
                                            viewModel.saveCustomSpace(rec.matchedTemplate.copy(defaultLux = ws.targetLux.toInt()))
                                        }) {
                                            Icon(Icons.Default.BookmarkAdd, null)
                                            Spacer(Modifier.width(4.dp))
                                            Text("حفظ كفراغ مخصص في القاعدة")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("شدة الإضاءة المستهدفة (Target Lux): ${ws.targetLux.toInt()} Lux", fontWeight = FontWeight.SemiBold)
                        Slider(
                            value = ws.targetLux.toFloat().coerceIn(20f, 1500f),
                            onValueChange = { viewModel.updateTargetLux(it.toDouble()) },
                            valueRange = 20f..1500f
                        )
                        NumberInputField("أو أدخل Target Lux يدوياً", ws.targetLux.toInt().toString()) {
                            it.toDoubleOrNull()?.let { v -> viewModel.updateTargetLux(v) }
                        }
                    }
                }

                // TAB 1: Dimensions & Room Shape
                1 -> {
                    item {
                        Text("شكل الفراغ والأبعاد الهندسية", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(RoomShape.entries) { shape ->
                                FilterChip(
                                    selected = ws.geometry.shape == shape,
                                    onClick = { viewModel.updateGeometry { it.copy(shape = shape, manualAreaM2 = null) } },
                                    label = { Text(shape.titleAr) }
                                )
                            }
                        }
                    }
                    item {
                        when (ws.geometry.shape) {
                            RoomShape.CIRCULAR -> {
                                NumberInputField("القطر (متر)", ws.geometry.diameterM.toString()) {
                                    it.toDoubleOrNull()?.let { d -> viewModel.updateGeometry { g -> g.copy(diameterM = d) } }
                                }
                            }
                            RoomShape.SQUARE -> {
                                NumberInputField("طول الضلع (متر)", ws.geometry.lengthM.toString()) {
                                    it.toDoubleOrNull()?.let { l -> viewModel.updateGeometry { g -> g.copy(lengthM = l, widthM = l) } }
                                }
                            }
                            else -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    NumberInputField("الطول (م)", ws.geometry.lengthM.toString(), Modifier.weight(1f)) {
                                        it.toDoubleOrNull()?.let { l -> viewModel.updateGeometry { g -> g.copy(lengthM = l) } }
                                    }
                                    NumberInputField("العرض (م)", ws.geometry.widthM.toString(), Modifier.weight(1f)) {
                                        it.toDoubleOrNull()?.let { w -> viewModel.updateGeometry { g -> g.copy(widthM = w) } }
                                    }
                                }
                            }
                        }
                    }
                    if (ws.geometry.shape == RoomShape.L_SHAPED) {
                        item {
                            Text("أبعاد الجزء المقتطع لحرف L", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                NumberInputField("طول الاقتطاع (م)", ws.geometry.lCutoutLengthM.toString(), Modifier.weight(1f)) {
                                    it.toDoubleOrNull()?.let { v -> viewModel.updateGeometry { g -> g.copy(lCutoutLengthM = v) } }
                                }
                                NumberInputField("عرض الاقتطاع (م)", ws.geometry.lCutoutWidthM.toString(), Modifier.weight(1f)) {
                                    it.toDoubleOrNull()?.let { v -> viewModel.updateGeometry { g -> g.copy(lCutoutWidthM = v) } }
                                }
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NumberInputField("ارتفاع السقف (م)", ws.geometry.ceilingHeightM.toString(), Modifier.weight(1f)) {
                                it.toDoubleOrNull()?.let { h -> viewModel.updateGeometry { g -> g.copy(ceilingHeightM = h) } }
                            }
                            NumberInputField("مستوى العمل (م)", ws.geometry.workplaneHeightM.toString(), Modifier.weight(1f)) {
                                it.toDoubleOrNull()?.let { wp -> viewModel.updateGeometry { g -> g.copy(workplaneHeightM = wp) } }
                            }
                        }
                    }
                    item {
                        NumberInputField("المساحة اليدوية م² (اختياري للمضلع غير المنتظم)", ws.geometry.manualAreaM2?.toString() ?: "") {
                            viewModel.updateGeometry { g -> g.copy(manualAreaM2 = it.toDoubleOrNull()) }
                        }
                    }
                    item {
                        Text("نوع السقف", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(SurfaceFinishesCatalog.ceilingTypesAr) { ct ->
                                FilterChip(
                                    selected = ws.geometry.ceilingTypeAr == ct,
                                    onClick = { viewModel.updateGeometry { it.copy(ceilingTypeAr = ct) } },
                                    label = { Text(ct) }
                                )
                            }
                        }
                    }
                    item {
                        val area = LightingCalculationEngine.calculateArea(ws.geometry)
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Text(
                                "المساحة المحسوبة: ${"%.2f".format(area)} م²  |  ارتفاع التثبيت الفعال Hm = ${"%.2f".format((ws.geometry.ceilingHeightM - ws.geometry.workplaneHeightM).coerceAtLeast(0.6))} م",
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // TAB 2: Wall, Ceiling, Floor Color & Reflectance Analysis
                2 -> {
                    item {
                        Text("ألوان وتشطيبات الفراغ", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "مبدأ هندسي: الألوان الداكنة تقلل الانعكاس وتخفض معامل الاستخدام (UF) فتزيد التدفق المطلوب للوصول لنفس Target Lux، ولا نرفع Target Lux بسبب لون الحوائط.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        ColorPresetSelector(
                            title = "لون الحوائط (${ws.surfaces.wallColorNameAr} — انعكاس ${(ws.surfaces.wallReflectance * 100).toInt()}%)",
                            presets = SurfaceFinishesCatalog.wallColors,
                            selectedName = ws.surfaces.wallColorNameAr,
                            reflectance = ws.surfaces.wallReflectance,
                            onSelectPreset = { p ->
                                viewModel.updateSurfaces { it.copy(wallColorNameAr = p.nameAr, wallHex = p.hexColor, wallReflectance = p.reflectance) }
                            },
                            onReflectanceChange = { r ->
                                viewModel.updateSurfaces { it.copy(wallColorNameAr = "لون مخصص", wallReflectance = r) }
                            }
                        )
                    }
                    item {
                        ColorPresetSelector(
                            title = "لون السقف (${ws.surfaces.ceilingColorNameAr} — انعكاس ${(ws.surfaces.ceilingReflectance * 100).toInt()}%)",
                            presets = SurfaceFinishesCatalog.ceilingColors,
                            selectedName = ws.surfaces.ceilingColorNameAr,
                            reflectance = ws.surfaces.ceilingReflectance,
                            onSelectPreset = { p ->
                                viewModel.updateSurfaces { it.copy(ceilingColorNameAr = p.nameAr, ceilingHex = p.hexColor, ceilingReflectance = p.reflectance) }
                            },
                            onReflectanceChange = { r ->
                                viewModel.updateSurfaces { it.copy(ceilingColorNameAr = "لون مخصص", ceilingReflectance = r) }
                            }
                        )
                    }
                    item {
                        ColorPresetSelector(
                            title = "لون الأرضية (${ws.surfaces.floorColorNameAr} — انعكاس ${(ws.surfaces.floorReflectance * 100).toInt()}%)",
                            presets = SurfaceFinishesCatalog.floorColors,
                            selectedName = ws.surfaces.floorColorNameAr,
                            reflectance = ws.surfaces.floorReflectance,
                            onSelectPreset = { p ->
                                viewModel.updateSurfaces { it.copy(floorColorNameAr = p.nameAr, floorHex = p.hexColor, floorReflectance = p.reflectance) }
                            },
                            onReflectanceChange = { r ->
                                viewModel.updateSurfaces { it.copy(floorColorNameAr = "لون مخصص", floorReflectance = r) }
                            }
                        )
                    }
                    item {
                        Text("نوع التشطيب السطحي (Surface Finish)", style = MaterialTheme.typography.titleMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(SurfaceFinishType.entries) { ft ->
                                FilterChip(
                                    selected = ws.surfaces.finishType == ft,
                                    onClick = { viewModel.updateSurfaces { it.copy(finishType = ft) } },
                                    label = { Text(ft.titleAr) }
                                )
                            }
                        }
                        Text(ws.surfaces.finishType.glareNoteAr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                    ws.calculationResult?.let { calc ->
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("أثر الألوان على معامل الاستخدام UF = ${"%.2f".format(calc.utilizationFactor)}", fontWeight = FontWeight.Bold)
                                    Text(calc.reflectanceImpactNoteAr, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                // TAB 3: Fixture Selection & Mixed Lighting
                3 -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تفعيل الإضاءة المختلطة (عدة أنواع بالغرفة)", style = MaterialTheme.typography.titleMedium)
                            Switch(checked = ws.useMixedMode, onCheckedChange = { viewModel.setMixedMode(it) })
                        }
                    }
                    item {
                        Text("معامل الصيانة (MF): ${"%.2f".format(ws.maintenanceFactor)}", fontWeight = FontWeight.Medium)
                        Slider(
                            value = ws.maintenanceFactor.toFloat(),
                            onValueChange = { viewModel.updateMaintenanceFactor(it.toDouble()) },
                            valueRange = 0.60f..0.90f
                        )
                    }
                    if (!ws.useMixedMode) {
                        item {
                            Text("وحدة الإضاءة الرئيسية المختارة:", style = MaterialTheme.typography.labelLarge)
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(ws.primaryFixture.nameAr, style = MaterialTheme.typography.titleMedium)
                                    Text("${ws.primaryFixture.wattage}W | ${ws.primaryFixture.lumens.toInt()} lm | زاوية ${ws.primaryFixture.beamAngleDeg}° | ${ws.primaryFixture.cctK}K | CRI ${ws.primaryFixture.cri} | ${ws.primaryFixture.ipRating}")
                                }
                            }
                        }
                        item { Text("اختر وحدة من المكتبة:", style = MaterialTheme.typography.labelLarge) }
                        items(fixtures.take(18)) { fix ->
                            ListItem(
                                headlineContent = { Text(fix.nameAr) },
                                supportingContent = { Text("${fix.wattage.toInt()}W | ${fix.lumens.toInt()} Lumen | ${fix.beamAngleDeg}° | ${fix.category.titleAr}") },
                                trailingContent = {
                                    if (fix.id == ws.primaryFixture.id) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary)
                                },
                                modifier = Modifier.clickable { viewModel.selectPrimaryFixture(fix) }
                            )
                            HorizontalDivider()
                        }
                    } else {
                        item {
                            MixedLightingComposer(
                                allFixtures = fixtures,
                                entries = ws.mixedEntries,
                                onAdd = { f, q, layer, grp, dim -> viewModel.addMixedFixture(f, q, layer, grp, dim) },
                                onChangeQty = { id, q -> viewModel.updateMixedEntryQty(id, q) }
                            )
                        }
                    }
                }

                // TAB 4: Complete Engineering Results
                4 -> {
                    ws.calculationResult?.let { calc ->
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("النتائج الهندسية — طريقة اللومن (Lumen Method)", style = MaterialTheme.typography.titleMedium)
                                    val maintained = ws.mixedResult?.estimatedMaintainedLux ?: calc.estimatedMaintainedLux
                                    val watts = ws.mixedResult?.totalWatts ?: calc.totalWatts
                                    val qty = if (ws.useMixedMode) ws.mixedEntries.sumOf { it.quantity } else calc.requiredQuantity
                                    val ufVal = ws.mixedResult?.uf ?: calc.utilizationFactor

                                    ResultRow("شدة الإضاءة المستهدفة (Target Lux)", "${calc.targetLux.toInt()} Lux")
                                    ResultRow("الإضاءة المصانة التقديرية", "${maintained.toInt()} Lux")
                                    ResultRow("إجمالي اللومن المطلوب", "${calc.requiredLumens.toInt()} Lumen")
                                    ResultRow("لومن الوحدة الواحدة", "${calc.fixtureLumens.toInt()} Lumen")
                                    ResultRow("عدد الوحدات المطلوب (مقرب للأعلى)", "$qty وحدة")
                                    ResultRow("إجمالي القدرة الكهربائية", "${"%.1f".format(watts)} Watt")
                                    ResultRow("كثافة القدرة (LPD)", "${"%.2f".format(watts / calc.roomAreaM2)} W/m²")
                                    ResultRow("معامل الاستخدام (UF) / الصيانة (MF)", "${"%.2f".format(ufVal)} / ${"%.2f".format(calc.maintenanceFactor)}")
                                }
                            }
                        }

                        ws.mixedResult?.let { mx ->
                            item {
                                ElevatedCard(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("تفصيل طبقات الإضاءة المختلطة", style = MaterialTheme.typography.titleMedium)
                                        Text(mx.adequacyNoteAr, color = MaterialTheme.colorScheme.secondary)
                                        mx.layerBreakdown.forEach { (layer, sum) ->
                                            ResultRow(layer.titleAr, "${sum.fixtureCount} وحدة | ${sum.estimatedLuxContribution.toInt()} Lux | ${"%.1f".format(sum.totalWatts)}W")
                                        }
                                        HorizontalDivider()
                                        Text("دوائر التشغيل والتعتيم (Switching & Dimming)", fontWeight = FontWeight.Bold)
                                        mx.groupBreakdown.values.forEach { grp ->
                                            ResultRow(grp.groupName, "${grp.fixtureCount} وحدة | ${"%.1f".format(grp.totalWatts)}W ${if (grp.hasDimming) "(قابل للتعتيم)" else ""}")
                                        }
                                    }
                                }
                            }
                        }

                        ws.spacingResult?.let { sp ->
                            item {
                                ElevatedCard(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text("توزيع السبوتات والمسافات (${sp.pattern.titleAr})", style = MaterialTheme.typography.titleMedium)
                                        ResultRow("التشكيل (أعمدة × صفوف)", "${sp.countX} × ${sp.countY} (إجمالي ${sp.totalFixtures})")
                                        ResultRow("المسافة بين السبوتات (X / Y)", "${"%.2f".format(sp.spacingXM)} م / ${"%.2f".format(sp.spacingYM)} م")
                                        ResultRow("المسافة من أول سبوت للحائط", "${"%.2f".format(sp.wallDistanceXM)} م / ${"%.2f".format(sp.wallDistanceYM)} م")
                                        Text(sp.uniformityStatusAr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }

                        item {
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("افتراضات الحساب الهندسي", style = MaterialTheme.typography.titleMedium)
                                    calc.assumptionsAr.forEach { a ->
                                        Text("• $a", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { viewModel.saveCurrentRoomToProject() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Save, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("حفظ في المشروع")
                                }
                                FilledTonalButton(
                                    onClick = { onNavigate("plan_2d") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Architecture, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("فتح مخطط 2D")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Step Bar
        Surface(tonalElevation = 4.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (activeTab > 0) {
                    OutlinedButton(onClick = { activeTab-- }) { Text("السابق") }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                if (activeTab < tabs.lastIndex) {
                    Button(onClick = { activeTab++ }) { Text("التالي") }
                } else {
                    Button(onClick = { viewModel.saveCurrentRoomToProject() }) { Text("حفظ الفراغ") }
                }
            }
        }
    }
}

@Composable
private fun ColorPresetSelector(
    title: String,
    presets: List<ColorPreset>,
    selectedName: String,
    reflectance: Double,
    onSelectPreset: (ColorPreset) -> Unit,
    onReflectanceChange: (Double) -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presets) { p ->
                    val isSelected = p.nameAr == selectedName
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onSelectPreset(p) }.padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(p.hexColor))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Gray,
                                    shape = CircleShape
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(p.nameAr, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Text("تعديل نسبة الانعكاس يدوياً (لون مخصص): ${(reflectance * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = reflectance.toFloat().coerceIn(0.05f, 0.90f),
                onValueChange = { onReflectanceChange(it.toDouble()) },
                valueRange = 0.05f..0.90f
            )
        }
    }
}

@Composable
private fun MixedLightingComposer(
    allFixtures: List<LightingFixture>,
    entries: List<MixedFixtureEntry>,
    onAdd: (LightingFixture, Int, LightingLayer, String, Boolean) -> Unit,
    onChangeQty: (String, Int) -> Unit
) {
    var selectedFix by remember { mutableStateOf(allFixtures.first()) }
    var qtyText by remember { mutableStateOf("4") }
    var selectedLayer by remember { mutableStateOf(LightingLayer.GENERAL) }
    var groupName by remember { mutableStateOf("دائرة 1") }
    var dimmable by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("إضافة وحدة إضاءة للفراغ", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(allFixtures.take(20)) { f ->
                        FilterChip(
                            selected = selectedFix.id == f.id,
                            onClick = { selectedFix = f; selectedLayer = f.defaultLayer },
                            label = { Text(f.nameAr) }
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(LightingLayer.entries) { layer ->
                        FilterChip(
                            selected = selectedLayer == layer,
                            onClick = { selectedLayer = layer },
                            label = { Text(layer.titleAr) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberInputField("الكمية", qtyText, Modifier.weight(1f)) { qtyText = it }
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("مجموعة التشغيل") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = dimmable, onCheckedChange = { dimmable = it })
                        Text("قابل للتعتيم (Dimming)")
                    }
                    Button(onClick = { onAdd(selectedFix, qtyText.toIntOrNull() ?: 1, selectedLayer, groupName, dimmable) }) {
                        Text("إضافة")
                    }
                }
            }
        }

        entries.forEach { e ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(e.fixture.nameAr, fontWeight = FontWeight.Bold)
                        Text("${e.layer.titleAr} | ${e.switchingGroup} | ${e.totalWatts.toInt()}W | ${e.totalLumens.toInt()} lm", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onChangeQty(e.id, e.quantity - 1) }) { Icon(Icons.Default.RemoveCircleOutline, null) }
                        Text("${e.quantity}", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onChangeQty(e.id, e.quantity + 1) }) { Icon(Icons.Default.AddCircleOutline, null) }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickLuxCalculatorScreen() {
    var areaStr by remember { mutableStateOf("24") }
    var luxStr by remember { mutableStateOf("300") }
    var ufStr by remember { mutableStateOf("0.65") }
    var mfStr by remember { mutableStateOf("0.80") }
    var fixLumensStr by remember { mutableStateOf("900") }
    var fixWattStr by remember { mutableStateOf("10") }

    val area = (areaStr.toDoubleOrNull() ?: 1.0).coerceAtLeast(0.5)
    val lux = (luxStr.toDoubleOrNull() ?: 100.0).coerceAtLeast(10.0)
    val uf = (ufStr.toDoubleOrNull() ?: 0.65).coerceIn(0.2, 0.95)
    val mf = (mfStr.toDoubleOrNull() ?: 0.80).coerceIn(0.5, 0.95)
    val fixLm = (fixLumensStr.toDoubleOrNull() ?: 900.0).coerceAtLeast(50.0)
    val fixW = (fixWattStr.toDoubleOrNull() ?: 10.0).coerceAtLeast(1.0)

    val reqLumens = (lux * area) / (uf * mf)
    val qty = ceil(reqLumens / fixLm).toInt().coerceAtLeast(1)
    val deliveredLux = (qty * fixLm * uf * mf) / area
    val totalW = qty * fixW

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("حاسبة Lux السريعة (Lumen Method)", style = MaterialTheme.typography.titleLarge)
            Text("Required Lumens = (Target Lux × Area) / (UF × MF)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberInputField("المساحة (م²)", areaStr, Modifier.weight(1f)) { areaStr = it }
                NumberInputField("Target Lux", luxStr, Modifier.weight(1f)) { luxStr = it }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberInputField("معامل الاستخدام UF", ufStr, Modifier.weight(1f)) { ufStr = it }
                NumberInputField("معامل الصيانة MF", mfStr, Modifier.weight(1f)) { mfStr = it }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberInputField("لومن الوحدة (lm)", fixLumensStr, Modifier.weight(1f)) { fixLumensStr = it }
                NumberInputField("قدرة الوحدة (W)", fixWattStr, Modifier.weight(1f)) { fixWattStr = it }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ResultRow("إجمالي اللومن المطلوب", "${reqLumens.toInt()} Lumen")
                    ResultRow("عدد الوحدات (مقرب للأعلى)", "$qty وحدة")
                    ResultRow("الإضاءة المصانة التقديرية", "${deliveredLux.toInt()} Lux")
                    ResultRow("إجمالي القدرة الكهربائية", "${"%.1f".format(totalW)} Watt")
                    ResultRow("كثافة القدرة (LPD)", "${"%.2f".format(totalW / area)} W/m²")
                }
            }
        }
    }
}

@Composable
fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NumberInputField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}
