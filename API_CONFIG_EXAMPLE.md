# Конфигурация API - Рекомендации по безопасности

## 🔐 Безопасное хранение API ключей

### Вариант 1: Local Properties (Рекомендуется для разработки)

1. Создайте файл `local.properties` в корне проекта (он уже в `.gitignore`):

```properties
# local.properties
openai.api.key=sk-your-actual-api-key-here
```

2. Обновите `build.gradle.kts` в shared модуле:

```kotlin
// shared/build.gradle.kts
android {
    // ...
    defaultConfig {
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }

        buildConfigField(
            "String",
            "OPENAI_API_KEY",
            "\"${properties.getProperty("openai.api.key", "")}\""
        )
    }
}
```

3. Используйте в коде:

```kotlin
// Android
val apiKey = BuildConfig.OPENAI_API_KEY
```

### Вариант 2: Переменные окружения

1. Установите переменную окружения:

```bash
# macOS/Linux
export OPENAI_API_KEY="sk-your-actual-api-key-here"

# Windows
set OPENAI_API_KEY=sk-your-actual-api-key-here
```

2. Читайте в коде:

```kotlin
val apiKey = System.getenv("OPENAI_API_KEY") ?: "default_key"
```

### Вариант 3: Gradle Properties

1. Добавьте в `~/.gradle/gradle.properties` (глобально):

```properties
OPENAI_API_KEY=sk-your-actual-api-key-here
```

2. Или в `gradle.properties` проекта (добавьте в .gitignore):

```properties
openaiApiKey=sk-your-actual-api-key-here
```

3. Используйте в build.gradle.kts:

```kotlin
val openaiApiKey: String by project

android {
    defaultConfig {
        buildConfigField("String", "OPENAI_API_KEY", "\"$openaiApiKey\"")
    }
}
```

### Вариант 4: Для iOS - Plist конфигурация

1. Создайте `Config.plist` (добавьте в .gitignore):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN">
<plist version="1.0">
<dict>
    <key>OPENAI_API_KEY</key>
    <string>sk-your-actual-api-key-here</string>
</dict>
</plist>
```

2. Читайте в Swift:

```swift
func getAPIKey() -> String {
    guard let path = Bundle.main.path(forResource: "Config", ofType: "plist"),
          let config = NSDictionary(contentsOfFile: path),
          let apiKey = config["OPENAI_API_KEY"] as? String else {
        return ""
    }
    return apiKey
}
```

## 🌐 Альтернативные AI API

### OpenAI (по умолчанию)

```kotlin
class AiRepository(
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1"
)

// Модели: gpt-3.5-turbo, gpt-4, gpt-4-turbo
```

### Ollama (локальный LLM)

```kotlin
class AiRepository(
    private val apiKey: String = "", // Не требуется для локального
    private val baseUrl: String = "http://localhost:11434/v1"
)

// В OpenAIRequest измените:
val model: String = "llama2" // или другая установленная модель
```

**Установка Ollama:**
```bash
# macOS
brew install ollama

# Linux
curl -fsSL https://ollama.com/install.sh | sh

# Запуск
ollama serve

# Скачать модель
ollama pull llama2
```

### Azure OpenAI

```kotlin
class AiRepository(
    private val apiKey: String,
    private val baseUrl: String = "https://YOUR_RESOURCE.openai.azure.com"
)

// Измените заголовки в HttpClient:
defaultRequest {
    header("api-key", apiKey)
    header("Content-Type", "application/json")
}
```

### Claude (Anthropic)

Требует изменения моделей запроса/ответа:

```kotlin
@Serializable
data class ClaudeRequest(
    val model: String = "claude-3-sonnet-20240229",
    val messages: List<ApiMessage>,
    val max_tokens: Int = 1024
)

class AiRepository(
    private val apiKey: String,
    private val baseUrl: String = "https://api.anthropic.com/v1"
)

defaultRequest {
    header("x-api-key", apiKey)
    header("anthropic-version", "2023-06-01")
    header("Content-Type", "application/json")
}
```

### LocalAI (самохостинг)

```kotlin
class AiRepository(
    private val apiKey: String = "",
    private val baseUrl: String = "http://localhost:8080/v1"
)

