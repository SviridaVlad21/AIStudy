package com.example.aistudy.model

import kotlinx.serialization.Serializable

/**
 * Модель сообщения для отображения в чате
 */
data class Message(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Модель сообщения для API запроса (OpenAI формат)
 */
@Serializable
data class ApiMessage(
    val role: String, // "user", "assistant", или "system"
    val content: String
)