package com.swparks.ui.ds

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.theme.JetpackWorkoutAppTheme

/**
 * Обертка для добавления мелкого заголовка контенту
 *
 * @param modifier Модификатор
 * @param titleID Идентификатор локализованной строки для заголовка (null - без заголовка)
 * @param addPaddingToTitle Нужно ли добавлять паддинг слева от заголовка
 * @param titleBottomPadding Паддинг снизу от заголовка, по умолчанию 2
 * @param content Контент
 */
@Composable
fun SectionView(
    modifier: Modifier = Modifier,
    @StringRes titleID: Int? = null,
    addPaddingToTitle: Boolean = true,
    titleBottomPadding: Dp = dimensionResource(id = R.dimen.spacing_small),
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(titleBottomPadding)
    ) {
        if (titleID != null) {
            Text(
                text = stringResource(id = titleID).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = if (addPaddingToTitle) dimensionResource(id = R.dimen.spacing_small) else 0.dp
                )
            )
        }
        content()
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun SectionViewPreview() {
    JetpackWorkoutAppTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_regular))
        ) {
            SectionView(
                titleID = R.string.about_app,
                addPaddingToTitle = false
            ) {
                ListRowView(data = ListRowData(leadingText = "Text"))
            }
            HorizontalDivider()
            SectionView(
                titleID = R.string.friends,
                titleBottomPadding = dimensionResource(R.dimen.spacing_xxsmall)
            ) {
                UserRowView(
                    data = UserRowData(
                        imageStringURL = null,
                        name = "Alica",
                        address = "Россия, Арзамас"
                    )
                )
            }
            HorizontalDivider()
            SectionView(
                titleID = R.string.requests,
                titleBottomPadding = dimensionResource(R.dimen.spacing_xxsmall)
            ) {
                FriendRequestRowView(
                    data = FriendRequestData(
                        imageStringURL = null,
                        name = "Alica",
                        address = "Россия, Арзамас",
                        onClickAccept = {},
                        onClickDecline = {}
                    )
                )
            }
        }
    }
}
