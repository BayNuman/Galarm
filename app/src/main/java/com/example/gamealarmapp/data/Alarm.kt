package com.example.gamealarmapp.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class GameType {
    NONE,
    MATH,
    MEMORY,
    SHAKE,
    SEQUENCE,
    COLOR_MATCH,
    ZIP
}

@Serializable
enum class GameDifficulty {
    EASY,
    MEDIUM,
    HARD
}

@Serializable
data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<Int> = emptySet(), // 1 = Monday, ..., 7 = Sunday
    val isEnabled: Boolean = true,
    val label: String = "Alarm",
    val isVibrate: Boolean = true,
    val gameType: GameType = GameType.NONE,
    val difficulty: GameDifficulty = GameDifficulty.MEDIUM,
    val soundName: String = "Default"
) {
    val isRepeating: Boolean
        get() = daysOfWeek.isNotEmpty()

    fun getFormattedTime(): String {
        val h = hour.toString().padStart(2, '0')
        val m = minute.toString().padStart(2, '0')
        return "$h:$m"
    }

    fun getRepeatDaysText(): String {
        if (daysOfWeek.isEmpty()) return "Once"
        if (daysOfWeek.size == 7) return "Every day"
        if (daysOfWeek.size == 5 && !daysOfWeek.contains(6) && !daysOfWeek.contains(7)) return "Weekdays"
        if (daysOfWeek.size == 2 && daysOfWeek.contains(6) && daysOfWeek.contains(7)) return "Weekends"
        
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return daysOfWeek.sorted().joinToString(", ") { dayNames[it - 1] }
    }
}
