# Foreground Timer Service App

This Android app demonstrates how to create a foreground service that tracks time and displays it in a persistent notification. The app allows users to start the service by pressing a button, pause the timer, and stop the service.

## Features
- Start a foreground timer service.
- Display elapsed time in a notification.
- Pause the timer using a pause button in the notification.
- Resume the timer using same button when clicked against stopped timer.
- Stop the service and remove the notification using a button in the notification.

---

## Step-by-Step Guide

### Step 1: Add Required Permissions to `AndroidManifest.xml`

Before implementing any functionality, you need to declare the necessary permissions for the service to run in the foreground.

#### **1.1 Add Permission for Foreground Service**
Add the following permission to the `AndroidManifest.xml` to allow the app to run foreground services:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"
android:minSdkVersion="34"/>
```

This permission is required to create a service that runs in the foreground and can display persistent notifications.

#### **1.2 Define the Service in the Manifest**
Next, you need to declare the `TimerService` in the `AndroidManifest.xml` file, so that it can be properly started by the app:

```xml
<service
    android:name=".TimerService"
    android:foregroundServiceType="dataSync"
    android:exported="true"
    tools:ignore="ForegroundServicePermission" />
```

This ensures that your service is registered and can be invoked.

---

### Step 2: Create Layout with Button to Start Service

To interact with the service, we need a button in the `MainActivity` layout to start the service.

#### **2.1 Add a Button in `activity_main.xml`**

In the layout file (`activity_main.xml`), add a simple button that the user can press to start the service. Here's an example layout:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <Button
        android:id="@+id/btClick"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Click Me"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

Here, we add a `Button` and assign it an ID (`startServiceButton`). The button will call the `startTimerService` method when clicked.

---

### Step 3: Create the `TimerService`

The heart of the app is the `TimerService`, a foreground service that tracks the elapsed time and displays it in a notification.

#### **3.1 Create the TimerService Class**

In your `TimerService` class, implement the logic to run the timer and update the notification every second.

```kotlin
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
```

---

### Step 4: Add Button Click Function in `MainActivity`

In `MainActivity`, create the method `startTimerService` to start the service when the button is clicked.

#### **4.1 Update `MainActivity.java`**

```kotlin
class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        if(ContextCompat.checkSelfPermission(this@MainActivity,Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.POST_NOTIFICATIONS),1)
        }
        if(ContextCompat.checkSelfPermission(this@MainActivity,Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.FOREGROUND_SERVICE),2)
        }
        var button = findViewById<Button>(R.id.btClick)
        button.setOnClickListener {
            val intent = Intent(this, TimerService::class.java)
            intent.putExtra("time", "demo")
            startForegroundService(intent)
        }

    }
}
```

---

### Step 5: Run and Test the App

1. **Launch the app** on an emulator or a physical device.
2. Grant the runtime permissions when prompted.
2. **Click the "Click Me"  button** to start the foreground service.
3. The notification will appear with the elapsed time and two buttons: "Pause" and "Stop".
4. **Click "Pause"** to stop the timer from running. Click again to resume the timer.
5. **Click "Stop"** to stop the service and remove the notification.

---

### Conclusion

This app demonstrates the use of foreground services in Android to track time and display it in a persistent notification. Users can start, pause, resume, and stop the timer using action buttons in the notification. The service continues to run  until explicitly stopped.

