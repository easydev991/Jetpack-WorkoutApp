package com.swparks.util

import android.content.Context
import android.os.Build
import com.swparks.R

sealed class LocationFeedback {
    abstract val subject: String
    abstract val body: String

    data class Country(
        override val subject: String,
        override val body: String
    ) : LocationFeedback()

    data class City(
        override val subject: String,
        override val body: String
    ) : LocationFeedback()

    companion object {
        fun createCountry(context: Context): Country {
            val appVersion = AppVersionProvider.getVersion(context)
            val androidSdk = Build.VERSION.SDK_INT.toString()
            val question = context.getString(R.string.feedback_country_question)
            return Country(
                subject = context.getString(R.string.feedback_email_subject_country),
                body = buildBody(appVersion, androidSdk, question)
            )
        }

        fun createCity(context: Context): City {
            val appVersion = AppVersionProvider.getVersion(context)
            val androidSdk = Build.VERSION.SDK_INT.toString()
            val question = context.getString(R.string.feedback_city_question)
            return City(
                subject = context.getString(R.string.feedback_email_subject_city),
                body = buildBody(appVersion, androidSdk, question)
            )
        }

        private fun buildBody(appVersion: String, androidSdk: String, question: String): String {
            return "Android SDK: $androidSdk\nApp Version: $appVersion\n\n$question"
        }
    }
}