// Совместим с OpenAI API
// Установка: https://localai.io/
```

## ⚙️ Дополнительные настройки Ktor Client

### Retry механизм

```kotlin
install(HttpRequestRetry) {
    retryOnServerErrors(maxRetries = 3)
    exponentialDelay()
}
```

### Custom Timeout

```kotlin
install(HttpTimeout) {
    requestTimeoutMillis = 120000  // 2 минуты
    connectTimeoutMillis = 60000   // 1 минута
    socketTimeoutMillis = 60000    // 1 минута
}
```

### Кастомный User-Agent

```kotlin
defaultRequest {
    header("User-Agent", "AI-Study-KMP/1.0")
}
```

### SSL Certificate Pinning (Production)

```kotlin
install(HttpClient) {
    engine {
        // Для Android OkHttp
        config {
            sslSocketFactory(sslContext.socketFactory, trustManager)
        }
    }
}
```

## 📊 Мониторинг использования API

### Логирование запросов

```kotlin
install(Logging) {
    logger = object : Logger {
        override fun log(message: String) {
            println("[HTTP] $message")
        }
    }
    level = LogLevel.ALL // NONE, INFO, HEADERS, BODY, ALL
}
```

### Подсчет токенов

```kotlin
var totalTokensUsed = 0

val response = client.post("$baseUrl/chat/completions") { ... }
val openAIResponse = response.body<OpenAIResponse>()

openAIResponse.usage?.let { usage ->
    totalTokensUsed += usage.totalTokens
    println("Tokens used: ${usage.totalTokens}")
    println("Total tokens: $totalTokensUsed")
}
```

## 🚦 Rate Limiting

```kotlin
class RateLimiter(private val requestsPerMinute: Int = 60) {
    private val timestamps = mutableListOf<Long>()

    suspend fun execute(block: suspend () -> Unit) {
        val now = System.currentTimeMillis()
        timestamps.removeAll { now - it > 60000 }

        if (timestamps.size >= requestsPerMinute) {
            val oldestRequest = timestamps.first()
            val waitTime = 60000 - (now - oldestRequest)
            delay(waitTime)
        }

        timestamps.add(System.currentTimeMillis())
        block()
    }
}

// Использование
val rateLimiter = RateLimiter(requestsPerMinute = 60)
rateLimiter.execute {
    agent.ask("question")
}
```

## 🧪 Тестирование с Mock API

### MockEngine для тестов

```kotlin
val mockEngine = MockEngine { request ->
    respond(
        content = """
        {
            "id": "test-id",
            "choices": [{
                "message": {
                    "role": "assistant",
                    "content": "Mock response"
                },
                "index": 0
            }]
        }
        """,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json")
    )
}

val testClient = HttpClient(mockEngine) {
    install(ContentNegotiation) {
        json()
    }
}
```

## 📈 Best Practices

1. **Никогда не коммитьте API ключи** в Git
2. **Используйте разные ключи** для dev/staging/production
3. **Ротируйте ключи** регулярно
4. **Мониторьте использование** для обнаружения утечек
5. **Устанавливайте лимиты** на стороне провайдера
6. **Шифруйте ключи** в production сборках
7. **Используйте backend proxy** для production (не вызывайте API напрямую)

## 🔄 Backend Proxy Pattern (Production)

Вместо прямых вызовов к OpenAI, используйте свой backend:

```
Mobile App → Your Backend → OpenAI API
```

Преимущества:
- API ключи хранятся на сервере
- Контроль доступа
- Кэширование ответов
- Аналитика
- Rate limiting на уровне пользователя

```kotlin
class AiRepository(
    private val baseUrl: String = "https://your-backend.com/api"
) {
    suspend fun askQuestion(question: String): Result<String> {
        // Аутентификация пользователя через JWT
        val response = client.post("$baseUrl/ai/chat") {
            header("Authorization", "Bearer $userToken")
            setBody(mapOf("question" to question))
        }
        // ...
    }
}
```

---

Выберите подход, который соответствует вашим требованиям безопасности!