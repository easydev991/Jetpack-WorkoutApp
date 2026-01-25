package com.swparks

import android.app.Application
import com.swparks.data.AppContainer
import com.swparks.data.DefaultAppContainer
import com.swparks.util.AndroidLogger
import com.swparks.util.Logger

class JetpackWorkoutApplication : Application() {
    lateinit var container: AppContainer

    val logger: Logger
        get() = AndroidLogger()

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(context = this)
    }
}