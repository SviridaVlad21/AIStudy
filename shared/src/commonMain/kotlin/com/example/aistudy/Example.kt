package com.example.aistudy

import com.example.aistudy.agent.AiAgent
import com.example.aistudy.config.ApiConfig
import com.example.aistudy.config.ApiKeyProvider
import kotlinx.coroutines.runBlocking

/**
 * Пример использования AI агента с DeepSeek
 *
 * ВАЖНО: API ключ должен быть установлен в local.properties:
 * deepseek.api.key=sk-ваш-ключ
 */
fun main() = runBlocking {
    // Инициализация API ключа из безопасного источника
    val apiKey = ApiKeyProvider.getApiKey()
    if (apiKey.isEmpty()) {
        println("⚠️ API ключ не настроен!")
        println("Добавьте в local.properties: deepseek.api.key=sk-ваш-ключ")
        return@runBlocking
    }

    ApiConfig.initialize(apiKey)

    // Создание экземпляра агента
    val agent = AiAgent()

    try {
        // Отправка вопроса
        val response = agent.ask("Что такое Kotlin Multiplatform?")
        println("Ответ: $response")

        // Пример с обработкой ошибок
        val result = agent.askSafe("Объясни преимущества KMP")
        result.onSuccess { answer ->
            println("Успешный ответ: $answer")
        }.onFailure { error ->
            println("Ошибка: ${error.message}")
        }
    } catch (e: Exception) {
        println("Произошла ошибка: ${e.message}")
    } finally {
        // Закрытие соединения
        agent.close()
    }
}

/**
 * Пример использования с несколькими вопросами
 */
suspend fun exampleMultipleQuestions() {
    val agent = AiAgent()

    val questions = listOf(
        "Что такое Kotlin?",
        "Что такое Multiplatform?",
        "Какие платформы поддерживает KMP?"
    )

    try {
        questions.forEach { question ->
            println("\nВопрос: $question")
            val result = agent.askSafe(question)
            result.onSuccess { println("Ответ: $it") }
                .onFailure { println("Ошибка: ${it.message}") }
        }
    } finally {
        agent.close()
    }
}