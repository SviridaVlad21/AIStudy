package com.example.aistudy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aistudy.agent.AiAgent
import com.example.aistudy.config.ApiConfig
import com.example.aistudy.config.ApiKeyProvider
import com.example.aistudy.model.Message
import com.example.aistudy.model.ApiMessage
import com.example.aistudy.model.ExpertType
import com.example.aistudy.model.AiStructuredResponse
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
     * Отправка сообщения в AI с получением 3 ответов с разными температурами
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

        // Температуры для получения разных ответов
        val temperatures = listOf(0.0, 0.7, 1.0)

        // Отправляем запросы к AI с разными температурами
        viewModelScope.launch {
            val successfulResponses = mutableListOf<Pair<Double, AiStructuredResponse>>()

            for (temperature in temperatures) {
                val result = aiAgent.askWithTemperatureSafe(messageHistory.toList(), temperature)

                result.onSuccess { structuredResponse ->
                    successfulResponses.add(temperature to structuredResponse)

                    // Добавляем ответ AI в UI
                    val aiMessage = Message(
                        text = structuredResponse.agentMessage,
                        isFromUser = false,
                        structuredData = structuredResponse,
                        temperature = temperature
                    )
                    _uiState.update {
                        it.copy(messages = it.messages + aiMessage)
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(error = "Ошибка для температуры $temperature: ${error.message}")
                    }
                }
            }

            // Если получен хотя бы один ответ, добавляем последний в историю
            if (successfulResponses.isNotEmpty()) {
                // Берем ответ с температурой 0.7 как основной для истории
                val mainResponse = successfulResponses.find { it.first == 0.7 }
                    ?: successfulResponses.first()

                val fullResponse = "{\"agentMessage\":\"${mainResponse.second.agentMessage}\"}"
                messageHistory.add(ApiMessage(role = "assistant", content = fullResponse))

                _uiState.update { it.copy(isLoading = false) }
            } else {
                // Удаляем последнее сообщение пользователя из истории при полной ошибке
                if (messageHistory.isNotEmpty()) {
                    messageHistory.removeAt(messageHistory.size - 1)
                }

                _uiState.update {
                    it.copy(
                        error = "Не удалось получить ни одного ответа",
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