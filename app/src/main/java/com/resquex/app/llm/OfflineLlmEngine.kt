package com.resquex.app.llm

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Clean abstraction for local Small Language Model (SLM) inference on Android.
 * Keeps inference decoupled from UI and allows alternative runtimes (llama.cpp, ONNX, MediaPipe)
 * to be swapped without affecting app logic.
 */
interface OfflineLlmEngine {
    /**
     * Whether a model is currently loaded in memory and ready for generation.
     */
    val isModelLoaded: Boolean

    /**
     * Loads the model into memory.
     * Reuses already loaded model instance unless unloaded.
     */
    suspend fun loadModel(modelFile: File): Result<Unit>

    /**
     * Streams generated tokens asynchronously off the UI thread.
     */
    fun generate(prompt: String, maxTokens: Int = 300): Flow<String>

    /**
     * Signals cancellation of ongoing generation.
     */
    fun cancelGeneration()

    /**
     * Frees native model and context memory.
     */
    fun unloadModel()
}

/**
 * Formats prompts according to the official Qwen ChatML specification.
 * Qwen2.5 / Qwen3 instruction-tuned models require this exact template.
 */
object QwenChatTemplate {
    const val IM_START = "<|im_start|>"
    const val IM_END = "<|im_end|>"
    const val END_OF_TEXT = "<|endoftext|>"

    fun format(systemPrompt: String, userMessage: String): String {
        val sb = StringBuilder()
        if (systemPrompt.isNotBlank()) {
            sb.append(IM_START).append("system\n")
                .append(systemPrompt.trim())
                .append("\n").append(IM_END).append("\n")
        }
        sb.append(IM_START).append("user\n")
            .append(userMessage.trim())
            .append("\n").append(IM_END).append("\n")
        sb.append(IM_START).append("assistant\n")
        return sb.toString()
    }
}
