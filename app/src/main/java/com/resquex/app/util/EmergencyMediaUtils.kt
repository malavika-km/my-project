package com.resquex.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

/**
 * Utility functions for handling multimodal emergency media (image compression,
 * base64 encoding/decoding, and camera thumbnail generation).
 */
object EmergencyMediaUtils {

    private const val TAG = "EmergencyMediaUtils"
    private const val MAX_IMAGE_DIMENSION = 1024 // Downscale large camera photos to save memory and network bandwidth
    private const val JPEG_QUALITY = 80

    /**
     * Converts a Bitmap into a compact Base64 JPEG string suitable for vision model submission.
     */
    fun bitmapToBase64(bitmap: Bitmap, maxDimension: Int = MAX_IMAGE_DIMENSION, quality: Int = JPEG_QUALITY): String {
        val scaled = scaleBitmapDown(bitmap, maxDimension)
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Decodes a Base64 string back into an Android Bitmap for UI display.
     */
    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding base64 to bitmap", e)
            null
        }
    }

    /**
     * Reads a Uri from device storage / photo picker and decodes it safely to a Bitmap.
     */
    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            val original = BitmapFactory.decodeStream(inputStream)
            original?.let { scaleBitmapDown(it, MAX_IMAGE_DIMENSION) }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading image Uri: $uri", e)
            null
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Rescales a bitmap down proportionally if either width or height exceeds maxDimension.
     */
    fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        val maxOriginal = max(originalWidth, originalHeight)

        if (maxOriginal <= maxDimension) {
            return bitmap
        }

        val scale = maxDimension.toFloat() / maxOriginal.toFloat()
        val matrix = Matrix().apply { postScale(scale, scale) }
        return Bitmap.createBitmap(bitmap, 0, 0, originalWidth, originalHeight, matrix, true)
    }
}
