---
## Goal

Implement `ParkDetailScreen` in Android following TDD methodology (RED → GREEN → REFACTOR), based on the logic of `EventDetailScreen` but with specific differences outlined in the plan document. The implementation is split into 10 iterations.

## Instructions

- Follow the TDD plan in `docs/screens/plan-park-detail-screen.md`
- Each iteration follows RED → GREEN → REFACTOR cycle
- Key differences from EventDetailScreen:
  - TopAppBar title: `park_title` (not `event_title`)
  - Header: only `title + address` (no `when`/`where`)
  - Main button: `create_event` → `Screen.CreateEventForPark`
  - Participants: title `park_trainees_title`, toggle `train_here`, mode `Park`
  - No Description section
  - Author section title: `added_by`
  - Edit action: only logging (ParkFormScreen doesn't exist yet)
  - Update action: `onParkUpdated` (for when ParkFormScreen will exist)
  - API: `changeTrainHereStatus`, `getPark`, `deleteParkPhoto`

## Discoveries

- `Screen.ParkTrainees` already exists in `Destinations.kt` with proper route structure
- `SWRepository` already has park endpoints - `deleteParkPhoto` was missing but now added
- `TextEntryMode` already supports park comments (`NewForPark`, `EditPark`)
- Navigation for Event participants uses separate coordinator + JSON in `SavedStateHandle`
- `PhotoDetailViewModel/PhotoDetailConfig` is event-specific and needs generalization for Park (Iteration 6)
- `Photo` model uses `photo: String` parameter (not `preview`)
- `User` model requires `image: String?` parameter
- `CommentAction` is in `com.swparks.ui.ds.CommentAction`
- `onParkUpdated` was added proactively for future ParkFormScreen integration (user requested this)

## Accomplished

**Iteration 1 complete ✅:** Navigation framework for ParkTrainees

Created files:
- `app/src/main/java/com/swparks/navigation/ParkNavigationCoordinator.kt`
- `app/src/main/java/com/swparks/navigation/ParkNavArgs.kt`
- `app/src/test/java/com/swparks/navigation/ParkNavigationCoordinatorTest.kt`
- `app/src/test/java/com/swparks/navigation/ParkNavArgsTest.kt`

Modified files:
- `app/src/main/java/com/swparks/navigation/NavArgsViewModels.kt` - added `ParkTraineesNavArgsViewModel`

**Iteration 2 complete ✅:** Repository gap for `deleteParkPhoto`

Modified files:
- `app/src/main/java/com/swparks/data/repository/SWRepository.kt` - added `deleteParkPhoto(parkId, photoId)` to interface
- `app/src/test/java/com/swparks/data/repository/SWRepositoryParksTest.kt` - added 2 tests

**Iteration 3 complete ✅:** ParkDetail contracts

Created files:
- `app/src/main/java/com/swparks/ui/state/ParkDetailUIState.kt` - sealed interface (`InitialLoading`, `Content`, `Error`)
- `app/src/main/java/com/swparks/ui/viewmodel/IParkDetailViewModel.kt` - interface with all properties and methods (including `onParkUpdated`)
- `app/src/main/java/com/swparks/ui/viewmodel/ParkDetailEvent.kt` - sealed class with UI events (including `ParkUpdated`)
- `app/src/test/java/com/swparks/ui/viewmodel/ParkDetailContractsTest.kt` - 20 contract tests

Key contracts implemented:
- `onTrainHereToggle` instead of `onParticipantToggle`
- `onTraineesCountClick` instead of `onParticipantsCountClick`
- `onCreateEventClick` unique to park
- `onParkUpdated(parkId)` for future ParkFormScreen support
- No `onAddToCalendarClick`/`onAddToCalendarFailed` (park has no calendar)
- `NavigateToTrainees` instead of `NavigateToParticipants`
- `NavigateToCreateEvent(parkId, parkName)` unique to park
- `SendCommentComplaint(Complaint.ParkComment)` instead of `Complaint.EventComment`

**Iteration 5 complete ✅:** ParkDetailViewModel — comments/photos

Added tests to:
- `app/src/test/java/com/swparks/ui/viewmodel/ParkDetailViewModelTest.kt` - 4 new tests:
  - `onAddCommentClick_thenEmitsOpenCommentTextEntryNewForPark`
  - `onCommentActionClick_whenEdit_thenEmitsOpenCommentTextEntryEditPark`
  - `onCommentActionClick_whenReport_thenEmitsSendCommentComplaintPark`
  - `onCommentDeleteConfirm_whenSuccess_thenCallsDeleteCommentWithTextEntryOptionPark`

Key implementations verified:
- Comments use `TextEntryMode.NewForPark(parkId)` for new comments
- Comments use `TextEntryMode.EditPark(EditInfo)` for editing
- Comments delete via `TextEntryOption.Park(parkId)`
- Complaints use `Complaint.ParkComment` with parkTitle, author, commentText
- Photo navigation emits `NavigateToPhotoDetail(photo, parkId, parkTitle, isParkAuthor)`
- Photo delete via `swRepository.deleteParkPhoto(parkId, photoId)` with local state update

**Iteration 4 complete ✅:** ParkDetailViewModel — core scenarios

Created files:
- `app/src/main/java/com/swparks/ui/viewmodel/ParkDetailViewModel.kt` - full implementation
- `app/src/test/java/com/swparks/ui/viewmodel/ParkDetailViewModelTest.kt` - 18 tests covering:
  - `init_whenParkLoaded_thenUiStateContent`
  - `init_whenLoadFails_thenUiStateError`
  - `init_whenRuntimeException_thenShowsError`
  - `refresh_whenCalled_thenReloadsPark`
  - `onTrainHereToggle_whenSuccess_thenUpdatesTrainHereAndUsers`
  - `onTrainHereToggle_whenFailure_thenRevertsOptimisticState`
  - `onTraineesCountClick_thenEmitsNavigateToTrainees`
  - `onCreateEventClick_thenEmitsNavigateToCreateEvent`
  - `onDeleteConfirm_whenSuccess_thenEmitsParkDeleted`
  - `onDeleteConfirm_whenRuntimeException_thenHandlesError`
  - `onDeleteClick_whenNotAuthor_thenDoesNotShowDialog`
  - `onOpenMapClick_thenEmitsOpenMap`
  - `onRouteClick_thenEmitsBuildRoute`
  - `onEditClick_thenOnlyLogsAndDoesNotNavigate`
  - `onParkUpdated_whenCalled_thenReloadsPark`
  - `onPhotoClick_thenEmitsNavigateToPhotoDetail`
  - `onPhotoDeleteConfirm_whenSuccess_thenEmitsPhotoDeleted`
  - `onPhotoDeleteConfirm_whenRuntimeException_thenHandlesError`

Key implementation details:
- `loadPark()` via `swRepository.getPark(parkId)`
- `onTrainHereToggle()` with optimistic update via `swRepository.changeTrainHereStatus()`
- `onDeleteConfirm()` via `swRepository.deletePark()`
- `onEditClick()` only logs (no navigation)
- `onParkUpdated(parkId)` calls `loadPark()` to refresh data
- `mapUriSet` computed from park coordinates
- `canManagePark()` helper for permission checks
- All nullable fields handled safely (no `!!`)

## Relevant files / directories

**Plan document:**
- `docs/screens/plan-park-detail-screen.md`

**Created:**
- `app/src/main/java/com/swparks/navigation/ParkNavigationCoordinator.kt`
- `app/src/main/java/com/swparks/navigation/ParkNavArgs.kt`
- `app/src/main/java/com/swparks/ui/state/ParkDetailUIState.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/IParkDetailViewModel.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/ParkDetailEvent.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/ParkDetailViewModel.kt`
- `app/src/test/java/com/swparks/navigation/ParkNavigationCoordinatorTest.kt`
- `app/src/test/java/com/swparks/navigation/ParkNavArgsTest.kt`
- `app/src/test/java/com/swparks/ui/viewmodel/ParkDetailContractsTest.kt`
- `app/src/test/java/com/swparks/ui/viewmodel/ParkDetailViewModelTest.kt`

**Modified:**
- `app/src/main/java/com/swparks/navigation/NavArgsViewModels.kt`
- `app/src/main/java/com/swparks/data/repository/SWRepository.kt`
- `app/src/test/java/com/swparks/data/repository/SWRepositoryParksTest.kt`

**Reference files (Event implementation to follow):**
- `app/src/main/java/com/swparks/ui/viewmodel/EventDetailViewModel.kt`
- `app/src/main/java/com/swparks/ui/viewmodel/IEventDetailViewModel.kt`
- `app/src/main/java/com/swparks/ui/state/EventDetailUIState.kt`
- `app/src/test/java/com/swparks/ui/viewmodel/EventDetailViewModelTest.kt`

## Next Steps

**Iteration 6:** Generalize PhotoDetailViewModel for Park
- RED: Add/update tests for `PhotoDetailViewModelTest` under 2 owner modes (Event/Park)
- GREEN: Generalize `PhotoDetailConfig` and `PhotoDetailViewModel` for both Event and Park photo owners
- REFACTOR: Remove event-specific naming (`eventId`, `eventTitle`, `isEventAuthor`) in favor of neutral
