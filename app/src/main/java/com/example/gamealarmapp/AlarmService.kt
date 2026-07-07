package com.example.gamealarmapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gamealarmapp.data.Alarm
import com.example.gamealarmapp.data.FileAlarmRepository
import com.example.gamealarmapp.data.AndroidAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.util.Calendar

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    companion object {
        const val ACTION_DISMISS = "com.example.gamealarmapp.ACTION_DISMISS"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "alarm_ringing_channel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("AlarmService", "Service Created")
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val alarmId = intent?.getStringExtra("ALARM_ID") ?: ""
        val alarmLabel = intent?.getStringExtra("ALARM_LABEL") ?: "Alarm"

        Log.d("AlarmService", "onStartCommand: action=$action, alarmId=$alarmId")

        if (action == ACTION_DISMISS) {
            dismissAlarm(alarmId)
            return START_NOT_STICKY
        }

        // Play Sound and Vibrate
        startRinging()

        // Show Foreground Notification with Fullscreen Intent
        showForegroundNotification(alarmId, alarmLabel)

        // Auto-dismiss after 7 minutes (420,000 milliseconds) to prevent battery drain
        serviceScope.launch {
            kotlinx.coroutines.delay(7 * 60 * 1000L)
            Log.d("AlarmService", "Auto-dismissing alarm due to 7-minute timeout")
            dismissAlarm(alarmId)
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "GameAlarmApp:WakeLock"
        ).apply {
            acquire(10 * 60 * 1000L) // 10 minutes max
        }
    }

    private fun startRinging() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }

            // Start vibration
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 800, 800)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Error playing alarm sound", e)
        }
    }

    private fun showForegroundNotification(alarmId: String, alarmLabel: String) {
        // Notification Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Ringing Screen",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for active alarm alarms"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Full Screen Intent
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", alarmLabel)
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(alarmLabel)
            .setContentText("Uyanma Vakti! Alarmı kapatmak için oyunu çözün.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun dismissAlarm(alarmId: String) {
        Log.d("AlarmService", "Dismissing alarm: $alarmId")
        stopRinging()

        // Reschedule alarm if it is repeating, else turn it off
        serviceScope.launch {
            val repository = FileAlarmRepository(this@AlarmService)
            val alarm = repository.getAlarm(alarmId)
            if (alarm != null) {
                if (alarm.isRepeating) {
                    // For repeating alarms, schedule the next occurrence
                    val scheduler = AndroidAlarmScheduler(this@AlarmService)
                    scheduler.schedule(alarm)
                } else {
                    // One-time alarms: disable after trigger
                    val updated = alarm.copy(isEnabled = false)
                    repository.saveAlarm(updated)
                    val scheduler = AndroidAlarmScheduler(this@AlarmService)
                    scheduler.cancel(alarm)
                }
            }
            stopSelf()
        }
    }


    private fun stopRinging() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onDestroy() {
        try {
            serviceScope.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopRinging()
        super.onDestroy()
        Log.d("AlarmService", "Service Destroyed")
    }
}
