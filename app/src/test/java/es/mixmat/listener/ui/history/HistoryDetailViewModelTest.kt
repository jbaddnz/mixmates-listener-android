package es.mixmat.listener.ui.history

import android.util.Log
import es.mixmat.listener.R
import es.mixmat.listener.data.repository.HistoryRepository
import es.mixmat.listener.domain.model.HistoryDetail
import es.mixmat.listener.domain.model.Platforms
import es.mixmat.listener.domain.model.SharedGroup
import es.mixmat.listener.ui.text.UiText
import io.mockk.coEvery
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Group selection and sharing moved to `TrackShareViewModelTest` when the picker
 * moved into the shared sheet — including the preselection regression test, which
 * now covers all three screens rather than only this one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var historyRepository: HistoryRepository

    private val detail = HistoryDetail(
        id = "h1",
        title = "Song",
        artist = "Artist",
        thumbnail = null,
        shortcode = "abc",
        shareUrl = "https://mixmat.es/abc",
        platforms = Platforms(spotify = "https://spotify.com", tidal = null, appleMusic = null),
        createdAt = "2026-01-01T00:00:00Z",
        bpm = null,
        musicalKey = null,
        keyScale = null,
        sharedTo = listOf(SharedGroup(groupId = "g1", groupName = "Group 1")),
    )

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

    @Test
    fun `load fetches the detail`() = runTest {
        coEvery { historyRepository.getDetail("h1") } returns detail

        val viewModel = HistoryDetailViewModel(historyRepository)
        viewModel.load("h1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.detail)
        assertEquals("Song", state.detail!!.title)
        assertEquals(1, state.detail!!.sharedTo.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun `load sets error on failure`() = runTest {
        coEvery { historyRepository.getDetail("h1") } throws RuntimeException("Network")

        val viewModel = HistoryDetailViewModel(historyRepository)
        viewModel.load("h1")
        testDispatcher.scheduler.advanceUntilIdle()

        // The exception's own text never reaches the user.
        assertEquals(UiText(R.string.history_detail_load_failed), viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
