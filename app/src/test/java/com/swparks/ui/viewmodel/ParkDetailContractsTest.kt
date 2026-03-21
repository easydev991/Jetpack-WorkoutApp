package com.swparks.ui.viewmodel

import com.swparks.data.model.Photo
import com.swparks.data.model.User
import com.swparks.ui.ds.CommentAction
import com.swparks.ui.model.MapUriSet
import com.swparks.ui.model.TextEntryMode
import com.swparks.ui.state.ParkDetailUIState
import com.swparks.util.Complaint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkDetailContractsTest {

    @Test
    fun parkDetailUIState_initialLoading_isDataObject() {
        val state = ParkDetailUIState.InitialLoading

        assertTrue(state is ParkDetailUIState.InitialLoading)
    }

    @Test
    fun parkDetailUIState_content_hasRequiredFields() {
        val park = createPark()
        val state = ParkDetailUIState.Content(
            park = park,
            address = "Москва, Россия",
            authorAddress = "Москва, Россия"
        )

        assertEquals(park, state.park)
        assertEquals("Москва, Россия", state.address)
        assertEquals("Москва, Россия", state.authorAddress)
    }

    @Test
    fun parkDetailUIState_error_hasNullableMessage() {
        val stateWithMessage = ParkDetailUIState.Error("Ошибка загрузки")
        val stateWithoutMessage = ParkDetailUIState.Error(null)

        assertEquals("Ошибка загрузки", stateWithMessage.message)
        assertNull(stateWithoutMessage.message)
    }

    @Test
    fun parkDetailEvent_showDeleteConfirmDialog_isDataObject() {
        val event = ParkDetailEvent.ShowDeleteConfirmDialog

        assertTrue(event is ParkDetailEvent.ShowDeleteConfirmDialog)
    }

    @Test
    fun parkDetailEvent_showDeletePhotoConfirmDialog_hasPhoto() {
        val photo = Photo(id = 1L, photo = "url")
        val event = ParkDetailEvent.ShowDeletePhotoConfirmDialog(photo)

        assertEquals(photo, event.photo)
    }

    @Test
    fun parkDetailEvent_showDeleteCommentConfirmDialog_isDataObject() {
        val event = ParkDetailEvent.ShowDeleteCommentConfirmDialog

        assertTrue(event is ParkDetailEvent.ShowDeleteCommentConfirmDialog)
    }

    @Test
    fun parkDetailEvent_parkDeleted_hasParkId() {
        val event = ParkDetailEvent.ParkDeleted(parkId = 123L)

        assertEquals(123L, event.parkId)
    }

    @Test
    fun parkDetailEvent_parkUpdated_hasParkId() {
        val event = ParkDetailEvent.ParkUpdated(parkId = 123L)

        assertEquals(123L, event.parkId)
    }

    @Test
    fun parkDetailEvent_photoDeleted_hasPhotoId() {
        val event = ParkDetailEvent.PhotoDeleted(photoId = 456L)

        assertEquals(456L, event.photoId)
    }

    @Test
    fun parkDetailEvent_openMap_isDataObject() {
        val event = ParkDetailEvent.OpenMap

        assertTrue(event is ParkDetailEvent.OpenMap)
    }

    @Test
    fun parkDetailEvent_buildRoute_isDataObject() {
        val event = ParkDetailEvent.BuildRoute

        assertTrue(event is ParkDetailEvent.BuildRoute)
    }

    @Test
    fun parkDetailEvent_navigateToTrainees_hasParkIdAndUsers() {
        val users = listOf(
            User(id = 1L, name = "User 1", image = null),
            User(id = 2L, name = "User 2", image = null)
        )
        val event = ParkDetailEvent.NavigateToTrainees(
            parkId = 100L,
            users = users
        )

        assertEquals(100L, event.parkId)
        assertEquals(users, event.users)
    }

    @Test
    fun parkDetailEvent_navigateToCreateEvent_hasParkIdAndParkName() {
        val event = ParkDetailEvent.NavigateToCreateEvent(
            parkId = 100L,
            parkName = "Test Park"
        )

        assertEquals(100L, event.parkId)
        assertEquals("Test Park", event.parkName)
    }

    @Test
    fun parkDetailEvent_sendCommentComplaint_hasParkComment() {
        val complaint = Complaint.ParkComment(
            parkTitle = "Park",
            author = "Author",
            commentText = "Text"
        )
        val event = ParkDetailEvent.SendCommentComplaint(complaint)

        assertEquals(complaint, event.complaint)
    }

    @Test
    fun parkDetailEvent_openCommentTextEntry_hasMode() {
        val mode = TextEntryMode.NewForPark(parkId = 1L)
        val event = ParkDetailEvent.OpenCommentTextEntry(mode)

        assertEquals(mode, event.mode)
    }

    @Test
    fun parkDetailEvent_navigateToPhotoDetail_hasRequiredFields() {
        val photo = Photo(id = 1L, photo = "url")
        val event = ParkDetailEvent.NavigateToPhotoDetail(
            photo = photo,
            parkId = 100L,
            parkTitle = "Test Park",
            isParkAuthor = true
        )

        assertEquals(photo, event.photo)
        assertEquals(100L, event.parkId)
        assertEquals("Test Park", event.parkTitle)
        assertTrue(event.isParkAuthor)
    }

    @Test
    fun parkDetailEvent_navigateToPhotoDetail_whenNotAuthor_isParkAuthorFalse() {
        val photo = Photo(id = 1L, photo = "url")
        val event = ParkDetailEvent.NavigateToPhotoDetail(
            photo = photo,
            parkId = 100L,
            parkTitle = "Test Park",
            isParkAuthor = false
        )

        assertFalse(event.isParkAuthor)
    }

    @Test
    fun iParkDetailViewModel_hasRequiredProperties() {
        val viewModel = FakeParkDetailViewModel()

        assertNotNull(viewModel.uiState)
        assertNotNull(viewModel.events)
        assertNotNull(viewModel.isRefreshing)
        assertNotNull(viewModel.isAuthorized)
        assertNotNull(viewModel.isParkAuthor)
        assertNotNull(viewModel.currentUserId)
    }

    @Test
    fun iParkDetailViewModel_hasRequiredMapUriSet() {
        val viewModel = FakeParkDetailViewModel()

        assertTrue(viewModel.mapUriSet is MapUriSet?)
    }

    @Test
    fun iParkDetailViewModel_hasRequiredMethods() {
        val viewModel = FakeParkDetailViewModel()

        viewModel.onEditClick()
        viewModel.onDeleteClick()
        viewModel.onDeleteConfirm()
        viewModel.onDeleteDismiss()
        viewModel.onShareClick()
        viewModel.onTrainHereToggle()
        viewModel.onTraineesCountClick()
        viewModel.onOpenMapClick()
        viewModel.onRouteClick()
        viewModel.onCreateEventClick()
        viewModel.onPhotoClick(Photo(id = 1L, photo = "url"))
        viewModel.onPhotoDeleteClick(Photo(id = 1L, photo = "url"))
        viewModel.onPhotoDeleteConfirm()
        viewModel.onPhotoDeleteDismiss()
        viewModel.onAddCommentClick()
        viewModel.onCommentActionClick(1L, CommentAction.EDIT)
        viewModel.onCommentDeleteConfirm()
        viewModel.onCommentDeleteDismiss()
        viewModel.onPhotoDeleted(1L)
        viewModel.onParkUpdated(1L)
        viewModel.refresh()

        assertTrue(viewModel.methodsCalled)
    }

    private fun createPark() = com.swparks.data.model.Park(
        id = 1L,
        name = "Test Park",
        sizeID = 1,
        typeID = 1,
        longitude = "37.5",
        latitude = "55.5",
        address = "Test Address",
        cityID = 1,
        countryID = 1,
        preview = "preview_url"
    )

    private class FakeParkDetailViewModel : IParkDetailViewModel {
        var methodsCalled = false

        override val uiState = kotlinx.coroutines.flow.MutableStateFlow<ParkDetailUIState>(
            ParkDetailUIState.InitialLoading
        )
        override val events = kotlinx.coroutines.flow.MutableSharedFlow<ParkDetailEvent>()
        override val isRefreshing = kotlinx.coroutines.flow.MutableStateFlow(false)
        override val isAuthorized = kotlinx.coroutines.flow.MutableStateFlow(false)
        override val isParkAuthor = kotlinx.coroutines.flow.MutableStateFlow(false)
        override val currentUserId = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
        override val mapUriSet: MapUriSet? = null

        override fun onEditClick() {
            methodsCalled = true
        }

        override fun onDeleteClick() {
            methodsCalled = true
        }

        override fun onDeleteConfirm() {
            methodsCalled = true
        }

        override fun onDeleteDismiss() {
            methodsCalled = true
        }

        override fun onShareClick() {
            methodsCalled = true
        }

        override fun onTrainHereToggle() {
            methodsCalled = true
        }

        override fun onTraineesCountClick() {
            methodsCalled = true
        }

        override fun onOpenMapClick() {
            methodsCalled = true
        }

        override fun onRouteClick() {
            methodsCalled = true
        }

        override fun onCreateEventClick() {
            methodsCalled = true
        }

        override fun onPhotoClick(photo: Photo) {
            methodsCalled = true
        }

        override fun onPhotoDeleteClick(photo: Photo) {
            methodsCalled = true
        }

        override fun onPhotoDeleteConfirm() {
            methodsCalled = true
        }

        override fun onPhotoDeleteDismiss() {
            methodsCalled = true
        }

        override fun onAddCommentClick() {
            methodsCalled = true
        }

        override fun onCommentActionClick(commentId: Long, action: CommentAction) {
            methodsCalled = true
        }

        override fun onCommentDeleteConfirm() {
            methodsCalled = true
        }

        override fun onCommentDeleteDismiss() {
            methodsCalled = true
        }

        override fun onPhotoDeleted(photoId: Long) {
            methodsCalled = true
        }

        override fun onParkUpdated(parkId: Long) {
            methodsCalled = true
        }

        override fun refresh() {
            methodsCalled = true
        }
    }
}
