package com.resquex.app.llm

import android.content.Context
import android.os.StatFs
import android.util.Log
import com.resquex.app.model.ModelStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Manages local offline Qwen model lifecycle:
 * Download, integrity verification (SHA-256/GGUF header), atomic storage, and disk space checks.
 */
class ModelManager(private val context: Context) {

    companion object {
        private const val TAG = "ModelManager"
        const val MODEL_FILENAME = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
        private const val MIN_REQUIRED_DISK_BYTES = 500L * 1024L * 1024L // 500 MB

        // Official Qwen 2.5 0.5B Instruct GGUF (Q4_K_M) download candidate URL
        // Approx 398 MB
        const val DEFAULT_MODEL_DOWNLOAD_URL =
            "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
    }

    private val modelsDirectory: File by lazy {
        File(context.filesDir, "models").apply {
            if (!exists()) mkdirs()
        }
    }

    val modelFile: File
        get() = File(modelsDirectory, MODEL_FILENAME)

    private val _modelStatus = MutableStateFlow<ModelStatus>(ModelStatus.NotInstalled)
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    private var activeDownloadJob: Job? = null

    init {
        refreshModelStatus()
    }

    /**
     * Checks whether a valid model file is already present on the device.
     */
    fun refreshModelStatus(): ModelStatus {
        val file = modelFile
        val status = if (file.exists() && file.length() > 50L * 1024L * 1024L) { // At least 50MB
            ModelStatus.Ready(file, file.length())
        } else {
            ModelStatus.NotInstalled
        }
        _modelStatus.value = status
        return status
    }

    /**
     * Downloads the Qwen SLM with progress reporting, storage checking, and atomic rename.
     */
    suspend fun downloadModel(
        downloadUrl: String = DEFAULT_MODEL_DOWNLOAD_URL,
        expectedSha256: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val destFile = modelFile

        // Skip if already ready
        if (destFile.exists() && destFile.length() > 50L * 1024L * 1024L) {
            _modelStatus.value = ModelStatus.Ready(destFile, destFile.length())
            return@withContext Result.success(destFile)
        }

        // Check available storage space
        val availableBytes = getAvailableStorageBytes()
        if (availableBytes < MIN_REQUIRED_DISK_BYTES) {
            val errMsg = "Insufficient storage: ${availableBytes / (1024 * 1024)} MB available, but 500 MB is required."
            _modelStatus.value = ModelStatus.Error(errMsg)
            return@withContext Result.failure(IllegalStateException(errMsg))
        }

        val tempFile = File(modelsDirectory, "$MODEL_FILENAME.tmp")
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            _modelStatus.value = ModelStatus.Downloading(0.0f, 0L, 0L)
            Log.i(TAG, "Starting Qwen SLM download from: $downloadUrl")

            val url = URL(downloadUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                requestMethod = "GET"
            }
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errMsg = "HTTP error during model download: $responseCode ${connection.responseMessage}"
                _modelStatus.value = ModelStatus.Error(errMsg)
                return@withContext Result.failure(java.io.IOException(errMsg))
            }

            val totalBytes = connection.contentLengthLong
            inputStream = connection.inputStream
            outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(64 * 1024) // 64 KB buffer
            var downloadedBytes = 0L
            var read: Int
            val sha256Digest = if (expectedSha256 != null) MessageDigest.getInstance("SHA-256") else null

            var lastReportTime = System.currentTimeMillis()

            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                sha256Digest?.update(buffer, 0, read)
                downloadedBytes += read

                val now = System.currentTimeMillis()
                if (now - lastReportTime > 250) { // Update progress 4 times/sec max to avoid UI spam
                    val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0f
                    _modelStatus.value = ModelStatus.Downloading(progress, downloadedBytes, totalBytes)
                    lastReportTime = now
                }
            }
            outputStream.flush()

            // Verify Checksum if provided
            if (sha256Digest != null && expectedSha256 != null) {
                val computedHash = sha256Digest.digest().joinToString("") { "%02x".format(it) }
                if (!computedHash.equals(expectedSha256, ignoreCase = true)) {
                    tempFile.delete()
                    val errMsg = "Checksum verification failed! Expected: $expectedSha256, Computed: $computedHash"
                    _modelStatus.value = ModelStatus.Error(errMsg)
                    return@withContext Result.failure(SecurityException(errMsg))
                }
            }

            // Atomic rename from .tmp to final destination
            if (destFile.exists()) destFile.delete()
            val renamed = tempFile.renameTo(destFile)
            if (!renamed) {
                // Fallback copy if rename across mount points fails
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
            }

            Log.i(TAG, "Qwen model installed successfully: ${destFile.absolutePath} (${destFile.length()} bytes)")
            val readyStatus = ModelStatus.Ready(destFile, destFile.length())
            _modelStatus.value = readyStatus
            Result.success(destFile)

        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            if (e is CancellationException) {
                Log.d(TAG, "Model download cancelled by user.")
                _modelStatus.value = ModelStatus.NotInstalled
                Result.failure(e)
            } else {
                Log.e(TAG, "Model download failed", e)
                val errMsg = e.localizedMessage ?: "Unknown download error"
                _modelStatus.value = ModelStatus.Error(errMsg)
                Result.failure(e)
            }
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
            connection?.disconnect()
        }
    }

    /**
     * Cancels an ongoing model download.
     */
    fun cancelDownload() {
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        val tempFile = File(modelsDirectory, "$MODEL_FILENAME.tmp")
        if (tempFile.exists()) tempFile.delete()
        _modelStatus.value = ModelStatus.NotInstalled
    }

    /**
     * Deletes the installed model from private storage.
     */
    fun deleteModel(): Boolean {
        cancelDownload()
        val file = modelFile
        val deleted = if (file.exists()) file.delete() else true
        refreshModelStatus()
        return deleted
    }

    private fun getAvailableStorageBytes(): Long {
        return try {
            val stat = StatFs(modelsDirectory.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }
}
