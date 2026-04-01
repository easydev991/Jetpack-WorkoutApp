package com.swparks

import android.app.Application
import com.swparks.data.AppContainer
import com.swparks.data.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class JetpackWorkoutApplication : Application() {
    lateinit var container: AppContainer

    // Scope для операций при старте приложения
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(context = this)

        // Загружаем токен из DataStore в память при старте приложения
        // Это нужно для синхронного доступа из TokenInterceptor
        applicationScope.launch {
            container.secureTokenRepository.loadTokenToCache()
        }
    }
}
