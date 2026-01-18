package com.swparks

import android.app.Application
import com.swparks.data.AppContainer
import com.swparks.data.DefaultAppContainer

class JetpackWorkoutApplication: Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(context = this)
    }
}