package nl.mistralmagister.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import nl.mistralmagister.api.MagisterEndpoints
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

data class AuthRequest(
    val authorizeUrl: String,
    val codeVerifier: String,
    val state: String,
    val nonce: String,
    val tenantHost: String,
)

object AuthRequestFactory {
    fun create(
        tenantHost: String,
        pkce: PkcePair = Pkce.generate(),
        state: String = UUID.randomUUID().toString(),
        nonce: String = UUID.randomUUID().toString(),
    ): AuthRequest {
        val host = tenantHost.trim().lowercase()
        val params = linkedMapOf(
            "client_id" to MagisterEndpoints.CLIENT_ID,
            "redirect_uri" to MagisterEndpoints.REDIRECT_URI,
            "response_type" to MagisterEndpoints.RESPONSE_TYPE,
            "scope" to MagisterEndpoints.SCOPE,
            "state" to state,
            "nonce" to nonce,
            "code_challenge" to pkce.challenge,
            "code_challenge_method" to MagisterEndpoints.CODE_CHALLENGE_METHOD,
            "prompt" to "select_account",
        )
        if (host.isNotBlank()) params["acr_values"] = "tenant:$host"
        val query = params.entries.joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }
        return AuthRequest(
            authorizeUrl = "${MagisterEndpoints.AUTHORIZE}?$query",
            codeVerifier = pkce.verifier,
            state = state,
            nonce = nonce,
            tenantHost = host,
        )
    }

    fun extractCode(redirectUrl: String): String? = params(redirectUrl)["code"]

    fun extractState(redirectUrl: String): String? = params(redirectUrl)["state"]

    fun extractError(redirectUrl: String): String? {
        val params = params(redirectUrl)
        val error = params["error"] ?: return null
        val description = params["error_description"]
        return if (description.isNullOrBlank()) error else "$error: $description"
    }

    private fun params(redirectUrl: String): Map<String, String> {
        val normalized = redirectUrl.replace('#', '?')
        if (!normalized.startsWith(MagisterEndpoints.REDIRECT_URI.trimEnd('/'))) return emptyMap()
        return normalized.substringAfter('?', "").split('&').mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq <= 0) return@mapNotNull null
            part.substring(0, eq) to java.net.URLDecoder.decode(
                part.substring(eq + 1),
                StandardCharsets.UTF_8,
            )
        }.toMap()
    }

    fun tokenForm(code: String, codeVerifier: String): Map<String, String> = mapOf(
        "grant_type" to "authorization_code",
        "code" to code,
        "redirect_uri" to MagisterEndpoints.REDIRECT_URI,
        "client_id" to MagisterEndpoints.CLIENT_ID,
        "code_verifier" to codeVerifier,
    )

    fun refreshForm(refreshToken: String): Map<String, String> = mapOf(
        "grant_type" to "refresh_token",
        "refresh_token" to refreshToken,
        "client_id" to MagisterEndpoints.CLIENT_ID,
    )

    private fun enc(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
}

/**
 * Tokens AES-GCM versleuteld via Android Keystore (EncryptedSharedPreferences).
 * De lopende PKCE-challenge staat hier ook in, die is kortlevend maar gevoelig.
 */
class TokenStore(context: Context) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "mistral_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    @Synchronized
    fun read(): StoredSession? {
        val access = prefs.getString(KEY_ACCESS) ?: return null
        return StoredSession(
            accessToken = access,
            refreshToken = prefs.getString(KEY_REFRESH),
            idToken = prefs.getString(KEY_ID),
            expiresAtEpochMs = prefs.getLong(KEY_EXPIRES, 0L),
            tenantHost = prefs.getString(KEY_TENANT).orEmpty(),
            personId = prefs.getInt(KEY_PERSON, -1).takeIf { it > 0 },
            displayName = prefs.getString(KEY_NAME),
        )
    }

    @Synchronized
    fun write(session: StoredSession) {
        prefs.edit()
            .putString(KEY_ACCESS, session.accessToken)
            .putString(KEY_REFRESH, session.refreshToken)
            .putString(KEY_ID, session.idToken)
            .putLong(KEY_EXPIRES, session.expiresAtEpochMs)
            .putString(KEY_TENANT, session.tenantHost)
            .putInt(KEY_PERSON, session.personId ?: -1)
            .putString(KEY_NAME, session.displayName)
            .apply()
    }

    @Synchronized
    fun updateTokens(tokens: TokenResponse, existing: StoredSession) {
        write(
            existing.copy(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken ?: existing.refreshToken,
                idToken = tokens.idToken ?: existing.idToken,
                expiresAtEpochMs = System.currentTimeMillis() + tokens.expiresIn * 1000L,
            ),
        )
    }

    @Synchronized
    fun clear() {
        prefs.edit().clear().apply()
    }

    fun savePending(verifier: String, state: String, tenantHost: String) {
        prefs.edit()
            .putString(KEY_PENDING_VERIFIER, verifier)
            .putString(KEY_PENDING_STATE, state)
            .putString(KEY_TENANT, tenantHost)
            .apply()
    }

    data class PendingAuth(val verifier: String, val state: String, val tenantHost: String)

    fun readPending(): PendingAuth? {
        val verifier = prefs.getString(KEY_PENDING_VERIFIER) ?: return null
        val state = prefs.getString(KEY_PENDING_STATE) ?: return null
        return PendingAuth(verifier, state, prefs.getString(KEY_TENANT).orEmpty())
    }

    fun clearPending() {
        prefs.edit()
            .remove(KEY_PENDING_VERIFIER)
            .remove(KEY_PENDING_STATE)
            .apply()
    }

    private companion object {
        const val KEY_ACCESS = "access"
        const val KEY_REFRESH = "refresh"
        const val KEY_ID = "id"
        const val KEY_EXPIRES = "expires"
        const val KEY_TENANT = "tenant"
        const val KEY_PERSON = "person"
        const val KEY_NAME = "name"
        const val KEY_PENDING_VERIFIER = "pending_verifier"
        const val KEY_PENDING_STATE = "pending_state"
    }
}
