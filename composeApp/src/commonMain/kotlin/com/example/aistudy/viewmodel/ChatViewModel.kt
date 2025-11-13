package com.example.aistudy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aistudy.agent.AiAgent
import com.example.aistudy.config.ApiConfig
import com.example.aistudy.config.ApiKeyProvider
import com.example.aistudy.model.Message
import com.example.aistudy.model.ApiMessage
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

    // Счетчик сообщений после последнего summary (пары user+assistant)
    private var messageCountSinceLastSummary = 0

    // Сохраненное summary предыдущих сообщений
    private var conversationSummary: ApiMessage? = null

    // Константа: создавать summary каждые 3 сообщений (пар user+assistant)
    private val MESSAGES_BEFORE_SUMMARY = 3

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
     * Формирует список сообщений для отправки в API
     * Если есть summary, отправляем summary + новые сообщения
     * Иначе отправляем всю историю
     */
    private fun getMessagesToSend(): List<ApiMessage> {
        return if (conversationSummary != null) {
            // Если есть summary, отправляем его вместе с новыми сообщениями
            listOf(conversationSummary!!) + messageHistory
        } else {
            // Иначе отправляем всю историю
            messageHistory.toList()
        }
    }

    /**
     * Отправка сообщения в AI
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

        // Отправляем запрос к AI
        viewModelScope.launch {
            // Используем метод для получения оптимизированного списка сообщений
            val messagesToSend = getMessagesToSend()
            val result = aiAgent.askWithHistorySafe(messagesToSend)

            result.onSuccess { structuredResponse ->
                // Добавляем ответ AI в UI
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

                // Добавляем ответ в историю для продолжения диалога
                val fullResponse = "{\"agentMessage\":\"${structuredResponse.agentMessage}\"}"
                messageHistory.add(ApiMessage(role = "assistant", content = fullResponse))

                // Увеличиваем счетчик сообщений (пара user + assistant = 1 сообщение)
                messageCountSinceLastSummary++

                // Проверяем, нужно ли создать summary
                if (messageCountSinceLastSummary >= MESSAGES_BEFORE_SUMMARY) {
                    createSummary()
                }
            }.onFailure { error ->
                // Удаляем последнее сообщение пользователя из истории при ошибке
                if (messageHistory.isNotEmpty()) {
                    messageHistory.removeAt(messageHistory.size - 1)
                }

                _uiState.update {
                    it.copy(
                        error = "Ошибка: ${error.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Создает summary предыдущих сообщений для оптимизации контекста
     * Summary сохраняется и используется вместо старых сообщений
     * При наличии предыдущего summary создает инкрементальное summary (предыдущее + новые сообщения)
     */
    private suspend fun createSummary() {
        if (messageHistory.isEmpty()) return

        // Формируем историю для суммаризации
        val summaryRequest = if (conversationSummary != null) {
            // Если есть предыдущее summary, создаем инкрементальное summary
            val summaryPrompt = """
                У тебя есть предыдущее резюме диалога и новые сообщения после него.
                Создай обновленное краткое резюме, которое объединяет информацию из предыдущего резюме и новых сообщений.
                Сохрани ключевые темы, важные детали и контекст всего разговора.
                Резюме должно быть достаточно информативным, чтобы продолжить разговор без потери контекста.
                В ответе пиши сразу резюме.
                Отвечай строго в формате JSON: {"agentMessage": "текст резюме"}
            """.trimIndent()

            // Включаем предыдущее summary + новые сообщения + промпт на суммаризацию
            listOf(conversationSummary!!) + messageHistory + listOf(
                ApiMessage(role = "user", content = summaryPrompt)
            )
        } else {
            // Если это первое summary, создаем его из всей истории
            val summaryPrompt = """
                Создай краткое резюме следующего диалога.
                Сохрани ключевые темы, важные детали и контекст разговора.
                Резюме должно быть достаточно информативным, чтобы продолжить разговор без потери контекста.
                Отвечай строго в формате JSON: {"agentMessage": "текст резюме"}
            """.trimIndent()

            messageHistory + listOf(
                ApiMessage(role = "user", content = summaryPrompt)
            )
        }

        // Отправляем запрос на создание summary
        val result = aiAgent.askWithHistorySafe(summaryRequest)

        result.onSuccess { structuredResponse ->
            // Сохраняем summary как системное сообщение
            conversationSummary = ApiMessage(
                role = "system",
                content = "Краткое резюме предыдущего разговора: ${structuredResponse.agentMessage}"
            )

            // Очищаем старую историю - теперь она заменена на summary
            messageHistory.clear()

            // Сбрасываем счетчик
            messageCountSinceLastSummary = 0
        }.onFailure { error ->
            // Если не удалось создать summary, просто логируем и продолжаем работу
            println("Не удалось создать summary: ${error.message}")
        }
    }

    /**
     * Очистка истории сообщений
     */
    fun clearHistory() {
        messageHistory.clear()
        conversationSummary = null
        messageCountSinceLastSummary = 0
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