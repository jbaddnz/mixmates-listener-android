package es.mixmat.listener.ui.sharesheet

import android.util.Log
import es.mixmat.listener.data.api.ApiException
import es.mixmat.listener.data.repository.AuthRepository
import es.mixmat.listener.data.repository.GroupRepository
import es.mixmat.listener.data.repository.HistoryRepository
import es.mixmat.listener.domain.model.Group
import es.mixmat.listener.domain.model.GroupList
import es.mixmat.listener.domain.model.UserProfile
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
class TrackShareViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var groupRepository: GroupRepository
    private lateinit var historyRepository: HistoryRepository

    private lateinit var authRepository: AuthRepository

    private val groups = listOf(
        Group(id = "g1", name = "Group 1", description = null),
        Group(id = "g2", name = "Group 2", description = "A group"),
    )
    private val listed = GroupList(groups, canCreate = false)

    private val newGroup = Group(
        id = "g_new",
        name = "Night Shift",
        description = null,
        inviteUrl = "https://example.test/invite/AbCd",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        groupRepository = mockk()
        historyRepository = mockk()
        authRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = TrackShareViewModel(groupRepository, historyRepository, authRepository)

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
        coEvery { groupRepository.getGroups() } returns listed

        val viewModel = viewModel()
        viewModel.load(preselecting = setOf("g1", "g_departed"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(setOf("g1"), viewModel.uiState.value.selectedGroupIds)
    }

    // -- the three states --

    @Test
    fun `load with no groups is loaded rather than failed`() = runTest {
        coEvery { groupRepository.getGroups() } returns GroupList(emptyList(), canCreate = false)

        val viewModel = viewModel()
        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(GroupsState.Loaded(emptyList(), canCreate = false), viewModel.uiState.value.groups)
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

        coEvery { groupRepository.getGroups() } returns listed
        viewModel.retry()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(GroupsState.Loaded(groups, canCreate = false), state.groups)
        assertEquals(setOf("g2"), state.selectedGroupIds)
    }

    // -- sharing --

    @Test
    fun `share posts the selection and stores the result`() = runTest {
        coEvery { groupRepository.getGroups() } returns listed
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
        coEvery { groupRepository.getGroups() } returns listed
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
        coEvery { groupRepository.getGroups() } returns listed

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
            coEvery { groupRepository.getGroups() } returns listed
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
        coEvery { groupRepository.getGroups() } returns listed
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
        coEvery { groupRepository.getGroups() } returns listed
        coEvery { historyRepository.share("h1", listOf("g1")) } throws RuntimeException("socket")

        val viewModel = viewModel()
        viewModel.load(preselecting = setOf("g1"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.share("h1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Couldn't share. Try again.", viewModel.uiState.value.shareError)
    }

    /** Matches iOS: a group the user has left drops out of the picker and the selection. */
    @Test
    fun `not_found re-reads the groups so the departed one drops out`() = runTest {
        coEvery { groupRepository.getGroups() } returns listed
        coEvery { historyRepository.share("h1", listOf("g1")) } throws apiException("not_found")

        val viewModel = viewModel()
        viewModel.load(preselecting = setOf("g1"))
        testDispatcher.scheduler.advanceUntilIdle()

        val remaining = listOf(groups[1])
        coEvery { groupRepository.getGroups() } returns GroupList(remaining, canCreate = false)
        viewModel.share("h1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(GroupsState.Loaded(remaining, canCreate = false), state.groups)
        assertTrue(state.selectedGroupIds.isEmpty())
        assertEquals("You're no longer in that group", state.shareError)
    }

    @Test
    fun `changing the selection clears a stale share error`() = runTest {
        coEvery { groupRepository.getGroups() } returns listed
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
        coEvery { groupRepository.getGroups() } returns listed
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

    // -- can_create: the server decides --

    @Test
    fun `can_create true is carried on loaded`() = runTest {
        coEvery { groupRepository.getGroups() } returns GroupList(groups, canCreate = true)

        val viewModel = viewModel()
        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(GroupsState.Loaded(groups, canCreate = true), viewModel.uiState.value.groups)
    }

    /** The list's contents play no part: an empty list does not mean "may create". */
    @Test
    fun `start does not open when can_create is false, even with no groups`() = runTest {
        coEvery { groupRepository.getGroups() } returns GroupList(emptyList(), canCreate = false)

        val viewModel = viewModel()
        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.openStartGroup()

        assertEquals(StartGroupState.Closed, viewModel.uiState.value.startGroup)
    }

    @Test
    fun `start does not open while loading or after a failed fetch`() = runTest {
        coEvery { groupRepository.getGroups() } throws RuntimeException("offline")

        val viewModel = viewModel()
        viewModel.load()
        viewModel.openStartGroup()
        assertEquals(StartGroupState.Closed, viewModel.uiState.value.startGroup)

        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.openStartGroup()
        assertEquals(StartGroupState.Closed, viewModel.uiState.value.startGroup)
    }

    // -- creating --

    private suspend fun startedViewModel(): TrackShareViewModel {
        coEvery { groupRepository.getGroups() } returns GroupList(groups, canCreate = true)
        val viewModel = viewModel()
        viewModel.load()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.openStartGroup()
        return viewModel
    }

    @Test
    fun `a create shows the new group, ticked in a refreshed picker`() = runTest {
        val viewModel = startedViewModel()
        coEvery { groupRepository.createGroup("Night Shift") } returns newGroup
        coEvery { groupRepository.getGroups() } returns GroupList(groups + newGroup, canCreate = false)

        viewModel.createGroup("  Night Shift  ")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(StartGroupState.Created(newGroup), state.startGroup)
        assertEquals(GroupsState.Loaded(groups + newGroup, canCreate = false), state.groups)
        assertTrue("g_new" in state.selectedGroupIds)

        viewModel.closeStartGroup()
        assertEquals(StartGroupState.Closed, viewModel.uiState.value.startGroup)
    }

    @Test
    fun `a blank name is never sent`() = runTest {
        val viewModel = startedViewModel()

        viewModel.createGroup("   ")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { groupRepository.createGroup(any()) }
        assertEquals(StartGroupState.Naming(), viewModel.uiState.value.startGroup)
    }

    @Test
    fun `name_taken keeps the user in the field with the exact copy`() = runTest {
        val viewModel = startedViewModel()
        coEvery { groupRepository.createGroup(any()) } throws apiException("name_taken", 409)

        viewModel.createGroup("Friends")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            StartGroupState.Naming(
                error = "That name's taken. Try adding something of your own to it.",
                draft = "Friends",
            ),
            viewModel.uiState.value.startGroup,
        )
    }

    @Test
    fun `already_has_group shows the exact copy and refreshes the list`() = runTest {
        val viewModel = startedViewModel()
        coEvery { groupRepository.createGroup(any()) } throws apiException("already_has_group")
        coEvery { groupRepository.getGroups() } returns GroupList(groups, canCreate = false)

        viewModel.createGroup("Friends")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(StartGroupState.Refused("This account already has a group."), state.startGroup)
        assertEquals(GroupsState.Loaded(groups, canCreate = false), state.groups)
    }

    // -- names --

    private val profile = UserProfile(
        id = "u1",
        displayName = "Jo",
        role = "listener",
        listenEnabled = true,
        preferredPlatform = null,
        rateLimit = null,
    )

    @Test
    fun `name_required on share asks for a name, saves it, and retries the share once`() = runTest {
        coEvery { groupRepository.getGroups() } returns listed
        coEvery { historyRepository.share("h1", listOf("g1")) } throws
            apiException("name_required") andThen mapOf("g1" to "shared")
        coEvery { authRepository.setDisplayName("Jo") } returns profile

        val viewModel = viewModel()
        viewModel.load(preselecting = setOf("g1"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.share("h1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(NamePrompt(), viewModel.uiState.value.namePrompt)
        assertNull(viewModel.uiState.value.shareError)

        viewModel.submitName(" Jo ")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.namePrompt)
        assertEquals(mapOf("g1" to "shared"), state.shareResult)
        coVerify(exactly = 1) { authRepository.setDisplayName("Jo") }
        coVerify(exactly = 2) { historyRepository.share("h1", listOf("g1")) }
    }

    @Test
    fun `name_required on create asks for a name, then creates`() = runTest {
        val viewModel = startedViewModel()
        coEvery { groupRepository.createGroup("Night Shift") } throws
            apiException("name_required") andThen newGroup
        coEvery { authRepository.setDisplayName("Jo") } returns profile

        viewModel.createGroup("Night Shift")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(NamePrompt(forCreate = true), viewModel.uiState.value.namePrompt)

        coEvery { groupRepository.getGroups() } returns GroupList(groups + newGroup, canCreate = false)
        viewModel.submitName("Jo")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(StartGroupState.Created(newGroup), viewModel.uiState.value.startGroup)
        coVerify(exactly = 2) { groupRepository.createGroup("Night Shift") }
    }

    /** Retried once only: a second name_required must not ask again in a loop. */
    @Test
    fun `a second name_required after the name is set does not ask again`() = runTest {
        coEvery { groupRepository.getGroups() } returns listed
        coEvery { historyRepository.share("h1", listOf("g1")) } throws apiException("name_required")
        coEvery { authRepository.setDisplayName("Jo") } returns profile

        val viewModel = viewModel()
        viewModel.load(preselecting = setOf("g1"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.share("h1")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.submitName("Jo")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.namePrompt)
        assertEquals("Couldn't share. Try again.", state.shareError)
        coVerify(exactly = 2) { historyRepository.share("h1", listOf("g1")) }
    }

    @Test
    fun `a private relay name shows the exact copy and retries nothing`() = runTest {
        coEvery { groupRepository.getGroups() } returns listed
        coEvery { historyRepository.share("h1", listOf("g1")) } throws apiException("name_required")
        coEvery { authRepository.setDisplayName(any()) } throws
            apiException("private_relay_name", 400)

        val viewModel = viewModel()
        viewModel.load(preselecting = setOf("g1"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.share("h1")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.submitName("abc@privaterelay.appleid.com")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            NamePrompt(error = "That's an Apple private address. Choose a name your friends will see."),
            viewModel.uiState.value.namePrompt,
        )
        coVerify(exactly = 1) { historyRepository.share("h1", listOf("g1")) }
    }

    @Test
    fun `cancelling the name prompt retries nothing`() = runTest {
        coEvery { groupRepository.getGroups() } returns listed
        coEvery { historyRepository.share("h1", listOf("g1")) } throws apiException("name_required")

        val viewModel = viewModel()
        viewModel.load(preselecting = setOf("g1"))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.share("h1")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.cancelName()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.namePrompt)
        coVerify(exactly = 0) { authRepository.setDisplayName(any()) }
        coVerify(exactly = 1) { historyRepository.share("h1", listOf("g1")) }
    }
}
