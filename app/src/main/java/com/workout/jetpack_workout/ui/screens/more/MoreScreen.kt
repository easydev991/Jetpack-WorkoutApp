package com.workout.jetpack_workout.ui.screens.more

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.workout.jetpack_workout.BuildConfig
import com.workout.jetpack_workout.R
import com.workout.jetpack_workout.ui.ds.ListRowView
import com.workout.jetpack_workout.ui.ds.SectionView
import com.workout.jetpack_workout.ui.theme.JetpackWorkoutAppTheme

private object Links {
    const val rateApp = "https://workout.su/android"
    const val termsOfUse = "https://workout.su/pravila"
    const val officialSite = "https://workout.su"
    const val appDeveloper = "https://t.me/easy_dev991"
    const val workoutShop = "https://workoutshop.ru"
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.more))
                },
                windowInsets = WindowInsets(top = 0)
            )
        }
    ) {
        ScreenContent(
            modifier = Modifier.padding(it),
            context = context,
            uriHandler = uriHandler
        )
    }
}

@Composable
private fun ScreenContent(
    modifier: Modifier = Modifier,
    context: Context,
    uriHandler: UriHandler
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            SectionView(
                titleID = R.string.about_app,
                addPaddingToTitle = false,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ListRowView(
                        leadingText = stringResource(id = R.string.send_feedback),
                        showChevron = true,
                        modifier = Modifier.clickable {
                            didTapSendFeedback(context)
                        }
                    )
                    ListRowView(
                        leadingText = stringResource(id = R.string.rate_app),
                        showChevron = true,
                        modifier = Modifier.clickable {
                            uriHandler.openUri(Links.rateApp)
                        }
                    )
                    ListRowView(
                        leadingText = stringResource(id = R.string.terms_of_use),
                        showChevron = true,
                        modifier = Modifier.clickable {
                            uriHandler.openUri(Links.termsOfUse)
                        }
                    )
                    ListRowView(
                        leadingText = stringResource(id = R.string.official_site),
                        showChevron = true,
                        modifier = Modifier.clickable {
                            uriHandler.openUri(Links.officialSite)
                        }
                    )
                    ListRowView(
                        leadingText = stringResource(id = R.string.app_developer),
                        showChevron = true,
                        modifier = Modifier.clickable {
                            uriHandler.openUri(Links.appDeveloper)
                        }
                    )
                    ListRowView(
                        leadingText = stringResource(id = R.string.app_version),
                        trailingText = BuildConfig.VERSION_NAME
                    )
                }
            }
            Divider()
            SectionView(
                titleID = R.string.support_project,
                addPaddingToTitle = false,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                ListRowView(
                    leadingText = stringResource(id = R.string.workout_shop),
                    showChevron = true,
                    modifier = Modifier
                        .clickable {
                            uriHandler.openUri(Links.workoutShop)
                        }
                )
            }
        }
    }
}

private fun didTapSendFeedback(context: Context) {
    val tag = "MoreScreen_Feedback"
    try {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(
            Intent.EXTRA_EMAIL,
            Feedback.recipient
        )
        intent.putExtra(
            Intent.EXTRA_SUBJECT,
            Feedback.subject
        )
        intent.putExtra(
            Intent.EXTRA_TEXT,
            Feedback.body
        )
        intent.setType(Feedback.intentType)
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.choose_email_client)
            )
        )
    } catch (e: ActivityNotFoundException) {
        // Нет приложения для отправки письма
        e.localizedMessage?.let {
            Log.e(
                tag,
                it
            )
            Toast.makeText(
                context,
                it,
                Toast.LENGTH_LONG
            ).show()
        }
    } catch (t: Throwable) {
        t.localizedMessage?.let {
            Log.e(
                tag,
                it
            )
            Toast.makeText(
                context,
                it,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun MoreScreenPreview() {
    JetpackWorkoutAppTheme {
        MoreScreen()
    }
}