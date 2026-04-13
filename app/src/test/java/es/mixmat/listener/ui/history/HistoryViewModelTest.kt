package es.mixmat.listener.ui.history

import app.cash.turbine.test
import es.mixmat.listener.data.repository.HistoryPage
import es.mixmat.listener.data.repository.HistoryRepository
import es.mixmat.listener.domain.model.HistoryItem
import es.mixmat.listener.domain.model.Platforms
import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var historyRepository: HistoryRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        historyRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createItem(id: String, title: String = "Track $id") = HistoryItem(
        id = id,
        title = title,
        artist = "Artist",
        thumbnail = null,
        shortcode = null,
        shareUrl = null,
        platforms = Platforms(spotify = null, tidal = null, appleMusic = null),
        createdAt = "2026-01-01T00:00:00Z",
    )

    @Test
    fun `init loads history`() = runTest {
        val items = listOf(createItem("1"), createItem("2"))
        coEvery { historyRepository.getHistory(any(), any()) } returns HistoryPage(
            items = items, cursor = "abc", hasMore = true,
        )

        val viewModel = HistoryViewModel(historyRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.items.size)
        assertEquals("abc", state.cursor)
        assertTrue(state.hasMore)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadHistory sets error on failure`() = runTest {
        coEvery { historyRepository.getHistory(any(), any()) } throws RuntimeException("Network error")

        val viewModel = HistoryViewModel(historyRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Failed to load history", state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadMore appends items`() = runTest {
        coEvery { historyRepository.getHistory(null, any()) } returns HistoryPage(
            items = listOf(createItem("1")), cursor = "c1", hasMore = true,
        )
        coEvery { historyRepository.getHistory("c1", any()) } returns HistoryPage(
            items = listOf(createItem("2")), cursor = null, hasMore = false,
        )

        val viewModel = HistoryViewModel(historyRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.items.size)
        assertEquals("1", state.items[0].id)
        assertEquals("2", state.items[1].id)
        assertFalse(state.hasMore)
    }

    @Test
    fun `deleteItem removes from list`() = runTest {
        coEvery { historyRepository.getHistory(any(), any()) } returns HistoryPage(
            items = listOf(createItem("1"), createItem("2")), cursor = null, hasMore = false,
        )
        coEvery { historyRepository.delete("1") } returns Unit

        val viewModel = HistoryViewModel(historyRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteItem("1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.items.size)
        assertEquals("2", state.items[0].id)
    }

    @Test
    fun `deleteItem sets error on failure`() = runTest {
        coEvery { historyRepository.getHistory(any(), any()) } returns HistoryPage(
            items = listOf(createItem("1")), cursor = null, hasMore = false,
        )
        coEvery { historyRepository.delete("1") } throws RuntimeException("Failed")

        val viewModel = HistoryViewModel(historyRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteItem("1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Couldn't remove — try again", viewModel.uiState.value.error)
        assertEquals(1, viewModel.uiState.value.items.size)
    }

    @Test
    fun `clearError clears error`() = runTest {
        coEvery { historyRepository.getHistory(any(), any()) } throws RuntimeException("fail")

        val viewModel = HistoryViewModel(historyRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }
}
