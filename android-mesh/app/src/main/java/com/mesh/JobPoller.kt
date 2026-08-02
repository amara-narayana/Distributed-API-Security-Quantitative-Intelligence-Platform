package com.mesh

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.UUID

class JobPoller(
    private val okHttpClient: OkHttpClient,
    private val serverUrl: String
) {

    companion object {
        private const val TAG = "JobPoller"
        private const val POLL_INTERVAL_MS = 5000L // 5 seconds
    }

    suspend fun pollForJobs(context: Context) {
        Log.d(TAG, "Starting job polling loop")
        
        while (true) {
            try {
                val job = fetchAvailableJob()
                
                if (job != null) {
                    Log.i(TAG, "Received job: ${job.optString("jobId")}")
                    executeJob(context, job)
                } else {
                    Log.d(TAG, "No jobs available, waiting...")
                }
                
                delay(POLL_INTERVAL_MS)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during job polling", e)
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun fetchAvailableJob(): JSONObject? {
        return try {
            val url = "$serverUrl/jobs/assign"
            
            val request = Request.Builder()
                .url(url)
                .post(JSONObject().put("deviceId", getDeviceId()).toRequestBody())
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful && response.body != null) {
                val responseBody = response.body!!.string()
                if (responseBody.isNotEmpty() && responseBody != "null") {
                    JSONObject(responseBody)
                } else {
                    null
                }
            } else {
                Log.w(TAG, "Failed to fetch job: ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching job", e)
            null
        }
    }

    private suspend fun executeJob(context: Context, job: JSONObject) {
        val jobId = job.optString("jobId")
        val jobType = job.optString("type")
        val targetUrl = job.optString("targetUrl")
        
        Log.i(TAG, "Executing job $jobId of type $jobType for $targetUrl")
        
        try {
            // Simulate job execution (in production, this would run actual security tests)
            val result = when (jobType) {
                "IDOR_TEST" -> executeIdorTest(targetUrl, job)
                "BOLA_TEST" -> executeBolaTest(targetUrl, job)
                "GRAPHQL_TEST" -> executeGraphqlTest(targetUrl, job)
                else -> executeGenericTest(targetUrl, job)
            }
            
            // Report result
            ResultReporter(okHttpClient, serverUrl).reportResult(jobId, result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Job execution failed", e)
            val errorResult = JSONObject().apply {
                put("success", false)
                put("error", e.message)
                put("timestamp", System.currentTimeMillis())
            }
            ResultReporter(okHttpClient, serverUrl).reportResult(jobId, errorResult)
        }
    }

    private suspend fun executeIdorTest(targetUrl: String, job: JSONObject): JSONObject {
        Log.d(TAG, "Executing IDOR test on $targetUrl")
        // In production, this would use the security testing engine
        return JSONObject().apply {
            put("success", true)
            put("testType", "IDOR")
            put("vulnerabilitiesFound", 0)
            put("details", "Test completed")
            put("timestamp", System.currentTimeMillis())
        }
    }

    private suspend fun executeBolaTest(targetUrl: String, job: JSONObject): JSONObject {
        Log.d(TAG, "Executing BOLA test on $targetUrl")
        return JSONObject().apply {
            put("success", true)
            put("testType", "BOLA")
            put("vulnerabilitiesFound", 0)
            put("details", "Test completed")
            put("timestamp", System.currentTimeMillis())
        }
    }

    private suspend fun executeGraphqlTest(targetUrl: String, job: JSONObject): JSONObject {
        Log.d(TAG, "Executing GraphQL test on $targetUrl")
        return JSONObject().apply {
            put("success", true)
            put("testType", "GRAPHQL")
            put("vulnerabilitiesFound", 0)
            put("details", "Test completed")
            put("timestamp", System.currentTimeMillis())
        }
    }

    private suspend fun executeGenericTest(targetUrl: String, job: JSONObject): JSONObject {
        Log.d(TAG, "Executing generic test on $targetUrl")
        return JSONObject().apply {
            put("success", true)
            put("testType", "GENERIC")
            put("vulnerabilitiesFound", 0)
            put("details", "Test completed")
            put("timestamp", System.currentTimeMillis())
        }
    }

    private fun getDeviceId(): String {
        // Generate a unique device ID
        return UUID.randomUUID().toString()
    }
}
