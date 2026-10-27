package com.resquex.app.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Text-To-Speech engine providing hands-free audio readout for emergency first-aid instructions.
 * Critical during emergencies where responders cannot look at their screen (e.g. performing CPR, applying pressure).
 */
class EmergencyTtsEngine(context: Context) {

    companion object {
        private const val TAG = "EmergencyTtsEngine"
        private const val UTTERANCE_ID = "RESQUEX_EMERGENCY_TTS"
    }

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var isInitialized = false
    private var pendingText: String? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English US if local language voice is not installed
                tts.setLanguage(Locale.US)
            }
            tts.setSpeechRate(0.95f) // Slightly slower for emergency comprehension
            isInitialized = true
            Log.d(TAG, "TextToSpeech successfully initialized")

            pendingText?.let {
                speak(it)
                pendingText = null
            }
        } else {
            Log.e(TAG, "Failed to initialize TextToSpeech (status: $status)")
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                Log.w(TAG, "TTS utterance error: $errorCode")
            }
        })
    }

    /**
     * Speaks the provided text, cleaning markdown/formatting for clear auditory guidance.
     */
    fun speak(text: String) {
        val sanitized = cleanTextForSpeech(text)
        if (!isInitialized) {
            pendingText = sanitized
            return
        }

        stop()
        _isSpeaking.value = true
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID)
        }
        tts.speak(sanitized, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
    }

    /**
     * Stops any currently active speech.
     */
    fun stop() {
        if (isInitialized) {
            tts.stop()
        }
        _isSpeaking.value = false
    }

    /**
     * Releases native TTS resources.
     */
    fun shutdown() {
        stop()
        tts.shutdown()
    }

    /**
     * Strips Markdown formatting, hashes, brackets, and URLs to ensure natural vocalization.
     */
    private fun cleanTextForSpeech(input: String): String {
        return input
            .replace(Regex("""(?m)^[#*\-]+\s*"""), "") // Remove markdown headers and list bullets
            .replace(Regex("""\*{1,2}([^*]+)\*{1,2}"""), "$1") // Bold/italic text
            .replace(Regex("""\[([^\]]+)\]\([^)]+\)"""), "$1") // Markdown links
            .replace(Regex("""[#>`~]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
