package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hayati_prefs")

data class UserPreferences(
    val userName: String = "أحمد محمود",
    val userEmail: String = "",
    val language: AppLanguage = AppLanguage.ARABIC,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val currency: String = "EGP",
    val timeZoneId: String = DateTimeUtils.DEFAULT_TIMEZONE,
    val morningSummaryEnabled: Boolean = true,
    val eveningSummaryEnabled: Boolean = true,
    val notificationsEnabled: Boolean = false,
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CURRENCY = stringPreferencesKey("currency")
        val TIME_ZONE = stringPreferencesKey("time_zone")
        val MORNING_SUMMARY = booleanPreferencesKey("morning_summary")
        val EVENING_SUMMARY = booleanPreferencesKey("evening_summary")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            userName = prefs[Keys.USER_NAME] ?: "مستخدم حياتي",
            userEmail = prefs[Keys.USER_EMAIL] ?: "",
            language = prefs[Keys.LANGUAGE]?.let { code ->
                AppLanguage.entries.firstOrNull { it.code == code }
            } ?: AppLanguage.ARABIC,
            themeMode = prefs[Keys.THEME_MODE]?.let { name ->
                runCatching { ThemeMode.valueOf(name) }.getOrNull()
            } ?: ThemeMode.LIGHT,
            currency = prefs[Keys.CURRENCY] ?: "EGP",
            timeZoneId = prefs[Keys.TIME_ZONE] ?: DateTimeUtils.DEFAULT_TIMEZONE,
            morningSummaryEnabled = prefs[Keys.MORNING_SUMMARY] ?: true,
            eveningSummaryEnabled = prefs[Keys.EVENING_SUMMARY] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
        )
    }

    suspend fun updateUserProfile(name: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name.trim().ifBlank { "مستخدم حياتي" }
            prefs[Keys.USER_EMAIL] = email.trim()
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { it[Keys.CURRENCY] = currency }
    }

    suspend fun setTimeZone(tzId: String) {
        context.dataStore.edit { it[Keys.TIME_ZONE] = tzId }
    }

    suspend fun setNotificationSettings(
        morningSummary: Boolean,
        eveningSummary: Boolean,
        notificationsEnabled: Boolean,
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MORNING_SUMMARY] = morningSummary
            prefs[Keys.EVENING_SUMMARY] = eveningSummary
            prefs[Keys.NOTIFICATIONS_ENABLED] = notificationsEnabled
        }
    }
}
