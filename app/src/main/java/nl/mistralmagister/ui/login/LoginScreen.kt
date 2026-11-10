package nl.mistralmagister.ui.login

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import nl.mistralmagister.api.School
import nl.mistralmagister.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: AppViewModel, onOpenAuth: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf<School?>(null) }
    var launching by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val results = remember(query) { viewModel.searchSchools(query) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Mistral Magister", style = MaterialTheme.typography.displaySmall)
        Text(
            "Onofficiële Magister-client. Log in met je schoolaccount.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Zoek je school") },
            supportingText = { Text("Typ een naam of een tenant-host zoals citadel.magister.net") },
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = false)
                .animateContentSize(),
        ) {
            items(results, key = { it.host }) { school ->
                SchoolRow(school, selected?.host == school.host) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    selected = school
                }
                HorizontalDivider()
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val school = selected ?: return@Button
                val url = viewModel.beginLogin(school)
                if (url != null) {
                    launching = true
                    onOpenAuth(url)
                }
            },
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (launching) {
                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp).width(18.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(if (launching) "Inloggen openen…" else "Inloggen")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SchoolRow(school: School, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(school.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(school.city.ifBlank { null }, school.host)
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (selected) {
                Text("✓", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
