# 📦 Изменения при переходе на DeepSeek API

## Что изменилось

### ✅ Добавлено

#### 1. Централизованная конфигурация API
```
shared/src/commonMain/kotlin/com/example/aistudy/config/
├── ApiConfig.kt           # Единая точка конфигурации
├── ApiKeyProvider.kt      # Expect/actual для платформ
└── (platform implementations)
```

**ApiConfig.kt** - центральное место для всех настроек:
- Base URL (DeepSeek API endpoint)
- Модель (deepseek-chat / deepseek-coder)
- Температура и max tokens
- API ключ (устанавливается один раз)

#### 2. Безопасное хранение API ключа

**local.properties.template** - шаблон для локальной конфигурации:
```properties
deepseek.api.key=YOUR_DEEPSEEK_API_KEY
```

**Преимущества:**
- ✅ Ключ не хардкодится в коде
- ✅ Уже в .gitignore (не попадет в Git)
- ✅ Легко менять без пересборки
- ✅ Работает локально для всех разработчиков

#### 3. Platform-specific провайдеры

**ApiKeyProvider** (expect/actual):
- **Android**: Читает из local.properties
- **iOS**: Читает из Config.plist
- Расширяемо для других платформ

### 🔄 Изменено

#### 1. AiRepository
**Было:**
```kotlin
class AiRepository(
    private val apiKey: String = "YOUR_API_KEY_HERE",
    private val baseUrl: String = "https://api.openai.com/v1"
)
```

**Стало:**
```kotlin
class AiRepository {
    // Использует ApiConfig для всех настроек
    // Автоматическая проверка наличия ключа
}
```

#### 2. AiAgent
**Было:**
```kotlin
class AiAgent(
    private val apiKey: String = "YOUR_API_KEY_HERE"
)
```

**Стало:**
```kotlin
class AiAgent {
    // Ключ берется из ApiConfig
    // Не нужно передавать в конструктор
}
```

#### 3. ChatViewModel (Android)
**Было:**
```kotlin
private val aiAgent = AiAgent(apiKey = "YOUR_API_KEY_HERE")
```

**Стало:**
```kotlin
private val aiAgent = AiAgent()

init {
    val apiKey = ApiKeyProvider.getApiKey()
    if (apiKey.isNotEmpty()) {
        ApiConfig.initialize(apiKey)
    }
}
```

#### 4. ContentView (iOS)
**Было:**
```swift
let agent = AiAgent(apiKey: "YOUR_API_KEY_HERE")
```

**Стало:**
```swift
let agent = AiAgent()

// В sendMessage() перед использованием:
if !ApiConfig.shared.isConfigured() {
    let apiKey = ApiKeyProvider.shared.getApiKey()
    ApiConfig.shared.initialize(key: apiKey)
}
```

### 🎯 API Endpoint

**Было:**
```
https://api.openai.com/v1/chat/completions
```

**Стало:**
```
https://api.deepseek.com/v1/chat/completions
```

**Модель:**
- Было: `gpt-3.5-turbo`
- Стало: `deepseek-chat`

## 📋 Чеклист миграции

### Для разработчиков

- [x] ✅ Код обновлен на использование ApiConfig
- [x] ✅ Удалены хардкоженные API ключи
- [x] ✅ Создан local.properties.template
- [x] ✅ .gitignore проверен (local.properties есть)

### Для пользователей

- [ ] Получить API ключ DeepSeek
- [ ] Скопировать `local.properties.template` → `local.properties`
- [ ] Вставить свой API ключ в `local.properties`
- [ ] Запустить приложение

## 🔄 Откат на OpenAI (если нужно)

### 1. Измените ApiConfig.kt:
```kotlin
object ApiConfig {
    const val BASE_URL = "https://api.openai.com"  // Изменить
    const val MODEL = "gpt-3.5-turbo"              // Изменить
    // ...
}
```

