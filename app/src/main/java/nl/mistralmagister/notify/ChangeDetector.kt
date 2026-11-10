package nl.mistralmagister.notify

import nl.mistralmagister.data.Appointment
import nl.mistralmagister.data.Grade
import nl.mistralmagister.data.MagisterTime
import java.time.LocalDateTime

/** Pure diff-logica, los van Android zodat die unit-testbaar is. */
object ChangeDetector {
    /** Status-wijzigingen (bijv. uitval) geven een nieuwe key, dus die vallen in de diff. */
    fun appointmentKey(a: Appointment): String = "${a.id}|${a.start}|${a.status ?: 0}"

    fun newGrades(seenIds: Set<String>, grades: List<Grade>): List<Grade> =
        grades.filter { it.cijferId > 0 && it.cijferId.toString() !in seenIds }

    fun changedLessons(seenKeys: Set<String>, lessons: List<Appointment>): List<Appointment> =
        lessons.filter { appointmentKey(it) !in seenKeys && (it.status ?: 0) != 0 }

    fun statusLabel(status: Int?): String = when (status) {
        null, 0 -> ""
        1 -> "Uitgevallen"
        4 -> "Verplaatst"
        5 -> "Uitgevallen"
        else -> "Gewijzigd"
    }

    fun isCancelled(status: Int?): Boolean = status == 1 || status == 5
}

/** Kiest de les die nu bezig is, anders de eerstvolgende. */
object NextLesson {
    fun pick(lessons: List<Appointment>, now: LocalDateTime = LocalDateTime.now()): Appointment? {
        val start = { MagisterTime.dateTime(it.start) ?: now }
        val end = { MagisterTime.dateTime(it.eind) ?: now }
        lessons.filter { start(it) <= now && end(it) > now }
            .minByOrNull(start)
            ?.let { return it }
        return lessons.filter { start(it) >= now }.minByOrNull(start)
    }
}
