package nl.mistralmagister.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import nl.mistralmagister.MainActivity
import nl.mistralmagister.R
import nl.mistralmagister.data.Appointment
import nl.mistralmagister.data.Grade
import nl.mistralmagister.data.MagisterTime
import java.time.format.DateTimeFormatter

object Notifier {
    const val CHANNEL_GRADES = "cijfers"
    const val CHANNEL_SCHEDULE = "rooster"
    const val CHANNEL_LIVE = "live"

    private val time = DateTimeFormatter.ofPattern("HH:mm")

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        listOf(
            NotificationChannel(CHANNEL_GRADES, "Nieuwe cijfers", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Melding bij een nieuw cijfer" },
            NotificationChannel(CHANNEL_SCHEDULE, "Leswijzigingen", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Melding bij lesuitval of roosterwijziging" },
            NotificationChannel(CHANNEL_LIVE, "Volgende les", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Live update met de huidige of volgende les" },
        ).forEach {
            if (manager.getNotificationChannel(it.id) == null) manager.createNotificationChannel(it)
        }
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun notifyNewGrades(context: Context, grades: List<Grade>) {
        if (grades.isEmpty() || !canNotify(context)) return
        ensureChannels(context)
        val title = if (grades.size == 1) {
            "Nieuw cijfer: ${grades.first().cijferLabel}"
        } else {
            "${grades.size} nieuwe cijfers"
        }
        val text = grades.take(5).joinToString { "${it.vakNaam}: ${it.cijferLabel}" }
        post(
            context, CHANNEL_GRADES, 2001,
            NotificationCompat.Builder(context, CHANNEL_GRADES)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true),
        )
    }

    fun notifyLessonChanges(context: Context, lessons: List<Appointment>) {
        if (lessons.isEmpty() || !canNotify(context)) return
        ensureChannels(context)
        val lines = lessons.take(6).map {
            val start = MagisterTime.dateTime(it.start)?.format(time) ?: ""
            "${ChangeDetector.statusLabel(it.status)}: ${it.vakNaam} $start ${it.locatie.orEmpty()}".trim()
        }
        val title = if (lessons.size == 1) "Leswijziging" else "${lessons.size} leswijzigingen"
        val text = lines.joinToString("\n")
        post(
            context, CHANNEL_SCHEDULE, 2002,
            NotificationCompat.Builder(context, CHANNEL_SCHEDULE)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(lines.first())
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true),
        )
    }

    /** Live update: doorlopende, stille notificatie die de huidige/volgende les toont. */
    fun updateLiveLesson(context: Context, lesson: Appointment?) {
        ensureChannels(context)
        if (lesson == null || !canNotify(context)) {
            NotificationManagerCompat.from(context).cancel(LIVE_NOTIF_ID)
            return
        }
        val start = MagisterTime.dateTime(lesson.start)?.format(time) ?: ""
        val end = MagisterTime.dateTime(lesson.eind)?.format(time) ?: ""
        val title = lesson.vakNaam.ifBlank { "Les" }
        val text = "$start–$end ${lesson.locatie.orEmpty()}".trim() +
            (lesson.lesuurLabel.takeIf { it.isNotBlank() }?.let { " · uur $it" } ?: "")
        post(
            context, CHANNEL_LIVE, LIVE_NOTIF_ID,
            NotificationCompat.Builder(context, CHANNEL_LIVE)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true),
        )
    }

    private const val LIVE_NOTIF_ID = 2100

    private fun post(
        context: Context,
        channel: String,
        id: Int,
        builder: NotificationCompat.Builder,
    ) {
        val tap = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        NotificationManagerCompat.from(context).notify(id, builder.setContentIntent(tap).build())
    }
}
