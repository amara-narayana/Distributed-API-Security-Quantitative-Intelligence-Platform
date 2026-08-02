package com.mesh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MeshNodeService : Service() {

    companion object {
        private const val TAG = "MeshNodeService"
        private const val CHANNEL_ID = "MeshNodeChannel"
        private const val NOTIFICATION_ID = 1001
        private const val SERVER_URL = "http://10.0.2.2:8080/api" // Use 10.0.2.2 for emulator, replace with actual IP for device
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var jobPoller: JobPoller
    private lateinit var healthMonitor: HealthMonitor
    private lateinit var resultReporter: ResultReporter

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MeshNodeService created")

        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        jobPoller = JobPoller(okHttpClient, SERVER_URL)
        healthMonitor = HealthMonitor(okHttpClient, SERVER_URL)
        resultReporter = ResultReporter(okHttpClient, SERVER_URL)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "MeshNodeService started")

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Start health monitoring (heartbeat every 30 seconds)
        serviceScope.launch {
            healthMonitor.startHeartbeat(this@MeshNodeService)
        }

        // Start job polling
        serviceScope.launch {
            jobPoller.pollForJobs(this@MeshNodeService)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MeshNodeService destroyed")
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mesh Node Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background service for mesh network coordination"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mesh Node Active")
            .setContentText("Running security tests and reporting results")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }
}
