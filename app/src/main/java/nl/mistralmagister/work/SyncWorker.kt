package nl.mistralmagister.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import nl.mistralmagister.data.Container
import nl.mistralmagister.data.MagisterTime
import nl.mistralmagister.magisterApp
import nl.mistralmagister.notify.ChangeDetector
import nl.mistralmagister.notify.NextLesson
import nl.mistralmagister.notify.Notifier
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Achtergrondsync elke 30 minuten: haalt rooster + laatste cijfers op,
 * meldt nieuwe cijfers en lesuitval en ververst de live-update-notificatie.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = applicationContext.magisterApp().container
        val session = runCatching { container.repository.ensureFreshToken() }
            .getOrNull() ?: return Result.success()
        val personId = session.personId ?: return Result.success()

        val from = LocalDate.now()
        val to = LocalDate.now().plusDays(7)
        val lessons = runCatching {
            container.repository.fetchAppointments(session, personId, from, to)
        }.getOrElse { return Result.retry() }
        val grades = runCatching {
            container.repository.fetchRecentGrades(session, personId)
        }.getOrElse { return Result.retry() }

        val prefs = container.prefs
        val snapshotReady = prefs.getBoolean(Container.KEY_SNAPSHOT_READY, false)
        val seenGrades = prefs.getStringSet(Container.KEY_SEEN_GRADES, emptySet()).orEmpty()
        val seenLessons = prefs.getStringSet(Container.KEY_SEEN_LESSONS, emptySet()).orEmpty()

        if (snapshotReady) {
            if (prefs.getBoolean(Container.KEY_NOTIFY_GRADES, true)) {
                Notifier.notifyNewGrades(applicationContext, ChangeDetector.newGrades(seenGrades, grades))
            }
            if (prefs.getBoolean(Container.KEY_NOTIFY_SCHEDULE, true)) {
                Notifier.notifyLessonChanges(applicationContext, ChangeDetector.changedLessons(seenLessons, lessons))
            }
        }

        prefs.edit()
            .putStringSet(Container.KEY_SEEN_GRADES, grades.map { it.cijferId.toString() }.toSet())
            .putStringSet(
                Container.KEY_SEEN_LESSONS,
                lessons.map { ChangeDetector.appointmentKey(it) }.toSet(),
            )
            .putBoolean(Container.KEY_SNAPSHOT_READY, true)
            .apply()

        Notifier.updateLiveLesson(applicationContext, NextLesson.pick(lessons))
        return Result.success()
    }

    companion object {
        const val UNIQUE = "mistral-sync"

        fun enqueue(context: Context) {
            Notifier.ensureChannels(context)
            val request = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                )
                .build()
            val manager = WorkManager.getInstance(context)
            manager.enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.KEEP, request)
            manager.enqueueUniqueWork(
                "$UNIQUE-once",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<SyncWorker>().build(),
            )
        }
    }
}
