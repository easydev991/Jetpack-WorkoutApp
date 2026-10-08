package com.swparks.testing

import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Тап по верхней кромке экрана — единственная видимая зона затемнения
 * вне полноэкранного модального листа.
 */
fun tapOutside() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val metrics = instrumentation.targetContext.resources.displayMetrics
    val down =
        MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN,
            metrics.widthPixels / 2f,
            10f,
            0
        )
    val up =
        MotionEvent.obtain(
            down.downTime,
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_UP,
            down.x,
            down.y,
            0
        )
    try {
        instrumentation.uiAutomation.injectInputEvent(down, true)
        instrumentation.uiAutomation.injectInputEvent(up, true)
    } finally {
        down.recycle()
        up.recycle()
    }
}
