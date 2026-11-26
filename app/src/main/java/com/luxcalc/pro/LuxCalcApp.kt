package com.luxcalc.pro

import android.app.Application
import com.luxcalc.pro.data.local.LuxCalcDatabase
import com.luxcalc.pro.data.preferences.SettingsRepository

class LuxCalcApp : Application() {
    val database: LuxCalcDatabase by lazy { LuxCalcDatabase.getInstance(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
