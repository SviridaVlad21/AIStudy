# 🤖 AI Study - Kotlin Multiplatform Chat App

<div align="center">

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-blue?logo=kotlin)
![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS-green)
![DeepSeek](https://img.shields.io/badge/AI-DeepSeek-orange)
![Compose](https://img.shields.io/badge/UI-Compose%20Multiplatform-purple)
![Ktor](https://img.shields.io/badge/HTTP-Ktor%203.0.3-red)

**Кроссплатформенный AI чат-ассистент на Kotlin Multiplatform с интеграцией DeepSeek API**

[Особенности](#-особенности) • [Технологии](#-технологии) • [Быстрый старт](#-быстрый-старт) • [Архитектура](#-архитектура) • [Документация](#-документация)

</div>

---

## 📱 О проекте

**AI Study** — это современное кроссплатформенное приложение для общения с AI-ассистентом, построенное на **Kotlin Multiplatform**.

### ✨ Особенности

- 🎯 **Kotlin Multiplatform** — единая кодовая база для Android и iOS
- 🤖 **DeepSeek AI** — быстрая и качественная языковая модель
- 🏗️ **MVVM архитектура** — чистая и масштабируемая структура
- 🔐 **Безопасность** — централизованное управление API ключами
- ⚡ **Ktor Client** — асинхронные HTTP запросы
- 🎨 **Modern UI** — Jetpack Compose для Android, SwiftUI для iOS
- 💾 **Kotlinx Serialization** — эффективная работа с JSON
- 🔄 **Coroutines** — реактивное программирование

---

## 🛠️ Технологии

### Shared (Общий код)

- **Kotlin Multiplatform** `2.2.20` — кроссплатформенная разработка
- **Ktor Client** `3.0.3` — HTTP клиент для сетевых запросов
- **kotlinx.serialization** `1.8.0` — сериализация JSON
- **kotlinx.coroutines** `1.10.1` — асинхронное программирование

### Android

- **Jetpack Compose** — современный UI toolkit
- **Compose Multiplatform** `1.9.1` — общий Compose код
- **Material Design 3** — дизайн-система Google
- **Lifecycle ViewModel** `2.9.5` — управление состоянием

### iOS

- **SwiftUI** — нативный iOS UI framework
- **iOS 14+** — поддержка современных версий

### AI Provider

- **DeepSeek API** — OpenAI-совместимый API
  - Модель: `deepseek-chat`
  - ⚡ Быстрые ответы
  - 💰 Экономичность (~10x дешевле GPT-4)
  - 🌟 Отличное качество

---

## 🚀 Быстрый старт

### Предварительные требования

- **JDK** 11 или выше
- **Android Studio** Arctic Fox или новее
- **Xcode** 14.0+ (только для iOS, требуется macOS)
- **Gradle** 8.0+
- **DeepSeek API Key** ([получить здесь](https://platform.deepseek.com))

### Установка

1. **Клонируйте репозиторий:**

2. **Настройте API ключ:**
```bash
# Скопируйте template
cp local.properties.template local.properties

# Откройте local.properties и вставьте ваш DeepSeek API ключ:
deepseek.api.key=sk-ваш-ключ-здесь
```

3. **Запустите приложение:**

#### 🤖 Android
```bash
./gradlew :composeApp:installDebug
```

Или откройте проект в Android Studio и нажмите Run ▶️

#### 🍎 iOS
```bash
open iosApp/iosApp.xcodeproj
```

Затем в Xcode: **Product → Run** (⌘R)

---

## 📁 Структура проекта

```
AIStudy/
├── 📱 shared/                           # Общий Kotlin Multiplatform модуль
│   ├── src/commonMain/kotlin/
│   │   ├── agent/                       # 🤖 AI агент
│   │   │   └── AiAgent.kt              # Главный класс агента
│   │   ├── repository/                  # 🌐 Сетевые запросы
│   │   │   └── AiRepository.kt         # Ktor HTTP клиент
│   │   ├── model/                       # 📦 Модели данных
│   │   │   ├── Message.kt              # Модель сообщения
│   │   │   └── ApiModels.kt            # API запросы/ответы
│   │   └── config/                      # ⚙️ Конфигурация
│   │       ├── ApiConfig.kt            # Центральная конфигурация
│   │       └── ApiKeyProvider.kt       # Expect/actual провайдер
│   │
│   ├── src/androidMain/                # Android-specific код
│   └── src/iosMain/                    # iOS-specific код
│
├── 🤖 composeApp/                       # Android приложение
│   ├── src/commonMain/kotlin/
│   │   ├── viewmodel/                   # 🎯 MVVM ViewModels
│   │   │   └── ChatViewModel.kt        # ViewModel для чата
│   │   └── ui/                          # 🎨 Compose UI
│   │       └── ChatScreen.kt           # Экран чата
│   │
│   └── src/androidMain/
│       ├── AndroidManifest.xml         # Android manifest
│       └── MainActivity.kt             # Главная Activity
│
├── 🍎 iosApp/                           # iOS приложение
│   └── iosApp/
│       ├── iOSApp.swift                # Точка входа iOS
│       └── ContentView.swift           # SwiftUI UI
│
├── 📚 Документация
│   ├── DEEPSEEK_SETUP.md              # Настройка DeepSeek
│   ├── MIGRATION_TO_DEEPSEEK.md       # Гайд по миграции
│   ├── QUICKSTART.md                  # Быстрый старт
│   └── AI_AGENT_README.md             # Архитектура проекта
│
├── local.properties.template          # Шаблон для API ключа
├── .gitignore                         # Git ignore правила
└── README.md                          # Этот файл
```

---

## 🏗️ Архитектура

Проект использует **Clean Architecture** с разделением на слои:

```
┌─────────────────────────────────┐
│         UI Layer                │
│  ┌───────────┐  ┌────────────┐ │
│  │  Android  │  │    iOS     │ │
│  │ (Compose) │  │ (SwiftUI)  │ │
│  └───────────┘  └────────────┘ │
└───────────┬─────────────────────┘
            │
┌───────────▼─────────────────────┐
│    Presentation Layer           │
│     ChatViewModel (MVVM)        │
└───────────┬─────────────────────┘
            │
┌───────────▼─────────────────────┐
│      Domain Layer               │
│         AiAgent                 │
└───────────┬─────────────────────┘
            │
┌───────────▼─────────────────────┐
│       Data Layer                │
│      AiRepository               │
│     (Ktor Client)               │
└───────────┬─────────────────────┘
            │
            ▼
      ┌──────────┐
      │ DeepSeek │
      │   API    │
      └──────────┘
```

### Ключевые компоненты

#### **ApiConfig** - Центральная конфигурация
```kotlin
object ApiConfig {
    const val BASE_URL = "https://api.deepseek.com"
    const val MODEL = "deepseek-chat"
    const val TEMPERATURE = 0.7
    var apiKey: String = ""
}
```

#### **AiAgent** - Бизнес-логика
```kotlin
class AiAgent {
    suspend fun ask(question: String): String
    suspend fun askSafe(question: String): Result<String>
}
```

#### **ChatViewModel** - MVVM паттерн
```kotlin
class ChatViewModel : ViewModel() {
    val uiState: StateFlow<ChatUiState>
    fun sendMessage()
    fun updateInputText(text: String)
}
```

---

## 💡 Примеры использования

### Базовый пример

```kotlin
import com.example.aistudy.agent.AiAgent
import com.example.aistudy.config.ApiConfig

// Инициализация API ключа
Введите свой api_key в ApiKeyProvider.getApiKey() для каждой из платформы

// Создание агента
val agent = AiAgent()

// Отправка вопроса
val response = agent.ask("Что такое Kotlin Multiplatform?")
println(response)

// Безопасная версия с обработкой ошибок
agent.askSafe("Объясни MVVM")
    .onSuccess { println("Ответ: $it") }
    .onFailure { println("Ошибка: ${it.message}") }
```

### В Android приложении

```kotlin
// ViewModel автоматически инициализирует ключ
class ChatViewModel : ViewModel() {
    fun sendMessage() {
        viewModelScope.launch {
            aiAgent.askSafe(inputText)
                .onSuccess { response ->
                    _uiState.update { it.copy(
                        messages = it.messages + Message(response, false)
                    )}
                }
        }
    }
}
```

---

## 🔐 Безопасность

### ✅ Рекомендуется:
- Хранить API ключ в `local.properties` (для разработки)
- Использовать environment variables (для CI/CD)
- Использовать backend proxy (для production)
- Регулярно ротировать ключи

### ❌ Не делайте:
- Не коммитьте `local.properties` в Git
- Не хардкодите ключи в исходном коде
- Не делитесь ключами публично
- Не используйте один ключ для всех окружений

---

## 📚 Документация

### Основные гайды

- 📖 [**DEEPSEEK_SETUP.md**](DEEPSEEK_SETUP.md) — Пошаговая настройка DeepSeek API
- ⚡ [**QUICKSTART.md**](QUICKSTART.md) — Быстрый старт за 3 шага
- 🏗️ [**AI_AGENT_README.md**](AI_AGENT_README.md) — Подробная архитектура

## 📄 Лицензия

Распространяется под лицензией MIT. См. `LICENSE` для деталей.

---

<div align="center">

⭐ Поставьте звезду, если проект был полезен!

</div>