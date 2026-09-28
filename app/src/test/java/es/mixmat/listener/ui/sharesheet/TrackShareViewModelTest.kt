package es.mixmat.listener.ui.sharesheet

import android.util.Log
import es.mixmat.listener.data.api.ApiException
import es.mixmat.listener.data.repository.GroupRepository
import es.mixmat.listener.data.repository.HistoryRepository
import es.mixmat.listener.domain.model.Group
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrackShareViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var groupRepository: GroupRepository
    private lateinit var historyRepository: HistoryRepository

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
        groupRepository = mockk()
        historyRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = TrackShareViewModel(groupRepository, historyRepository)

    private fun apiException(code: String, status: Int = 403) =
        ApiException(code = code, serverMessage = "server said so", httpStatus = status)

    // -- preselection --

    /**
     * The regression this sheet was partly built to fix.
     *
     * A group the track was shared into but which the endpoint no longer returns
     * must not stay selected. The departed id is the whole test: asserting that
     * the selection equals what was passed in is what the buggy code already
     * passed.
     */
    @Test
    fun `load ignores preselected groups that are no longer available`() = runTest {
        coEvery { groupRepository.getGroups() } returns groups

        val viewModel = viewModel()
        viewModel.load(preselecting = setOf("g1", "g_departed"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(setOf("g1"), viewModel.uiState.value.selectedGroupIds)
    }

    // -- the three states --

    @Test
    fun `load with no groups is loaded rather than failed`() = runTest {
        coEvery { groupRepository.getGroups() } returns emptyList()

        val viewModel = viewModel()
        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(GroupsState.Loaded(emptyList()), viewModel.uiState.value.groups)
    }

    @Test
    fun `load failure is failed rather than empty`() = runTest {
        coEvery { groupRepository.getGroups() } throws RuntimeException("Network")

        val viewModel = viewModel()
        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value.groups
        assertTrue(state is GroupsState.Failed)
        assertEquals("Couldn't load your groups", (state as GroupsState.Failed).message)
        assertTrue(state.retryable)
    }

    /** An admin kill-flag is permanent, so offering Try again against it would be a lie. */
    @Test
    fun `listen disabled is a failure with no retry offered`() = runTest {
        coEvery { groupRepository.getGroups() } throws apiException("auth_listen_disabled")

        val viewModel = viewModel()
        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value.groups as GroupsState.Failed
        assertEquals("Listening isn't enabled on this account", state.message)
        assertFalse(state.retryable)
    }

    @Test
    fun `retry after a failure reaches loaded and keeps the preselection`() = runTest {
        coEvery { groupRepository.getGroups() } throws RuntimeException("Network")

        val viewModel = viewModel()
        viewModel.load(preselecting = setOf("g2"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.groups is GroupsState.Failed)

        coEvery { groupRepository.getGroups() } returns groups
        viewModel.retry()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(GroupsState.Loaded(groups), state.groups)
        assertEquals(setOf("g2"), state.selectedGroupIds)
    }

    // -- sharing --

    @Test
    fun `share posts the selection and stores the result`() = runTest {
        coEvery { groupRepository.getGroups() } returns groups
        coEvery { historyRepository.share("h1", listOf("g1")) } returns mapOf("g1" to "shared")

        val viewModel = viewModel()
        viewModel.load(preselecting = setOf("g1"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.share("h1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSharing)
        assertEquals(mapOf("g1" to "shared"), state.shareResult)
        assertNull(state.shareError)
    }

    @Test
    fun `backToPicker drops the result so the picker shows again`() = runTest {
        coEvery { groupRepository.getGroups() } returns groups
        coEvery { historyRepository.share("h1", listOf("g1")) } returns mapOf("g1" to "shared")

        val viewModel = viewModel()
        viewModel.load(preselecting = setOf("g1"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.share("h1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.backToPicker()

        assertNull(viewModel.uiState.value.shareResult)
    }

    @Test
    fun `share with nothing selected does nothing`() = runTest {
        coEvery { groupRepository.getGroups() } returns groups

        val viewModel = viewModel()
        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.share("h1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSharing)
        assertNull(state.shareResult)
    }

    // -- share error copy, keyed on the code and never the status --

    /**
     * All three land on 403. Keying the copy on the status would tell someone who
     * has simply left a group that it has stopped accepting tracks.
     */
    @Test
    fun `each of the three 403 codes gets its own sentence`() = runTest {
        val expected = mapOf(
            "group_locked" to "This group is no longer accepting new tracks",
            "not_found" to "You're no longer in that group",
            "auth_listen_disabled" to "Listening isn't enabled on this account",
        )

        expected.forEach { (code, sentence) ->
            coEvery { groupRepository.getGroups() } returns groups
            coEvery { historyRepository.share("h1", listOf("g1")) } throws apiException(code)

            val viewModel = viewModel()
            viewModel.load(preselecting = setOf("g1"))
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.share("h1")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(sentence, viewModel.uiState.value.shareError)
        }
    }

    @Test
    fun `an unrecognised error code falls back to the generic failure`() = runTest {
        coEvery { groupRepository.getGroups() } returns groups
        coEvery { historyRepository.share("h1", listOf("g1")) } throws apiException("teapot", 418)

        val viewModel = viewModel()
        viewModel.load(preselecting = setOf("g1"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.share("h1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Two sentences, no dash: em and en dashes are out in this flow.
        assertEquals("Couldn't share. Try again.", viewModel.uiState.value.shareError)
    }

    @Test
    fun `a plain exception falls back to the generic failure`() = runTest {
        coEvery { groupRepository.getGroups() } returns groups
        coEvery { historyRepository.share("h1", listOf("g1")) } throws RuntimeException("socket")

        val viewModel = viewModel()
        viewModel.load(preselecting = setOf("g1"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.share("h1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Couldn't share. Try again.", viewModel.uiState.value.shareError)
    }

    @Test
    fun `changing the selection clears a stale share error`() = runTest {
        coEvery { groupRepository.getGroups() } returns groups
        coEvery { historyRepository.share("h1", listOf("g1")) } throws apiException("group_locked")

        val viewModel = viewModel()
        viewModel.load(preselecting = setOf("g1"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.share("h1")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            "This group is no longer accepting new tracks",
            viewModel.uiState.value.shareError,
        )

        viewModel.toggleGroup("g2")

        assertNull(viewModel.uiState.value.shareError)
    }

    // -- reopening --

    /**
     * The sheet leaves composition when it closes, so load runs again on every
     * open. That is what returns a reopened sheet to the picker rather than the
     * result it was last showing.
     */
    @Test
    fun `loading again clears a previous result`() = runTest {
        coEvery { groupRepository.getGroups() } returns groups
        coEvery { historyRepository.share("h1", listOf("g1")) } returns mapOf("g1" to "shared")

        val viewModel = viewModel()
        viewModel.load(preselecting = setOf("g1"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.share("h1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.load(preselecting = setOf("g1"))

        // Synchronously back to loading, before the fetch has even returned, so
        // there is no flash of the old result.
        assertEquals(GroupsState.Loading, viewModel.uiState.value.groups)
        assertNull(viewModel.uiState.value.shareResult)
    }
}
