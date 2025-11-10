package com.example.aistudy.repository

import com.example.aistudy.config.ApiConfig
import com.example.aistudy.model.ApiMessage
import com.example.aistudy.model.OpenAIRequest
import com.example.aistudy.model.OpenAIResponse
import com.example.aistudy.model.ApiError
import com.example.aistudy.model.AiStructuredResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

val systemPrompt = """
                Ты — универсальный интеллектуальный ассистент и ИИ-агент, умеющий вести гибкий диалог с пользователем для получения полезного, структурированного результата, который ВСЕГДА отвечает строго в формате JSON.
                Никаких пояснений, текста или комментариев вне JSON быть не должно.
                Структура JSON должна быть ВСЕГДА абсолютно одинаковой для всех ответов, независимо от вопроса пользователя.

                Никогда не изменяй названия полей и не добавляй новые.
                Ответ ВСЕГДА должен быть корректным JSON-объектом, который можно распарсить без ошибок.

                Всегда используй только следующую структуру без дополнительных вставок в начале и в конце строки:
                {
                  "agentMessage": "<вопрос или подробный ответ>"
                }
                Ответ ВСЕГДА должен начинаться с { и заканчиваться на }
                Пример корректного ответа:
                {
                  "agentMessage": "Android - это операционная система"
                }
                
                Твоя цель — понять запрос пользователя, собрать недостающую информацию (не более 5 вопросов), а затем сформировать чёткий, понятный и применимый итог — в подходящей форме (например, рецепт, ТЗ, план, инструкция, список идей, стратегия, сценарий и т.д.).
                Правила поведения:
                Определи контекст запроса.
                Если пользователь не уточнил, в какой области задача (кулинария, бизнес, IT, путешествия и т.п.), задай короткий уточняющий вопрос.
                Собери недостающие данные, задавая не более 10 лаконичных вопросов.
                Каждый вопрос должен быть осмысленным и необходимым для формирования полноценного результата.
                Формулируй вопросы чётко, по одному за раз.
                Избегай шаблонных уточнений — спрашивай только то, что поможет сделать результат точным и полезным.
                Когда информации достаточно, сообщи пользователю, что готов выдать итог, и представь структурированный ответ, оформленный в логичном виде (таблица, список, план, документ и т.п.).
                Форма итогового результата подбирается в зависимости от темы:
                Если это задача → дай план, инструкцию или пошаговое решение.
                Если проект → выдай ТЗ, описание или концепцию.
                Если кулинария → выдай рецепт и рекомендации.
                Если обучение → выдай учебный план.
                Если идея или стратегия → оформи документ с целями, шагами и рекомендациями.
                Структура ответа (пример шаблона):
                Тип результата: (например, Рецепт / Техническое задание / План / Идея / Концепция)
                Название / тема: ...
                Цель / суть: ...
                Основные шаги / разделы / ингредиенты: ...
                Дополнительные рекомендации: ...
                Формат можно адаптировать под задачу.
                Если пользователь не знает часть ответов — предложи разумные варианты или предположения, пометив их как (предложение модели).
                Общайся дружелюбно, уверенно и естественно, как эксперт, который помогает понять и реализовать идею.
                В конце каждого результата спроси, нужно ли что-то изменить, уточнить или дополнить.
                 Примеры сценариев:
                Пример 1 — создание приложения:
                Пользователь: Хочу создать приложение.
                Модель: Отлично! Чтобы подготовить описание, уточню: это мобильное или веб-приложение?
                (после сбора информации → выдаёт ТЗ)
                Пример 2 — ужин:
                Пользователь: Хочу приготовить вкусный ужин.
                Модель: Отлично! Какие ингредиенты у вас есть?
                (спрашивает 2–3 уточнения → выдаёт рецепт)
                Пример 3 — обучение:
                Пользователь: Хочу выучить Python.
                Модель: Супер! Скажи, у тебя есть опыт в программировании?
                (после ответов → выдаёт учебный план)
            """.trimIndent()

/**
 * Репозиторий для работы с DeepSeek AI API
 * DeepSeek использует OpenAI-совместимый API
 */
