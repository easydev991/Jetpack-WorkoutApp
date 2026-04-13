package com.swparks.ui.screens.more

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.swparks.BuildConfig
import com.swparks.R
import com.swparks.analytics.AnalyticsEvent
import com.swparks.analytics.AnalyticsService
import com.swparks.analytics.AppScreen
import com.swparks.analytics.UserActionType
import com.swparks.navigation.Screen
import com.swparks.ui.ds.ListRowData
import com.swparks.ui.ds.ListRowView
import com.swparks.ui.ds.SectionView
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.util.AppConstants
import java.util.Locale

private object Links {
    const val RATE_APP = AppConstants.APP_RATE_URL
    const val OFFICIAL_SITE = "https://workout.su"
    const val APP_DEVELOPER = "https://t.me/easy_dev991"
}

@Composable
fun MoreScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    analyticsService: AnalyticsService? = null
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var localeBeforeSettings by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner, context, analyticsService, localeBeforeSettings) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && localeBeforeSettings != null) {
                    val currentLocale = currentLocaleTag(context)
                    if (currentLocale != localeBeforeSettings) {
                        analyticsService?.log(
                            AnalyticsEvent.UserAction(UserActionType.SELECT_LANGUAGE)
                        )
                    }
                    localeBeforeSettings = null
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ScreenContent(
        modifier = modifier,
        context = context,
        uriHandler = uriHandler,
        navController = navController,
        analyticsService = analyticsService,
        onOpenLanguageSettings = {
            localeBeforeSettings = currentLocaleTag(context)
            analyticsService?.log(
                AnalyticsEvent.UserAction(UserActionType.OPEN_LANGUAGE_SETTINGS)
            )
            openLanguageSettings(context)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreTopAppBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(text = stringResource(id = R.string.more))
        }
    )
}

@Composable
private fun ScreenContent(
    modifier: Modifier = Modifier,
    context: Context,
    uriHandler: UriHandler,
    navController: NavHostController? = null,
    analyticsService: AnalyticsService? = null,
    onOpenLanguageSettings: () -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_small_plus)),
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
    ) {
        SettingsSection(
            navController = navController,
            analyticsService = analyticsService,
            onOpenLanguageSettings = onOpenLanguageSettings
        )
        HorizontalDivider()
        AboutAppSection(
            context = context,
            uriHandler = uriHandler,
            analyticsService = analyticsService
        )
        HorizontalDivider()
        OtherAppsSection(uriHandler = uriHandler)
        HorizontalDivider()
        SupportProjectSection(uriHandler = uriHandler)
    }
}

@Composable
private fun SettingsSection(
    navController: NavHostController?,
    analyticsService: AnalyticsService? = null,
    onOpenLanguageSettings: () -> Unit = {}
) {
    SectionView(
        titleID = R.string.settings,
        addPaddingToTitle = false,
        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.spacing_regular))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_xxsmall))
        ) {
            LanguageSettingsRow(onClick = onOpenLanguageSettings)
            ThemeAndIconRow(navController = navController, analyticsService = analyticsService)
        }
    }
}

@Composable
private fun LanguageSettingsRow(onClick: () -> Unit) {
    ListRowView(
        data =
            ListRowData(
                leadingText = stringResource(id = R.string.app_language),
                showChevron = true,
                modifier = Modifier.clickable(onClick = onClick)
            )
    )
}

@Composable
private fun AboutAppSection(
    context: Context,
    uriHandler: UriHandler,
    analyticsService: AnalyticsService? = null
) {
    SectionView(
        titleID = R.string.about_app,
        addPaddingToTitle = false,
        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.spacing_regular))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_xxsmall))
        ) {
            SendFeedbackRow(context = context, analyticsService = analyticsService)
            ExternalLinkRow(
                textResId = R.string.rate_app,
                url = Links.RATE_APP,
                uriHandler = uriHandler
            )
            ExternalLinkRow(
                textResId = R.string.official_site,
                url = Links.OFFICIAL_SITE,
                uriHandler = uriHandler
            )
            ExternalLinkRow(
                textResId = R.string.app_developer,
                url = Links.APP_DEVELOPER,
                uriHandler = uriHandler
            )
            ShareAppRow(context = context)

            AppVersionRow()
        }
    }
}

