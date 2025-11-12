package com.example.aistudy.model

import kotlinx.serialization.Serializable
import com.example.aistudy.model.Usage

/**
 * Структурированный ответ от AI в формате JSON
 */
@Serializable
data class AiStructuredResponse(
    val agentMessage: String = "",
    val usage: Usage? = null
)

/**
 * Модель сообщения для отображения в чате
 */
data class Message(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = 0L,
    // Структурированные данные для ответов AI
    val structuredData: AiStructuredResponse? = null,
    // Тип эксперта, который ответил (null для сообщений пользователя)
    val expertType: ExpertType? = null,
    // Температура, с которой был получен ответ (null для сообщений пользователя)
    val temperature: Double? = null
)

/**
 * Модель сообщения для API запроса (OpenAI формат)
 */
@Serializable
data class ApiMessage(
    val role: String, // "user", "assistant", или "system"
    val content: String
)