class AiRepository {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 60000
        }

        defaultRequest {
            header("Authorization", "Bearer ${ApiConfig.apiKey}")
            header("Content-Type", "application/json")
        }
    }

    /**
     * Отправляет вопрос в DeepSeek AI и получает ответ
     * @param question Вопрос пользователя
     * @return Ответ от AI или ошибка
     */
    suspend fun askQuestion(question: String): Result<String> {
        // Проверка, что API ключ установлен
        if (!ApiConfig.isConfigured()) {
            return Result.failure(Exception("API ключ не настроен. Установите deepseek.api.key в local.properties"))
        }

        return try {
            val request = OpenAIRequest(
                model = ApiConfig.MODEL,
                messages = listOf(
                    ApiMessage(role = "system", content = systemPrompt),
                    ApiMessage(role = "user", content = question)
                ),
                temperature = ApiConfig.TEMPERATURE,
                maxTokens = ApiConfig.MAX_TOKENS
            )

            val response: HttpResponse = client.post("${ApiConfig.BASE_URL}/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val openAIResponse = response.body<OpenAIResponse>()
                    val answer = openAIResponse.choices.firstOrNull()?.message?.content
                        ?: "Не удалось получить ответ"
                    Result.success(answer)
                }

                else -> {
                    val errorBody = try {
                        response.body<ApiError>()
                    } catch (e: Exception) {
                        null
                    }
                    val errorMessage = errorBody?.error?.message
                        ?: "Ошибка API: ${response.status.description}"
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Обрабатывает различные типы исключений
     */
    private fun handleException(e: Exception): Exception {
        return when (e) {
            is HttpRequestTimeoutException -> Exception("Превышено время ожидания. Проверьте подключение к интернету.")
            is kotlinx.io.IOException -> Exception("Ошибка сети. Проверьте подключение к интернету.")
            else -> Exception("Неизвестная ошибка: ${e.message ?: "Нет описания"}")
        }
    }

    /**
     * Отправляет вопрос в DeepSeek AI и получает структурированный ответ
     * @param question Вопрос пользователя
     * @return Структурированный ответ от AI или ошибка
     */
    suspend fun askQuestionStructured(question: String): Result<AiStructuredResponse> {
        val result = askQuestion(question)

        return result.fold(
            onSuccess = { jsonString ->
                try {
                    // Парсим JSON ответ в структурированный формат
                    val json = Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                    val structured = json.decodeFromString<AiStructuredResponse>(jsonString)
                    Result.success(structured)
                } catch (e: Exception) {
                    // Если парсинг не удался, возвращаем ошибку
                    Result.failure(Exception("Не удалось распарсить ответ от AI: ${e.message}"))
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    /**
     * Отправляет запрос в DeepSeek AI с историей сообщений (multi-round conversation)
     * @param messageHistory История сообщений (список ApiMessage с role: user/assistant)
     * @return Структурированный ответ от AI или ошибка
     */
    suspend fun askWithHistory(messageHistory: List<ApiMessage>): Result<AiStructuredResponse> {
        return askWithHistoryAndPrompt(messageHistory, systemPrompt)
    }

    /**
     * Отправляет запрос в DeepSeek AI с указанной температурой
     * @param messageHistory История сообщений (список ApiMessage с role: user/assistant)
     * @param temperature Температура для генерации (0.0 - детерминированный, 1.0 - креативный)
     * @return Структурированный ответ от AI или ошибка
     */
    suspend fun askWithHistoryAndTemperature(
        messageHistory: List<ApiMessage>,
        temperature: Double
    ): Result<AiStructuredResponse> {
        return askWithHistoryPromptAndTemperature(messageHistory, systemPrompt, temperature)
    }

    /**
     * Отправляет запрос в DeepSeek AI с кастомным system prompt и историей сообщений
     * @param messageHistory История сообщений (список ApiMessage с role: user/assistant)
     * @param customSystemPrompt Кастомный system prompt для специфической роли (например, для эксперта)
     * @return Структурированный ответ от AI или ошибка
     */
    suspend fun askWithHistoryAndPrompt(
        messageHistory: List<ApiMessage>,
        customSystemPrompt: String
    ): Result<AiStructuredResponse> {
        return askWithHistoryPromptAndTemperature(messageHistory, customSystemPrompt, ApiConfig.TEMPERATURE)
    }

    /**
     * Отправляет запрос в DeepSeek AI с кастомным system prompt, историей сообщений и температурой
     * @param messageHistory История сообщений (список ApiMessage с role: user/assistant)
     * @param customSystemPrompt Кастомный system prompt для специфической роли (например, для эксперта)
     * @param temperature Температура для генерации (0.0 - детерминированный, 1.0 - креативный)
     * @return Структурированный ответ от AI или ошибка
     */
    private suspend fun askWithHistoryPromptAndTemperature(
        messageHistory: List<ApiMessage>,
        customSystemPrompt: String,
        temperature: Double
    ): Result<AiStructuredResponse> {
        // Проверка, что API ключ установлен
        if (!ApiConfig.isConfigured()) {
            return Result.failure(Exception("API ключ не настроен. Установите deepseek.api.key в local.properties"))
        }

        return try {
            // Формируем список сообщений: system промпт + история
            val messages =
                listOf(ApiMessage(role = "system", content = customSystemPrompt)) + messageHistory

            val request = OpenAIRequest(
                model = ApiConfig.MODEL,
                messages = messages,
                temperature = temperature,
                maxTokens = ApiConfig.MAX_TOKENS
            )

            val response: HttpResponse = client.post("${ApiConfig.BASE_URL}/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val openAIResponse = response.body<OpenAIResponse>()
                    val answer = openAIResponse.choices.firstOrNull()?.message?.content
                        ?: return Result.failure(Exception("Не удалось получить ответ"))

                    // Парсим JSON ответ в структурированный формат
                    try {
                        val json = Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        }
                        val structured = json.decodeFromString<AiStructuredResponse>(answer)
                        Result.success(structured)
                    } catch (e: Exception) {
                        Result.failure(Exception("Не удалось распарсить ответ от AI: ${e.message}"))
                    }
                }

                else -> {
                    val errorBody = try {
                        response.body<ApiError>()
                    } catch (e: Exception) {
                        null
                    }
                    val errorMessage = errorBody?.error?.message
                        ?: "Ошибка API: ${response.status.description}"
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Закрывает HTTP клиент
     */
    fun close() {
        client.close()
    }
}