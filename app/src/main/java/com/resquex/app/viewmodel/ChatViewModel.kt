package com.resquex.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.resquex.app.audio.EmergencyTtsEngine
import com.resquex.app.model.ChatMessage
import com.resquex.app.model.ChatMode
import com.resquex.app.model.MessageSender
import com.resquex.app.model.ModelStatus
import com.resquex.app.repository.ChatRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing the emergency chatbot screen state, user interaction,
 * generation cancellation, TTS audio guidance, and model lifecycle events.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    val repository: ChatRepository = ChatRepository(application.applicationContext)
    val ttsEngine: EmergencyTtsEngine = EmergencyTtsEngine(application.applicationContext)

    val messages: StateFlow<List<ChatMessage>> = repository.messages
    val chatMode: StateFlow<ChatMode> = repository.chatMode
    val isGenerating: StateFlow<Boolean> = repository.isGenerating
    val modelStatus: StateFlow<ModelStatus> = repository.modelManager.modelStatus
    val isSpeaking: StateFlow<Boolean> = ttsEngine.isSpeaking

    fun sendMessage(
        query: String,
        imageBase64: String? = null,
        imageUri: String? = null
    ) {
        repository.sendMessage(query, imageBase64, imageUri)
    }

    fun speak(text: String) {
        ttsEngine.speak(text)
    }

    fun stopSpeaking() {
        ttsEngine.stop()
    }

    fun setChatMode(mode: ChatMode) {
        repository.setChatMode(mode)
    }

    fun cancelGeneration() {
        repository.cancelGeneration()
    }

    fun clearConversation() {
        stopSpeaking()
        repository.clearConversation()
    }

    fun retryLastMessage() {
        val lastUserMessage = messages.value.lastOrNull { it.sender == MessageSender.USER }
        if (lastUserMessage != null) {
            sendMessage(lastUserMessage.text, lastUserMessage.imageBase64, lastUserMessage.imageUri)
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsEngine.shutdown()
    }

    fun downloadModel() {
        viewModelScope.launch {
            repository.modelManager.downloadModel()
        }
    }

    fun cancelDownload() {
        repository.modelManager.cancelDownload()
    }

    fun deleteModel() {
        repository.modelManager.deleteModel()
    }

    fun setBackendUrl(url: String) {
        repository.onlineAiService.backendBaseUrl = url
    }
}
