package nl.mistralmagister.ui.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.mistralmagister.data.Appointment
import nl.mistralmagister.data.Grade
import nl.mistralmagister.data.MagisterTime
import nl.mistralmagister.notify.NextLesson
import nl.mistralmagister.ui.AppViewModel
import nl.mistralmagister.ui.UiState
import nl.mistralmagister.ui.schedule.LessonCard
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val time = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(viewModel: AppViewModel, ready: UiState.Ready) {
    val now = LocalDateTime.now()
    val today = ready.lessons.filter {
        MagisterTime.dateTime(it.start)?.toLocalDate() == now.toLocalDate()
    }
    val next = NextLesson.pick(ready.lessons, now)
    val latestGrade = ready.grades.firstOrNull()

    PullToRefreshBox(
        isRefreshing = ready.refreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp, vertical = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column {
                    Text(
                        ready.displayName ?: "Vandaag",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    ready.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
            if (next != null) {
                item { NextLessonCard(next, now) }
            }
            if (latestGrade != null) {
                item { LatestGradeCard(latestGrade) }
            }
            item {
                Text("Rooster vandaag", style = MaterialTheme.typography.titleMedium)
            }
            if (today.isEmpty()) {
                item { Text("Geen lessen vandaag 🎉") }
            } else {
                items(today, key = { it.id }) { lesson ->
                    LessonCard(lesson, highlight = lesson.id == next?.id)
                }
            }
        }
    }
}

@Composable
private fun NextLessonCard(next: Appointment, now: LocalDateTime) {
    val start = MagisterTime.dateTime(next.start)
    val progress by animateFloatAsState(
        targetValue = if (start != null && start.isBefore(now)) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "live",
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Nu / volgende les", style = MaterialTheme.typography.labelMedium)
            Text(next.vakNaam, style = MaterialTheme.typography.titleLarge)
            Text(
                "${start?.format(time) ?: ""}–${MagisterTime.dateTime(next.eind)?.format(time) ?: ""} " +
                    next.locatie.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
            )
            next.afgelastingInfo?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun LatestGradeCard(grade: Grade) {
    val color by animateColorAsState(
        targetValue = when {
            (grade.cijfer ?: 10.0) < 5.5 -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        },
        label = "gradeColor",
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Laatste cijfer", style = MaterialTheme.typography.labelMedium)
                Text(grade.vakNaam, style = MaterialTheme.typography.titleMedium)
                Text(grade.kolomNaam, style = MaterialTheme.typography.bodySmall)
            }
            Text(grade.cijferLabel, style = MaterialTheme.typography.headlineMedium, color = color)
        }
    }
}
