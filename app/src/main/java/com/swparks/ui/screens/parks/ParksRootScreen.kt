package com.swparks.ui.screens.parks

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.swparks.R
import com.swparks.data.model.Park
import com.swparks.navigation.AppState
import com.swparks.ui.screen.components.ParksListView

@Composable
fun ParksRootScreen(
    modifier: Modifier = Modifier,
    parks: List<Park>,
    onParkClick: (Park) -> Unit = {}
) {
    ParksListView(
        modifier = modifier,
        parks = parks,
        onParkClick = onParkClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UnusedParameter")
@Composable
fun ParksTopAppBar(
    appState: AppState,
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

/**
 * Пример кнопки создания площадки (показывается для авторизованных пользователей)
 */
@Composable
fun CreateParkFab(
    appState: AppState,
    onClick: () -> Unit = {}
) {
    if (appState.isAuthorized) {
        androidx.compose.material3.FloatingActionButton(
            onClick = {
                Log.i("ParksScreen", "Нажата кнопка создания площадки")
                onClick()
            }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Создать площадку"
            )
        }
    }
}