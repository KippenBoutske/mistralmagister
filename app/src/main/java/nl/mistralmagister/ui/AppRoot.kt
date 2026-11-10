package nl.mistralmagister.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import nl.mistralmagister.ui.grades.GradesScreen
import nl.mistralmagister.ui.login.LoginScreen
import nl.mistralmagister.ui.schedule.ScheduleScreen
import nl.mistralmagister.ui.settings.SettingsScreen
import nl.mistralmagister.ui.today.TodayScreen

private enum class Tab(val label: String) {
    TODAY("Vandaag"),
    SCHEDULE("Rooster"),
    GRADES("Cijfers"),
    SETTINGS("Meer"),
}

@Composable
fun AppRoot(viewModel: AppViewModel, onOpenAuth: (String) -> Unit) {
    val state by viewModel.state.collectAsStateSafe()

    when (val s = state) {
        UiState.Loading -> LoadingScreen()
        is UiState.LoggedOut -> LoginScreen(viewModel, onOpenAuth, s.error)
        is UiState.Ready -> MainScaffold(viewModel, s)
    }
}

@Composable
private fun MainScaffold(viewModel: AppViewModel, ready: UiState.Ready) {
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    val haptics = LocalHapticFeedback.current

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = {
                            if (tab != item) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                tab = item
                            }
                        },
                        icon = {
                            Icon(
                                when (item) {
                                    Tab.TODAY -> Icons.Rounded.Home
                                    Tab.SCHEDULE -> Icons.Rounded.CalendarMonth
                                    Tab.GRADES -> Icons.Rounded.Grade
                                    Tab.SETTINGS -> Icons.Rounded.Settings
                                },
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            modifier = Modifier.padding(padding),
            transitionSpec = {
                val enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 12 }
                val exit = fadeOut(tween(120))
                enter togetherWith exit
            },
            label = "tab",
        ) { current ->
            when (current) {
                Tab.TODAY -> TodayScreen(viewModel, ready)
                Tab.SCHEDULE -> ScheduleScreen(ready)
                Tab.GRADES -> GradesScreen(ready)
                Tab.SETTINGS -> SettingsScreen(viewModel, ready)
            }
        }
    }
}
