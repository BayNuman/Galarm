package com.example.gamealarmapp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File

interface AlarmRepository {
    val alarms: Flow<List<Alarm>>
    suspend fun getAlarm(id: String): Alarm?
    suspend fun saveAlarm(alarm: Alarm)
    suspend fun deleteAlarm(id: String)
}

class FileAlarmRepository(private val context: Context) : AlarmRepository {
    private val file = File(context.filesDir, "alarms.json")
    private val mutex = Mutex()
    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    override val alarms: Flow<List<Alarm>> = _alarms.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    init {
        loadAlarms()
    }

    private fun loadAlarms() {
        if (!file.exists()) {
            _alarms.value = emptyList()
            return
        }
        try {
            val content = file.readText()
            val list = json.decodeFromString<List<Alarm>>(content)
            _alarms.value = list.sortedWith(compareBy<Alarm>({ it.hour }, { it.minute }))
        } catch (e: Exception) {
            e.printStackTrace()
            _alarms.value = emptyList()
        }
    }

    override suspend fun getAlarm(id: String): Alarm? = mutex.withLock {
        _alarms.value.find { it.id == id }
    }

    override suspend fun saveAlarm(alarm: Alarm) = mutex.withLock {
        val currentList = _alarms.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            currentList[index] = alarm
        } else {
            currentList.add(alarm)
        }
        val newList = currentList.sortedWith(compareBy<Alarm>({ it.hour }, { it.minute }))
        _alarms.value = newList
        saveToFile(newList)
    }

    override suspend fun deleteAlarm(id: String) = mutex.withLock {
        val currentList = _alarms.value.toMutableList()
        if (currentList.removeAll { it.id == id }) {
            _alarms.value = currentList
            saveToFile(currentList)
        }
    }

    private suspend fun saveToFile(list: List<Alarm>) = withContext(Dispatchers.IO) {
        try {
            val content = json.encodeToString(list)
            file.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
