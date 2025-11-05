package com.example.aistudy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.aistudy.model.Message
import com.example.aistudy.viewmodel.ChatUiState

/**
 * Главный экран чата
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onErrorDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Автопрокрутка к последнему сообщению
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Chat Assistant") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = {
            // Показываем ошибки
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = onErrorDismiss) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Список сообщений
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Начните разговор с AI ассистентом",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(uiState.messages) { message ->
                    MessageBubble(message = message)
                }

                // Индикатор загрузки
                if (uiState.isLoading) {
                    item {
                        LoadingIndicator()
                    }
                }
            }

            // Поле ввода
            InputSection(
                text = uiState.inputText,
                onTextChange = onInputChange,
                onSendClick = onSendClick,
                enabled = !uiState.isLoading
            )
        }
    }
}

/**
 * Компонент для отображения сообщения
 */
@Composable
fun MessageBubble(message: Message) {
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (message.isFromUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Заголовок (Вы или AI)
                Text(
                    text = if (message.isFromUser) "Вы" else "AI",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (message.isFromUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )

                // Для пользователя показываем только текст
                if (message.isFromUser) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    // Для AI показываем структурированные данные
                    message.structuredData?.let { data ->
                        // 1. Вопрос (если есть)
                        if (data.question.isNotEmpty()) {
                            StructuredField(
                                label = "Вопрос",
                                value = data.question
                            )
                        }

                        // 2. Краткий ответ
                        if (data.summary.isNotEmpty()) {
                            StructuredField(
                                label = "Краткий ответ",
                                value = data.summary,
                                emphasized = true
                            )
                        }

                        // 3. Подробное объяснение
                        if (data.explanation.isNotEmpty()) {
                            StructuredField(
                                label = "Подробно",
                                value = data.explanation
                            )
                        }

                        // 4. Пример кода (если есть)
                        if (data.code_example.isNotEmpty()) {
                            StructuredField(
                                label = "Пример кода",
                                value = data.code_example,
                                isCode = true
                            )
                        }

                        // 5. Источники (ссылки)
                        if (data.sources.isNotEmpty()) {
                            Text(
                                text = "Источники:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            data.sources.forEach { source ->
                                Text(
                                    text = source,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        try {
                                            uriHandler.openUri(source)
                                        } catch (e: Exception) {
                                            // Игнорируем ошибки открытия ссылки
                                        }
                                    }
                                )
                            }
                        }

                        // 6. Уверенность
                        if (data.confidence.isNotEmpty()) {
                            Text(
                                text = "Уверенность: ${data.confidence}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    } ?: run {
                        // Если структурированных данных нет, показываем обычный текст
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Компонент для отображения одного поля структурированного ответа
 */
@Composable
fun StructuredField(
    label: String,
    value: String,
    emphasized: Boolean = false,
    isCode: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = value,
            style = if (isCode) {
                MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            fontWeight = if (emphasized) FontWeight.Medium else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * Индикатор загрузки
 */
@Composable
fun LoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.widthIn(max = 100.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "AI думает...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

/**
 * Секция ввода сообщения
 */
@Composable
fun InputSection(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Введите ваш вопрос...") },
                enabled = enabled,
                maxLines = 4
            )

            Button(
                onClick = onSendClick,
                enabled = enabled && text.isNotBlank(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Отправить")
            }
        }
    }
}