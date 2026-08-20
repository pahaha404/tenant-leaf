package com.seipseip.feature.property.presentation

import androidx.lifecycle.SavedStateHandle
import com.seipseip.core.common.AppError
import com.seipseip.core.common.AppResult
import com.seipseip.core.ui.ContentState
import com.seipseip.feature.property.domain.PropertyRepository
import com.seipseip.feature.property.domain.model.Property
import com.seipseip.feature.property.domain.model.PropertyDraft
import com.seipseip.feature.property.domain.model.PropertyPage
import com.seipseip.feature.property.domain.model.PropertyPatch
import com.seipseip.feature.property.domain.usecase.CreatePropertyUseCase
import com.seipseip.feature.property.domain.usecase.GetPropertyUseCase
import com.seipseip.feature.property.domain.usecase.ListPropertiesUseCase
import com.seipseip.feature.property.domain.usecase.UpdatePropertyUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class PropertyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `list view model exposes empty state when server page has no items`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = FakePropertyRepository().apply {
                listResult = AppResult.Success(PropertyPage(0, 20, 0, 0, emptyList()))
            }

            val viewModel = PropertyListViewModel(ListPropertiesUseCase(repository))
            advanceUntilIdle()

            assertEquals(ContentState.Empty, viewModel.state.value)
        }

    @Test
    fun `list view model separates network failure from empty state`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = FakePropertyRepository().apply {
                listResult = AppResult.Failure(AppError.Network)
            }

            val viewModel = PropertyListViewModel(ListPropertiesUseCase(repository))
            advanceUntilIdle()

            assertTrue(viewModel.state.value is ContentState.NetworkError)
        }

    @Test
    fun `form rejects a blank name before calling the repository`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = FakePropertyRepository()
            val viewModel = formViewModel(repository)

            viewModel.save()

            assertEquals("매물 이름을 입력해 주세요.", viewModel.state.value.validationErrors["name"])
            assertEquals(0, repository.createCalls)
        }

    @Test
    fun `valid form creates a property and publishes saved event`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = FakePropertyRepository().apply {
                createResult = AppResult.Success(TEST_PROPERTY)
            }
            val viewModel = formViewModel(repository)
            val event = async(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.first() }
            viewModel.updateFields { it.copy(name = "  테스트 매물  ") }

            viewModel.save()
            advanceUntilIdle()

            assertEquals(PropertyFormEvent.Saved(TEST_PROPERTY.id), event.await())
            assertEquals("테스트 매물", repository.lastCreatedDraft?.name)
        }

    private fun formViewModel(repository: PropertyRepository) = PropertyFormViewModel(
        savedStateHandle = SavedStateHandle(),
        getProperty = GetPropertyUseCase(repository),
        createProperty = CreatePropertyUseCase(repository),
        updateProperty = UpdatePropertyUseCase(repository),
    )

    private class FakePropertyRepository : PropertyRepository {
        var listResult: AppResult<PropertyPage> = AppResult.Success(PropertyPage(0, 20, 0, 0, emptyList()))
        var createResult: AppResult<Property> = AppResult.Success(TEST_PROPERTY)
        var createCalls: Int = 0
        var lastCreatedDraft: PropertyDraft? = null

        override suspend fun list(page: Int, size: Int): AppResult<PropertyPage> = listResult

        override suspend fun get(id: UUID): AppResult<Property> = AppResult.Success(TEST_PROPERTY)

        override suspend fun create(draft: PropertyDraft): AppResult<Property> {
            createCalls += 1
            lastCreatedDraft = draft
            return createResult
        }

        override suspend fun update(id: UUID, patch: PropertyPatch): AppResult<Property> =
            AppResult.Success(TEST_PROPERTY)

        override suspend fun delete(id: UUID): AppResult<Unit> = AppResult.Success(Unit)
    }

    private companion object {
        val TEST_PROPERTY = Property(
            id = UUID.fromString("10000000-0000-0000-0000-000000000001"),
            name = "테스트 매물",
            addressSummary = null,
            depositAmount = null,
            monthlyRentAmount = null,
            maintenanceFeeAmount = null,
            areaSquareMeters = null,
            floor = null,
            options = emptySet(),
            brokerContact = null,
            note = null,
            createdAt = OffsetDateTime.parse("2026-08-18T10:00:00+09:00"),
            updatedAt = OffsetDateTime.parse("2026-08-18T10:00:00+09:00"),
        )
    }
}

