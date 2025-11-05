package com.example.aistudy.repository

import com.example.aistudy.config.ApiConfig
import com.example.aistudy.model.ApiMessage
import com.example.aistudy.model.OpenAIRequest
import com.example.aistudy.model.OpenAIResponse
import com.example.aistudy.model.ApiError
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
                    ApiMessage(role = "system", content = "You are a helpful assistant."),
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
     * Закрывает HTTP клиент
     */
    fun close() {
        client.close()
    }
}