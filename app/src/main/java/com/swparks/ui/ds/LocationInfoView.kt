package com.swparks.ui.ds

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Конфигурация для компонента отображения локации.
 *
 * @param latitude Широта (может быть null, если координаты неизвестны)
 * @param longitude Долгота (может быть null, если координаты неизвестны)
 * @param address Текстовый адрес (не отображается в UI, используется для fallback)
 * @param onOpenMapClick Callback при нажатии на кнопку "Открыть на карте"
 * @param onRouteClick Callback при нажатии на кнопку "Построить маршрут"
 */
data class LocationInfoConfig(
    val latitude: String?,
    val longitude: String?,
    val address: String,
    val onOpenMapClick: () -> Unit,
    val onRouteClick: () -> Unit
)

/**
 * Компонент для отображения действий с локацией мероприятия.
 *
 * Содержит:
 * - Кнопки "Открыть на карте" и "Построить маршрут"
 *
 * Адрес отображается отдельно в родительском компоненте.
 *
 * @param config Конфигурация компонента [LocationInfoConfig]
 * @param modifier Модификатор
 */
@Composable
fun LocationInfoView(
    config: LocationInfoConfig,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
    ) {
        SWButton(
            config = ButtonConfig(
                modifier = Modifier.weight(1f),
                size = SWButtonSize.SMALL,
                mode = SWButtonMode.TINTED,
                imageVector = Icons.Default.Map,
                text = stringResource(R.string.event_open_map),
                onClick = config.onOpenMapClick
            )
        )

        SWButton(
            config = ButtonConfig(
                modifier = Modifier.weight(1f),
                size = SWButtonSize.SMALL,
                mode = SWButtonMode.TINTED,
                imageVector = Icons.Default.Navigation,
                text = stringResource(R.string.event_build_route),
                onClick = config.onRouteClick
            )
        )
    }
}

@Preview(
    showBackground = true,
    locale = "ru"
)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
fun LocationInfoViewPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            LocationInfoView(
                config = LocationInfoConfig(
                    latitude = "55.7558",
                    longitude = "37.6173",
                    address = "Россия, Москва, Парк Горького",
                    onOpenMapClick = {},
                    onRouteClick = {}
                ),
                modifier = Modifier.padding(dimensionResource(R.dimen.spacing_regular))
            )
        }
    }
}
