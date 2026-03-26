package com.swparks.ui.screens.parks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.data.model.Park
import com.swparks.data.model.ParkSize
import com.swparks.data.model.ParkType
import com.swparks.ui.theme.JetpackWorkoutAppTheme

private val PARK_INFO_CARD_MAX_WIDTH = 420.dp

@Composable
fun ParkInfoCard(
    park: Park,
    onDetailsClick: (Park) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(max = PARK_INFO_CARD_MAX_WIDTH),
        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_small)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimensionResource(R.dimen.elevation_medium)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_regular)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
        ) {
            ParkInfoCardHeader(
                parkName = park.name,
                onDismiss = onDismiss
            )

            ParkInfoDescription(park = park)

            Button(
                onClick = { onDetailsClick(park) }
            ) {
                Text(
                    text = stringResource(R.string.see_details),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun ParkInfoCardHeader(
    parkName: String,
    onDismiss: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = parkName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ParkInfoDescription(park: Park) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xxsmall))
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xxsmall))
        ) {
            park.size?.let { size ->
                Text(
                    text = stringResource(size.description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            park.type?.let { type ->
                Text(
                    text = stringResource(type.description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (park.address.isNotBlank()) {
            Text(
                text = park.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun ParkInfoCardPreview() {
    val park = Park(
        id = 1L,
        name = "Спортивная площадка",
        sizeID = ParkSize.LARGE.rawValue,
        typeID = ParkType.MODERN.rawValue,
        longitude = "37.6173",
        latitude = "55.7558",
        address = "ул. Пушкина, д. 10",
        cityID = 1,
        countryID = 1,
        preview = ""
    )
    JetpackWorkoutAppTheme {
        ParkInfoCard(
            park = park,
            onDetailsClick = {},
            onDismiss = {}
        )
    }
}
