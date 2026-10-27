package com.resquex.app.model

import java.io.File

/**
 * Indicates who authored a message in the chat session.
 */
enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * Represents the provenance of the assistant response.
 * This ensures strict transparency between verified medical protocols,
 * on-device Qwen inference, local fallback, and cloud models.
 */
enum class MessageSource {
    VERIFIED_EMERGENCY,
    OFFLINE_QWEN,
    LOCAL_KNOWLEDGE_BASE,
    CLOUD_AI,
    SYSTEM_NOTICE
}

/**
 * Message transmission/generation lifecycle state.
 */
enum class MessageStatus {
    SENDING,
    SUCCESS,
    ERROR,
    CANCELED
}

/**
 * User-selected mode preference for routing.
 */
enum class ChatMode {
    AUTO,
    ONLINE_ONLY,
    OFFLINE_ONLY
}

/**
 * A single conversation item.
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis(),
    val source: MessageSource = MessageSource.SYSTEM_NOTICE,
    val citations: List<String> = emptyList(),
    val emergencyCard: EmergencyActionCard? = null,
    val status: MessageStatus = MessageStatus.SUCCESS,
    val errorMessage: String? = null,
    val imageBase64: String? = null,
    val imageUri: String? = null
)

/**
 * Deterministic action card for life-threatening emergencies.
 * Displayed immediately without waiting for model inference.
 */
data class EmergencyActionCard(
    val category: String,
    val title: String,
    val immediateSteps: List<String>,
    val warnings: List<String>,
    val emergencyNumber: String = "112",
    val sourceCitation: String
)

/**
 * Reviewed first-aid knowledge entity for offline RAG and direct fallback.
 */
data class FirstAidDocument(
    val id: String,
    val category: String,
    val title: String,
    val keywords: List<String>,
    val steps: List<String>,
    val warnings: List<String>,
    val whenToCallAmbulance: List<String>,
    val sourceName: String,
    val sourceReference: String,
    val reviewDate: String,
    val version: Int = 1
) {
    /**
     * Compact formatted context string passed into Qwen SLM system prompt.
     */
    fun toGroundedContextString(): String {
        val stepsJoined = steps.mapIndexed { idx, s -> "${idx + 1}. $s" }.joinToString("\n")
        val warningsJoined = if (warnings.isNotEmpty()) "\nContraindications/Warnings:\n" + warnings.joinToString("\n") { "- $it" } else ""
        val callWhenJoined = if (whenToCallAmbulance.isNotEmpty()) "\nEmergency Triggers:\n" + whenToCallAmbulance.joinToString("\n") { "- $it" } else ""
        return """
            [Topic: $title]
            Category: $category
            Verified Steps:
            $stepsJoined$warningsJoined$callWhenJoined
            Source: $sourceName ($sourceReference)
        """.trimIndent()
    }
}

/**
 * State of the offline Qwen Small Language Model installation.
 */
sealed class ModelStatus {
    object NotInstalled : ModelStatus()
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : ModelStatus()
    data class Ready(
        val modelFile: File,
        val sizeBytes: Long
    ) : ModelStatus()
    data class Error(
        val message: String
    ) : ModelStatus()
}
