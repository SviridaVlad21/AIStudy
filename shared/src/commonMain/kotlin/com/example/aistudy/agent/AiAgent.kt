package com.example.aistudy.agent

import com.example.aistudy.repository.AiRepository
import com.example.aistudy.model.AiStructuredResponse
import com.example.aistudy.model.ApiMessage
import com.example.aistudy.model.ExpertType

/**
 * AI Агент для взаимодействия с DeepSeek API
 * API ключ должен быть установлен в ApiConfig перед использованием
 */
class AiAgent {
    private val repository = AiRepository()

    /**
     * Отправляет вопрос в AI и получает ответ
     * @param question Вопрос пользователя
     * @return Ответ от AI в виде строки
     * @throws Exception если произошла ошибка
     */
    suspend fun ask(question: String): String {
        if (question.isBlank()) {
            throw IllegalArgumentException("Вопрос не может быть пустым")
        }

        return repository.askQuestion(question).getOrThrow()
    }

    /**
     * Безопасная версия ask, которая возвращает Result вместо выброса исключения
     */
    suspend fun askSafe(question: String): Result<String> {
        if (question.isBlank()) {
            return Result.failure(IllegalArgumentException("Вопрос не может быть пустым"))
        }

        return repository.askQuestion(question)
    }

    /**
     * Отправляет вопрос в AI и получает структурированный ответ
     * @param question Вопрос пользователя
     * @return Структурированный ответ от AI
     * @throws Exception если произошла ошибка
     */
    suspend fun askStructured(question: String): AiStructuredResponse {
        if (question.isBlank()) {
            throw IllegalArgumentException("Вопрос не может быть пустым")
        }

        return repository.askQuestionStructured(question).getOrThrow()
    }

    /**
     * Безопасная версия askStructured, которая возвращает Result вместо выброса исключения
     */
    suspend fun askStructuredSafe(question: String): Result<AiStructuredResponse> {
        if (question.isBlank()) {
            return Result.failure(IllegalArgumentException("Вопрос не может быть пустым"))
        }

        return repository.askQuestionStructured(question)
    }

    /**
     * Отправляет запрос в AI с историей сообщений (multi-round conversation)
     * @param messageHistory История сообщений (список ApiMessage с role: user/assistant)
     * @return Структурированный ответ от AI
     * @throws Exception если произошла ошибка
     */
    suspend fun askWithHistory(messageHistory: List<ApiMessage>): AiStructuredResponse {
        if (messageHistory.isEmpty()) {
            throw IllegalArgumentException("История сообщений не может быть пустой")
        }

        return repository.askWithHistory(messageHistory).getOrThrow()
    }

    /**
     * Безопасная версия askWithHistory, которая возвращает Result вместо выброса исключения
     */
    suspend fun askWithHistorySafe(messageHistory: List<ApiMessage>): Result<AiStructuredResponse> {
        if (messageHistory.isEmpty()) {
            return Result.failure(IllegalArgumentException("История сообщений не может быть пустой"))
        }

        return repository.askWithHistory(messageHistory)
    }

    /**
     * Отправляет запрос в AI с указанной температурой
     * @param messageHistory История сообщений
     * @param temperature Температура для генерации (0.0 - детерминированный, 1.0 - креативный)
     * @return Структурированный ответ от AI
     */
    suspend fun askWithTemperatureSafe(
        messageHistory: List<ApiMessage>,
        temperature: Double
    ): Result<AiStructuredResponse> {
        if (messageHistory.isEmpty()) {
            return Result.failure(IllegalArgumentException("История сообщений не может быть пустой"))
        }

        return repository.askWithHistoryAndTemperature(messageHistory, temperature)
    }

    /**
     * Получает ответ от конкретного эксперта
     * @param messageHistory История сообщений
     * @param expertType Тип эксперта
     * @return Структурированный ответ от эксперта
     */
    suspend fun askExpert(
        messageHistory: List<ApiMessage>,
        expertType: ExpertType
    ): Result<AiStructuredResponse> {
        if (messageHistory.isEmpty()) {
            return Result.failure(IllegalArgumentException("История сообщений не может быть пустой"))
        }

        val expertPrompt = expertType.getSystemPrompt()
        return repository.askWithHistoryAndPrompt(messageHistory, expertPrompt)
    }

    /**
     * Консультация со всеми экспертами
     * Возвращает список пар: тип эксперта и его ответ
     * @param messageHistory История сообщений
     * @return Список результатов от каждого эксперта
     */
    suspend fun consultExperts(
        messageHistory: List<ApiMessage>
    ): List<Pair<ExpertType, Result<AiStructuredResponse>>> {
        if (messageHistory.isEmpty()) {
            return emptyList()
        }

        val consultingExperts = ExpertType.getConsultingExperts()
        val results = mutableListOf<Pair<ExpertType, Result<AiStructuredResponse>>>()

        // Получаем ответы от каждого эксперта последовательно
        for (expert in consultingExperts) {
            val result = askExpert(messageHistory, expert)
            results.add(expert to result)
        }

        return results
    }

    /**
     * Генерирует общий вывод на основе ответов всех экспертов
     * @param messageHistory История сообщений
     * @param expertResponses Ответы от экспертов
     * @return Общий вывод
     */
    suspend fun generateSummary(
        messageHistory: List<ApiMessage>,
        expertResponses: List<Pair<ExpertType, AiStructuredResponse>>
    ): Result<AiStructuredResponse> {
        if (expertResponses.isEmpty()) {
            return Result.failure(IllegalArgumentException("Нет ответов от экспертов для создания общего вывода"))
        }

        // Находим последний вопрос пользователя
        val lastUserMessage = messageHistory.lastOrNull { it.role == "user" }
            ?: return Result.failure(IllegalArgumentException("Не найден вопрос пользователя"))

        // Формируем компактную историю: только последний вопрос и ответы экспертов
        val summaryHistory = listOf(
            lastUserMessage,
            ApiMessage(
                role = "assistant",
                content = buildString {
                    append("Получены ответы от трёх экспертов:\n\n")
                    expertResponses.forEach { (expert, response) ->
                        append("${expert.getDisplayName()}: ${response.agentMessage}\n\n")
                    }
                }
            ),
            ApiMessage(
                role = "user",
                content = "На основе этих трёх мнений сформируй краткий общий вывод"
            )
        )

        return repository.askWithHistoryAndPrompt(summaryHistory, ExpertType.SUMMARY.getSystemPrompt())
    }

    /**
     * Закрывает соединение с репозиторием
     */
    fun close() {
        repository.close()
    }
}