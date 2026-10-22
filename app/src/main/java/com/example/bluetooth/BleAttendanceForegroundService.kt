package com.example.bluetooth

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that runs in the background to monitor and detect the
 * classroom ESP32 beacon ('BLE_Attendance') without being killed by Android battery optimization.
 */
class BleAttendanceForegroundService : Service() {

    companion object {
        private const val TAG = "BleAttendanceService"
        const val CHANNEL_ID = "ble_attendance_service_channel"
        const val NOTIFICATION_ID = 1001
        const val EVENT_NOTIFICATION_ID = 1002

        const val ACTION_START = "com.example.bluetooth.action.START_SERVICE"
        const val ACTION_STOP = "com.example.bluetooth.action.STOP_SERVICE"

        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, BleAttendanceForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BleAttendanceForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var wakeLock: PowerManager.WakeLock? = null
    private var bleScanner: BleScanner? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BleAttendanceForegroundService created")
        createNotificationChannel()
        acquireWakeLock()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createForegroundNotification("Classroom BLE monitor is running")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        isRunning = true
        startPeriodicScanning()

        return START_STICKY
    }

    private fun startPeriodicScanning() {
        bleScanner = BleScanner(
            context = this,
            onDeviceFound = { device, name, rssi ->
                Log.d(TAG, "Background detected ESP32: $name ($rssi dBm)")
                showDetectionNotification(name, rssi)
            },
            onScanFailed = { code, msg ->
                Log.w(TAG, "Background scan failed: $msg (code $code)")
            },
            onScanTimeout = {
                Log.d(TAG, "Background scan cycle timeout")
            }
        )

        serviceScope.launch {
            while (isActive && isRunning) {
                if (BluetoothPermissionHelper.hasPermissions(applicationContext) &&
                    BluetoothPermissionHelper.isBluetoothEnabled(applicationContext)
                ) {
                    try {
                        bleScanner?.startScan()
                        delay(12_000) // scan for 12 seconds
                        bleScanner?.stopScan()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during periodic background scan", e)
                    }
                }
                delay(30_000) // pause 30 seconds before next scan cycle
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart Attendance Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors classroom ESP32 beacon for automated attendance"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, BleAttendanceForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart Attendance Active")
            .setContentText(statusText)
            .setSubText("Background Monitoring")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showDetectionNotification(deviceName: String, rssi: Int) {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            2,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Classroom Device Detected")
            .setContentText("$deviceName is nearby ($rssi dBm). Tap to mark attendance.")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(EVENT_NOTIFICATION_ID, notification)
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SmartAttendance:BleServiceWakeLock"
            )
            wakeLock?.acquire()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire wake lock: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            bleScanner?.stopScan()
            serviceJob.cancel()
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up BleAttendanceForegroundService", e)
        }
        Log.d(TAG, "BleAttendanceForegroundService destroyed")
    }
}
