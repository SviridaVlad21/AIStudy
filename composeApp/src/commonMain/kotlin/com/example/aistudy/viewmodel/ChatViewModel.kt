package com.example.aistudy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aistudy.agent.AiAgent
import com.example.aistudy.config.ApiConfig
import com.example.aistudy.config.ApiKeyProvider
import com.example.aistudy.model.Message
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
 */
class ChatViewModel : ViewModel() {
    private val aiAgent = AiAgent()

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
     * Отправка сообщения в AI
     */
    fun sendMessage() {
        val inputText = _uiState.value.inputText.trim()
        if (inputText.isEmpty()) return

        // Добавляем сообщение пользователя
        val userMessage = Message(text = inputText, isFromUser = true)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true,
                error = null
            )
        }

        // Отправляем запрос к AI и получаем структурированный ответ
        viewModelScope.launch {
            val result = aiAgent.askStructuredSafe(inputText)

            result.onSuccess { structuredResponse ->
                // Создаем сообщение с структурированными данными
                val aiMessage = Message(
                    text = structuredResponse.summary,  // Используем краткий ответ как основной текст
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