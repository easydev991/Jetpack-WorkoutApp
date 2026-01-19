package com.swparks.ui.screens.parks

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.model.Park
import com.swparks.ui.ds.ParkRowView
import com.swparks.ui.theme.JetpackWorkoutAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParksRootScreen(
    parks: List<Park>,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(R.string.parks_title, parks.size.toString()))
                },
                windowInsets = WindowInsets(top = 0),
                actions = {
                    IconButton(
                        onClick = {
                            Log.d("ParksRootScreen", "Кнопка фильтрации нажата")
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier,
            contentPadding = PaddingValues(
                start = 16.dp,
                top = paddingValues.calculateTopPadding(),
                end = 16.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            items(parks, key = { it.id }) { park ->
                ParkRowView(
                    imageStringURL = park.preview,
                    name = park.name,
                    address = park.address,
                    peopleTrainCount = park.trainingUsersCount ?: 0,
                    onClick = {
                        Log.d("ParksRootScreen", "Нажата площадка: ${park.name}")
                    }
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    locale = "ru"
)
@Composable
fun ParksRootScreenPreview() {
    JetpackWorkoutAppTheme {
        ParksRootScreen(
            parks = listOf(
                Park(
                    id = 1,
                    name = "Тестовая площадка",
                    sizeID = 1,
                    typeID = 1,
                    longitude = "0.0",
                    latitude = "0.0",
                    address = "Тестовый адрес",
                    cityID = 1,
                    countryID = 1,
                    preview = "",
                    trainingUsersCount = 5
                )
            )
        )
    }
}