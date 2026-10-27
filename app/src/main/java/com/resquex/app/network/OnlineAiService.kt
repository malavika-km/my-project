package com.resquex.app.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException

/**
 * Validated response returned by the secure backend cloud AI service.
 */
data class CloudAiResponse(
    val answerText: String,
    val provider: String,
    val modelName: String,
    val isVerified: Boolean = false,
    val disclaimer: String = "AI generated guidance. In an emergency, contact local emergency services immediately."
)

/**
 * Client for the cloud AI backend service.
 * Enforces data minimization, fast timeouts for offline fallback,
 * and keeps secret API keys strictly off the mobile device.
 */
class OnlineAiService(
    var backendBaseUrl: String = DEFAULT_BACKEND_URL
) {

    companion object {
        private const val TAG = "OnlineAiService"
        // 10.0.2.2 is the Android emulator's loopback alias for the host workstation (localhost:8000)
        const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8000"
        private const val TIMEOUT_MS = 8000 // 8 second timeout before offline fallback
    }

    /**
     * Sends the sanitized first-aid query to the secure backend.
     * Optionally includes a base64 encoded photo of an injury/wound for vision models.
     * Keeps PII strictly off the wire.
     */
    suspend fun queryEmergencyAssistant(
        query: String,
        imageBase64: String? = null
    ): Result<CloudAiResponse> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val endpoint = "$backendBaseUrl/api/v1/chat"
            Log.d(TAG, "Submitting query to cloud backend: $endpoint (hasImage: ${imageBase64 != null})")

            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "POST"
                doOutput = true
                doInput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            // Construct sanitized payload (data minimization)
            val requestBody = JSONObject().apply {
                put("query", query.trim())
                if (!imageBase64.isNullOrBlank()) {
                    put("imageBase64", imageBase64)
                }
                put("timestamp", System.currentTimeMillis())
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val statusCode = connection.responseCode
            if (statusCode in 200..299) {
                val responseString = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { reader ->
                    reader.readText()
                }

                val jsonResponse = JSONObject(responseString)
                val status = jsonResponse.optString("status", "error")
                if (status != "success") {
                    val msg = jsonResponse.optString("message", "Backend reported an error")
                    return@withContext Result.failure(Exception(msg))
                }

                val dataObj = jsonResponse.getJSONObject("data")
                val answer = dataObj.getString("answer")
                val provider = dataObj.optString("provider", "Cloud AI")
                val modelName = dataObj.optString("model", "qwen-cloud")
                val disclaimer = dataObj.optString("disclaimer", "Emergency guidance. Verify with medical professionals.")

                val result = CloudAiResponse(
                    answerText = answer,
                    provider = provider,
                    modelName = modelName,
                    disclaimer = disclaimer
                )
                Result.success(result)
            } else {
                val errorStream = connection.errorStream
                val errorBody = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
                } else "HTTP $statusCode"

                val parsedMessage = try {
                    JSONObject(errorBody).optString("message", errorBody)
                } catch (_: Exception) {
                    errorBody
                }

                Log.w(TAG, "Backend returned error code $statusCode: $parsedMessage")
                Result.failure(Exception("Cloud backend error ($statusCode): $parsedMessage"))
            }

        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "Cloud request timed out after ${TIMEOUT_MS}ms. Triggering offline fallback.")
            Result.failure(Exception("Cloud service timed out. Switching to offline emergency guidance."))
        } catch (e: java.net.ConnectException) {
            Log.w(TAG, "Unable to connect to cloud backend: ${e.message}")
            Result.failure(Exception("Cloud service is unreachable. Using offline emergency mode."))
        } catch (e: Exception) {
            Log.e(TAG, "Network exception communicating with cloud backend", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
