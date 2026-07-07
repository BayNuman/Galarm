package com.example.gamealarmapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gamealarmapp.data.Alarm
import com.example.gamealarmapp.data.AlarmRepository
import com.example.gamealarmapp.data.AlarmScheduler
import com.example.gamealarmapp.data.AndroidAlarmScheduler
import com.example.gamealarmapp.data.FileAlarmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AlarmRepository = FileAlarmRepository(application)
    private val scheduler: AlarmScheduler = AndroidAlarmScheduler(application)

    val alarms: StateFlow<List<Alarm>> = repository.alarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            repository.saveAlarm(updated)
            if (updated.isEnabled) {
                scheduler.schedule(updated)
            } else {
                scheduler.cancel(updated)
            }
        }
    }

    fun saveAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.saveAlarm(alarm)
            if (alarm.isEnabled) {
                scheduler.schedule(alarm)
            } else {
                scheduler.cancel(alarm)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm.id)
            scheduler.cancel(alarm)
        }
    }
}
