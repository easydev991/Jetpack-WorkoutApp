package com.swparks.ui.screens.parks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.swparks.R
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonSize

@Composable
fun NoParksFoundView(
    modifier: Modifier = Modifier,
    onSelectCity: () -> Unit,
    onOpenFilters: () -> Unit,
    isSizeTypeFilterEdited: Boolean
) {
    val noParksFoundDesc = stringResource(R.string.no_parks_found)
    val selectAnotherCityDesc = stringResource(R.string.select_another_city)
    val changeFiltersDesc = stringResource(R.string.change_filters)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.spacing_large)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = noParksFoundDesc,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SWButton(
            config = ButtonConfig(
                modifier = Modifier
                    .padding(top = dimensionResource(R.dimen.spacing_regular))
                    .semantics { contentDescription = selectAnotherCityDesc },
                size = SWButtonSize.SMALL,
                text = selectAnotherCityDesc,
                onClick = onSelectCity
            )
        )

        if (isSizeTypeFilterEdited) {
            SWButton(
                config = ButtonConfig(
                    modifier = Modifier
                        .padding(top = dimensionResource(R.dimen.spacing_xsmall))
                        .semantics { contentDescription = changeFiltersDesc },
                    size = SWButtonSize.SMALL,
                    text = changeFiltersDesc,
                    onClick = onOpenFilters
                )
            )
        }
    }
}

@Preview
@Composable
private fun NoParksFoundViewFullPreview() {
    NoParksFoundView(
        onSelectCity = {},
        onOpenFilters = {},
        isSizeTypeFilterEdited = true
    )
}

@Preview
@Composable
private fun NoParksFoundViewOneButtonPreview() {
    NoParksFoundView(
        onSelectCity = {},
        onOpenFilters = {},
        isSizeTypeFilterEdited = false
    )
}
