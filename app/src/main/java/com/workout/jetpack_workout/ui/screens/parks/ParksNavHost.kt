package com.workout.jetpack_workout.ui.screens.parks

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.workout.jetpack_workout.R
import com.workout.jetpack_workout.model.Park
import com.workout.jetpack_workout.model.TabBarItem
import com.workout.jetpack_workout.utils.ReadJSONFromAssets
import com.workout.jetpack_workout.utils.WorkoutAppJson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParksNavHost() {
    val navController = rememberNavController()
    val oldParks = ReadJSONFromAssets(
        LocalContext.current,
        "parks.json"
    )
    val decodedParks = WorkoutAppJson.decodeFromString<List<Park>>(oldParks)
    NavHost(
        navController,
        startDestination = TabBarItem.Parks.route
    ) {
        composable(TabBarItem.Parks.route) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(
                                    id = R.string.parks_title,
                                    decodedParks.size
                                )
                            )
                        },
                        windowInsets = WindowInsets(top = 0),
                        actions = {
                            IconButton(onClick = { TODO(reason = "Нужно подключить экран с фильтрами") }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_filter_list_24),
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = stringResource(id = R.string.parks_filter)
                                )
                            }
                        }
                    )
                }
            ) {
                ParksRootScreen(
                    modifier = Modifier.padding(it),
                    parks = decodedParks
                )
            }
        }
    }
}