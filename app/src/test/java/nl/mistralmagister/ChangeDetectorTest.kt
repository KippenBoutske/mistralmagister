package nl.mistralmagister

import nl.mistralmagister.data.Appointment
import nl.mistralmagister.data.Grade
import nl.mistralmagister.notify.ChangeDetector
import nl.mistralmagister.notify.NextLesson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ChangeDetectorTest {
    private fun lesson(id: Int, start: String = "2030-01-01T09:00:00", status: Int? = 0) =
        Appointment(id = id, start = start, eind = "2030-01-01T10:00:00", status = status)

    @Test
    fun `nieuwe cijfers worden gedetecteerd`() {
        val grades = listOf(Grade(cijferId = 1), Grade(cijferId = 2))
        val fresh = ChangeDetector.newGrades(setOf("1"), grades)
        assertEquals(listOf(2), fresh.map { it.cijferId })
    }

    @Test
    fun `lesuitval verandert de key`() {
        val key = ChangeDetector.appointmentKey(lesson(5, status = 0))
        val changed = ChangeDetector.appointmentKey(lesson(5, status = 1))
        assertTrue(key != changed)
    }

    @Test
    fun `gewijzigde lessen alleen bij status ongelijk nul`() {
        val lessons = listOf(lesson(1), lesson(2, status = 1))
        val changed = ChangeDetector.changedLessons(setOf(ChangeDetector.appointmentKey(lesson(1))), lessons)
        assertEquals(listOf(2), changed.map { it.id })
    }

    @Test
    fun `statuslabels`() {
        assertEquals("Uitgevallen", ChangeDetector.statusLabel(1))
        assertEquals("Verplaatst", ChangeDetector.statusLabel(4))
        assertEquals("Uitgevallen", ChangeDetector.statusLabel(5))
        assertEquals("Gewijzigd", ChangeDetector.statusLabel(9))
        assertEquals("", ChangeDetector.statusLabel(0))
    }

    @Test
    fun `les die nu bezig is wordt getoond`() {
        val now = LocalDateTime.parse("2030-01-01T08:00:00")
        val lessons = listOf(
            lesson(1, "2030-01-01T08:00:00"),
            lesson(2, "2030-01-01T09:00:00"),
            lesson(3, "2030-01-02T09:00:00"),
        )
        assertEquals(1, NextLesson.pick(lessons, now)?.id)
    }

    @Test
    fun `na de laatste les van vandaag komt de eerste van morgen`() {
        val now = LocalDateTime.parse("2030-01-01T10:30:00")
        val lessons = listOf(
            lesson(1, "2030-01-01T08:00:00"),
            lesson(2, "2030-01-01T09:00:00"),
            lesson(3, "2030-01-02T09:00:00"),
        )
        assertEquals(3, NextLesson.pick(lessons, now)?.id)
    }

    @Test
    fun `geen lessen levert null op`() {
        assertNull(NextLesson.pick(emptyList()))
    }
}
