package com.swparks.ui.screens.more

import android.content.Context
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.swparks.BuildConfig
import com.swparks.R
import com.swparks.navigation.Screen
import com.swparks.ui.ds.ListRowData
import com.swparks.ui.ds.ListRowView
import com.swparks.ui.ds.SectionView
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.util.AppConstants

private object Links {
    const val rateApp = AppConstants.APP_RATE_URL
    const val officialSite = "https://workout.su"
    const val appDeveloper = "https://t.me/easy_dev991"
    const val workoutShop = "https://workoutshop.ru"
}

@Composable
fun MoreScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController? = null
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    ScreenContent(
        modifier = modifier,
        context = context,
        uriHandler = uriHandler,
        navController = navController
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreTopAppBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(text = stringResource(id = R.string.more))
        },
    )
}

@Composable
private fun ScreenContent(
    modifier: Modifier = Modifier,
    context: Context,
    uriHandler: UriHandler,
    navController: NavHostController? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_small_plus)),
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsSection(navController = navController)
        HorizontalDivider()
        AboutAppSection(
            context = context,
            uriHandler = uriHandler
        )
        HorizontalDivider()
        OtherAppsSection(uriHandler = uriHandler)
        HorizontalDivider()
        SupportProjectSection(uriHandler = uriHandler)
    }
}

@Composable
private fun SettingsSection(
    navController: NavHostController?
) {
    SectionView(
        titleID = R.string.settings,
        addPaddingToTitle = false,
        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.spacing_regular))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_xxsmall))
        ) {
            ThemeAndIconRow(navController = navController)
        }
    }
}

@Composable
private fun AboutAppSection(
    context: Context,
    uriHandler: UriHandler
) {
    SectionView(
        titleID = R.string.about_app,
        addPaddingToTitle = false,
        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.spacing_regular))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_xxsmall))
        ) {
            SendFeedbackRow(context = context)
            ExternalLinkRow(
                textResId = R.string.rate_app,
                url = Links.rateApp,
                uriHandler = uriHandler
            )
            ExternalLinkRow(
                textResId = R.string.official_site,
                url = Links.officialSite,
                uriHandler = uriHandler
            )
            ExternalLinkRow(
                textResId = R.string.app_developer,
                url = Links.appDeveloper,
                uriHandler = uriHandler
            )
            ShareAppRow(context = context)

            AppVersionRow()
        }
    }
}

@Composable
private fun SendFeedbackRow(
    context: Context
) {
    ListRowView(
        data = ListRowData(
            leadingText = stringResource(id = R.string.send_feedback),
            showChevron = true,
            modifier = Modifier.clickable {
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
        data = ListRowData(
            leadingText = stringResource(id = textResId),
            showChevron = true,
            modifier = Modifier.clickable {
                uriHandler.openUri(url)
            }
        )
    )
}

@Composable
private fun AppVersionRow() {
    ListRowView(
        data = ListRowData(
            leadingText = stringResource(id = R.string.app_version),
            trailingText = BuildConfig.VERSION_NAME
        )
    )
}

@Composable
private fun SupportProjectSection(
    uriHandler: UriHandler
) {
    SectionView(
        titleID = R.string.support_project,
        addPaddingToTitle = false,
        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.spacing_regular))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_xxsmall))
        ) {
            ExternalLinkRow(
                textResId = R.string.workout_shop,
                url = Links.workoutShop,
                uriHandler = uriHandler
            )
            GithubRow(context = LocalContext.current)
        }
    }
}

@Composable
private fun OtherAppsSection(
    uriHandler: UriHandler
) {
    SectionView(
        titleID = R.string.other_apps,
        addPaddingToTitle = false,
        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.spacing_regular))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.spacing_xxsmall))
        ) {
            DaysCounterRow(uriHandler = uriHandler)
        }
    }
}

@Composable
private fun ThemeAndIconRow(
    navController: NavHostController?
) {
    ListRowView(
        data = ListRowData(
            leadingText = stringResource(id = R.string.app_theme_and_icon),
            showChevron = true,
            modifier = Modifier.clickable {
                navController?.navigate(Screen.ThemeIcon.route)
            }
        )
    )
}

@Composable
private fun ShareAppRow(
    context: Context
) {
    ListRowView(
        data = ListRowData(
            leadingText = stringResource(id = R.string.share_the_app),
            showChevron = true,
            modifier = Modifier.clickable {
                shareApp(context)
            }
        )
    )
}

@Composable
private fun DaysCounterRow(
    uriHandler: UriHandler
) {
    ListRowView(
        data = ListRowData(
            leadingText = stringResource(id = R.string.days_counter_app),
            showChevron = true,
            modifier = Modifier.clickable {
                uriHandler.openUri(AppConstants.DAYS_COUNTER_APP_STORE_URL)
            }
        )
    )
}

@Composable
private fun GithubRow(
    context: Context
) {
    ListRowView(
        data = ListRowData(
            leadingText = stringResource(id = R.string.github_page),
            showChevron = true,
            modifier = Modifier.clickable {
                openGitHub(context)
            }
        )
    )
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun MoreScreenPreview() {
    JetpackWorkoutAppTheme {
        MoreScreen()
    }
}