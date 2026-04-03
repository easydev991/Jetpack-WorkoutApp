package com.swparks.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

@Composable
fun SearchCityButton(
    cityName: String?,
    onClick: () -> Unit,
    onClearClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    TextField(
        value = cityName ?: "",
        onValueChange = { },
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.spacing_regular))
                .clickable { onClick() },
        enabled = false,
        readOnly = true,
        placeholder = {
            Text(
                text = stringResource(R.string.select_city),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (onClearClick != null && cityName != null) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors =
            TextFieldDefaults.colors(
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledIndicatorColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
        singleLine = true,
        shape = RoundedCornerShape(dimensionResource(R.dimen.spacing_xsmall)),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
    )
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
fun SearchCityButtonEmptyPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            SearchCityButton(
                cityName = null,
                onClick = { },
                onClearClick = null,
                modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small))
            )
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
fun SearchCityButtonWithCityPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            SearchCityButton(
                cityName = "Москва",
                onClick = { },
                onClearClick = { },
                modifier = Modifier.padding(dimensionResource(R.dimen.spacing_small))
            )
        }
    }
}
