package com.bandit.astro.data

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class Session(val id: Int, val name: String, val avatar: String, internal val token: String, val expiresAt: Long)
data class OAuthResponse(val token: String, val expiresAt: Long)
fun validateOAuth(values: Map<String, String>, expected: String?, created: Long, now: Long): OAuthResponse {
    require(!expected.isNullOrBlank() && values["state"] == expected && now >= created && now - created < 900_000) { "Sign-in could not be verified. Start again in Astro." }
    require(values["error"] == null) { "AniList sign-in was cancelled." }
    val token = values["access_token"].orEmpty()
    require(token.isNotBlank() && token.length <= 10000 && token.none { it.isWhitespace() } && values["token_type"].equals("bearer", true)) { "Invalid sign-in response." }
    val seconds = values["expires_in"]?.toLongOrNull() ?: 0
    require(seconds > 0) { "This sign-in has expired." }
    return OAuthResponse(token, now + seconds.coerceAtMost(31_536_000) * 1000)
}
class AccountStore(context: Context) {
    private val prefs = context.getSharedPreferences("astro_secure_session", Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (ks.getKey("astro-session", null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("astro-session", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun read(): Session? = try {
        val raw = prefs.getString("session", null)
        if (raw == null) null else {
            val parts = raw.split(":")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
            val d = JSONObject(String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8))
            Session(d.getInt("id"), d.getString("name"), d.optString("avatar"), d.getString("token"), d.getLong("expires"))
        }
    } catch (_: Exception) { null }
    private val state = MutableStateFlow(read())
    val session = state.asStateFlow()
    var clientId: String
        get() = prefs.getString("client", "").orEmpty()
        set(value) { require(value.matches(Regex("[0-9]{1,12}"))); prefs.edit().putString("client", value).commit() }
    fun begin(useWebsite: Boolean = true): Uri {
        require(useWebsite || clientId.isNotBlank()) { "Configure the native AniList client ID first." }
        val nonce = ByteArray(32).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        check(prefs.edit().putString("state", nonce).putLong("created", System.currentTimeMillis()).commit())
        if (useWebsite) return Uri.parse("https://astro-anime-bandit.bandit44.chatgpt.site/auth/android").buildUpon()
            .encodedFragment("state=$nonce").build()
        return Uri.parse("https://anilist.co/api/v2/oauth/authorize").buildUpon().appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "token").appendQueryParameter("state", nonce).build()
    }
    fun callback(uri: Uri): OAuthResponse {
        require(uri.scheme == "astro" && uri.host == "oauth" && uri.path == "/anilist") { "Unexpected sign-in link." }
        val fragment = Uri.parse("https://localhost/?${uri.encodedFragment.orEmpty()}")
        val values = fragment.queryParameterNames.associateWith { fragment.getQueryParameter(it).orEmpty() }
        val expected = prefs.getString("state", null); val created = prefs.getLong("created", 0)
        prefs.edit().remove("state").remove("created").commit()
        return validateOAuth(values, expected, created, System.currentTimeMillis())
    }
    fun save(session: Session) {
        val data = JSONObject().put("id", session.id).put("name", session.name).put("avatar", session.avatar)
            .put("token", session.token).put("expires", session.expiresAt).toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(data)
        check(prefs.edit().putString("session", Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit())
        state.value = session
    }
    fun logout() { check(prefs.edit().remove("session").remove("state").remove("created").commit()); state.value = null }
}
