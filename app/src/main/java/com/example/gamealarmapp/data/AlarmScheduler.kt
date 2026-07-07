package com.example.gamealarmapp.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar
import com.example.gamealarmapp.AlarmReceiver
import com.example.gamealarmapp.MainActivity

interface AlarmScheduler {
    fun schedule(alarm: Alarm)
    fun cancel(alarm: Alarm)
}

class AndroidAlarmScheduler(private val context: Context) : AlarmScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancel(alarm)
            return
        }

        // On Android 12+, check if we can schedule exact alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("AlarmScheduler", "Cannot schedule exact alarms - permission not granted!")
                // We should still try scheduling, but it might throw or fail. We'll attempt setAlarmClock.
            }
        }

        val triggerTime = calculateNextTriggerTime(alarm.hour, alarm.minute, alarm.daysOfWeek)
        
        // Intent that will trigger when alarm fires
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to launch MainActivity when clicking the alarm clock icon in the status bar
        val mainIntent = Intent(context, MainActivity::class.java)
        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id.hashCode() + 1,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showIntent)
        
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d("AlarmScheduler", "Scheduled alarm ${alarm.id} for $triggerTime (${alarm.hour}:${alarm.minute})")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "SecurityException scheduling exact alarm", e)
        }
    }

    override fun cancel(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Cancelled alarm ${alarm.id}")
        }
    }

    companion object {
        fun calculateNextTriggerTime(hour: Int, minute: Int, daysOfWeek: Set<Int>): Long {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val now = System.currentTimeMillis()
            if (daysOfWeek.isEmpty()) {
                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return calendar.timeInMillis
            } else {
                // Calendar.DAY_OF_WEEK: Sunday = 1, Monday = 2, ..., Saturday = 7
                val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val currentMappedDay = when (currentDayOfWeek) {
                    Calendar.MONDAY -> 1
                    Calendar.TUESDAY -> 2
                    Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4
                    Calendar.FRIDAY -> 5
                    Calendar.SATURDAY -> 6
                    Calendar.SUNDAY -> 7
                    else -> 1
                }

                var daysToAdd = -1
                // Look through today and the next 6 days (total 7 days)
                for (i in 0..6) {
                    val checkDay = ((currentMappedDay - 1 + i) % 7) + 1
                    if (daysOfWeek.contains(checkDay)) {
                        if (i == 0) {
                            if (calendar.timeInMillis > now) {
                                daysToAdd = 0
                                break
                            }
                        } else {
                            daysToAdd = i
                            break
                        }
                    }
                }
                
                if (daysToAdd == -1) {
                    // If not found in the week, check the closest day next week
                    for (i in 7..14) {
                        val checkDay = ((currentMappedDay - 1 + i) % 7) + 1
                        if (daysOfWeek.contains(checkDay)) {
                            daysToAdd = i
                            break
                        }
                    }
                }

                calendar.add(Calendar.DAY_OF_YEAR, if (daysToAdd == -1) 1 else daysToAdd)
                return calendar.timeInMillis
            }
        }
    }
}
