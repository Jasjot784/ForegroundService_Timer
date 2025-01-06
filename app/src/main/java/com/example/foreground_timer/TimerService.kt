package com.example.foreground_timer

import android.Manifest.permission.FOREGROUND_SERVICE
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager

class TimerService : Service() {
    val TAG = "TimerService"
    private val CHANNEL_ID = "foreground_timer_channel" // Define your channel ID
    private var timeInSeconds = 0 // Variable to store elapsed time in seconds
    private val handler = Handler(Looper.getMainLooper()) // Handler to update every second
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            // Increase the time by 1 second
            timeInSeconds++

            // Update the notification with the new time
            updateNotification(timeInSeconds)

            // Re-run this task every second (1000 ms)
            handler.postDelayed(this, 1000)
        }
    }

    // Flags for controlling the timer
    private var isPaused = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val time = intent?.getStringExtra("time")
        Log.d(TAG, "onStartCommand: $time")

        when (intent?.action) {
            "PAUSE_TIMER" -> pauseTimer()
            "STOP_TIMER" -> stopTimer()
        }

        // Start or resume the timer
        if (!isPaused) {
            handler.post(updateTimeRunnable)
        }

        return START_STICKY
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()

        // Create the notification channel if running on Android Oreo (API 26) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Foreground Timer Service"
            val descriptionText = "Channel for Foreground Timer Service"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Create intents for the buttons
        val pauseIntent = Intent(this, TimerService::class.java).apply {
            action = "PAUSE_TIMER"
        }
        val pausePendingIntent: PendingIntent = PendingIntent.getService(
            this, 0, pauseIntent, PendingIntent.FLAG_MUTABLE
        )

        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = "STOP_TIMER"
        }
        val stopPendingIntent: PendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_MUTABLE
        )

        // Initial notification with pause and stop buttons
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Timer")
            .setContentText("0 seconds")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .addAction(R.drawable.ic_launcher_background, "Pause", pausePendingIntent) // Pause button
            .addAction(R.drawable.ic_launcher_background, "Stop", stopPendingIntent)  // Stop button
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_MUTABLE))
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startForeground(1, notification)
            Log.d(TAG, "onCreate: Running on older Android version")
        } else {
            Log.d(TAG, "onCreate: Running on Android Tiramisu or newer")
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        }
    }

    private fun updateNotification(timeInSeconds: Int) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE)

        // Create a new notification with the updated time
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Timer")
            .setContentText("$timeInSeconds seconds")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .addAction(R.drawable.ic_launcher_foreground, "Pause", getPausePendingIntent())  // Pause button
            .addAction(R.drawable.ic_launcher_foreground, "Stop", getStopPendingIntent())  // Stop button
            .setContentIntent(pendingIntent)
            .build()

        // Update the foreground service notification
        startForeground(1, notification)
    }

    // Get PendingIntent for pause
    private fun getPausePendingIntent(): PendingIntent {
        val pauseIntent = Intent(this, TimerService::class.java).apply {
            action = "PAUSE_TIMER"
        }
        return PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_MUTABLE)
    }

    // Get PendingIntent for stop
    private fun getStopPendingIntent(): PendingIntent {
        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = "STOP_TIMER"
        }
        return PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_MUTABLE)
    }

    private fun pauseTimer() {
        if (isPaused){
            isPaused = false
            return
        }
        isPaused = true
        handler.removeCallbacks(updateTimeRunnable)  // Stop updating the timer
    }

    private fun stopTimer() {
        stopSelf()  // Stop the service and remove notification
        handler.removeCallbacks(updateTimeRunnable)  // Stop updating the timer
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the handler from running when the service is destroyed
        handler.removeCallbacks(updateTimeRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}
