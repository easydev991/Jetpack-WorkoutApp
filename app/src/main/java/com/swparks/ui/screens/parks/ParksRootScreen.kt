package com.swparks.ui.screens.parks

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.swparks.R
import com.swparks.data.model.NewParkDraft
import com.swparks.data.model.Park
import com.swparks.navigation.AppState
import com.swparks.ui.ds.ParksListView

@Composable
fun ParksRootScreen(
    modifier: Modifier = Modifier,
    parks: List<Park>,
    onParkClick: (Park) -> Unit = {},
    onCreateParkClick: (NewParkDraft) -> Unit = {},
    appState: AppState
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            CreateParkFab(
                appState = appState,
                onClick = { onCreateParkClick(NewParkDraft.EMPTY) }
            )
        }
    ) { innerPadding ->
        ParksListView(
            modifier = Modifier.fillMaxSize(),
            parks = parks,
            onParkClick = onParkClick
        )
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

/**
 * Пример кнопки создания площадки (показывается для авторизованных пользователей)
 */
@Composable
fun CreateParkFab(
    appState: AppState,
    onClick: () -> Unit = {}
) {
    if (appState.isAuthorized) {
        FloatingActionButton(
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
