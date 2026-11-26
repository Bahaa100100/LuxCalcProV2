package com.luxcalc.pro.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "luxcalc_settings")

data class AppSettings(
    val isDarkTheme: Boolean = false,
    val isArabicDefault: Boolean = true,
    val defaultMaintenanceFactor: Double = 0.80,
    val dailyOperatingHours: Double = 6.0,
    val electricityTariffPerKwh: Double = 1.60,
    val geminiApiKey: String = ""
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val ARABIC_DEFAULT = booleanPreferencesKey("arabic_default")
        val DEFAULT_MF = doublePreferencesKey("default_mf")
        val DAILY_HOURS = doublePreferencesKey("daily_hours")
        val TARIFF = doublePreferencesKey("tariff_kwh")
        val GEMINI_KEY = stringPreferencesKey("gemini_api_key")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            isDarkTheme = prefs[Keys.DARK_THEME] ?: false,
            isArabicDefault = prefs[Keys.ARABIC_DEFAULT] ?: true,
            defaultMaintenanceFactor = prefs[Keys.DEFAULT_MF] ?: 0.80,
            dailyOperatingHours = prefs[Keys.DAILY_HOURS] ?: 6.0,
            electricityTariffPerKwh = prefs[Keys.TARIFF] ?: 1.60,
            geminiApiKey = prefs[Keys.GEMINI_KEY] ?: ""
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setArabicDefault(arabic: Boolean) {
        context.dataStore.edit { it[Keys.ARABIC_DEFAULT] = arabic }
    }

    suspend fun setDefaultMf(mf: Double) {
        context.dataStore.edit { it[Keys.DEFAULT_MF] = mf.coerceIn(0.50, 0.95) }
    }

    suspend fun setEnergyParams(dailyHours: Double, tariff: Double) {
        context.dataStore.edit {
            it[Keys.DAILY_HOURS] = dailyHours.coerceIn(1.0, 24.0)
            it[Keys.TARIFF] = tariff.coerceAtLeast(0.0)
        }
    }

    suspend fun setGeminiApiKey(apiKey: String) {
        context.dataStore.edit { it[Keys.GEMINI_KEY] = apiKey.trim() }
    }
}
