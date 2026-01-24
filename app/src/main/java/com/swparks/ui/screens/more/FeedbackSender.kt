package com.swparks.ui.screens.more

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.swparks.BuildConfig
import com.swparks.R
import com.swparks.util.AppConstants

private object Feedback {
    const val recipient = "info@workout.su"
    const val subject = "Jetpack WorkoutApp: Обратная связь"
    val body = """
        Android SDK: ${Build.VERSION.SDK_INT}
        App version: ${BuildConfig.VERSION_NAME}
        Над чем нам стоит поработать?
    """.trimIndent()
    const val intentType = "message/rfc822"
}

private const val TAG = "MoreScreen_Feedback"

/**
 * Отправляет отзыв по электронной почте.
 */
fun sendFeedback(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_EMAIL, Feedback.recipient)
            putExtra(Intent.EXTRA_SUBJECT, Feedback.subject)
            putExtra(Intent.EXTRA_TEXT, Feedback.body)
            type = Feedback.intentType
        }
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.choose_email_client)
            )
        )
    } catch (e: ActivityNotFoundException) {
        logAndShowError(context, e)
    } catch (e: SecurityException) {
        logAndShowError(context, e)
    } catch (e: IllegalArgumentException) {
        logAndShowError(context, e)
    } catch (e: IllegalStateException) {
        logAndShowError(context, e)
    }
}

/**
 * Делится ссылкой на приложение через ShareSheet.
 */
fun shareApp(context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            context.getString(R.string.share_text) + "\n" + AppConstants.APP_SHARE_URL
        )
        putExtra(Intent.EXTRA_TITLE, context.getString(R.string.share_chooser_title))
    }

    try {
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.share_chooser_title)
            )
        )
    } catch (e: ActivityNotFoundException) {
        Log.e(TAG, "Не удалось открыть ShareSheet: ${e.message}")
    }
}

/**
 * Открывает репозиторий приложения на GitHub.
 */
fun openGitHub(context: Context) {
    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse(AppConstants.GITHUB_REPOSITORY_URL)
    )

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.e(TAG, "Не удалось открыть GitHub: ${e.message}")
    }
}

private fun logAndShowError(context: Context, error: Exception) {
    error.localizedMessage?.let {
        Log.e(TAG, it)
        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
    }
}
