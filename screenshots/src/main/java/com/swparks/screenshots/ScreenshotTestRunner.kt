package com.swparks.screenshots

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class ScreenshotTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader,
        className: String,
        context: Context
    ): Application = super.newApplication(cl, ScreenshotTestApplication::class.java.name, context)
}
