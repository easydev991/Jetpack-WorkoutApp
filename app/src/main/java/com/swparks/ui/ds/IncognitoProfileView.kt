package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Вьюшка, закрывающая фичу для неавторизованного пользователя
 *
 * @param modifier Модификатор
 * @param enabled Доступность кнопок
 * @param onClickAuth Действие по нажатию на кнопку "Авторизация"
 * @param onClickRegister Действие по нажатию на кнопку "Регистрация"
 */
@Composable
fun IncognitoProfileView(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClickAuth: () -> Unit,
    onClickRegister: () -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(id = R.string.auth_invitation),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.spacing_medium_plus))
        )
        SWButton(
            config = ButtonConfig(
                text = stringResource(id = R.string.authorization),
                enabled = enabled,
                onClick = onClickAuth
            )
        )
        Spacer(modifier = Modifier.padding(dimensionResource(id = R.dimen.spacing_small)))
        SWButton(
            config = ButtonConfig(
                mode = SWButtonMode.TINTED,
                text = stringResource(id = R.string.register),
                enabled = enabled,
                onClick = onClickRegister
            )
        )
    }
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
fun IncognitoProfileViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            IncognitoProfileView(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_regular)),
                onClickAuth = {}
            )
        }
    }
}