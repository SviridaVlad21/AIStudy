package com.example.aistudy.config

/**
 * iOS реализация провайдера API ключа
 */
actual object ApiKeyProvider {
    actual fun getApiKey(): String {
        // Ввести свой api_key
        return ""
    }
}