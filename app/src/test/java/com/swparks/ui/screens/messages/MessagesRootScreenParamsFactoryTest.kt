package com.swparks.ui.screens.messages

import com.swparks.data.database.entity.DialogEntity
import com.swparks.ui.state.DialogsUiState
import com.swparks.ui.viewmodel.IDialogsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagesRootScreenParamsFactoryTest {

    @Test
    fun createDialogsContentParams_whenDependenciesChange_usesLatestViewModelAndAction() {
        val dialog = createDialogEntity()

        var actionV1Calls = 0
        var actionV2Calls = 0

        val vm1 = TrackingDialogsViewModel()
        val vm2 = TrackingDialogsViewModel()

        val loadingState = DialogsLoadingState(
            isRefreshing = false,
            isUpdating = false,
            syncError = null
        )

        val callbacksV1 = DialogsCallbacks(
            onAction = { actionV1Calls++ },
            onDeleteClick = {}
        )
        val callbacksV2 = DialogsCallbacks(
            onAction = { actionV2Calls++ },
            onDeleteClick = {}
        )

        val paramsV1 = createDialogsContentParams(
            uiState = DialogsUiState.Success(emptyList()),
            loadingState = loadingState,
            currentUser = null,
            viewModel = vm1,
            callbacks = callbacksV1
        )
        val paramsV2 = createDialogsContentParams(
            uiState = DialogsUiState.Success(emptyList()),
            loadingState = loadingState,
            currentUser = null,
            viewModel = vm2,
            callbacks = callbacksV2
        )

        paramsV1.onRefresh()
        paramsV1.onDialogClick(dialog)
        assertEquals(1, vm1.refreshCalls)
        assertEquals(1, vm1.dialogClickCalls)
        assertEquals(1, actionV1Calls)

        paramsV2.onRefresh()
        paramsV2.onDialogClick(dialog)
        assertEquals(1, vm2.refreshCalls)
        assertEquals(1, vm2.dialogClickCalls)
        assertEquals(1, actionV2Calls)
        assertTrue("Старый callback не должен вызываться повторно", actionV1Calls == 1)
    }

    private fun createDialogEntity(): DialogEntity = DialogEntity(
        id = 42L,
        anotherUserId = 7,
        name = "Test User",
        image = "https://example.com/avatar.jpg",
        lastMessageText = "Hi",
        lastMessageDate = "2026-03-17T10:00:00",
        unreadCount = 0
    )

    private class TrackingDialogsViewModel : IDialogsViewModel {
        override val uiState: StateFlow<DialogsUiState> =
            MutableStateFlow(DialogsUiState.Success(emptyList()))
        override val isRefreshing: StateFlow<Boolean> = MutableStateFlow(false)
        override val isLoadingDialogs: StateFlow<Boolean> = MutableStateFlow(false)
        override val syncError: StateFlow<String?> = MutableStateFlow(null)
        override val isDeleting: StateFlow<Boolean> = MutableStateFlow(false)
        override val isMarkingAsRead: StateFlow<Boolean> = MutableStateFlow(false)
        override val isUpdating: StateFlow<Boolean> = MutableStateFlow(false)

        var refreshCalls = 0
        var dialogClickCalls = 0

        override fun refresh() {
            refreshCalls++
        }

        override fun loadDialogsAfterAuth() = Unit

        override fun onDialogClick(dialogId: Long, userId: Int?) {
            dialogClickCalls++
        }

        override fun dismissSyncError() = Unit

        override fun deleteDialog(dialogId: Long) = Unit

        override fun markDialogAsRead(dialogId: Long, userId: Int) = Unit
    }
}
