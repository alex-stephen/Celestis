package com.example.astrolume

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.astrolume.database.AppDatabase
import com.example.astrolume.database.DatabaseDriverFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val driverFactory = DatabaseDriverFactory(applicationContext)
        val driver = driverFactory.createDriver()

        val database = AppDatabase(driver)

        setContent {
            App(database)
        }
    }
}