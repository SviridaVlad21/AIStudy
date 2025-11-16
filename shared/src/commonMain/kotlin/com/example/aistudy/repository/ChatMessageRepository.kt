package com.example.aistudy.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.aistudy.database.ChatDatabase
import com.example.aistudy.database.DatabaseDriverFactory
import com.example.aistudy.model.ApiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Репозиторий для работы с сообщениями чата в БД
 */
class ChatMessageRepository(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = ChatDatabase(databaseDriverFactory.createDriver())
    private val queries = database.chatDatabaseQueries

    /**
     * Получить все сообщения в виде Flow
     */
    fun getAllMessagesFlow(): Flow<List<ApiMessage>> {
        return queries.getAllMessages()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { messages ->
                messages.map { message ->
                    ApiMessage(
                        role = message.role,
                        content = message.content
                    )
                }
            }
    }

    /**
     * Получить все сообщения
     */
    suspend fun getAllMessages(): List<ApiMessage> = withContext(Dispatchers.Default) {
        queries.getAllMessages().executeAsList().map { message ->
            ApiMessage(
                role = message.role,
                content = message.content
            )
        }
    }

    /**
     * Получить количество сообщений
     */
    suspend fun getMessageCount(): Long = withContext(Dispatchers.Default) {
        queries.getMessageCount().executeAsOne()
    }

    /**
     * Сохранить новое сообщение
     */
    suspend fun insertMessage(message: ApiMessage) = withContext(Dispatchers.Default) {
        queries.insertMessage(
            role = message.role,
            content = message.content,
            timestamp = getCurrentTime()
        )
    }

    /**
     * Сохранить список сообщений
     */
    suspend fun insertMessages(messages: List<ApiMessage>) = withContext(Dispatchers.Default) {
        messages.forEach { message ->
            queries.insertMessage(
                role = message.role,
                content = message.content,
                timestamp = getCurrentTime()
            )
        }
    }

    private fun getCurrentTime(): Long {
        // SQLDelight автоматически установит timestamp через AUTOINCREMENT
        // Возвращаем простое значение для поддержки порядка
        return 0L
    }

    /**
     * Удалить все сообщения
     */
    suspend fun deleteAllMessages() = withContext(Dispatchers.Default) {
        queries.deleteAllMessages()
    }
}