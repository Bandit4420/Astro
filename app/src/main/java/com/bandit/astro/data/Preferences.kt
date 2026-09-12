package com.bandit.astro.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.astroPreferences by preferencesDataStore("astro_preferences")
data class Preferences(val theme: String = "Astro", val dynamic: Boolean = true,
    val spoilers: Boolean = true, val reminders: Boolean = false, val ist: Boolean = false)
class PreferenceStore(context: Context) {
    private val store = context.astroPreferences
    val flow = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map {
        Preferences(it[stringPreferencesKey("theme")] ?: "Astro", it[booleanPreferencesKey("dynamic")] ?: true,
            it[booleanPreferencesKey("spoilers")] ?: true, it[booleanPreferencesKey("reminders")] ?: false,
            it[booleanPreferencesKey("ist")] ?: false)
    }
    suspend fun theme(value: String) { store.edit { it[stringPreferencesKey("theme")] = value } }
    suspend fun flag(name: String, value: Boolean) { store.edit { it[booleanPreferencesKey(name)] = value } }
}