### 2. Используйте OpenAI ключ:
```properties
# В local.properties
deepseek.api.key=sk-ваш-openai-ключ
```

## 📁 Структура изменений

```
+ shared/src/commonMain/kotlin/com/example/aistudy/config/
    + ApiConfig.kt           ← Новый файл
    + ApiKeyProvider.kt      ← Новый файл

+ shared/src/androidMain/kotlin/com/example/aistudy/config/
    + ApiKeyProvider.android.kt  ← Новый файл

+ shared/src/iosMain/kotlin/com/example/aistudy/config/
    + ApiKeyProvider.ios.kt      ← Новый файл

~ shared/src/commonMain/kotlin/com/example/aistudy/repository/AiRepository.kt
    - Удален параметр apiKey из конструктора
    + Используется ApiConfig
    + Проверка isConfigured()

~ shared/src/commonMain/kotlin/com/example/aistudy/agent/AiAgent.kt
    - Удален параметр apiKey
    + Конструктор без параметров

~ composeApp/src/commonMain/kotlin/com/example/aistudy/viewmodel/ChatViewModel.kt
    + Инициализация ApiConfig в init{}
    + Import ApiConfig и ApiKeyProvider

~ iosApp/iosApp/ContentView.swift
    + Проверка и инициализация ApiConfig
    + Обработка случая отсутствия ключа

+ local.properties.template  ← Новый файл
+ DEEPSEEK_SETUP.md         ← Новая документация
+ MIGRATION_TO_DEEPSEEK.md  ← Этот файл
```

## 💡 Преимущества новой архитектуры

### 1. Безопасность
- ❌ Нет хардкоженных ключей в коде
- ✅ Ключ в local.properties (не коммитится)
- ✅ Легко менять без пересборки

### 2. Удобство
- 🎯 Один конфигурационный файл
- 🔄 Легко переключаться между провайдерами
- 🛠️ Централизованные настройки

### 3. Гибкость
- 🔌 Поддержка любого OpenAI-compatible API
- 🎛️ Легко менять модель и параметры
- 📱 Platform-specific реализации

### 4. Масштабируемость
- 🏗️ Чистая архитектура
- 🧩 Расширяемо для новых платформ
- 📦 Модульная структура

## 🚀 Быстрый старт после миграции

```bash
# 1. Скопируйте template
cp local.properties.template local.properties

# 2. Откройте и вставьте ваш DeepSeek API ключ
# deepseek.api.key=sk-ваш-ключ-здесь

# 3. Запустите приложение
./gradlew :composeApp:installDebug   # Android
# или
open iosApp/iosApp.xcodeproj          # iOS
```

## 📚 Документация

- **DEEPSEEK_SETUP.md** - Полная инструкция по настройке DeepSeek
- **QUICKSTART.md** - Быстрый старт (обновлен под DeepSeek)
- **AI_AGENT_README.md** - Общая документация проекта

## ❓ FAQ

### Q: Нужно ли менять код для использования других API?
**A:** Нет! Просто измените `BASE_URL` и `MODEL` в `ApiConfig.kt`.

### Q: Как работает с существующим local.properties?
**A:** Просто добавьте строку `deepseek.api.key=...` в ваш файл.

### Q: Безопасно ли хранить ключ в local.properties?
**A:** Да, для локальной разработки. Файл в .gitignore и не попадет в Git.

### Q: Что делать для production?
**A:** Используйте:
- Environment variables
- Secrets management (AWS Secrets Manager, etc.)
- Backend proxy (рекомендуется)

### Q: Могу ли я использовать старый код?
**A:** Старый код работать не будет. Нужно обновить согласно этой инструкции.

### Q: Как вернуться на OpenAI?
**A:** Измените `BASE_URL` и `MODEL` в `ApiConfig.kt` и используйте OpenAI ключ.

---

**Все изменения обратно совместимы по архитектуре!**
Просто нужно один раз настроить API ключ в `local.properties`.