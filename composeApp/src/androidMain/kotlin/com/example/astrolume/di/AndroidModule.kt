package com.example.astrolume.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.astrolume.database.AppDatabase
import org.koin.dsl.module

val androidModule = module {
    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = AppDatabase.Schema,
            context = get(),
            name = "astrolume.db"
        )
    }
}