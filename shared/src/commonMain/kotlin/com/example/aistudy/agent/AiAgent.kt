package com.example.aistudy.agent

import com.example.aistudy.repository.AiRepository
import com.example.aistudy.model.AiStructuredResponse
import com.example.aistudy.model.ApiMessage

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
     * Закрывает соединение с репозиторием
     */
    fun close() {
        repository.close()
    }
}