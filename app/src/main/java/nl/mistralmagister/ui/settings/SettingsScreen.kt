package nl.mistralmagister.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import nl.mistralmagister.haptics.Haptics
import nl.mistralmagister.ui.AppViewModel
import nl.mistralmagister.ui.UiState

@Composable
fun SettingsScreen(viewModel: AppViewModel, ready: UiState.Ready) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var notifyGrades by remember { mutableStateOf(viewModel.notifyGradesEnabled()) }
    var notifySchedule by remember { mutableStateOf(viewModel.notifyScheduleEnabled()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Account", style = MaterialTheme.typography.titleMedium)
        Text(ready.displayName ?: "Ingelogd")
        Text(ready.schoolHost, style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text("Meldingen", style = MaterialTheme.typography.titleMedium)
        ToggleRow("Nieuwe cijfers", notifyGrades) {
            notifyGrades = it
            viewModel.setNotifyGrades(it)
            Haptics.tick(context)
        }
        ToggleRow("Lesuitval en roosterwijzigingen", notifySchedule) {
            notifySchedule = it
            viewModel.setNotifySchedule(it)
            Haptics.tick(context)
        }
        HorizontalDivider()
        Text(
            "Live update: een doorlopende melding toont je huidige of volgende les " +
                "en wordt elke 30 minuten automatisch ververst.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                Haptics.confirm(context)
                viewModel.syncNow()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Nu synchroniseren")
        }
        Button(
            onClick = {
                Haptics.warn(context)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.logout()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Uitloggen")
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
