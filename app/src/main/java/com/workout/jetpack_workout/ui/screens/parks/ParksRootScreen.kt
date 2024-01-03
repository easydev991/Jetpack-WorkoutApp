package com.workout.jetpack_workout.ui.screens.parks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.workout.jetpack_workout.model.Park
import com.workout.jetpack_workout.ui.ds.ParkRowView
import com.workout.jetpack_workout.ui.theme.JetpackWorkoutAppTheme

@Composable
fun ParksRootScreen(
    modifier: Modifier = Modifier,
    parks: List<Park> = emptyList()
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
        modifier = modifier
    ) {
        items(parks, key = { it.id }) {
            ParkRowView(
                imageStringURL = it.preview,
                name = it.name,
                address = it.address,
                peopleTrainCount = it.trainingUsersCount ?: 0
            )
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
        ParksRootScreen()
    }
}