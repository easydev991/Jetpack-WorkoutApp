package com.swparks.ui.ds

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.swparks.R

/**
 * Нижний бар для кнопок форм, который поднимается над клавиатурой через `imePadding()`.
 *
 * Важно: на экранах, где используется этот бар в `Scaffold.bottomBar`, глобальный BottomNavigation
 * (из RootScreen) должен быть скрыт, иначе появится двойной нижний отступ.
 */
@Composable
fun KeyboardAwareBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_regular),
                vertical = dimensionResource(R.dimen.spacing_regular)
            )
    ) {
        content()
    }
}
