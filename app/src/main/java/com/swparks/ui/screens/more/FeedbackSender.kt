package com.swparks.ui.screens.more

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import com.swparks.BuildConfig
import com.swparks.R
import com.swparks.util.AppConstants
import com.swparks.util.Complaint
import com.swparks.util.Feedback
import com.swparks.util.LocationFeedback

private object FeedbackData {
    const val SUBJECT = "Jetpack WorkoutApp: Обратная связь"
    val BODY =
        """
        Android SDK: ${Build.VERSION.SDK_INT}
        App version: ${BuildConfig.VERSION_NAME}
        Над чем нам стоит поработать?
        """.trimIndent()
}

private const val TAG = "FeedbackSender"

/**
 * Отправляет отзыв по электронной почте.
 */
fun sendFeedback(context: Context) {
    val encodedSubject = Uri.encode(FeedbackData.SUBJECT)
    val encodedBody = Uri.encode(FeedbackData.BODY)
    val uri =
        "mailto:${Feedback.recipients.joinToString(",")}?subject=$encodedSubject&body=$encodedBody".toUri()
    val intent = Intent(Intent.ACTION_SENDTO, uri)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.e(TAG, "Почтовый клиент не найден", e)
        Toast.makeText(context, R.string.no_email_client, Toast.LENGTH_LONG).show()
    }
}

/**
 * Отправляет жалобу на контент по электронной почте.
 *
 * @param complaint Модель жалобы с темой и телом письма
 * @param context Android Context
 */
fun sendComplaint(
    complaint: Complaint,
    context: Context
) {
    val encodedSubject = Uri.encode(complaint.subject)
    val encodedBody = Uri.encode(complaint.body)
    val uri =
        "mailto:${Feedback.recipients.joinToString(",")}?subject=$encodedSubject&body=$encodedBody".toUri()
    val intent = Intent(Intent.ACTION_SENDTO, uri)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.e(TAG, "Почтовый клиент не найден", e)
        Toast.makeText(context, R.string.no_email_client, Toast.LENGTH_LONG).show()
    }
}

fun sendLocationFeedback(
    context: Context,
    feedback: LocationFeedback
) {
    val encodedSubject = Uri.encode(feedback.subject)
    val encodedBody = Uri.encode(feedback.body)
    val uri =
        "mailto:${Feedback.recipients.joinToString(",")}?subject=$encodedSubject&body=$encodedBody".toUri()
    val intent = Intent(Intent.ACTION_SENDTO, uri)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.e(TAG, "Почтовый клиент не найден", e)
        Toast.makeText(context, R.string.no_email_client, Toast.LENGTH_LONG).show()
    }
}

/**
 * Делится ссылкой на приложение через ShareSheet.
 */
fun shareApp(context: Context) {
    val intent =
        Intent(Intent.ACTION_SEND).apply {
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
