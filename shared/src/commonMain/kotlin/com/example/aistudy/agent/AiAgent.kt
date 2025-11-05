package com.example.aistudy.agent

import com.example.aistudy.repository.AiRepository

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
     * Закрывает соединение с репозиторием
     */
    fun close() {
        repository.close()
    }
}