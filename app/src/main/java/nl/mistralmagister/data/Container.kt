package nl.mistralmagister.data

import android.content.Context
import android.content.SharedPreferences
import nl.mistralmagister.auth.TokenStore

/** Simpele service-locator die de app en de WorkManager-worker delen. */
class Container(context: Context) {
    val prefs: SharedPreferences =
        context.getSharedPreferences("mistral_state", Context.MODE_PRIVATE)
    val tokenStore: TokenStore = TokenStore(context)
    val repository: MagisterRepository = MagisterRepository(tokenStore)

    companion object {
        const val KEY_SEEN_GRADES = "seen_grades"
        const val KEY_SEEN_LESSONS = "seen_lessons"
        const val KEY_SNAPSHOT_READY = "snapshot_ready"
        const val KEY_NOTIFY_GRADES = "notify_grades"
        const val KEY_NOTIFY_SCHEDULE = "notify_schedule"
    }
}
