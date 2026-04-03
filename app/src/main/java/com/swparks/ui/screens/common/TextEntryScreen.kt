@file:Suppress("UnusedPrivateMember")

package com.swparks.ui.screens.common

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.swparks.R
import com.swparks.ui.ds.ButtonConfig
import com.swparks.ui.ds.LoadingOverlayView
import com.swparks.ui.ds.SWButton
import com.swparks.ui.ds.SWButtonMode
import com.swparks.ui.ds.SWButtonSize
import com.swparks.ui.ds.SWTextEditor
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.model.getFormattedTitle
import com.swparks.ui.model.getPlaceholder
import com.swparks.ui.state.TextEntryEvent
import com.swparks.ui.state.TextEntryUiState
import com.swparks.ui.theme.JetpackWorkoutAppTheme
import com.swparks.ui.viewmodel.ITextEntryViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEntryScreen(
    modifier: Modifier = Modifier,
    viewModel: ITextEntryViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val resources = LocalContext.current.resources

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = uiState.mode.getFormattedTitle(resources))
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close_button_content_description)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            TextEntryContent(
                uiState = uiState,
                onTextChanged = viewModel::onTextChanged,
                onSend = viewModel::onSend,
                modifier = Modifier.padding(paddingValues)
            )

            if (uiState.isLoading) {
                LoadingOverlayView()
            }
        }
    }
}

@Composable
private fun TextEntryContent(
    uiState: TextEntryUiState,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.spacing_regular))
                .padding(bottom = dimensionResource(R.dimen.spacing_regular))
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_regular)))

        SWTextEditor(
            text = uiState.text,
            onTextChange = onTextChanged,
            labelID = uiState.mode.getPlaceholder(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 200.dp),
            enabled = !uiState.isLoading
        )

        Spacer(modifier = Modifier.weight(1f))

        SWButton(
            config =
                ButtonConfig(
                    modifier = Modifier.fillMaxWidth(),
                    size = SWButtonSize.LARGE,
                    mode = SWButtonMode.FILLED,
                    text = stringResource(R.string.send_button_text),
                    enabled = uiState.isSendEnabled && !uiState.isLoading,
                    onClick = onSend
                )
        )
    }
}

/** Mock ViewModel для превью */
private class PreviewTextEntryViewModel : ITextEntryViewModel {
    private val _uiState =
        MutableStateFlow(
            TextEntryUiState(
                mode = TextEntryMode.NewForPark(parkId = 1L),
                text = "Это пример текста для превью экрана ввода комментария",
                isLoading = false
            )
        )
    override val uiState: StateFlow<TextEntryUiState> = _uiState

    override val events: Flow<TextEntryEvent> = emptyFlow()

    override fun onTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(text = text)
    }

    override fun onSend() = Unit

    override fun onDismissError() = Unit

    override fun resetState() = Unit
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
private fun TextEntryScreenPreview() {
    JetpackWorkoutAppTheme {
        Surface {
            TextEntryScreen(
                viewModel = PreviewTextEntryViewModel(),
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    locale = "ru"
)
@Composable
private fun TextEntryScreenJournalPreview() {
    val viewModel =
        object : ITextEntryViewModel {
            private val _uiState =
                MutableStateFlow(
                    TextEntryUiState(
                        mode = TextEntryMode.NewForJournal(ownerId = 1L, journalId = 1L),
                        text = "Это пример текста для превью экрана ввода записи в дневник",
                        isLoading = false
                    )
                )
            override val uiState: StateFlow<TextEntryUiState> = _uiState

            override val events: Flow<TextEntryEvent> = emptyFlow()

            override fun onTextChanged(text: String) {
                _uiState.value = _uiState.value.copy(text = text)
            }

            override fun onSend() = Unit

            override fun onDismissError() = Unit

            override fun resetState() = Unit
        }
    JetpackWorkoutAppTheme {
        Surface {
            TextEntryScreen(
                viewModel = viewModel,
                onDismiss = {}
            )
        }
    }
}
