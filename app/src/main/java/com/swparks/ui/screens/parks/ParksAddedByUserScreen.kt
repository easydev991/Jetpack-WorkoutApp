package com.swparks.ui.screens.parks

import android.util.Log
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.swparks.R
import com.swparks.data.model.Park
import com.swparks.ui.screen.components.ParksListView

/**
 * Экран для отображения добавленных пользователем площадок
 *
 * @param parks Список добавленных площадок
 * @param onBackClick Замыкание для навигации назад
 * @param onParkClick Замыкание, вызываемое при клике на площадку
 * @param modifier Modifier для настройки внешнего вида
 * @param parentPaddingValues Родительские отступы для учета BottomNavigationBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParksAddedByUserScreen(
    modifier: Modifier = Modifier,
    parks: List<Park>,
    onBackClick: () -> Unit,
    onParkClick: (Park) -> Unit = { park ->
        Log.d("ParksAddedByUserScreen", "Нажата площадка: ${park.name}")
    },
    parentPaddingValues: androidx.compose.foundation.layout.PaddingValues
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(R.string.added_parks))
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        ParksListView(
            modifier = modifier
                .padding(parentPaddingValues)
                .padding(innerPadding),
            parks = parks,
            onParkClick = onParkClick
        )
    }
}
