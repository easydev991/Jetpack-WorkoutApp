package com.workout.jetpack_workoutapp

import android.app.Application
import com.workout.jetpack_workoutapp.data.AppContainer
import com.workout.jetpack_workoutapp.data.DefaultAppContainer

class JetpackWorkoutApplication: Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}