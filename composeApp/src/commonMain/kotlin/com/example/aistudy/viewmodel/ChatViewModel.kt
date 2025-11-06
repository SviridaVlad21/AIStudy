package com.example.aistudy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aistudy.agent.AiAgent
import com.example.aistudy.config.ApiConfig
import com.example.aistudy.config.ApiKeyProvider
import com.example.aistudy.model.Message
import com.example.aistudy.model.ApiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State для чата
 */
data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = ""
)

/**
 * ViewModel для управления чатом с DeepSeek AI
 * API ключ загружается из безопасного хранилища (local.properties)
 * Поддерживает multi-round conversation (запоминание истории)
 */
class ChatViewModel : ViewModel() {
    private val aiAgent = AiAgent()

    // История сообщений для API (в формате ApiMessage)
    private val messageHistory = mutableListOf<ApiMessage>()

    init {
        // Инициализируем API ключ при создании ViewModel
        val apiKey = ApiKeyProvider.getApiKey()
        if (apiKey.isNotEmpty()) {
            ApiConfig.initialize(apiKey)
        }
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /**
     * Обновление текста ввода
     */
    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * Отправка сообщения в AI с учетом истории
     */
    fun sendMessage() {
        val inputText = _uiState.value.inputText.trim()
        if (inputText.isEmpty()) return

        // Добавляем сообщение пользователя в UI
        val userMessage = Message(text = inputText, isFromUser = true)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true,
                error = null
            )
        }

        // Добавляем сообщение пользователя в историю для API
        messageHistory.add(ApiMessage(role = "user", content = inputText))

        // Отправляем запрос к AI с историей
        viewModelScope.launch {
            val result = aiAgent.askWithHistorySafe(messageHistory.toList())

            result.onSuccess { structuredResponse ->
                // Добавляем ответ AI в историю
                val fullResponse = "{\"agentMessage\":\"${structuredResponse.agentMessage}\"}"
                messageHistory.add(ApiMessage(role = "assistant", content = fullResponse))

                // Создаем сообщение с структурированными данными для UI
                val aiMessage = Message(
                    text = structuredResponse.agentMessage,
                    isFromUser = false,
                    structuredData = structuredResponse
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + aiMessage,
                        isLoading = false
                    )
                }
            }.onFailure { error ->
                // Удаляем последнее сообщение пользователя из истории при ошибке
                if (messageHistory.isNotEmpty()) {
                    messageHistory.removeAt(messageHistory.size - 1)
                }

                _uiState.update {
                    it.copy(
                        error = error.message ?: "Неизвестная ошибка",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Очистка истории сообщений
     */
    fun clearHistory() {
        messageHistory.clear()
        _uiState.update { it.copy(messages = emptyList()) }
    }

    /**
     * Очистка сообщения об ошибке
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        aiAgent.close()
    }
}