package nl.mistralmagister.ui.schedule

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import nl.mistralmagister.data.Appointment
import nl.mistralmagister.data.MagisterTime
import nl.mistralmagister.notify.ChangeDetector
import nl.mistralmagister.ui.UiState
import java.time.format.DateTimeFormatter

private val time = DateTimeFormatter.ofPattern("HH:mm")
private val day = DateTimeFormatter.ofPattern("EEEE d MMMM")

@Composable
fun ScheduleScreen(ready: UiState.Ready) {
    val byDay = ready.lessons
        .groupBy { MagisterTime.dateTime(it.start)?.toLocalDate() }
        .entries
        .sortedBy { it.key?.toEpochDay() ?: Long.MIN_VALUE }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        byDay.forEach { (date, lessons) ->
            item(key = "day-$date") {
                Text(
                    date?.format(day)?.replaceFirstChar { it.uppercase() } ?: "Onbekend",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            items(lessons, key = { it.id }) { lesson ->
                LessonCard(lesson)
            }
        }
        if (byDay.isEmpty()) {
            item { Text("Geen rooster gevonden.") }
        }
    }
}

@Composable
fun LessonCard(lesson: Appointment, highlight: Boolean = false) {
    val cancelled = ChangeDetector.isCancelled(lesson.status)
    val container by animateColorAsState(
        targetValue = if (cancelled) MaterialTheme.colorScheme.errorContainer
        else if (highlight) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "lesson",
    )
    val statusText = ChangeDetector.statusLabel(lesson.status)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (cancelled) 0.75f else 1f),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = container),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(lesson.vakNaam, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(
                        lesson.locatie?.takeIf { it.isNotBlank() },
                        lesson.lesuurLabel.takeIf { it.isNotBlank() }?.let { "uur $it" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
                lesson.afgelastingInfo?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    "${MagisterTime.dateTime(lesson.start)?.format(time) ?: ""}",
                    style = MaterialTheme.typography.titleSmall,
                )
                if (statusText.isNotBlank()) {
                    Text(
                        statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
