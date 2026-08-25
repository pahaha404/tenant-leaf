package com.seipseip.feature.inspection.presentation

import androidx.lifecycle.SavedStateHandle
import com.seipseip.core.common.AppResult
import com.seipseip.core.ui.ContentState
import com.seipseip.feature.inspection.domain.InspectionRepository
import com.seipseip.feature.inspection.domain.model.Inspection
import com.seipseip.feature.inspection.domain.model.InspectionAnalysisStatus
import com.seipseip.feature.inspection.domain.model.InspectionPage
import com.seipseip.feature.inspection.domain.model.InspectionStatus
import com.seipseip.feature.inspection.domain.usecase.CreateInspectionUseCase
import com.seipseip.feature.inspection.domain.usecase.GetInspectionUseCase
import com.seipseip.feature.inspection.domain.usecase.ListInspectionsUseCase
import com.seipseip.feature.inspection.domain.usecase.UpdateInspectionStatusUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.OffsetDateTime
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class InspectionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `empty inspection page is exposed as empty content`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = FakeInspectionRepository()
            val viewModel = InspectionListViewModel(
                SavedStateHandle(mapOf(InspectionListViewModel.PROPERTY_ID_ARGUMENT to PROPERTY_ID.toString())),
                ListInspectionsUseCase(repository),
                CreateInspectionUseCase(repository),
            )

            advanceUntilIdle()

            assertEquals(ContentState.Empty, viewModel.state.value.content)
        }

    @Test
    fun `starting an inspection publishes its id`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = FakeInspectionRepository()
            val viewModel = InspectionListViewModel(
                SavedStateHandle(mapOf(InspectionListViewModel.PROPERTY_ID_ARGUMENT to PROPERTY_ID.toString())),
                ListInspectionsUseCase(repository),
                CreateInspectionUseCase(repository),
            )
            advanceUntilIdle()
            val event = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.first() }

            viewModel.start()
            advanceUntilIdle()

            assertEquals(InspectionListEvent.Created(INSPECTION_ID), event.await())
        }

    @Test
    fun `ended inspection cannot be changed again`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = FakeInspectionRepository().apply { current = INSPECTION.copy(status = InspectionStatus.ENDED) }
            val viewModel = InspectionDetailViewModel(
                SavedStateHandle(mapOf(InspectionDetailViewModel.INSPECTION_ID_ARGUMENT to INSPECTION_ID.toString())),
                GetInspectionUseCase(repository),
                UpdateInspectionStatusUseCase(repository),
            )
            advanceUntilIdle()

            viewModel.cancel()
            advanceUntilIdle()

            assertEquals(0, repository.updateCalls)
            assertTrue(viewModel.state.value.content is ContentState.Success)
        }

    @Test
    fun `ending an inspection publishes the changed status`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = FakeInspectionRepository()
            val viewModel = InspectionDetailViewModel(
                SavedStateHandle(mapOf(InspectionDetailViewModel.INSPECTION_ID_ARGUMENT to INSPECTION_ID.toString())),
                GetInspectionUseCase(repository),
                UpdateInspectionStatusUseCase(repository),
            )
            advanceUntilIdle()
            val event = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.first() }

            viewModel.end()
            advanceUntilIdle()

            assertEquals(InspectionDetailEvent.StatusChanged(InspectionStatus.ENDED), event.await())
        }

    @Test
    fun `cancelling before inspection load completes still publishes the changed status`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = FakeInspectionRepository().apply { blockGet = true }
            val viewModel = InspectionDetailViewModel(
                SavedStateHandle(mapOf(InspectionDetailViewModel.INSPECTION_ID_ARGUMENT to INSPECTION_ID.toString())),
                GetInspectionUseCase(repository),
                UpdateInspectionStatusUseCase(repository),
            )
            repository.getStarted.await()
            val event = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.first() }

            viewModel.cancel()
            advanceUntilIdle()

            assertEquals(1, repository.updateCalls)
            assertEquals(InspectionDetailEvent.StatusChanged(InspectionStatus.CANCELLED), event.await())
            repository.releaseGet.complete(repository.current)
        }

    private class FakeInspectionRepository : InspectionRepository {
        var current = INSPECTION
        var updateCalls = 0
        var blockGet = false
        val getStarted = CompletableDeferred<Unit>()
        val releaseGet = CompletableDeferred<Inspection>()

        override suspend fun create(propertyId: UUID): AppResult<Inspection> = AppResult.Success(current)

        override suspend fun list(propertyId: UUID, page: Int, size: Int): AppResult<InspectionPage> =
            AppResult.Success(InspectionPage(page, size, 0, 0, emptyList()))

        override suspend fun get(inspectionId: UUID): AppResult<Inspection> {
            if (blockGet) {
                getStarted.complete(Unit)
                return AppResult.Success(releaseGet.await())
            }
            return AppResult.Success(current)
        }

        override suspend fun updateStatus(inspectionId: UUID, status: InspectionStatus): AppResult<Inspection> {
            updateCalls += 1
            current = current.copy(status = status)
            return AppResult.Success(current)
        }
    }

    private companion object {
        val PROPERTY_ID: UUID = UUID.fromString("10000000-0000-0000-0000-000000000001")
        val INSPECTION_ID: UUID = UUID.fromString("20000000-0000-0000-0000-000000000001")
        val INSPECTION = Inspection(
            id = INSPECTION_ID,
            propertyId = PROPERTY_ID,
            status = InspectionStatus.IN_PROGRESS,
            analysisStatus = InspectionAnalysisStatus.NOT_STARTED,
            startedAt = OffsetDateTime.parse("2026-08-18T10:00:00+09:00"),
            endedAt = null,
            cancelledAt = null,
            archivedAt = null,
            createdAt = OffsetDateTime.parse("2026-08-18T10:00:00+09:00"),
        )
    }
}
