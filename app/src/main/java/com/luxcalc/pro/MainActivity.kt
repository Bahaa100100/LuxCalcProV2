package com.luxcalc.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luxcalc.pro.ui.navigation.LuxCalcRoot
import com.luxcalc.pro.ui.theme.LuxCalcTheme
import com.luxcalc.pro.ui.viewmodel.LuxCalcViewModel
import com.luxcalc.pro.ui.viewmodel.LuxCalcViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as LuxCalcApp
        setContent {
            val vm: LuxCalcViewModel = viewModel(
                factory = LuxCalcViewModelFactory(app.database, app.settingsRepository)
            )
            val settings by vm.settings.collectAsState()
            val layoutDir = if (settings.isArabicDefault) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
                LuxCalcTheme(darkTheme = settings.isDarkTheme) {
                    LuxCalcRoot(viewModel = vm)
                }
            }
        }
    }
}
