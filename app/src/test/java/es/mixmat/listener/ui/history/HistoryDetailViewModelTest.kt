package es.mixmat.listener.ui.history

import es.mixmat.listener.data.repository.GroupRepository
import es.mixmat.listener.data.repository.HistoryRepository
import es.mixmat.listener.domain.model.Group
import es.mixmat.listener.domain.model.HistoryDetail
import es.mixmat.listener.domain.model.Platforms
import es.mixmat.listener.domain.model.SharedGroup
import android.util.Log
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var historyRepository: HistoryRepository
    private lateinit var groupRepository: GroupRepository

    private val detail = HistoryDetail(
        id = "h1",
        title = "Song",
        artist = "Artist",
        thumbnail = null,
        shortcode = "abc",
        shareUrl = "https://mixmat.es/abc",
        platforms = Platforms(spotify = "https://spotify.com", tidal = null, appleMusic = null),
        createdAt = "2026-01-01T00:00:00Z",
        sharedTo = listOf(SharedGroup(groupId = "g1", groupName = "Group 1")),
    )

    private val groups = listOf(
        Group(id = "g1", name = "Group 1", description = null),
        Group(id = "g2", name = "Group 2", description = "A group"),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        historyRepository = mockk()
        groupRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load fetches detail and groups`() = runTest {
        coEvery { historyRepository.getDetail("h1") } returns detail
        coEvery { groupRepository.getGroups() } returns groups

        val viewModel = HistoryDetailViewModel(historyRepository, groupRepository)
        viewModel.load("h1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.detail)
        assertEquals("Song", state.detail!!.title)
        assertEquals(2, state.groups.size)
        assertTrue(state.selectedGroupIds.contains("g1"))
        assertFalse(state.isLoading)
    }

    @Test
    fun `load sets error on failure`() = runTest {
        coEvery { historyRepository.getDetail("h1") } throws RuntimeException("Network")
        coEvery { groupRepository.getGroups() } returns groups

        val viewModel = HistoryDetailViewModel(historyRepository, groupRepository)
        viewModel.load("h1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `toggleGroup adds and removes`() = runTest {
        coEvery { historyRepository.getDetail("h1") } returns detail
        coEvery { groupRepository.getGroups() } returns groups

        val viewModel = HistoryDetailViewModel(historyRepository, groupRepository)
        viewModel.load("h1")
        testDispatcher.scheduler.advanceUntilIdle()

        // g1 is already selected (shared_to), toggle it off
        viewModel.toggleGroup("g1")
        assertFalse(viewModel.uiState.value.selectedGroupIds.contains("g1"))

        // toggle g2 on
        viewModel.toggleGroup("g2")
        assertTrue(viewModel.uiState.value.selectedGroupIds.contains("g2"))
    }

    @Test
    fun `share sends selected groups`() = runTest {
        coEvery { historyRepository.getDetail("h1") } returns detail
        coEvery { groupRepository.getGroups() } returns groups
        coEvery { historyRepository.share("h1", listOf("g1")) } returns mapOf("g1" to "shared")

        val viewModel = HistoryDetailViewModel(historyRepository, groupRepository)
        viewModel.load("h1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.share()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSharing)
        assertNotNull(state.shareResult)
        assertEquals("shared", state.shareResult!!["g1"])
    }

    @Test
    fun `share sets error on failure`() = runTest {
        coEvery { historyRepository.getDetail("h1") } returns detail
        coEvery { groupRepository.getGroups() } returns groups
        coEvery { historyRepository.share("h1", listOf("g1")) } throws RuntimeException("Fail")

        val viewModel = HistoryDetailViewModel(historyRepository, groupRepository)
        viewModel.load("h1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.share()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Couldn't share — try again", viewModel.uiState.value.error)
    }
}
