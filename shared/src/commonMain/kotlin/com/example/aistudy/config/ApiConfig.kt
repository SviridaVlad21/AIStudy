package com.example.aistudy.config

/**
 * Централизованная конфигурация для AI API
 *
 * API ключ должен быть задан в local.properties:
 * deepseek.api.key=sk-ваш-ключ-здесь
 */
object ApiConfig {
    /**
     * DeepSeek API endpoint
     * Совместим с OpenAI API
     */
    const val BASE_URL = "https://api.deepseek.com"

    /**
     * Модель DeepSeek для использования
     * Доступные модели:
     * - deepseek-chat (рекомендуется)
     * - deepseek-coder (для кода)
     */
    const val MODEL = "deepseek-chat"

    /**
     * Температура генерации (0.0 - 1.0)
     * Меньше = более предсказуемо, больше = более креативно
     */
    const val TEMPERATURE = 0.7

    /**
     * Максимальное количество токенов в ответе
     */
    const val MAX_TOKENS = 2048

    /**
     * API ключ (получается из platform-specific источника)
     */
    var apiKey: String = ""
        private set

    /**
     * Инициализация API ключа
     * Вызывается при старте приложения
     */
    fun initialize(key: String) {
        apiKey = key
    }

    /**
     * Проверка, инициализирован ли ключ
     */
    fun isConfigured(): Boolean = apiKey.isNotEmpty()
}