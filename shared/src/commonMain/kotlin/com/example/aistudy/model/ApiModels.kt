package com.example.aistudy.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Запрос к OpenAI API
 */
@Serializable
data class OpenAIRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<ApiMessage>,
    val temperature: Double = 0.7,
    @SerialName("max_tokens")
    val maxTokens: Int? = null
)

/**
 * Ответ от OpenAI API
 */
@Serializable
data class OpenAIResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage? = null
)

@Serializable
data class Choice(
    val message: ApiMessage,
    val index: Int,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)

/**
 * Модель ошибки API
 */
@Serializable
data class ApiError(
    val error: ErrorDetails
)

@Serializable
data class ErrorDetails(
    val message: String,
    val type: String? = null,
    val code: String? = null
)