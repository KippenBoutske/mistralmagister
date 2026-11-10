package nl.mistralmagister.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import nl.mistralmagister.api.KnownSchools
import nl.mistralmagister.api.School
import nl.mistralmagister.api.Tenant
import nl.mistralmagister.auth.AuthRequestFactory
import nl.mistralmagister.auth.StoredSession
import nl.mistralmagister.data.Appointment
import nl.mistralmagister.data.Container
import nl.mistralmagister.data.Grade
import nl.mistralmagister.data.MagisterTime
import java.time.LocalDate

sealed interface UiState {
    data object Loading : UiState
    data class LoggedOut(val error: String? = null) : UiState
    data class Ready(
        val displayName: String?,
        val schoolHost: String,
        val lessons: List<Appointment> = emptyList(),
        val grades: List<Grade> = emptyList(),
        val refreshing: Boolean = false,
        val error: String? = null,
    ) : UiState
}

class AppViewModel(private val container: Container) : ViewModel() {
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _schoolQuery = MutableStateFlow("")
    val schoolQuery: StateFlow<String> = _schoolQuery.asStateFlow()

    init {
        viewModelScope.launch {
            val session = runCatching { container.repository.ensureFreshToken() }.getOrNull()
            if (session?.personId != null) load() else _state.value = UiState.LoggedOut()
        }
    }

    fun searchSchools(query: String): List<School> {
        _schoolQuery.value = query
        return KnownSchools.search(query)
    }

    /**
     * Start de OAuth-PKCE-flow: bouwt de authorize-URL, bewaart de challenge
     * en geeft de URL terug zodat de activity een Custom Tab kan openen.
     */
    fun beginLogin(school: School): String? {
        val host = Tenant.normalize(school.host)
        if (host.isBlank()) return null
        val request = AuthRequestFactory.create(host)
        container.tokenStore.savePending(request.codeVerifier, request.state, host)
        return request.authorizeUrl
    }

    /** Verwerkt de m6loapp://oauth2redirect/-callback. */
    fun handleRedirect(url: String) {
        val pending = container.tokenStore.readPending() ?: return
        val error = AuthRequestFactory.extractError(url)
        if (error != null) {
            _state.value = UiState.LoggedOut("Inloggen mislukt: $error")
            container.tokenStore.clearPending()
            return
        }
        val code = AuthRequestFactory.extractCode(url) ?: return
        if (AuthRequestFactory.extractState(url) != pending.state) return
        viewModelScope.launch {
            try {
                val tokens = container.repository.exchangeToken(code, pending.verifier)
                val provisional = StoredSession(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    idToken = tokens.idToken,
                    expiresAtEpochMs = System.currentTimeMillis() + tokens.expiresIn * 1000L,
                    tenantHost = pending.tenantHost,
                    personId = null,
                    displayName = null,
                )
                val account = container.repository.fetchAccount(provisional)
                val personId = account.id
                    ?: throw IllegalStateException("Magister gaf geen persoon-id terug")
                container.tokenStore.write(
                    provisional.copy(
                        personId = personId,
                        displayName = account.displayName,
                    ),
                )
                container.tokenStore.clearPending()
                load()
            } catch (e: Exception) {
                container.tokenStore.clear()
                _state.value = UiState.LoggedOut(
                    "Inloggen mislukt: " + (e.message ?: "onbekende fout"),
                )
            }
        }
    }

    fun refresh() {
        val current = _state.value
        if (current !is UiState.Ready || current.refreshing) return
        _state.value = current.copy(refreshing = true)
        viewModelScope.launch { load(keepRefreshingFlag = true) }
    }

    fun syncNow(onDone: () -> Unit = {}) {
        viewModelScope.launch { load(); onDone() }
    }

    fun setNotifyGrades(enabled: Boolean) {
        container.prefs.edit().putBoolean(Container.KEY_NOTIFY_GRADES, enabled).apply()
    }

    fun setNotifySchedule(enabled: Boolean) {
        container.prefs.edit().putBoolean(Container.KEY_NOTIFY_SCHEDULE, enabled).apply()
    }

    fun notifyGradesEnabled(): Boolean =
        container.prefs.getBoolean(Container.KEY_NOTIFY_GRADES, true)

    fun notifyScheduleEnabled(): Boolean =
        container.prefs.getBoolean(Container.KEY_NOTIFY_SCHEDULE, true)

    fun logout() {
        container.tokenStore.clear()
        container.prefs.edit()
            .remove(Container.KEY_SEEN_GRADES)
            .remove(Container.KEY_SEEN_LESSONS)
            .remove(Container.KEY_SNAPSHOT_READY)
            .apply()
        _state.value = UiState.LoggedOut()
    }

    private fun load(keepRefreshingFlag: Boolean = false) {
        viewModelScope.launch {
            val session = runCatching { container.repository.ensureFreshToken() }.getOrNull()
            val personId = session?.personId
            if (session == null || personId == null) {
                _state.value = UiState.LoggedOut()
                return@launch
            }
            val previous = _state.value as? UiState.Ready
            try {
                val from = LocalDate.now()
                val to = LocalDate.now().plusDays(7)
                val lessons = container.repository.fetchAppointments(session, personId, from, to)
                    .sortedBy { MagisterTime.dateTime(it.start) }
                val grades = container.repository.fetchRecentGrades(session, personId)
                _state.value = UiState.Ready(
                    displayName = session.displayName,
                    schoolHost = session.tenantHost,
                    lessons = lessons,
                    grades = grades,
                    refreshing = false,
                    error = null,
                )
            } catch (e: Exception) {
                _state.value = UiState.Ready(
                    displayName = session.displayName,
                    schoolHost = session.tenantHost,
                    lessons = previous?.lessons.orEmpty(),
                    grades = previous?.grades.orEmpty(),
                    refreshing = false,
                    error = e.message ?: "Vernieuwen mislukt",
                )
            }
        }
    }
}