@Composable
private fun SendFeedbackRow(
    context: Context,
    analyticsService: AnalyticsService? = null
) {
    ListRowView(
        data =
            ListRowData(
                leadingText = stringResource(id = R.string.send_feedback),
                showChevron = true,
                modifier =
                    Modifier.clickable {
                        analyticsService?.log(
                            AnalyticsEvent.UserAction(
                                UserActionType.SEND_FEEDBACK,
                                mapOf("source" to "more")
                            )
                        )
                        sendFeedback(context)
                    }
            )
    )
}

@Composable
private fun ExternalLinkRow(
    textResId: Int,
    url: String,
    uriHandler: UriHandler
) {
    ListRowView(
        data =
            ListRowData(
                leadingText = stringResource(id = textResId),
                showChevron = true,
                modifier =
                    Modifier.clickable {
                        uriHandler.openUri(url)
                    }
            )
    )
}

@Composable
private fun AppVersionRow() {
    ListRowView(
        data =
            ListRowData(
                leadingText = stringResource(id = R.string.app_version),
                trailingText = BuildConfig.VERSION_NAME
            )
    )
}

@Composable
private fun SupportProjectSection(uriHandler: UriHandler) {
    SectionView(
        titleID = R.string.support_project,
        addPaddingToTitle = false,
        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.spacing_regular))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_xxsmall))
        ) {
            ExternalLinkRow(
                textResId = R.string.github_page,
                url = AppConstants.GITHUB_REPOSITORY_URL,
                uriHandler = uriHandler
            )
        }
    }
}

@Composable
private fun OtherAppsSection(uriHandler: UriHandler) {
    SectionView(
        titleID = R.string.other_apps,
        addPaddingToTitle = false,
        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.spacing_regular))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_xxsmall))
        ) {
            ExternalLinkRow(
                textResId = R.string.days_counter_app,
                url = AppConstants.DAYS_COUNTER_APP_STORE_URL,
                uriHandler = uriHandler
            )
        }
    }
}

@Composable
private fun ThemeAndIconRow(
    navController: NavHostController?,
    analyticsService: AnalyticsService? = null
) {
    ListRowView(
        data =
            ListRowData(
                leadingText = stringResource(id = R.string.appearance),
                showChevron = true,
                modifier =
                    Modifier.clickable {
                        analyticsService?.log(
                            AnalyticsEvent.ScreenView(AppScreen.THEME_ICON)
                        )
                        navController?.navigate(Screen.ThemeIcon.route)
                    }
            )
    )
}

@Composable
private fun ShareAppRow(context: Context) {
    ListRowView(
        data =
            ListRowData(
                leadingText = stringResource(id = R.string.share_the_app),
                showChevron = true,
                modifier =
                    Modifier.clickable {
                        shareApp(context)
                    }
            )
    )
}

private fun openLanguageSettings(context: Context) {
    val packageUri = Uri.fromParts("package", context.packageName, null)
    val localeSettingsIntent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                data = packageUri
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = packageUri
            }
        }

    runCatching {
        context.startActivity(localeSettingsIntent)
    }.onFailure {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = packageUri
            }
        )
    }
}

private fun currentLocaleTag(context: Context): String {
    val locales = context.resources.configuration.locales
    return if (!locales.isEmpty) {
        locales[0]?.toLanguageTag().orEmpty()
    } else {
        Locale.getDefault().toLanguageTag()
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun MoreScreenPreview() {
    JetpackWorkoutAppTheme {
        MoreScreen()
    }
}
