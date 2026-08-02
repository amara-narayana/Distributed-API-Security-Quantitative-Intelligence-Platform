package com.mesh

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress

class HealthMonitor(
    private val okHttpClient: OkHttpClient,
    private val serverUrl: String
) {

    companion object {
        private const val TAG = "HealthMonitor"
        private const val HEARTBEAT_INTERVAL_MS = 30000L // 30 seconds
    }

    suspend fun startHeartbeat(context: Context) {
        Log.d(TAG, "Starting health monitor heartbeat")
        
        while (true) {
            try {
                sendHeartbeat(context)
                delay(HEARTBEAT_INTERVAL_MS)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending heartbeat", e)
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private suspend fun sendHeartbeat(context: Context) {
        try {
            val url = "$serverUrl/devices/heartbeat"
            
            val deviceInfo = JSONObject().apply {
                put("deviceId", getDeviceId())
                put("status", "ACTIVE")
                put("currentLoad", getCurrentLoad())
                put("timestamp", System.currentTimeMillis())
                put("networkStatus", getNetworkStatus())
            }
            
            val request = Request.Builder()
                .url(url)
                .post(deviceInfo.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                Log.d(TAG, "Heartbeat sent successfully")
            } else {
                Log.w(TAG, "Heartbeat failed with code: ${response.code}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending heartbeat", e)
        }
    }

    private fun getDeviceId(): String {
        // In production, use Android ID or a generated UUID stored in SharedPreferences
        return java.util.UUID.randomUUID().toString()
    }

    private fun getCurrentLoad(): Int {
        // Simulate current load (0-100)
        // In production, measure CPU/memory usage
        return (Math.random() * 30).toInt() // Random load between 0-30%
    }

    private fun getNetworkStatus(): String {
        return "CONNECTED"
    }

    suspend fun checkConnectivity(serverHost: String): Boolean {
        return try {
            val address = InetAddress.getByName(serverHost)
            address.isReachable(5000)
        } catch (e: Exception) {
            Log.e(TAG, "Connectivity check failed", e)
            false
        }
    }
}
