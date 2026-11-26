@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.luxcalc.pro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luxcalc.pro.domain.engine.SmartSpaceRecognizer
import com.luxcalc.pro.domain.model.*
import com.luxcalc.pro.ui.viewmodel.LuxCalcViewModel

@Composable
fun SpaceDatabaseScreen(
    viewModel: LuxCalcViewModel,
    onSelectSpace: (SpaceTemplate) -> Unit
) {
    val spaces by viewModel.allSpaces.collectAsState()
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<SpaceCategory?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = remember(spaces, query, selectedCategory) {
        val normQ = SmartSpaceRecognizer.normalizeArabic(query)
        spaces.filter { sp ->
            (selectedCategory == null || sp.category == selectedCategory) &&
            (normQ.isBlank() || SmartSpaceRecognizer.normalizeArabic(sp.nameAr).contains(normQ) || sp.category.titleAr.contains(query))
        }
    }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("قاعدة بيانات الفراغات (${spaces.size} فراغ)", style = MaterialTheme.typography.titleLarge)
            FilledTonalButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("فراغ مخصص")
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("ابحث عن أي فراغ (مثلاً: ريسبشن، غرفة Gaming، عمليات، مخزن...)") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(selected = selectedCategory == null, onClick = { selectedCategory = null }, label = { Text("الكل") })
            }
            items(SpaceCategory.entries) { cat ->
                FilterChip(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, label = { Text(cat.titleAr) })
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { space ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().clickable { onSelectSpace(space) }
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(space.nameAr, style = MaterialTheme.typography.titleMedium)
                            AssistChip(onClick = { onSelectSpace(space) }, label = { Text("${space.defaultLux} Lux") })
                        }
                        Text(
                            "${space.category.titleAr} | النطاق: ${space.minLux}-${space.maxLux} Lux | CCT: ${space.recommendedCctAr} | CRI >= ${space.minCri}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(space.lightingRequirementsAr, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${space.source.labelAr} — ${space.referenceAr}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var luxStr by remember { mutableStateOf("300") }
        var cctStr by remember { mutableStateOf("4000") }
        var criStr by remember { mutableStateOf("90") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("إضافة فراغ مخصص جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الفراغ") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField("Target Lux", luxStr, Modifier.weight(1f)) { luxStr = it }
                        NumberInputField("CCT (K)", cctStr, Modifier.weight(1f)) { cctStr = it }
                    }
                    NumberInputField("الحد الأدنى CRI", criStr) { criStr = it }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("متطلبات الإضاءة") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        val defLux = luxStr.toIntOrNull() ?: 300
                        viewModel.saveCustomSpace(
                            SpaceTemplate(
                                id = "custom_${System.currentTimeMillis()}",
                                nameAr = name.trim(),
                                category = SpaceCategory.CUSTOM,
                                minLux = (defLux * 0.75).toInt(),
                                defaultLux = defLux,
                                maxLux = (defLux * 1.35).toInt(),
                                recommendedCctAr = "${cctStr}K",
                                defaultCctK = cctStr.toIntOrNull() ?: 4000,
                                minCri = criStr.toIntOrNull() ?: 85,
                                lightingRequirementsAr = notes.ifBlank { "فراغ مخصص محفوظ من قبل المستخدم" },
                                source = RecommendationSource.PROFESSIONAL_RECOMMENDATION,
                                referenceAr = "مدخل بواسطة المهندس المصمم",
                                isCustom = true
                            )
                        )
                        showAddDialog = false
                    }
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
fun FixtureLibraryScreen(
    viewModel: LuxCalcViewModel,
    onUseFixture: (LightingFixture) -> Unit
) {
    val fixtures by viewModel.allFixtures.collectAsState()
    var query by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<FixtureCategory?>(null) }
    var editingFixture by remember { mutableStateOf<LightingFixture?>(null) }

    val filtered = remember(fixtures, query, selectedCat) {
        fixtures.filter { f ->
            (selectedCat == null || f.category == selectedCat) &&
            (query.isBlank() || f.nameAr.contains(query, ignoreCase = true) || f.modelCode.contains(query, ignoreCase = true))
        }
    }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("مكتبة وحدات الإضاءة (${fixtures.size})", style = MaterialTheme.typography.titleLarge)
            FilledTonalButton(onClick = {
                editingFixture = LightingFixture(
                    id = "custom_fix_${System.currentTimeMillis()}",
                    nameAr = "وحدة إضاءة مخصصة",
                    category = FixtureCategory.SPOTLIGHTS,
                    defaultLayer = LightingLayer.GENERAL,
                    wattage = 12.0,
                    lumens = 1100.0,
                    beamAngleDeg = 38,
                    cctK = 3000,
                    cri = 90,
                    ipRating = "IP20",
                    mountingTypeAr = "غاطس",
                    isCustom = true
                )
            }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("إضافة وحدة")
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("بحث في وحدات الإضاءة (سبوت، تراك، بروفايل، بانل، هاي باي...)") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { FilterChip(selected = selectedCat == null, onClick = { selectedCat = null }, label = { Text("الكل") }) }
            items(FixtureCategory.entries) { cat ->
                FilterChip(selected = selectedCat == cat, onClick = { selectedCat = cat }, label = { Text(cat.titleAr) })
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { fix ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(fix.nameAr, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text("${"%.0f".format(fix.efficacyLmPerW)} lm/W", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                        Text(
                            "${fix.wattage}W | ${fix.lumens.toInt()} Lumen | زاوية ${fix.beamAngleDeg}° | ${fix.cctK}K | CRI ${fix.cri} | ${fix.ipRating}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "التركيب: ${fix.mountingTypeAr} | المصادر: ${fix.lightSourcesCount} | الموديل: ${fix.modelCode} ${if (fix.photometricFileRef.isNotBlank()) "| IES/LDT: ${fix.photometricFileRef}" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onUseFixture(fix) }) { Text("استخدام في الحساب") }
                            TextButton(onClick = { editingFixture = fix }) { Text("تعديل المواصفات") }
                        }
                    }
                }
            }
        }
    }

    editingFixture?.let { target ->
        var name by remember(target.id) { mutableStateOf(target.nameAr) }
        var watt by remember(target.id) { mutableStateOf(target.wattage.toString()) }
        var lm by remember(target.id) { mutableStateOf(target.lumens.toString()) }
        var beam by remember(target.id) { mutableStateOf(target.beamAngleDeg.toString()) }
        var cct by remember(target.id) { mutableStateOf(target.cctK.toString()) }
        var cri by remember(target.id) { mutableStateOf(target.cri.toString()) }
        var ip by remember(target.id) { mutableStateOf(target.ipRating) }
        var mfg by remember(target.id) { mutableStateOf(target.manufacturer) }
        var model by remember(target.id) { mutableStateOf(target.modelCode) }
        var iesRef by remember(target.id) { mutableStateOf(target.photometricFileRef) }

        AlertDialog(
            onDismissRequest = { editingFixture = null },
            title = { Text("تعديل المواصفات الفنية للوحدة") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 420.dp)) {
                    item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الوحدة") }, singleLine = true) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberInputField("القدرة (Watt)", watt, Modifier.weight(1f)) { watt = it }
                            NumberInputField("اللومن (Lumen)", lm, Modifier.weight(1f)) { lm = it }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberInputField("زاوية الإشعاع (°)", beam, Modifier.weight(1f)) { beam = it }
                            NumberInputField("CCT (K)", cct, Modifier.weight(1f)) { cct = it }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberInputField("CRI", cri, Modifier.weight(1f)) { cri = it }
                            OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("حماية IP") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = mfg, onValueChange = { mfg = it }, label = { Text("المُصنّع") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("الموديل") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                    }
                    item {
                        OutlinedTextField(value = iesRef, onValueChange = { iesRef = it }, label = { Text("مرجع ملف IES / LDT (اختياري)") }, singleLine = true)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveCustomFixture(
                        target.copy(
                            nameAr = name.ifBlank { target.nameAr },
                            wattage = watt.toDoubleOrNull() ?: target.wattage,
                            lumens = lm.toDoubleOrNull() ?: target.lumens,
                            beamAngleDeg = beam.toIntOrNull() ?: target.beamAngleDeg,
                            cctK = cct.toIntOrNull() ?: target.cctK,
                            cri = cri.toIntOrNull() ?: target.cri,
                            ipRating = ip.ifBlank { target.ipRating },
                            manufacturer = mfg,
                            modelCode = model,
                            photometricFileRef = iesRef,
                            isCustom = true
                        )
                    )
                    editingFixture = null
                }) { Text("حفظ المواصفات") }
            },
            dismissButton = { TextButton(onClick = { editingFixture = null }) { Text("إلغاء") } }
        )
    }
}
