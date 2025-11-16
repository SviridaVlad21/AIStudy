package com.example.aistudy.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Фабрика для создания SqlDriver на разных платформах
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}