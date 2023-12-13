package com.workout.jetpack_workout

import android.app.Application
import com.workout.jetpack_workout.data.AppContainer
import com.workout.jetpack_workout.data.DefaultAppContainer

class JetpackWorkoutApplication: Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(context = this)
    }
}