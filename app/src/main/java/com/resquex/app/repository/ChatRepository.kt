package com.resquex.app.repository

import android.content.Context
import android.util.Log
import com.resquex.app.llm.LlamaCppOfflineQwenEngine
import com.resquex.app.llm.ModelManager
import com.resquex.app.llm.OfflineLlmEngine
import com.resquex.app.llm.QwenChatTemplate
import com.resquex.app.model.ChatMessage
import com.resquex.app.model.ChatMode
import com.resquex.app.model.MessageSender
import com.resquex.app.model.MessageSource
import com.resquex.app.model.MessageStatus
import com.resquex.app.model.ModelStatus
import com.resquex.app.network.OnlineAiService
import com.resquex.app.rag.FirstAidRetriever
import com.resquex.app.safety.EmergencyRouter
import com.resquex.app.safety.SafetyCheckResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Central orchestrator and repository for the RESQUE-X emergency chat experience.
 * Orchestrates:
 * 1. Deterministic Emergency Safety Router
 * 2. Cloud AI Service
 * 3. Local Qwen SLM with RAG
 * 4. Zero-AI Verified First-Aid Knowledge Base Fallback
 */
class ChatRepository(
    private val context: Context,
    val modelManager: ModelManager = ModelManager(context),
    val offlineEngine: OfflineLlmEngine = LlamaCppOfflineQwenEngine(),
    val onlineAiService: OnlineAiService = OnlineAiService()
) {

    companion object {
        private const val TAG = "ChatRepository"
        val INITIAL_GREETING = ChatMessage(
            text = "Welcome to RESQUE-X Emergency Assistant.\n\nAsk any first-aid question, or tap an emergency card for immediate protocol.\n\n⚠️ IN AN IMMEDIATE LIFE-THREATENING EMERGENCY, CALL 112 / 911 FIRST.",
            sender = MessageSender.SYSTEM,
            source = MessageSource.SYSTEM_NOTICE
        )
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(INITIAL_GREETING))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _chatMode = MutableStateFlow(ChatMode.AUTO)
    val chatMode: StateFlow<ChatMode> = _chatMode.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private var activeJob: Job? = null

    fun setChatMode(mode: ChatMode) {
        _chatMode.value = mode
        Log.i(TAG, "Chat mode switched to: $mode")
    }

    /**
     * Clear all conversation messages and restore initial greeting.
     */
    fun clearConversation() {
        cancelGeneration()
        _messages.value = listOf(INITIAL_GREETING)
    }

    /**
     * Cancels any active generation job.
     */
    fun cancelGeneration() {
        activeJob?.cancel()
        activeJob = null
        offlineEngine.cancelGeneration()
        _isGenerating.value = false

        // Mark any in-progress message as CANCELED
        val currentList = _messages.value.toMutableList()
        val lastIdx = currentList.indexOfLast { it.sender == MessageSender.ASSISTANT && it.status == MessageStatus.SENDING }
        if (lastIdx != -1) {
            val msg = currentList[lastIdx]
            currentList[lastIdx] = msg.copy(
                status = MessageStatus.CANCELED,
                text = if (msg.text.isBlank()) "[Generation cancelled by user]" else msg.text + "\n\n[Generation cancelled]"
            )
            _messages.value = currentList
        }
    }

    /**
     * Primary entry point for submitting a user question.
     * Supports multimodal input (optional imageBase64 and imageUri).
     */
    fun sendMessage(
        userText: String,
        imageBase64: String? = null,
        imageUri: String? = null
    ) {
        val rawQuery = userText.trim()
        val query = if (rawQuery.isEmpty() && !imageBase64.isNullOrBlank()) {
            "Please analyze this injury image and provide emergency first-aid instructions."
        } else {
            rawQuery
        }

        if (query.isEmpty() || _isGenerating.value) return

        // 1. Append user message
        val userMessage = ChatMessage(
            text = query,
            sender = MessageSender.USER,
            status = MessageStatus.SUCCESS,
            imageBase64 = imageBase64,
            imageUri = imageUri
        )
        val assistantMessageId = java.util.UUID.randomUUID().toString()
        val placeholderAssistant = ChatMessage(
            id = assistantMessageId,
            text = "",
            sender = MessageSender.ASSISTANT,
            status = MessageStatus.SENDING,
            source = MessageSource.SYSTEM_NOTICE
        )

        _messages.value = _messages.value + userMessage + placeholderAssistant
        _isGenerating.value = true

        activeJob = scope.launch {
            try {
                // 2. Safety Layer: Deterministic Emergency Routing
                val safetyResult = EmergencyRouter.evaluate(query)
                when (safetyResult) {
                    is SafetyCheckResult.CriticalEmergency -> {
                        updateMessage(assistantMessageId) {
                            it.copy(
                                text = "CRITICAL EMERGENCY DETECTED:\n${safetyResult.card.title}",
                                source = MessageSource.VERIFIED_EMERGENCY,
                                emergencyCard = safetyResult.card,
                                citations = listOf(safetyResult.card.sourceCitation),
                                status = MessageStatus.SUCCESS
                            )
                        }
                        _isGenerating.value = false
                        return@launch
                    }
                    is SafetyCheckResult.BlockedUnsafe -> {
                        updateMessage(assistantMessageId) {
                            it.copy(
                                text = "⚠️ Notice: ${safetyResult.reason}\n\n${safetyResult.safeAdvisory}",
                                source = MessageSource.SYSTEM_NOTICE,
                                status = MessageStatus.ERROR,
                                errorMessage = safetyResult.reason
                            )
                        }
                        _isGenerating.value = false
                        return@launch
                    }
                    is SafetyCheckResult.ProceedToAi -> {
                        // Safe to proceed to AI generation / RAG
                    }
                }

                // 3. Mode Evaluation: Should we attempt Cloud AI?
                val currentMode = _chatMode.value
                var cloudSuccess = false

                if (currentMode != ChatMode.OFFLINE_ONLY) {
                    val cloudResult = onlineAiService.queryEmergencyAssistant(query, imageBase64)
                    if (cloudResult.isSuccess) {
                        val response = cloudResult.getOrThrow()
                        updateMessage(assistantMessageId) {
                            it.copy(
                                text = "${response.answerText}\n\nDisclaimer: ${response.disclaimer}",
                                source = MessageSource.CLOUD_AI,
                                citations = listOf("${response.provider} (${response.modelName})"),
                                status = MessageStatus.SUCCESS
                            )
                        }
                        cloudSuccess = true
                        _isGenerating.value = false
                        return@launch
                    } else {
                        Log.w(TAG, "Cloud AI query failed: ${cloudResult.exceptionOrNull()?.message}. Attempting offline fallback.")
                        if (currentMode == ChatMode.ONLINE_ONLY) {
                            // User specifically requested online only
                            val err = cloudResult.exceptionOrNull()?.message ?: "Cloud AI connection failed."
                            updateMessage(assistantMessageId) {
                                it.copy(
                                    text = "Online AI is currently unavailable.\n\nError: $err\n\nPlease switch to Auto or Offline mode to use on-device emergency guidance.",
                                    source = MessageSource.CLOUD_AI,
                                    status = MessageStatus.ERROR,
                                    errorMessage = err
                                )
                            }
                            _isGenerating.value = false
                            return@launch
                        }
                    }
                }

                // 4. Offline Fallback: Local Knowledge Base Retrieval
                val retrievedDocs = FirstAidRetriever.search(query, topK = 2)
                val modelStatus = modelManager.refreshModelStatus()

                if (modelStatus is ModelStatus.Ready) {
                    // Local Qwen SLM is installed and ready
                    val modelFile = modelStatus.modelFile
                    val loadResult = offlineEngine.loadModel(modelFile)
                    if (loadResult.isSuccess) {
                        val groundedPrompt = FirstAidRetriever.buildGroundedPrompt(query, retrievedDocs)
                        val chatMlPrompt = QwenChatTemplate.format(
                            systemPrompt = "You are RESQUE-X emergency assistant. Follow verified first-aid instructions strictly.",
                            userMessage = groundedPrompt
                        )

                        val accumulatedText = StringBuilder()
                        val citations = retrievedDocs.map { "${it.document.sourceName} (${it.document.sourceReference})" }

                        offlineEngine.generate(chatMlPrompt).collect { token ->
                            accumulatedText.append(token)
                            updateMessage(assistantMessageId) {
                                it.copy(
                                    text = accumulatedText.toString(),
                                    source = MessageSource.OFFLINE_QWEN,
                                    citations = citations,
                                    status = MessageStatus.SENDING
                                )
                            }
                        }

                        // Complete generation
                        updateMessage(assistantMessageId) {
                            it.copy(
                                text = accumulatedText.toString(),
                                source = MessageSource.OFFLINE_QWEN,
                                citations = citations,
                                status = MessageStatus.SUCCESS
                            )
                        }
                    } else {
                        // Model failed to load in memory -> fall back to direct knowledge base
                        Log.e(TAG, "Failed to load local Qwen model", loadResult.exceptionOrNull())
                        val fallbackText = FirstAidRetriever.formatDirectFallbackResponse(query, retrievedDocs)
                        updateMessage(assistantMessageId) {
                            it.copy(
                                text = "⚠️ Local Qwen model failed to load into memory.\n\n$fallbackText",
                                source = MessageSource.LOCAL_KNOWLEDGE_BASE,
                                citations = retrievedDocs.map { d -> d.document.sourceName },
                                status = MessageStatus.SUCCESS
                            )
                        }
                    }
                } else {
                    // Model is not downloaded -> direct local verified knowledge base fallback
                    val fallbackText = FirstAidRetriever.formatDirectFallbackResponse(query, retrievedDocs)
                    updateMessage(assistantMessageId) {
                        it.copy(
                            text = fallbackText,
                            source = MessageSource.LOCAL_KNOWLEDGE_BASE,
                            citations = retrievedDocs.map { d -> "${d.document.sourceName} (${d.document.sourceReference})" },
                            status = MessageStatus.SUCCESS
                        )
                    }
                }

            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.d(TAG, "Message processing cancelled.")
                } else {
                    Log.e(TAG, "Unhandled error in sendMessage", e)
                    updateMessage(assistantMessageId) {
                        it.copy(
                            text = "An unexpected error occurred while processing your request: ${e.localizedMessage ?: "Unknown"}",
                            status = MessageStatus.ERROR,
                            errorMessage = e.message
                        )
                    }
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun updateMessage(id: String, transform: (ChatMessage) -> ChatMessage) {
        val current = _messages.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = transform(current[index])
            _messages.value = current
        }
    }
}
