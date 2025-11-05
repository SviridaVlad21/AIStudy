package com.example.aistudy.model

import kotlinx.serialization.Serializable

/**
 * Структурированный ответ от AI в формате JSON
 */
@Serializable
data class AiStructuredResponse(
    val question: String = "",
    val summary: String = "",
    val explanation: String = "",
    val code_example: String = "",
    val sources: List<String> = emptyList(),
    val confidence: String = ""
)

/**
 * Модель сообщения для отображения в чате
 */
data class Message(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    // Структурированные данные для ответов AI
    val structuredData: AiStructuredResponse? = null
)

/**
 * Модель сообщения для API запроса (OpenAI формат)
 */
@Serializable
data class ApiMessage(
    val role: String, // "user", "assistant", или "system"
    val content: String
)