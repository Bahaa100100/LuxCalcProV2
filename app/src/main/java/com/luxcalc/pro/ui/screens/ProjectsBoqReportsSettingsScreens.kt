@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.luxcalc.pro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luxcalc.pro.ui.viewmodel.LuxCalcViewModel

@Composable
fun ProjectsScreen(
    viewModel: LuxCalcViewModel,
    onOpenRoom: () -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    val activePid by viewModel.activeProjectId.collectAsState()
    val rooms by viewModel.activeProjectRooms.collectAsState()
    val totals = remember(rooms) { viewModel.computeProjectTotals(rooms) }
    var showNewProjDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("إدارة المشاريع الهندسية", style = MaterialTheme.typography.titleLarge)
                Button(onClick = { showNewProjDialog = true }) {
                    Icon(Icons.Default.CreateNewFolder, null)
                    Spacer(Modifier.width(6.dp))
                    Text("مشروع جديد")
                }
            }
        }

        items(projects, key = { it.id }) { proj ->
            val selected = proj.id == activePid
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.selectProject(proj.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(proj.nameAr, style = MaterialTheme.typography.titleMedium)
                        Text("${proj.projectTypeAr} | ${proj.clientNameAr.ifBlank { "عميل عام" }}", style = MaterialTheme.typography.labelMedium)
                    }
                    Row {
                        IconButton(onClick = { viewModel.duplicateProject(proj) }) { Icon(Icons.Default.ContentCopy, "نسخ") }
                        if (projects.size > 1) {
                            IconButton(onClick = { viewModel.deleteProject(proj) }) { Icon(Icons.Default.Delete, "حذف") }
                        }
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("إجمالي أحمال المشروع النشط", style = MaterialTheme.typography.titleMedium)
                    ResultRow("عدد الفراغات / إجمالي المساحة", "${totals.roomCount} غرفة | ${"%.1f".format(totals.totalAreaM2)} م²")
                    ResultRow("إجمالي الوحدات / اللومن", "${totals.totalFixtures} وحدة | ${totals.totalLumens.toInt()} lm")
                    ResultRow("الحمل الكهربائي الكلي", "${"%.1f".format(totals.totalWatts)} Watt (${"%.2f".format(totals.totalWatts / 1000.0)} kW)")
                    ResultRow("متوسط كثافة القدرة (LPD)", "${"%.2f".format(totals.avgPowerDensityWPerM2)} W/m²")
                    ResultRow("الاستهلاك الشهري التقديري", "${"%.1f".format(totals.estimatedMonthlyKwh)} kWh")
                }
            }
        }

        item { Text("الفراغات داخل المشروع النشط (${rooms.size})", style = MaterialTheme.typography.titleMedium) }
        if (rooms.isEmpty()) {
            item { Text("لا توجد فراغات محفوظة بعد في هذا المشروع. ابدأ من (حساب إضاءة جديد) ثم اضغط حفظ.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(rooms, key = { it.id }) { room ->
                ElevatedCard(Modifier.fillMaxWidth().clickable {
                    viewModel.loadRoomIntoWorkspace(room)
                    onOpenRoom()
                }) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(room.roomNameAr, fontWeight = FontWeight.Bold)
                            Text("${"%.1f".format(room.areaM2)} م² | ${room.totalFixturesCount} وحدة | ${"%.1f".format(room.totalWatts)}W | ${room.maintainedLux.toInt()} Lux")
                        }
                        IconButton(onClick = { viewModel.deleteRoom(room) }) { Icon(Icons.Default.DeleteOutline, "حذف") }
                    }
                }
            }
        }
    }

    if (showNewProjDialog) {
        var name by remember { mutableStateOf("") }
        var client by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("سكني") }
        AlertDialog(
            onDismissRequest = { showNewProjDialog = false },
            title = { Text("إنشاء مشروع جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المشروع (مثلاً: فيلا التجمع الخامس)") }, singleLine = true)
                    OutlinedTextField(value = client, onValueChange = { client = it }, label = { Text("اسم العميل / المالك") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("سكني", "تجاري", "صناعي", "فندقي").forEach { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createProject(name, client, type)
                    showNewProjDialog = false
                }) { Text("إنشاء") }
            },
            dismissButton = { TextButton(onClick = { showNewProjDialog = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
fun BoqScreen(viewModel: LuxCalcViewModel) {
    val rooms by viewModel.activeProjectRooms.collectAsState()
    val boq = remember(rooms) { viewModel.computeProjectBoq(rooms) }
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("جدول الكميات المجمع (BOQ)", style = MaterialTheme.typography.titleLarge)
            FilledTonalButton(onClick = { viewModel.exportExcelCsvBoq(context) }) {
                Icon(Icons.Default.Download, null)
                Spacer(Modifier.width(4.dp))
                Text("Excel / CSV")
            }
        }

        if (boq.isEmpty()) {
            Text("لا توجد بنود في جدول الكميات بعد. احفظ فراغاً واحداً على الأقل في المشروع النشط.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(12.dp)) {
                    ResultRow("إجمالي البنود", "${boq.size} نوع")
                    ResultRow("إجمالي عدد الوحدات", "${boq.sumOf { it.totalQuantity }} وحدة")
                    ResultRow("إجمالي القدرة الكهربائية", "${"%.1f".format(boq.sumOf { it.totalWatts })} Watt")
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(boq) { item ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.fixtureNameAr, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                AssistChip(onClick = {}, label = { Text("الكمية: ${item.totalQuantity}") })
                            }
                            Text(item.specsSummaryAr, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            Text("إجمالي القدرة: ${"%.1f".format(item.totalWatts)}W | اللومن: ${item.totalLumens.toInt()} lm", style = MaterialTheme.typography.bodyMedium)
                            Text("الفراغات: ${item.roomsUsedIn.joinToString("، ")}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsScreen(viewModel: LuxCalcViewModel) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsState()
    val activePid by viewModel.activeProjectId.collectAsState()
    val rooms by viewModel.activeProjectRooms.collectAsState()
    val activeProj = projects.firstOrNull { it.id == activePid }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("التقارير الهندسية والتصدير (عربي RTL)", style = MaterialTheme.typography.titleLarge)
            Text("المشروع الحالي: ${activeProj?.nameAr ?: "—"} (${rooms.size} فراغات)", color = MaterialTheme.colorScheme.secondary)
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("تقرير PDF هندسي متكامل (عربي RTL)", style = MaterialTheme.typography.titleMedium)
                    Text("يتضمن: بيانات المشروع، أبعاد الفراغات، ألوان وتشطيبات السطوح والانعكاسات، افتراضات UF/MF، المسافات، مخطط 2D لكل غرفة، وجدول الكميات BOQ.")
                    Button(onClick = { viewModel.exportPdfReport(context) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PictureAsPdf, null)
                        Spacer(Modifier.width(8.dp))
                        Text("إنشاء ومشاركة تقرير PDF العربي")
                    }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("تصدير جدول الكميات لبرنامج Excel (CSV UTF-8)", style = MaterialTheme.typography.titleMedium)
                    Text("ملف متوافق مع Microsoft Excel وGoogle Sheets بترميز UTF-8 BOM لعرض العربية بشكل سليم.")
                    FilledTonalButton(onClick = { viewModel.exportExcelCsvBoq(context) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.TableChart, null)
                        Spacer(Modifier.width(8.dp))
                        Text("تصدير جدول الكميات Excel/CSV")
                    }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("تصدير مخطط 2D الحالي كصورة PNG عالية الدقة", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { viewModel.exportCurrent2DPlanPng(context) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Image, null)
                        Spacer(Modifier.width(8.dp))
                        Text("تصدير مخطط الإضاءة PNG")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: LuxCalcViewModel) {
    val settings by viewModel.settings.collectAsState()
    var geminiKey by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var hoursText by remember(settings.dailyOperatingHours) { mutableStateOf(settings.dailyOperatingHours.toString()) }
    var tariffText by remember(settings.electricityTariffPerKwh) { mutableStateOf(settings.electricityTariffPerKwh.toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("الإعدادات العامة والهندسية", style = MaterialTheme.typography.titleLarge) }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("الوضع الليلي (Dark Theme)")
                        Switch(checked = settings.isDarkTheme, onCheckedChange = { viewModel.setDarkTheme(it) })
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("اللغة العربية والاتجاه RTL (افتراضي)")
                            Text("الواجهة العربية RTL هي الافتراضية دائماً", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = settings.isArabicDefault, onCheckedChange = { viewModel.setArabicDefault(it) })
                    }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المعاملات الهندسية والطاقة", style = MaterialTheme.typography.titleMedium)
                    Text("معامل الصيانة الافتراضي MF: ${"%.2f".format(settings.defaultMaintenanceFactor)}")
                    Slider(
                        value = settings.defaultMaintenanceFactor.toFloat(),
                        onValueChange = { viewModel.setDefaultMf(it.toDouble()) },
                        valueRange = 0.60f..0.90f
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField("ساعات التشغيل/يوم", hoursText, Modifier.weight(1f)) { hoursText = it }
                        NumberInputField("سعر الـ kWh", tariffText, Modifier.weight(1f)) { tariffText = it }
                    }
                    Button(onClick = {
                        viewModel.setEnergyParams(hoursText.toDoubleOrNull() ?: 6.0, tariffText.toDoubleOrNull() ?: 1.6)
                        viewModel.showMessage("تم حفظ إعدادات الطاقة")
                    }) { Text("حفظ معاملات الطاقة") }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تكامل Gemini AI (اختياري تماماً)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "جميع الحسابات الهندسية وقاعدة البيانات والتعرف الذكي المحلي تعمل 100% بدون إنترنت. يمكنك اختيارياً إدخال مفتاح Gemini API لتحليل الفراغات غير المألوفة بشكل أعمق (يُحفظ محلياً على جهازك فقط).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        label = { Text("Gemini API Key (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(onClick = { viewModel.setGeminiApiKey(geminiKey) }) { Text("حفظ المفتاح محلياً") }
                }
            }
        }
    }
}
