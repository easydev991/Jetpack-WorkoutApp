package com.swparks.ui.screen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.swparks.R
import com.swparks.model.Park
import com.swparks.ui.ds.ParkRowData
import com.swparks.ui.ds.ParkRowView

/**
 * Компонент для отображения списка площадок.
 *
 * @param parks Список площадок для отображения
 * @param onParkClick Замыкание, вызываемое при клике на площадку
 * @param modifier Modifier для настройки внешнего вида
 */
@Composable
fun ParksListView(
    parks: List<Park>,
    onParkClick: (Park) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding =
            PaddingValues(
                start = dimensionResource(R.dimen.spacing_regular),
                top = dimensionResource(R.dimen.spacing_small),
                end = dimensionResource(R.dimen.spacing_regular),
                bottom = dimensionResource(R.dimen.spacing_regular)
            ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
        horizontalAlignment = Alignment.Start,
    ) {
        items(parks, key = { it.id }) { park ->
            ParkRowView(
                data =
                    ParkRowData(
                        imageStringURL = park.preview,
                        name = park.name,
                        address = park.address,
                        peopleTrainCount = park.trainingUsersCount ?: 0,
                        onClick = { onParkClick(park) }
                    )
            )
        }
    }
}
