package com.example.aistudy.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS реализация DatabaseDriverFactory
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = ChatDatabase.Schema,
            name = "chat.db"
        )
    }
}