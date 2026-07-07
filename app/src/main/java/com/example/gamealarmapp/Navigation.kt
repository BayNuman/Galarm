package com.example.gamealarmapp

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.gamealarmapp.ui.AlarmViewModel
import com.example.gamealarmapp.ui.DashboardScreen
import com.example.gamealarmapp.ui.AddEditAlarmScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Main)
    val alarmViewModel: AlarmViewModel = viewModel()

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Main> {
                DashboardScreen(
                    viewModel = alarmViewModel,
                    onAddAlarm = { backStack.add(AddEditAlarm(null)) },
                    onEditAlarm = { id -> backStack.add(AddEditAlarm(id)) },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
            entry<AddEditAlarm> { key ->
                AddEditAlarmScreen(
                    alarmId = key.alarmId,
                    viewModel = alarmViewModel,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.safeDrawingPadding()
                )
            }
        }
    )
}
