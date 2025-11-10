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
     * Отправка сообщения в AI с консультацией экспертов
     * Каждый вопрос проходит через трёх экспертов: Тимлид, Дизайнер, Аналитик
     * После чего формируется Общий вывод
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

        // Консультируемся с экспертами
        viewModelScope.launch {
            val expertResults = aiAgent.consultExperts(messageHistory.toList())
            val successfulExperts = mutableListOf<Pair<ExpertType, AiStructuredResponse>>()

            // Обрабатываем результаты от каждого эксперта
            for ((expertType, result) in expertResults) {
                result.onSuccess { structuredResponse ->
                    successfulExperts.add(expertType to structuredResponse)

                    // Добавляем ответ эксперта в UI
                    val expertMessage = Message(
                        text = structuredResponse.agentMessage,
                        isFromUser = false,
                        structuredData = structuredResponse,
                        expertType = expertType
                    )
                    _uiState.update {
                        it.copy(messages = it.messages + expertMessage)
                    }

                    // Добавляем ответ эксперта в историю
                    val expertResponse = "{\"agentMessage\":\"${structuredResponse.agentMessage}\"}"
                    messageHistory.add(ApiMessage(role = "assistant", content = expertResponse))
                }.onFailure { error ->
                    // Логируем ошибку, но продолжаем с другими экспертами
                    _uiState.update {
                        it.copy(error = "Ошибка от ${expertType.getDisplayName()}: ${error.message}")
                    }
                }
            }

            // Если есть хотя бы один успешный ответ, генерируем общий вывод
            if (successfulExperts.isNotEmpty()) {
                val summaryResult = aiAgent.generateSummary(
                    messageHistory.toList(),
                    successfulExperts
                )

                summaryResult.onSuccess { summaryResponse ->
                    // Добавляем общий вывод в UI
                    val summaryMessage = Message(
                        text = summaryResponse.agentMessage,
                        isFromUser = false,
                        structuredData = summaryResponse,
                        expertType = ExpertType.SUMMARY
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + summaryMessage,
                            isLoading = false
                        )
                    }

                    // Добавляем общий вывод в историю
                    val summaryResponseJson = "{\"agentMessage\":\"${summaryResponse.agentMessage}\"}"
                    messageHistory.add(ApiMessage(role = "assistant", content = summaryResponseJson))
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = "Ошибка при формировании общего вывода: ${error.message}",
                            isLoading = false
                        )
                    }
                }
            } else {
                // Удаляем последнее сообщение пользователя из истории при полной ошибке
                if (messageHistory.isNotEmpty()) {
                    messageHistory.removeAt(messageHistory.size - 1)
                }

                _uiState.update {
                    it.copy(
                        error = "Не удалось получить ответы от экспертов",
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