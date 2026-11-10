package nl.mistralmagister.ui.grades

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.mistralmagister.ui.UiState

@Composable
fun GradesScreen(ready: UiState.Ready) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Laatste cijfers", style = MaterialTheme.typography.titleLarge)
        }
        items(ready.grades, key = { it.cijferId }) { grade ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(grade.vakNaam, style = MaterialTheme.typography.titleSmall)
                        Text(grade.kolomNaam, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        grade.cijferLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        color = if ((grade.cijfer ?: 10.0) < 5.5) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
        }
        if (ready.grades.isEmpty()) {
            item { Text("Nog geen cijfers gevonden.") }
        }
    }
}
