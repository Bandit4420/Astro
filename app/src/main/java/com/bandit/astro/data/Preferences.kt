package com.bandit.astro.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.astroPreferences by preferencesDataStore("astro_preferences")
data class Preferences(val theme: String = "Astro", val dynamic: Boolean = true,
    val spoilers: Boolean = true, val reminders: Boolean = false, val ist: Boolean = false,
    val autoSync: Boolean = false, val glass: Boolean = false, val blur: Boolean = false,
    val artwork: Boolean = true, val animations: Boolean = true, val compact: Boolean = false,
    val blurAmount: Float = 14f, val transparency: Float = .75f, val radius: Float = 18f,
    val amoled:Boolean=false, val accentHex:String="", val buttonAccent:String="", val navigationAccent:String="",
    val fontStyle:String="Modern", val highRefresh:Boolean=false)
class PreferenceStore(context: Context) {
    private val store = context.astroPreferences
    val flow = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map { p ->
        fun flag(name: String, default: Boolean=false)=p[booleanPreferencesKey(name)] ?: default
        Preferences(p[stringPreferencesKey("theme")] ?: "Astro",flag("dynamic",true),flag("spoilers",true),flag("reminders"),flag("ist"),
            flag("autoSync"),flag("glass"),flag("blur"),flag("artwork",true),flag("animations",true),flag("compact"),
            (p[floatPreferencesKey("blurAmount")] ?: 14f).coerceIn(0f,32f), (p[floatPreferencesKey("transparency")] ?: .75f).coerceIn(.35f,1f),
            (p[floatPreferencesKey("radius")] ?: 18f).coerceIn(0f,32f),
            flag("amoled"),p[stringPreferencesKey("accentHex")].orEmpty(),p[stringPreferencesKey("buttonAccent")].orEmpty(),
            p[stringPreferencesKey("navigationAccent")].orEmpty(),p[stringPreferencesKey("fontStyle")] ?: "Modern",flag("highRefresh"))
    }
    suspend fun text(name:String,value:String) { store.edit {it[stringPreferencesKey(name)]=value} }
    suspend fun theme(value: String) { store.edit { it[stringPreferencesKey("theme")] = value } }
    suspend fun flag(name: String, value: Boolean) { store.edit { it[booleanPreferencesKey(name)] = value } }
    suspend fun number(name: String, value: Float) { store.edit { it[floatPreferencesKey(name)] = value } }
}
