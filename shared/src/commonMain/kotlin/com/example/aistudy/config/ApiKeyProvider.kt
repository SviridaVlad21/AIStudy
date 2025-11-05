package com.example.aistudy.config

/**
 * Platform-specific провайдер API ключа
 */
expect object ApiKeyProvider {
    fun getApiKey(): String
}