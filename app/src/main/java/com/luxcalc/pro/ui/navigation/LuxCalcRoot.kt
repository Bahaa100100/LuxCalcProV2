@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.luxcalc.pro.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.luxcalc.pro.ui.screens.*
import com.luxcalc.pro.ui.viewmodel.LuxCalcViewModel
import kotlinx.coroutines.launch

data class NavDestination(val route: String, val titleAr: String, val icon: ImageVector)

val mainDestinations = listOf(
    NavDestination("dashboard", "لوحة التحكم", Icons.Default.Dashboard),
    NavDestination("new_calc", "حساب إضاءة جديد", Icons.Default.Calculate),
    NavDestination("spaces", "قاعدة بيانات الفراغات", Icons.Default.MeetingRoom),
    NavDestination("fixtures", "مكتبة وحدات الإضاءة", Icons.Default.Lightbulb),
    NavDestination("quick_lux", "حاسبة Lux", Icons.Default.FlashOn),
    NavDestination("spacing", "توزيع السبوتات", Icons.Default.GridOn),
    NavDestination("plan_2d", "مخطط الإضاءة", Icons.Default.Architecture),
    NavDestination("projects", "إدارة المشاريع", Icons.Default.FolderCopy),
    NavDestination("boq", "جدول الكميات", Icons.Default.TableChart),
    NavDestination("reports", "التقارير", Icons.Default.PictureAsPdf),
    NavDestination("settings", "الإعدادات", Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuxCalcRoot(viewModel: LuxCalcViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "dashboard"
    val currentTitle = mainDestinations.firstOrNull { it.route == currentRoute }?.titleAr ?: "LuxCalc Pro"

    val statusMsg by viewModel.statusMessage.collectAsState()
    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
        }
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text("LuxCalc Pro", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary)
                    Text("مصمم الإضاءة الهندسي العربي", style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                mainDestinations.forEach { dest ->
                    NavigationDrawerItem(
                        label = { Text(dest.titleAr) },
                        selected = currentRoute == dest.route,
                        onClick = { navigateTo(dest.route) },
                        icon = { Icon(dest.icon, null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentTitle) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "القائمة")
                        }
                    },
                    actions = {
                        if (currentRoute != "dashboard") {
                            IconButton(onClick = { navigateTo("dashboard") }) {
                                Icon(Icons.Default.Home, contentDescription = "الرئيسية")
                            }
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding).fillMaxSize()) {
                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") { DashboardScreen(viewModel, onNavigate = ::navigateTo) }
                    composable("new_calc") { NewCalculationScreen(viewModel, onNavigate = ::navigateTo) }
                    composable("spaces") {
                        SpaceDatabaseScreen(viewModel) { template ->
                            viewModel.applySpaceTemplate(template)
                            navigateTo("new_calc")
                        }
                    }
                    composable("fixtures") {
                        FixtureLibraryScreen(viewModel) { fixture ->
                            viewModel.selectPrimaryFixture(fixture)
                            navigateTo("new_calc")
                        }
                    }
                    composable("quick_lux") { QuickLuxCalculatorScreen() }
                    composable("spacing") { SpotlightSpacingScreen(viewModel, onNavigate = ::navigateTo) }
                    composable("plan_2d") { Interactive2DPlanScreen(viewModel) }
                    composable("projects") { ProjectsScreen(viewModel, onOpenRoom = { navigateTo("new_calc") }) }
                    composable("boq") { BoqScreen(viewModel) }
                    composable("reports") { ReportsScreen(viewModel) }
                    composable("settings") { SettingsScreen(viewModel) }
                }
            }
        }
    }
}
