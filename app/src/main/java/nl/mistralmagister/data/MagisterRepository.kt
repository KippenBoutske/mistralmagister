package nl.mistralmagister.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import nl.mistralmagister.api.MagisterEndpoints
import nl.mistralmagister.auth.AuthRequestFactory
import nl.mistralmagister.auth.StoredSession
import nl.mistralmagister.auth.TokenResponse
import nl.mistralmagister.auth.TokenStore
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MagisterException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Magister REST + OAuth-client. Zelfde header-set als de officiële app:
 * Bearer-token + `X-API-Client-ID: M6LOAPP`; User-Agent strikt ASCII
 * (OkHttp weigert non-ASCII headerwaarden).
 */
class MagisterRepository(private val tokenStore: TokenStore) {
    private val client = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun exchangeToken(code: String, verifier: String): TokenResponse =
        postForm(MagisterEndpoints.TOKEN, AuthRequestFactory.tokenForm(code, verifier))

    suspend fun refresh(refreshToken: String): TokenResponse =
        postForm(MagisterEndpoints.TOKEN, AuthRequestFactory.refreshForm(refreshToken))

    suspend fun ensureFreshToken(): StoredSession? {
        val session = tokenStore.read() ?: return null
        if (!session.isExpired()) return session
        val refresh = session.refreshToken ?: return session
        val tokens = runCatching { refresh(refresh) }.getOrElse {
            throw MagisterException("Token verversen mislukt", it)
        }
        tokenStore.updateTokens(tokens, session)
        return tokenStore.read()
    }

    suspend fun fetchAccount(session: StoredSession): Account {
        val body = apiGet(MagisterEndpoints.account(root(session)), session)
        return decode(Account.serializer(), body)
    }

    suspend fun fetchAppointments(
        session: StoredSession,
        personId: Int,
        from: LocalDate,
        to: LocalDate,
    ): List<Appointment> {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val url = MagisterEndpoints.appointments(root(session), personId, from.format(fmt), to.format(fmt))
        return decode(AppointmentResponse.serializer(), apiGet(url, session)).items
    }

    suspend fun fetchRecentGrades(session: StoredSession, personId: Int): List<Grade> {
        val url = MagisterEndpoints.recentGrades(root(session), personId)
        return decode(GradeResponse.serializer(), apiGet(url, session)).items
    }

    private fun root(session: StoredSession): String =
        MagisterEndpoints.tenantRoot(session.tenantHost)

    private suspend fun apiGet(url: String, session: StoredSession): String {
        val first = execute(buildGet(url, session.accessToken))
        if (first.isSuccessful) return first.body
        val refreshed = refreshOnce(session)
        val second = execute(buildGet(url, refreshed.accessToken))
        if (second.isSuccessful) return second.body
        throw MagisterException("Magister gaf HTTP ${second.code}")
    }

    private suspend fun refreshOnce(session: StoredSession): StoredSession {
        val refresh = session.refreshToken ?: throw MagisterException("Sessie verlopen")
        tokenStore.updateTokens(refresh(refresh), session)
        return tokenStore.read() ?: throw MagisterException("Sessie verlopen")
    }

    private fun buildGet(url: String, token: String): Request =
        Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .header("X-API-Client-ID", MagisterEndpoints.CLIENT_ID)
            .header("User-Agent", "MistralMagister/0.1.0 (unofficial)")
            .build()

    private suspend fun postForm(url: String, params: Map<String, String>): TokenResponse =
        withContext(Dispatchers.IO) {
            val form = FormBody.Builder().apply { params.forEach { (k, v) -> add(k, v) } }.build()
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "MistralMagister/0.1.0 (unofficial)")
                .post(form)
                .build()
            val response = client.newCall(request).execute()
            response.use {
                val body = it.body?.string().orEmpty()
                if (!it.isSuccessful) throw MagisterException("OAuth gaf HTTP ${it.code}")
                decode(TokenResponse.serializer(), body)
            }
        }

    private data class HttpResult(val code: Int, val body: String, val isSuccessful: Boolean)

    private suspend fun execute(request: Request): HttpResult = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use {
            HttpResult(it.code, it.body?.string().orEmpty(), it.isSuccessful)
        }
    }

    private fun <T> decode(serializer: kotlinx.serialization.KSerializer<T>, body: String): T =
        try {
            json.decodeFromString(serializer, body)
        } catch (e: Exception) {
            throw MagisterException("Onverwacht antwoord van Magister", e)
        }
}
