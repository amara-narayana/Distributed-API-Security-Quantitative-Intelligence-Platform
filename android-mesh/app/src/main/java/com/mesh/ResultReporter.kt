package com.mesh

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class ResultReporter(
    private val okHttpClient: OkHttpClient,
    private val serverUrl: String
) {

    companion object {
        private const val TAG = "ResultReporter"
    }

    suspend fun reportResult(jobId: String, result: JSONObject) {
        Log.i(TAG, "Reporting result for job $jobId")
        
        try {
            val url = "$serverUrl/jobs/submit-result"
            
            val payload = JSONObject().apply {
                put("jobId", jobId)
                put("result", result)
                put("timestamp", System.currentTimeMillis())
            }
            
            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                Log.i(TAG, "Result reported successfully for job $jobId")
            } else {
                Log.e(TAG, "Failed to report result: ${response.code}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception reporting result", e)
        }
    }
}
