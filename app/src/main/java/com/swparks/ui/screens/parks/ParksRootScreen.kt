package com.swparks.ui.screens.parks

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.swparks.R
import com.swparks.model.Park
import com.swparks.ui.ds.ParkRowData
import com.swparks.ui.ds.ParkRowView

@Composable
fun ParksRootScreen(
    parks: List<Park>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
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
                data = ParkRowData(
                    imageStringURL = park.preview,
                    name = park.name,
                    address = park.address,
                    peopleTrainCount = park.trainingUsersCount ?: 0,
                    onClick = {
                        Log.d("ParksRootScreen", "Нажата площадка: ${park.name}")
                    }
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParksTopAppBar(
    parksCount: Int,
    onFilterClick: () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(text = stringResource(R.string.parks_title, parksCount.toString()))
        },
        actions = {
            IconButton(
                onClick = {
                    Log.d("ParksRootScreen", "Кнопка фильтрации нажата")
                    onFilterClick()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.FilterAlt,
                    contentDescription = stringResource(R.string.filter_parks)
                )
            }
        }
    )
}