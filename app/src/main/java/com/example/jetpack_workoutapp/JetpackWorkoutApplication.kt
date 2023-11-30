package com.example.jetpack_workoutapp

import android.app.Application
import com.example.jetpack_workoutapp.data.AppContainer
import com.example.jetpack_workoutapp.data.DefaultAppContainer

class JetpackWorkoutApplication: Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}